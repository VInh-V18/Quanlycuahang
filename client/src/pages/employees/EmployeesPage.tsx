import { Fragment, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useAppSelector } from "@/store/hooks";
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
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useToast } from "@/components/ui/use-toast";
import { DataTable, type DataTableColumn } from "@/components/common/DataTable";
import { DateRangePicker } from "@/components/common/DateRangePicker";
import { listAuditLogs, type AuditLogItem } from "@/lib/api/auditLogs";
import {
  createEmployee,
  deactivateEmployee,
  listEmployees,
  updateEmployee,
  type Employee,
} from "@/lib/api/employees";
import { listPermissions, listRoles, type Role } from "@/lib/api/roles";
import { useDebouncedValue } from "@/lib/hooks/useDebouncedValue";
import { getApiErrorMessage } from "@/lib/http/errors";
import { formatDateTime } from "@/lib/utils";

const RESOURCE_GROUPS: { title: string; resources: string[] }[] = [
  { title: "Hệ thống & nhân sự", resources: ["employee", "branch", "settings", "audit-log"] },
  { title: "Sản phẩm & danh mục", resources: ["product", "category"] },
  { title: "Bán hàng (POS)", resources: ["order", "return", "promotion"] },
  { title: "Kho", resources: ["inventory", "purchase-order", "stock-take"] },
  { title: "Đối tác", resources: ["customer", "supplier", "debt"] },
  { title: "Vận hành & hóa đơn", resources: ["shift", "cash-transaction", "invoice"] },
  { title: "Báo cáo", resources: ["report"] },
];

function EmployeeFormDialog({
  employee,
  roles,
  onClose,
}: {
  employee: Employee | null;
  roles: Role[];
  onClose: () => void;
}) {
  const isEdit = !!employee;
  const [username, setUsername] = useState(employee?.username ?? "");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState(employee?.fullName ?? "");
  const [phone, setPhone] = useState(employee?.phone ?? "");
  const [roleIds, setRoleIds] = useState<number[]>(employee?.roles.map((r) => r.id) ?? []);
  const [active, setActive] = useState(employee?.active ?? true);
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: () =>
      isEdit
        ? updateEmployee(employee.id, { fullName, phone, roleIds, active })
        : createEmployee({ username, password, fullName, phone, roleIds }),
    onSuccess: () => {
      toast({ title: isEdit ? "Đã cập nhật nhân viên" : "Đã thêm nhân viên" });
      queryClient.invalidateQueries({ queryKey: ["employees"] });
      onClose();
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể lưu", description: getApiErrorMessage(err) });
    },
  });

  function toggleRole(id: number) {
    setRoleIds((prev) => (prev.includes(id) ? prev.filter((r) => r !== id) : [...prev, id]));
  }

  const canSubmit =
    fullName.trim().length > 0 &&
    roleIds.length > 0 &&
    (isEdit || (username.trim().length > 0 && password.length >= 8));

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? "Sửa nhân viên" : "Thêm nhân viên"}</DialogTitle>
        </DialogHeader>
        <div className="space-y-3">
          <div>
            <Label htmlFor="emp-username">Tên đăng nhập</Label>
            <Input
              id="emp-username"
              value={username}
              disabled={isEdit}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>
          {!isEdit && (
            <div>
              <Label htmlFor="emp-password">Mật khẩu</Label>
              <Input
                id="emp-password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Ít nhất 8 ký tự"
              />
            </div>
          )}
          <div>
            <Label htmlFor="emp-fullname">Họ tên</Label>
            <Input
              id="emp-fullname"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="emp-phone">Số điện thoại</Label>
            <Input
              id="emp-phone"
              value={phone ?? ""}
              onChange={(e) => setPhone(e.target.value)}
            />
          </div>
          <div>
            <Label>Vai trò</Label>
            <div className="mt-1 grid grid-cols-2 gap-2 rounded-md border p-3">
              {roles.map((role) => (
                <label key={role.id} className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    className="h-4 w-4 accent-primary"
                    checked={roleIds.includes(role.id)}
                    onChange={() => toggleRole(role.id)}
                  />
                  {role.displayName}
                </label>
              ))}
            </div>
          </div>
          {isEdit && (
            <div className="flex items-center justify-between rounded-md border p-3">
              <Label htmlFor="emp-active" className="mb-0">
                Đang hoạt động
              </Label>
              <Switch id="emp-active" checked={active} onCheckedChange={setActive} />
            </div>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Hủy
          </Button>
          <Button disabled={!canSubmit || mutation.isPending} onClick={() => mutation.mutate()}>
            Lưu
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

const AUDIT_PAGE_SIZE = 20;

function formatJson(raw: string | null): string {
  if (!raw) return "—";
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

function AuditLogDetailDialog({ log, onClose }: { log: AuditLogItem; onClose: () => void }) {
  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>{log.action}</DialogTitle>
          <DialogDescription>
            {formatDateTime(log.createdAt)} · {log.userFullName ?? "Hệ thống"}
            {log.entityName && ` · ${log.entityName}${log.entityId ? ` #${log.entityId}` : ""}`}
          </DialogDescription>
        </DialogHeader>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <div className="mb-1 text-xs font-semibold uppercase text-muted-foreground">Trước</div>
            <pre className="max-h-72 overflow-auto rounded-md border bg-muted/30 p-2 text-xs">
              {formatJson(log.before)}
            </pre>
          </div>
          <div>
            <div className="mb-1 text-xs font-semibold uppercase text-muted-foreground">Sau</div>
            <pre className="max-h-72 overflow-auto rounded-md border bg-muted/30 p-2 text-xs">
              {formatJson(log.after)}
            </pre>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

/** Xem lai nhat ky audit (ghi qua AuditAspect voi method co @Audited, chi doc). */
function AuditLogPanel() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [detailTarget, setDetailTarget] = useState<AuditLogItem | null>(null);
  const [range, setRange] = useState<{ from: Date; to: Date }>(() => {
    const today = new Date();
    const from = new Date(today);
    from.setDate(from.getDate() - 30);
    return { from, to: today };
  });

  const debouncedSearch = useDebouncedValue(search);

  const params = useMemo(
    () => ({
      page,
      size: AUDIT_PAGE_SIZE,
      search: debouncedSearch,
      from: range.from.toISOString().slice(0, 10),
      to: range.to.toISOString().slice(0, 10),
    }),
    [page, debouncedSearch, range],
  );

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["audit-logs", params],
    queryFn: () => listAuditLogs(params),
  });

  const columns: DataTableColumn<AuditLogItem>[] = [
    {
      key: "createdAt",
      header: "Thời gian",
      render: (row) => formatDateTime(row.createdAt),
    },
    { key: "userFullName", header: "Người thực hiện", render: (row) => row.userFullName ?? "Hệ thống" },
    {
      key: "action",
      header: "Hành động",
      render: (row) => <span className="font-mono text-xs">{row.action}</span>,
    },
    {
      key: "entityName",
      header: "Đối tượng",
      render: (row) =>
        row.entityName ? `${row.entityName}${row.entityId ? ` #${row.entityId}` : ""}` : "—",
    },
    {
      key: "id",
      header: "",
      render: (row) => (
        <Button variant="ghost" size="sm" onClick={() => setDetailTarget(row)}>
          Xem chi tiết
        </Button>
      ),
    },
  ];

  return (
    <Card>
      <CardContent className="space-y-4 p-4">
        <div className="flex flex-wrap items-center gap-2">
          <Input
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            placeholder="Tìm theo hành động, đối tượng, người thực hiện..."
            className="w-72"
          />
          <DateRangePicker
            value={range}
            onChange={(r) => {
              if (r?.from && r?.to) {
                setRange({ from: r.from, to: r.to });
                setPage(0);
              }
            }}
          />
        </div>
        <DataTable
          columns={columns}
          data={data?.data ?? []}
          rowKey={(row) => row.id}
          meta={data?.meta}
          loading={isLoading}
          error={isError ? getApiErrorMessage(error) : null}
          onPageChange={setPage}
          emptyMessage="Chưa có nhật ký nào trong khoảng thời gian này"
        />
      </CardContent>
      {detailTarget && (
        <AuditLogDetailDialog log={detailTarget} onClose={() => setDetailTarget(null)} />
      )}
    </Card>
  );
}

export function EmployeesPage() {
  const [tab, setTab] = useState<"employees" | "permissions" | "audit">("employees");
  const [showForm, setShowForm] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState<Employee | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<Employee | null>(null);
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const permissionCodes = useAppSelector((state) => state.auth.permissions);
  const canViewAuditLog = permissionCodes.includes("audit-log:view");

  const employeesQuery = useQuery({ queryKey: ["employees"], queryFn: listEmployees });
  const rolesQuery = useQuery({
    queryKey: ["roles"],
    queryFn: listRoles,
    enabled: tab === "permissions" || showForm,
  });
  const permissionsQuery = useQuery({
    queryKey: ["permissions"],
    queryFn: listPermissions,
    enabled: tab === "permissions",
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: number) => deactivateEmployee(id),
    onSuccess: () => {
      toast({ title: "Đã khóa tài khoản nhân viên" });
      queryClient.invalidateQueries({ queryKey: ["employees"] });
      setDeactivateTarget(null);
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể khóa", description: getApiErrorMessage(err) });
    },
  });

  // Ma tran quyen la du lieu TOAN CUC dung chung moi cua hang (khong rieng cua tenant nay) - viec
  // SUA da chuyen han ve Super Admin (platform-admin) sau 1 lo hong bao mat, tab nay CHI XEM.
  // Toan bo state sua/dirty-tracking/mutation luu ma tran cu da bi go bo cung dot do: no la code
  // chet (nut Luu khong bao gio hien) va endpoint tenant PUT /roles/{id}/permissions cung khong
  // con ton tai - giu lai chi gay hieu lam (phat hien khi ra soat).
  const matrix = useMemo(() => {
    const next: Record<number, Set<string>> = {};
    (rolesQuery.data ?? []).forEach((role) => {
      next[role.id] = new Set(role.permissionCodes);
    });
    return next;
  }, [rolesQuery.data]);

  const groupedResources = new Set(RESOURCE_GROUPS.flatMap((g) => g.resources));
  const otherResources = Array.from(
    new Set(
      (permissionsQuery.data ?? [])
        .map((p) => p.code.split(":")[0])
        .filter((r) => !groupedResources.has(r)),
    ),
  );
  const groups =
    otherResources.length > 0
      ? [...RESOURCE_GROUPS, { title: "Khác", resources: otherResources }]
      : RESOURCE_GROUPS;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold">Nhân viên & phân quyền</h1>
        <div className="flex items-center gap-2">
          {tab === "employees" && (
            <Button
              size="lg"
              onClick={() => {
                setEditingEmployee(null);
                setShowForm(true);
              }}
            >
              <Plus className="h-4 w-4" />
              Thêm nhân viên
            </Button>
          )}
        </div>
      </div>

      <Tabs value={tab} onValueChange={(v) => setTab(v as "employees" | "permissions" | "audit")}>
        <TabsList>
          <TabsTrigger value="employees">Nhân viên</TabsTrigger>
          <TabsTrigger value="permissions">Ma trận phân quyền</TabsTrigger>
          {canViewAuditLog && <TabsTrigger value="audit">Nhật ký audit</TabsTrigger>}
        </TabsList>
      </Tabs>

      {tab === "employees" ? (
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
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {employeesQuery.data?.map((emp) => (
                  <TableRow key={emp.id}>
                    <TableCell className="font-medium">{emp.fullName}</TableCell>
                    <TableCell className="font-mono text-sm">{emp.username}</TableCell>
                    <TableCell>{emp.phone ?? "—"}</TableCell>
                    <TableCell>
                      <div className="flex flex-wrap gap-1">
                        {emp.roles.map((r) => (
                          <Badge key={r.id} variant="secondary">
                            {r.displayName}
                          </Badge>
                        ))}
                      </div>
                    </TableCell>
                    <TableCell>
                      {emp.active ? (
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
                          setEditingEmployee(emp);
                          setShowForm(true);
                        }}
                      >
                        Sửa
                      </Button>
                      {emp.active && (
                        <Button
                          variant="link"
                          size="sm"
                          className="text-destructive"
                          onClick={() => setDeactivateTarget(emp)}
                        >
                          Khóa
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
                {employeesQuery.data?.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} className="py-8 text-center text-muted-foreground">
                      {employeesQuery.isLoading ? "Đang tải..." : "Chưa có nhân viên"}
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      ) : tab === "permissions" ? (
        <Card>
          <p className="px-4 pt-4 text-sm text-muted-foreground">
            Ma trận quyền dùng chung cho mọi cửa hàng trên hệ thống, chỉ xem được ở đây — liên hệ
            quản trị hệ thống nếu cần thay đổi quyền của 1 vai trò.
          </p>
          <CardContent className="max-h-[70vh] overflow-auto p-0">
            {/* table thuong (khong dung wrapper <Table>) vi <Table> boc san 1 div overflow-auto
             * rieng — long 2 lop overflow-auto lam sticky "dinh" nham vao div con (khong bao gio
             * thuc su cuon vi cao tu do) thay vi CardContent (noi nguoi dung thuc su cuon), khien
             * dong tieu de khong dinh duoc nhu mong doi. */}
            <table className="w-full caption-bottom text-sm">
              <TableHeader>
                <TableRow>
                  <TableHead className="sticky left-0 top-0 z-30 min-w-[280px] bg-background">
                    Quyền
                  </TableHead>
                  {rolesQuery.data?.map((role) => (
                    <TableHead key={role.id} className="sticky top-0 z-20 bg-background text-center">
                      {role.displayName}
                    </TableHead>
                  ))}
                </TableRow>
              </TableHeader>
              <TableBody>
                {groups.map((group) => {
                  const rows = (permissionsQuery.data ?? []).filter((p) =>
                    group.resources.includes(p.code.split(":")[0]),
                  );
                  if (rows.length === 0) return null;
                  return (
                    <Fragment key={group.title}>
                      <TableRow className="bg-muted/50 hover:bg-muted/50">
                        <TableCell
                          colSpan={(rolesQuery.data?.length ?? 0) + 1}
                          className="py-2 text-xs font-semibold uppercase text-muted-foreground"
                        >
                          {group.title}
                        </TableCell>
                      </TableRow>
                      {rows.map((perm) => (
                        <TableRow key={perm.code}>
                          <TableCell className="sticky left-0 z-10 bg-background">
                            {perm.description}
                          </TableCell>
                          {rolesQuery.data?.map((role) => (
                            <TableCell key={role.id} className="text-center">
                              <input
                                type="checkbox"
                                className="h-4 w-4 accent-primary disabled:opacity-40"
                                checked={matrix[role.id]?.has(perm.code) ?? false}
                                disabled
                              />
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </Fragment>
                  );
                })}
                {permissionsQuery.data?.length === undefined && (
                  <TableRow>
                    <TableCell
                      colSpan={(rolesQuery.data?.length ?? 0) + 1}
                      className="py-8 text-center text-muted-foreground"
                    >
                      Đang tải...
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </table>
          </CardContent>
        </Card>
      ) : (
        <AuditLogPanel />
      )}

      {showForm && (
        <EmployeeFormDialog
          employee={editingEmployee}
          roles={rolesQuery.data ?? []}
          onClose={() => setShowForm(false)}
        />
      )}

      <Dialog open={!!deactivateTarget} onOpenChange={(open) => !open && setDeactivateTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Khóa tài khoản nhân viên</DialogTitle>
            <DialogDescription>
              Bạn có chắc muốn khóa tài khoản "{deactivateTarget?.fullName}"? Nhân viên này sẽ không
              thể đăng nhập cho đến khi được mở lại.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeactivateTarget(null)}>
              Hủy
            </Button>
            <Button
              variant="destructive"
              disabled={deactivateMutation.isPending}
              onClick={() => deactivateTarget && deactivateMutation.mutate(deactivateTarget.id)}
            >
              Khóa
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
