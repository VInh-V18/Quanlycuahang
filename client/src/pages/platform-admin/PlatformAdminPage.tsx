import { useEffect, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
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
import { useToast } from "@/components/ui/use-toast";
import { FormField } from "@/components/common/FormField";
import { getApiErrorMessage } from "@/lib/http/errors";
import { Switch } from "@/components/ui/switch";
import {
  createTenant,
  createTenantUser,
  deactivateTenantUser,
  getPlatformAdminAccessToken,
  listPlatformAdminRoles,
  listTenants,
  listTenantUsers,
  platformAdminChangePassword,
  platformAdminLogin,
  platformAdminLogout,
  resetTenantUserPassword,
  setPlatformAdminAccessToken,
  setTenantActive,
  updateTenantUser,
  type PlatformAdminRole,
  type TenantDto,
  type TenantUser,
} from "@/lib/api/platformAdmin";

const loginSchema = z.object({
  username: z.string().min(1, "Vui lòng nhập tên đăng nhập"),
  password: z.string().min(1, "Vui lòng nhập mật khẩu"),
});
type LoginFormValues = z.infer<typeof loginSchema>;

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
 * Trang quan tri Super Admin (KHONG danh cho chu cua hang) - tao/xem/khoa Tenant. Tach biet hoan
 * toan khoi ung dung chinh: khong Redux, khong RequireAuth/MainLayout cua tenant User, tu quan ly
 * trang thai dang nhap cuc bo trong 1 component duy nhat vi day chi la 1 cong cu noi bo don gian.
 */
export function PlatformAdminPage() {
  const [authed, setAuthed] = useState(() => !!getPlatformAdminAccessToken());

  if (!authed) {
    return <PlatformAdminLoginView onLoggedIn={() => setAuthed(true)} />;
  }
  return <PlatformAdminDashboardView onLoggedOut={() => setAuthed(false)} />;
}

function PlatformAdminLoginView({ onLoggedIn }: { onLoggedIn: () => void }) {
  const [submitting, setSubmitting] = useState(false);
  const { toast } = useToast();
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: "", password: "" },
  });

  async function onSubmit(values: LoginFormValues) {
    setSubmitting(true);
    try {
      await platformAdminLogin(values);
      onLoggedIn();
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
    <div
      className="flex min-h-svh items-center justify-center p-4"
      style={{ background: "linear-gradient(135deg, hsl(222 47% 11%), hsl(222 47% 22%))" }}
    >
      <div className="w-full max-w-md">
        <Card className="shadow-card">
          <CardHeader>
            <CardTitle>Quản trị hệ thống</CardTitle>
            <p className="text-sm text-muted-foreground">
              Dành riêng cho Super Admin - quản lý các cửa hàng trên hệ thống.
            </p>
          </CardHeader>
          <CardContent>
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="username"
                  label="Tên đăng nhập"
                  placeholder="superadmin"
                  required
                />
                <FormField
                  control={form.control}
                  name="password"
                  label="Mật khẩu"
                  type="password"
                  placeholder="••••••••"
                  required
                />
                <Button type="submit" className="w-full" size="lg" disabled={submitting}>
                  {submitting ? "Đang đăng nhập..." : "Đăng nhập"}
                </Button>
              </form>
            </Form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
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

  const toggleActiveMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => setTenantActive(id, active),
    onSuccess: (tenant) => {
      toast({
        title: tenant.active ? "Đã mở khoá cửa hàng" : "Đã khoá cửa hàng",
        description: tenant.name,
      });
      queryClient.invalidateQueries({ queryKey: ["platform-admin-tenants"] });
    },
    onError: (error) => {
      toast({
        variant: "destructive",
        title: "Không cập nhật được trạng thái",
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
            <p className="text-sm text-muted-foreground">Danh sách cửa hàng đang sử dụng hệ thống</p>
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
                        {tenant.active ? "Đang hoạt động" : "Đã khoá"}
                      </Badge>
                    </TableCell>
                    <TableCell>{new Date(tenant.createdAt).toLocaleDateString("vi-VN")}</TableCell>
                    <TableCell className="text-right space-x-2">
                      <Button variant="outline" size="sm" onClick={() => setManagingTenant(tenant)}>
                        Tài khoản
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={toggleActiveMutation.isPending}
                        onClick={() =>
                          toggleActiveMutation.mutate({ id: tenant.id, active: !tenant.active })
                        }
                      >
                        {tenant.active ? "Khoá" : "Mở khoá"}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

const tenantUserCreateSchema = z.object({
  username: z.string().min(1, "Vui lòng nhập tên đăng nhập"),
  password: z.string().min(8, "Tối thiểu 8 ký tự"),
  fullName: z.string().min(1, "Vui lòng nhập họ tên"),
  phone: z.string().optional(),
});
type TenantUserCreateFormValues = z.infer<typeof tenantUserCreateSchema>;

const tenantUserEditSchema = z.object({
  fullName: z.string().min(1, "Vui lòng nhập họ tên"),
  phone: z.string().optional(),
});
type TenantUserEditFormValues = z.infer<typeof tenantUserEditSchema>;

const resetPasswordSchema = z.object({
  newPassword: z.string().min(8, "Tối thiểu 8 ký tự"),
});
type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;

/** Super Admin quan ly tai khoan (User) cua 1 tenant cu the - "drill-down" thay the noi dung chinh
 * cua trang (khong dung Dialog long nhau) - Form tao/sua tai khoan van la Dialog rieng vi view nay
 * ban than KHONG phai Dialog. */
function TenantUsersView({ tenant, onBack }: { tenant: TenantDto; onBack: () => void }) {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editingUser, setEditingUser] = useState<TenantUser | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<TenantUser | null>(null);
  const [resetPasswordTarget, setResetPasswordTarget] = useState<TenantUser | null>(null);

  const usersQueryKey = ["platform-admin-tenant-users", tenant.id];
  const usersQuery = useQuery({
    queryKey: usersQueryKey,
    queryFn: () => listTenantUsers(tenant.id),
  });
  const rolesQuery = useQuery({
    queryKey: ["platform-admin-roles"],
    queryFn: listPlatformAdminRoles,
  });

  const deactivateMutation = useMutation({
    mutationFn: (userId: number) => deactivateTenantUser(tenant.id, userId),
    onSuccess: () => {
      toast({ title: "Đã vô hiệu hoá tài khoản" });
      queryClient.invalidateQueries({ queryKey: usersQueryKey });
      setDeactivateTarget(null);
    },
    onError: (error) => {
      toast({
        variant: "destructive",
        title: "Không vô hiệu hoá được",
        description: getApiErrorMessage(error),
      });
    },
  });

  const resetPasswordForm = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: "" },
  });
  const resetPasswordMutation = useMutation({
    mutationFn: (values: ResetPasswordFormValues) =>
      resetTenantUserPassword(tenant.id, resetPasswordTarget!.id, values.newPassword),
    onSuccess: () => {
      toast({ title: "Đã đặt lại mật khẩu" });
      resetPasswordForm.reset();
      setResetPasswordTarget(null);
    },
    onError: (error) => {
      toast({
        variant: "destructive",
        title: "Không đặt lại được mật khẩu",
        description: getApiErrorMessage(error),
      });
    },
  });

  return (
    <div className="min-h-svh bg-muted/30 p-6">
      <div className="mx-auto max-w-5xl space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <Button variant="link" className="h-auto p-0 text-sm" onClick={onBack}>
              ← Quay lại danh sách cửa hàng
            </Button>
            <h1 className="text-2xl font-bold">Tài khoản — {tenant.name}</h1>
          </div>
          <Button
            onClick={() => {
              setEditingUser(null);
              setShowForm(true);
            }}
          >
            Thêm tài khoản
          </Button>
        </div>

        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Họ tên</TableHead>
                  <TableHead>Tên đăng nhập</TableHead>
                  <TableHead>Số điện thoại</TableHead>
                  <TableHead>Vai trò</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="text-right">Hành động</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {usersQuery.isLoading && (
                  <TableRow>
                    <TableCell colSpan={6} className="py-6 text-center text-muted-foreground">
                      Đang tải...
                    </TableCell>
                  </TableRow>
                )}
                {!usersQuery.isLoading && (usersQuery.data ?? []).length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} className="py-6 text-center text-muted-foreground">
                      Chưa có tài khoản nào
                    </TableCell>
                  </TableRow>
                )}
                {usersQuery.data?.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell className="font-medium">{user.fullName}</TableCell>
                    <TableCell className="font-mono text-sm">{user.username}</TableCell>
                    <TableCell>{user.phone ?? "—"}</TableCell>
                    <TableCell>
                      <div className="flex flex-wrap gap-1">
                        {user.roles.map((r) => (
                          <Badge key={r.id} variant="secondary">
                            {r.displayName}
                          </Badge>
                        ))}
                      </div>
                    </TableCell>
                    <TableCell>
                      {user.active ? (
                        <Badge variant="success">Đang hoạt động</Badge>
                      ) : (
                        <Badge variant="destructive">Đã khoá</Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="link"
                        size="sm"
                        onClick={() => {
                          setEditingUser(user);
                          setShowForm(true);
                        }}
                      >
                        Sửa
                      </Button>
                      <Button
                        variant="link"
                        size="sm"
                        onClick={() => {
                          resetPasswordForm.reset();
                          setResetPasswordTarget(user);
                        }}
                      >
                        Đặt lại mật khẩu
                      </Button>
                      {user.active && (
                        <Button
                          variant="link"
                          size="sm"
                          className="text-destructive"
                          onClick={() => setDeactivateTarget(user)}
                        >
                          Vô hiệu hoá
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>

      {showForm && (
        <TenantUserFormDialog
          tenantId={tenant.id}
          user={editingUser}
          roles={rolesQuery.data ?? []}
          onClose={() => setShowForm(false)}
        />
      )}

      <Dialog
        open={!!deactivateTarget}
        onOpenChange={(open) => !open && setDeactivateTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Vô hiệu hoá tài khoản</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Bạn có chắc muốn vô hiệu hoá tài khoản "{deactivateTarget?.fullName}"? Tài khoản này sẽ
            không thể đăng nhập cho đến khi được mở lại.
          </p>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeactivateTarget(null)}>
              Huỷ
            </Button>
            <Button
              variant="destructive"
              disabled={deactivateMutation.isPending}
              onClick={() => deactivateTarget && deactivateMutation.mutate(deactivateTarget.id)}
            >
              Vô hiệu hoá
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!resetPasswordTarget}
        onOpenChange={(open) => !open && setResetPasswordTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Đặt lại mật khẩu — {resetPasswordTarget?.fullName}</DialogTitle>
          </DialogHeader>
          <Form {...resetPasswordForm}>
            <form
              onSubmit={resetPasswordForm.handleSubmit((values) =>
                resetPasswordMutation.mutate(values),
              )}
              className="space-y-4"
            >
              <FormField
                control={resetPasswordForm.control}
                name="newPassword"
                label="Mật khẩu mới"
                type="password"
                description="Tối thiểu 8 ký tự - gửi cho chủ cửa hàng/nhân viên để tự đổi lại sau"
                required
              />
              <DialogFooter>
                <Button type="submit" disabled={resetPasswordMutation.isPending}>
                  {resetPasswordMutation.isPending ? "Đang lưu..." : "Đặt lại mật khẩu"}
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

/** Vai tro (checkbox) + trang thai hoat dong (Switch, chi khi sua) - tach rieng vi day la state
 * thuong (roleIds/active), khong phai truong react-hook-form nen dung chung duoc giua form tao va
 * form sua ma khong vuong loi kieu du lieu (khac 2 form Create/Edit dung 2 kieu Field khac nhau). */
function RoleAndActiveFields({
  roles,
  roleIds,
  onToggleRole,
  active,
  onActiveChange,
  showActive,
}: {
  roles: PlatformAdminRole[];
  roleIds: number[];
  onToggleRole: (id: number) => void;
  active: boolean;
  onActiveChange: (value: boolean) => void;
  showActive: boolean;
}) {
  return (
    <>
      <div>
        <label className="text-sm font-medium">Vai trò</label>
        <div className="mt-1 grid grid-cols-2 gap-2 rounded-md border p-3">
          {roles.map((role) => (
            <label key={role.id} className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                className="h-4 w-4 accent-primary"
                checked={roleIds.includes(role.id)}
                onChange={() => onToggleRole(role.id)}
              />
              {role.displayName}
            </label>
          ))}
        </div>
      </div>
      {showActive && (
        <div className="flex items-center justify-between rounded-md border p-3">
          <label className="text-sm font-medium">Đang hoạt động</label>
          <Switch checked={active} onCheckedChange={onActiveChange} />
        </div>
      )}
    </>
  );
}

function TenantUserFormDialog({
  tenantId,
  user,
  roles,
  onClose,
}: {
  tenantId: number;
  user: TenantUser | null;
  roles: PlatformAdminRole[];
  onClose: () => void;
}) {
  const isEdit = !!user;
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [roleIds, setRoleIds] = useState<number[]>(user?.roles.map((r) => r.id) ?? []);
  const [active, setActive] = useState(user?.active ?? true);

  const createForm = useForm<TenantUserCreateFormValues>({
    resolver: zodResolver(tenantUserCreateSchema),
    defaultValues: {
      username: "",
      password: "",
      fullName: "",
      phone: "",
    },
  });
  const editForm = useForm<TenantUserEditFormValues>({
    resolver: zodResolver(tenantUserEditSchema),
    defaultValues: { fullName: user?.fullName ?? "", phone: user?.phone ?? "" },
  });

  function toggleRole(id: number) {
    setRoleIds((prev) => (prev.includes(id) ? prev.filter((r) => r !== id) : [...prev, id]));
  }

  const usersQueryKey = ["platform-admin-tenant-users", tenantId];

  const mutation = useMutation({
    mutationFn: () =>
      isEdit
        ? updateTenantUser(tenantId, user.id, {
            fullName: editForm.getValues("fullName"),
            phone: editForm.getValues("phone"),
            roleIds,
            active,
          })
        : createTenantUser(tenantId, {
            username: createForm.getValues("username"),
            password: createForm.getValues("password"),
            fullName: createForm.getValues("fullName"),
            phone: createForm.getValues("phone"),
            roleIds,
          }),
    onSuccess: () => {
      toast({ title: isEdit ? "Đã cập nhật tài khoản" : "Đã thêm tài khoản" });
      queryClient.invalidateQueries({ queryKey: usersQueryKey });
      onClose();
    },
    onError: (error) => {
      toast({
        variant: "destructive",
        title: "Không thể lưu",
        description: getApiErrorMessage(error),
      });
    },
  });

  const canSubmit = roleIds.length > 0;

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? "Sửa tài khoản" : "Thêm tài khoản"}</DialogTitle>
        </DialogHeader>
        {isEdit ? (
          <Form {...editForm}>
            <div className="space-y-3">
              <FormField control={editForm.control} name="fullName" label="Họ tên" required />
              <FormField control={editForm.control} name="phone" label="Số điện thoại" />
              <RoleAndActiveFields
                roles={roles}
                roleIds={roleIds}
                onToggleRole={toggleRole}
                active={active}
                onActiveChange={setActive}
                showActive
              />
            </div>
          </Form>
        ) : (
          <Form {...createForm}>
            <div className="space-y-3">
              <FormField
                control={createForm.control}
                name="username"
                label="Tên đăng nhập"
                placeholder="cashier_namlong"
                required
              />
              <FormField
                control={createForm.control}
                name="password"
                label="Mật khẩu"
                type="password"
                description="Tối thiểu 8 ký tự"
                required
              />
              <FormField control={createForm.control} name="fullName" label="Họ tên" required />
              <FormField control={createForm.control} name="phone" label="Số điện thoại" />
              <RoleAndActiveFields
                roles={roles}
                roleIds={roleIds}
                onToggleRole={toggleRole}
                active={active}
                onActiveChange={setActive}
                showActive={false}
              />
            </div>
          </Form>
        )}
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Huỷ
          </Button>
          <Button disabled={!canSubmit || mutation.isPending} onClick={() => mutation.mutate()}>
            Lưu
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
