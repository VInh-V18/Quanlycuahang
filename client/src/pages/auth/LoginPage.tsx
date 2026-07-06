import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { useLocation, useNavigate } from "react-router-dom";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Form } from "@/components/ui/form";
import { Switch } from "@/components/ui/switch";
import { FormField } from "@/components/common/FormField";
import { useToast } from "@/components/ui/use-toast";
import { login } from "@/lib/api/auth";
import { getApiErrorMessage } from "@/lib/http/errors";
import { useAppDispatch } from "@/store/hooks";
import { setAccessToken } from "@/store/slices/authSlice";

const loginSchema = z.object({
  username: z.string().min(1, "Vui lòng nhập tên đăng nhập"),
  password: z.string().min(8, "Tối thiểu 8 ký tự"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export function LoginPage() {
  const [submitting, setSubmitting] = useState(false);
  const [rememberDevice, setRememberDevice] = useState(true);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const { toast } = useToast();

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
            F
          </div>
          <div>
            <div className="text-lg font-bold leading-tight">FruitHouse ERP</div>
            <div className="text-sm leading-tight text-muted-foreground">
              Quản lý kho &amp; bán trái cây nhập khẩu
            </div>
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
            <div className="flex items-center gap-2">
              <Switch checked={rememberDevice} onCheckedChange={setRememberDevice} id="remember-device" />
              <label htmlFor="remember-device" className="text-sm font-medium">
                Ghi nhớ thiết bị này
              </label>
            </div>
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
