import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useQuery } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { useForm } from "react-hook-form";
import { useLocation, useNavigate } from "react-router-dom";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Form } from "@/components/ui/form";
import { FormField } from "@/components/common/FormField";
import { useToast } from "@/components/ui/use-toast";
import { login } from "@/lib/api/auth";
import { platformAdminLogin } from "@/lib/api/platformAdmin";
import { getBranding } from "@/lib/api/settings";
import { getApiErrorMessage } from "@/lib/http/errors";
import { useAppDispatch } from "@/store/hooks";
import { setAccessToken } from "@/store/slices/authSlice";

const DEFAULT_STORE_NAME = "Cửa hàng";
const DEFAULT_SLOGAN = "Hệ thống quản lý bán hàng";

const loginSchema = z.object({
  username: z.string().min(1, "Vui lòng nhập tên đăng nhập"),
  password: z.string().min(8, "Tối thiểu 8 ký tự"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export function LoginPage() {
  const [submitting, setSubmitting] = useState(false);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const { toast } = useToast();
  const { data: branding } = useQuery({ queryKey: ["branding"], queryFn: getBranding });
  const storeName = branding?.storeName?.trim() || DEFAULT_STORE_NAME;
  const slogan = branding?.storeSlogan?.trim() || DEFAULT_SLOGAN;

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: "", password: "" },
  });

  async function onSubmit(values: LoginFormValues) {
    setSubmitting(true);
    try {
      const accessToken = await login(values);
      dispatch(setAccessToken(accessToken));
      const from = (location.state as { from?: Location })?.from?.pathname ?? "/";
      navigate(from, { replace: true });
    } catch (error) {
      // Trang đăng nhập DÙNG CHUNG cho cả Super Admin: nếu không phải tài khoản cửa hàng (401)
      // hoặc IP đang bị giới hạn ở luồng tenant (429), thử tiếp luồng đăng nhập Super Admin —
      // 2 luồng vẫn tách biệt hoàn toàn ở Backend (JWT/cookie/filter chain riêng), chỉ gộp ở UI.
      // Đánh đổi chấp nhận được: mỗi lần Super Admin đăng nhập tốn 1 lượt thử sai của rate-limit
      // tenant theo IP (mặc định 5 lượt/15 phút) vì username superadmin không tồn tại trong users.
      const status = isAxiosError(error) ? error.response?.status : undefined;
      if (status === 401 || status === 429) {
        try {
          await platformAdminLogin(values);
          navigate("/platform-admin", { replace: true });
          return;
        } catch {
          // Rơi xuống thông báo lỗi của luồng tenant bên dưới — thông điệp "sai tên đăng nhập
          // hoặc mật khẩu" đúng cho cả 2 trường hợp, không tiết lộ tài khoản nào tồn tại.
        }
      }
      toast({
        variant: "destructive",
        title: "Đăng nhập thất bại",
        description: getApiErrorMessage(error),
      });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card className="shadow-card">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-primary text-lg font-bold text-primary-foreground">
            {storeName.slice(0, 1).toUpperCase()}
          </div>
          <div>
            <div className="text-lg font-bold leading-tight">{storeName}</div>
            <div className="text-sm leading-tight text-muted-foreground">{slogan}</div>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="username"
              label="Tên đăng nhập"
              placeholder="owner01"
              required
            />
            <FormField
              control={form.control}
              name="password"
              label="Mật khẩu"
              type="password"
              placeholder="••••••••"
              description="Tối thiểu 8 ký tự"
              required
            />
            {/* Công tắc "Ghi nhớ thiết bị này" đã gỡ bỏ: state chỉ tồn tại trên UI, không hề được
                gửi lên trong payload đăng nhập — một cài đặt trông như liên quan bảo mật nhưng
                không làm gì cả (phát hiện khi rà soát). Thêm lại khi Backend thật sự hỗ trợ điều
                chỉnh thời hạn refresh cookie theo lựa chọn này. */}
            <Button type="submit" className="w-full" size="lg" disabled={submitting}>
              {submitting ? "Đang đăng nhập..." : "Đăng nhập"}
            </Button>
            <p className="text-center text-sm text-muted-foreground">
              Quên mật khẩu? Liên hệ Chủ cửa hàng để đặt lại.
            </p>
          </form>
        </Form>
      </CardContent>
    </Card>
  );
}
