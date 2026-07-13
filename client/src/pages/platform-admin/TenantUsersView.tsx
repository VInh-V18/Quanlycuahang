import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
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
import { Switch } from "@/components/ui/switch";
import { useToast } from "@/components/ui/use-toast";
import { FormField } from "@/components/common/FormField";
import { getApiErrorMessage } from "@/lib/http/errors";
import {
  createTenantUser,
  deactivateTenantUser,
  deleteTenantUser,
  listPlatformAdminRoles,
  listTenantUsers,
  resetTenantUserPassword,
  updateTenantUser,
  type PlatformAdminRole,
  type TenantDto,
  type TenantUser,
} from "@/lib/api/platformAdmin";
import { TypeToConfirmDialog } from "./TypeToConfirmDialog";

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
export function TenantUsersView({ tenant, onBack }: { tenant: TenantDto; onBack: () => void }) {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editingUser, setEditingUser] = useState<TenantUser | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<TenantUser | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<TenantUser | null>(null);
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
      toast({ title: "Đã vô hiệu hóa tài khoản" });
      queryClient.invalidateQueries({ queryKey: usersQueryKey });
      setDeactivateTarget(null);
    },
    onError: (error) => {
      toast({
        variant: "destructive",
        title: "Không vô hiệu hóa được",
        description: getApiErrorMessage(error),
      });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (userId: number) => deleteTenantUser(tenant.id, userId),
    onSuccess: () => {
      toast({ title: "Đã xóa vĩnh viễn tài khoản" });
      queryClient.invalidateQueries({ queryKey: usersQueryKey });
      setDeleteTarget(null);
    },
    onError: (error) => {
      // Backend tu choi (TENANT_USER_HAS_ACTIVITY) neu tai khoan da phat sinh don hang/ca lam
      // viec... - getApiErrorMessage da doc dung thong bao ro rang tu Backend, khong can dich lai.
      toast({
        variant: "destructive",
        title: "Không xóa được tài khoản",
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
                        <Badge variant="destructive">Đã khóa</Badge>
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
                          Vô hiệu hóa
                        </Button>
                      )}
                      <Button
                        variant="link"
                        size="sm"
                        className="text-destructive"
                        onClick={() => setDeleteTarget(user)}
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
            <DialogTitle>Vô hiệu hóa tài khoản</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Bạn có chắc muốn vô hiệu hóa tài khoản "{deactivateTarget?.fullName}"? Tài khoản này sẽ
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
              Vô hiệu hóa
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <TypeToConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title="Xóa vĩnh viễn tài khoản"
        description={
          <>
            Tài khoản "{deleteTarget?.fullName}" ({deleteTarget?.username}) sẽ bị xóa VĨNH VIỄN,
            KHÔNG THỂ khôi phục. Chỉ xóa được nếu tài khoản chưa từng phát sinh hoạt động (đơn
            hàng, ca làm việc...) — nếu đã có hoạt động, hệ thống sẽ từ chối và bạn nên dùng "Vô
            hiệu hóa" thay thế.
          </>
        }
        confirmWord={deleteTarget?.username ?? ""}
        confirmLabel="Xóa vĩnh viễn"
        pending={deleteMutation.isPending}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
      />

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

  // mutationFn nhan du lieu DA QUA validate (tu editForm.handleSubmit/createForm.handleSubmit ben
  // duoi), khong tu doc .getValues() nua - truoc day nut "Luu" goi thang mutation.mutate() ngoai
  // handleSubmit (khong boc trong <form> that) nen zod resolver KHONG BAO GIO chay, cho phep luu
  // tai khoan voi username/mat khau/ho ten RONG (phat hien khi rieng soat).
  const editMutation = useMutation({
    mutationFn: (values: TenantUserEditFormValues) => {
      if (!user) {
        return Promise.reject(new Error("Thiếu tài khoản cần sửa"));
      }
      return updateTenantUser(tenantId, user.id, {
        fullName: values.fullName,
        phone: values.phone,
        roleIds,
        active,
      });
    },
    onSuccess: () => {
      toast({ title: "Đã cập nhật tài khoản" });
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

  const createMutation = useMutation({
    mutationFn: (values: TenantUserCreateFormValues) =>
      createTenantUser(tenantId, {
        username: values.username,
        password: values.password,
        fullName: values.fullName,
        phone: values.phone,
        roleIds,
      }),
    onSuccess: () => {
      toast({ title: "Đã thêm tài khoản" });
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
            <form onSubmit={editForm.handleSubmit((values) => editMutation.mutate(values))} className="space-y-3">
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
              <DialogFooter>
                <Button type="button" variant="outline" onClick={onClose}>
                  Huỷ
                </Button>
                <Button type="submit" disabled={!canSubmit || editMutation.isPending}>
                  Lưu
                </Button>
              </DialogFooter>
            </form>
          </Form>
        ) : (
          <Form {...createForm}>
            <form
              onSubmit={createForm.handleSubmit((values) => createMutation.mutate(values))}
              className="space-y-3"
            >
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
              <DialogFooter>
                <Button type="button" variant="outline" onClick={onClose}>
                  Huỷ
                </Button>
                <Button type="submit" disabled={!canSubmit || createMutation.isPending}>
                  Lưu
                </Button>
              </DialogFooter>
            </form>
          </Form>
        )}
      </DialogContent>
    </Dialog>
  );
}
