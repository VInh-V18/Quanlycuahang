import { useEffect, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { useForm } from "react-hook-form";
import { Navigate } from "react-router-dom";
import { z } from "zod";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Form } from "@/components/ui/form";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useToast } from "@/components/ui/use-toast";
import { FormField } from "@/components/common/FormField";
import { getApiErrorMessage } from "@/lib/http/errors";
import { formatDate } from "@/lib/utils";
import {
  createTenant,
  deleteTenant,
  getPlatformAdminAccessToken,
  listTenants,
  platformAdminChangePassword,
  platformAdminLogout,
  setPlatformAdminAccessToken,
  setTenantActive,
  type TenantDto,
} from "@/lib/api/platformAdmin";
import { PermissionMatrixView } from "./PermissionMatrixView";
import { TenantUsersView } from "./TenantUsersView";
import { TypeToConfirmDialog } from "./TypeToConfirmDialog";

const createTenantSchema = z.object({
  tenantName: z.string().min(1, "Vui lòng nhập tên cửa hàng"),
  branchName: z.string().optional(),
  ownerUsername: z.string().min(1, "Vui lòng nhập tên đăng nhập"),
  ownerPassword: z.string().min(8, "Tối thiểu 8 ký tự"),
  ownerFullName: z.string().min(1, "Vui lòng nhập họ tên"),
});
type CreateTenantFormValues = z.infer<typeof createTenantSchema>;

const changePasswordSchema = z.object({
  oldPassword: z.string().min(1, "Vui lòng nhập mật khẩu hiện tại"),
  newPassword: z.string().min(8, "Tối thiểu 8 ký tự"),
});
type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;

/**
 * Trang quan tri Super Admin (KHONG danh cho chu cua hang) - tao/xem/khoa Tenant, sua ma tran
 * phan quyen toan cuc. Tach biet hoan toan khoi ung dung chinh: khong Redux, khong RequireAuth/
 * MainLayout cua tenant User. DANG NHAP da gop vao trang /login chung (LoginPage tu nhan dien:
 * thu luong tenant truoc, that bai thi thu luong Super Admin) - trang nay khong con form dang
 * nhap rieng, chua xac thuc thi chuyen ve /login. He thong chi co DUY NHAT 1 tai khoan Super
 * Admin (rang buoc o DB, xem V20__single_platform_admin.sql) nen khong co UI quan ly admin.
 *
 * Cac view con (ma tran phan quyen, danh sach tai khoan tenant, dialog xac nhan go-lai-ten) da
 * duoc tach thanh file rieng trong cung thu muc (PermissionMatrixView/TenantUsersView/
 * TypeToConfirmDialog) — truoc day gop chung 1 file ~1200 dong, lon gap ~4 lan trang lon thu 2
 * (SettingsPage), kho review/de conflict khi them tinh nang Super Admin moi (phat hien khi rieng
 * soat).
 */
export function PlatformAdminPage() {
  const [authed, setAuthed] = useState(() => !!getPlatformAdminAccessToken());

  if (!authed) {
    return <Navigate to="/login" replace />;
  }
  return <PlatformAdminDashboardView onLoggedOut={() => setAuthed(false)} />;
}

function PlatformAdminDashboardView({ onLoggedOut }: { onLoggedOut: () => void }) {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [changePasswordOpen, setChangePasswordOpen] = useState(false);
  const [managingTenant, setManagingTenant] = useState<TenantDto | null>(null);

  const tenantsQuery = useQuery({
    queryKey: ["platform-admin-tenants"],
    queryFn: listTenants,
  });

  useEffect(() => {
    if (
      tenantsQuery.isError &&
      isAxiosError(tenantsQuery.error) &&
      tenantsQuery.error.response?.status === 401
    ) {
      setPlatformAdminAccessToken(null);
      onLoggedOut();
    }
  }, [tenantsQuery.isError, tenantsQuery.error, onLoggedOut]);

  const createForm = useForm<CreateTenantFormValues>({
    resolver: zodResolver(createTenantSchema),
    defaultValues: {
      tenantName: "",
      branchName: "",
      ownerUsername: "",
      ownerPassword: "",
      ownerFullName: "",
    },
  });

  const createMutation = useMutation({
    mutationFn: createTenant,
    onSuccess: (tenant) => {
      toast({ title: "Đã tạo cửa hàng", description: `"${tenant.name}" sẵn sàng để sử dụng` });
      queryClient.invalidateQueries({ queryKey: ["platform-admin-tenants"] });
      createForm.reset();
      setCreateOpen(false);
    },
    onError: (error) => {
      toast({
        variant: "destructive",
        title: "Không tạo được cửa hàng",
        description: getApiErrorMessage(error),
      });
    },
  });

  const changePasswordForm = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { oldPassword: "", newPassword: "" },
  });

  const changePasswordMutation = useMutation({
    mutationFn: ({ oldPassword, newPassword }: ChangePasswordFormValues) =>
      platformAdminChangePassword(oldPassword, newPassword),
    onSuccess: () => {
      toast({ title: "Đã đổi mật khẩu" });
      changePasswordForm.reset();
      setChangePasswordOpen(false);
    },
    onError: (error) => {
      toast({
        variant: "destructive",
        title: "Không đổi được mật khẩu",
        description: getApiErrorMessage(error),
      });
    },
  });

  const [lockTarget, setLockTarget] = useState<TenantDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<TenantDto | null>(null);
  const [tab, setTab] = useState<"tenants" | "permissions">("tenants");

  const toggleActiveMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => setTenantActive(id, active),
    onSuccess: (tenant) => {
      toast({
        title: tenant.active ? "Đã mở khóa cửa hàng" : "Đã khóa cửa hàng",
        description: tenant.name,
      });
      queryClient.invalidateQueries({ queryKey: ["platform-admin-tenants"] });
      setLockTarget(null);
    },
    onError: (error) => {
      toast({
        variant: "destructive",
        title: "Không cập nhật được trạng thái",
        description: getApiErrorMessage(error),
      });
    },
  });

  const deleteTenantMutation = useMutation({
    mutationFn: (id: number) => deleteTenant(id),
    onSuccess: (_, id) => {
      const name = tenants.find((t) => t.id === id)?.name;
      toast({ title: "Đã xóa vĩnh viễn cửa hàng", description: name });
      queryClient.invalidateQueries({ queryKey: ["platform-admin-tenants"] });
      setDeleteTarget(null);
    },
    onError: (error) => {
      toast({
        variant: "destructive",
        title: "Không xóa được cửa hàng",
        description: getApiErrorMessage(error),
      });
    },
  });

  async function handleLogout() {
    await platformAdminLogout();
    onLoggedOut();
  }

  const tenants = tenantsQuery.data ?? [];

  if (managingTenant) {
    return <TenantUsersView tenant={managingTenant} onBack={() => setManagingTenant(null)} />;
  }

  return (
    <div className="min-h-svh bg-muted/30 p-6">
      <div className="mx-auto max-w-5xl space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">Quản trị hệ thống</h1>
            <p className="text-sm text-muted-foreground">
              Tài khoản Super Admin duy nhất — quản lý cửa hàng và ma trận phân quyền toàn hệ thống
            </p>
          </div>
          <div className="flex gap-2">
            <Dialog open={createOpen} onOpenChange={setCreateOpen}>
              <DialogTrigger asChild>
                <Button>Tạo cửa hàng mới</Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Tạo cửa hàng mới</DialogTitle>
                </DialogHeader>
                <Form {...createForm}>
                  <form
                    onSubmit={createForm.handleSubmit((values) => createMutation.mutate(values))}
                    className="space-y-4"
                  >
                    <FormField
                      control={createForm.control}
                      name="tenantName"
                      label="Tên cửa hàng"
                      placeholder="Cửa hàng trái cây Nam Long"
                      required
                    />
                    <FormField
                      control={createForm.control}
                      name="branchName"
                      label="Tên chi nhánh đầu tiên"
                      placeholder="Chi nhánh 1 (để trống dùng mặc định)"
                    />
                    <FormField
                      control={createForm.control}
                      name="ownerFullName"
                      label="Họ tên chủ cửa hàng"
                      placeholder="Nguyễn Văn A"
                      required
                    />
                    <FormField
                      control={createForm.control}
                      name="ownerUsername"
                      label="Tên đăng nhập chủ cửa hàng"
                      placeholder="owner_namlong"
                      required
                    />
                    <FormField
                      control={createForm.control}
                      name="ownerPassword"
                      label="Mật khẩu ban đầu"
                      type="password"
                      description="Tối thiểu 8 ký tự - gửi cho chủ cửa hàng để đổi lại sau khi đăng nhập lần đầu"
                      required
                    />
                    <DialogFooter>
                      <Button type="submit" disabled={createMutation.isPending}>
                        {createMutation.isPending ? "Đang tạo..." : "Tạo cửa hàng"}
                      </Button>
                    </DialogFooter>
                  </form>
                </Form>
              </DialogContent>
            </Dialog>
            <Dialog open={changePasswordOpen} onOpenChange={setChangePasswordOpen}>
              <DialogTrigger asChild>
                <Button variant="outline">Đổi mật khẩu</Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Đổi mật khẩu Super Admin</DialogTitle>
                </DialogHeader>
                <Form {...changePasswordForm}>
                  <form
                    onSubmit={changePasswordForm.handleSubmit((values) =>
                      changePasswordMutation.mutate(values),
                    )}
                    className="space-y-4"
                  >
                    <FormField
                      control={changePasswordForm.control}
                      name="oldPassword"
                      label="Mật khẩu hiện tại"
                      type="password"
                      required
                    />
                    <FormField
                      control={changePasswordForm.control}
                      name="newPassword"
                      label="Mật khẩu mới"
                      type="password"
                      description="Tối thiểu 8 ký tự"
                      required
                    />
                    <DialogFooter>
                      <Button type="submit" disabled={changePasswordMutation.isPending}>
                        {changePasswordMutation.isPending ? "Đang lưu..." : "Đổi mật khẩu"}
                      </Button>
                    </DialogFooter>
                  </form>
                </Form>
              </DialogContent>
            </Dialog>
            <Button variant="outline" onClick={handleLogout}>
              Đăng xuất
            </Button>
          </div>
        </div>

        <Tabs value={tab} onValueChange={(v) => setTab(v as typeof tab)}>
          <TabsList>
            <TabsTrigger value="tenants">Cửa hàng</TabsTrigger>
            <TabsTrigger value="permissions">Ma trận phân quyền</TabsTrigger>
          </TabsList>
        </Tabs>

        {tab === "permissions" && <PermissionMatrixView />}

        {tab === "tenants" && (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Tên cửa hàng</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead>Ngày tạo</TableHead>
                  <TableHead className="text-right">Hành động</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {tenantsQuery.isLoading && (
                  <TableRow>
                    <TableCell colSpan={5} className="py-6 text-center text-muted-foreground">
                      Đang tải...
                    </TableCell>
                  </TableRow>
                )}
                {!tenantsQuery.isLoading && tenants.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} className="py-6 text-center text-muted-foreground">
                      Chưa có cửa hàng nào
                    </TableCell>
                  </TableRow>
                )}
                {tenants.map((tenant: TenantDto) => (
                  <TableRow key={tenant.id}>
                    <TableCell>{tenant.id}</TableCell>
                    <TableCell className="font-medium">{tenant.name}</TableCell>
                    <TableCell>
                      <Badge variant={tenant.active ? "success" : "destructive"}>
                        {tenant.active ? "Đang hoạt động" : "Đã khóa"}
                      </Badge>
                    </TableCell>
                    <TableCell>{formatDate(tenant.createdAt)}</TableCell>
                    <TableCell className="text-right space-x-2">
                      <Button variant="outline" size="sm" onClick={() => setManagingTenant(tenant)}>
                        Tài khoản
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={toggleActiveMutation.isPending}
                        onClick={() =>
                          tenant.active
                            ? setLockTarget(tenant)
                            : toggleActiveMutation.mutate({ id: tenant.id, active: true })
                        }
                      >
                        {tenant.active ? "Khóa" : "Mở khóa"}
                      </Button>
                      <Button
                        variant="link"
                        size="sm"
                        className="text-destructive"
                        onClick={() => setDeleteTarget(tenant)}
                      >
                        Xóa vĩnh viễn
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
        )}
      </div>

      <Dialog open={!!lockTarget} onOpenChange={(open) => !open && setLockTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Khóa cửa hàng</DialogTitle>
            <DialogDescription>
              Bạn có chắc muốn khóa cửa hàng "{lockTarget?.name}"? Toàn bộ tài khoản của cửa hàng
              này sẽ không thể đăng nhập cho đến khi được mở khóa lại.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setLockTarget(null)}>
              Huỷ
            </Button>
            <Button
              variant="destructive"
              disabled={toggleActiveMutation.isPending}
              onClick={() =>
                lockTarget && toggleActiveMutation.mutate({ id: lockTarget.id, active: false })
              }
            >
              Khóa
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <TypeToConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title="Xóa vĩnh viễn cửa hàng"
        description={
          <>
            Toàn bộ dữ liệu của "{deleteTarget?.name}" — sản phẩm, khách hàng, đơn hàng, hóa đơn,
            công nợ, tài khoản nhân viên... — sẽ bị xóa VĨNH VIỄN, KHÔNG THỂ khôi phục. Nếu chỉ
            muốn tạm ngừng, hãy dùng nút "Khóa" thay vì xóa.
          </>
        }
        confirmWord={deleteTarget?.name ?? ""}
        confirmLabel="Xóa vĩnh viễn"
        pending={deleteTenantMutation.isPending}
        onConfirm={() => deleteTarget && deleteTenantMutation.mutate(deleteTarget.id)}
      />
    </div>
  );
}
