import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient, type QueryKey } from "@tanstack/react-query";
import { useLocation } from "react-router-dom";
import { MoreHorizontal, Plus, Users2 } from "lucide-react";
import { useAppSelector } from "@/store/hooks";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { DataTable, type DataTableColumn } from "@/components/common/DataTable";
import { Money } from "@/components/common/Money";
import { PermissionGate } from "@/components/common/PermissionGate";
import { useToast } from "@/components/ui/use-toast";
import { CustomerGroupManagerDialog } from "@/components/partners/CustomerGroupManagerDialog";
import {
  createCustomer,
  listCustomersWithStats,
  updateCustomer,
  type CustomerListItem,
  type CustomerRequest,
} from "@/lib/api/customers";
import { listCustomerGroups, type CustomerGroup } from "@/lib/api/customerGroups";
import {
  createSupplier,
  listSuppliersWithStats,
  updateSupplier,
  type Supplier,
  type SupplierRequest,
} from "@/lib/api/suppliers";
import { useDebouncedValue } from "@/lib/hooks/useDebouncedValue";
import { getApiErrorMessage } from "@/lib/http/errors";
import { formatDate as formatDateShared } from "@/lib/utils";
import type { ApiSuccess } from "@/types/api";

const PAGE_SIZE = 20;

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return formatDateShared(iso);
}

/** Dùng chung cho cả "Thêm" và "Sửa" (tránh 2 bản form lệch nhau theo thời gian) — có `customer`
 * thì là sửa (PUT + optimistic update ngay trên trang danh sách đang xem), không thì là thêm mới. */
export function CustomerFormDialog({
  groups,
  customer,
  listQueryKey,
  onClose,
}: {
  groups: CustomerGroup[];
  customer?: CustomerListItem;
  listQueryKey: QueryKey;
  onClose: () => void;
}) {
  const isEdit = !!customer;
  const [form, setForm] = useState<CustomerRequest>(
    customer
      ? {
          name: customer.name,
          phone: customer.phone,
          address: customer.address,
          debtLimit: customer.debtLimit,
          customerGroupId: customer.customerGroupId,
        }
      : { name: "", phone: "", address: "", debtLimit: 0, customerGroupId: null },
  );
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: () => (isEdit ? updateCustomer(customer.id, form) : createCustomer(form)),
    onMutate: async () => {
      if (!isEdit) return undefined;
      await queryClient.cancelQueries({ queryKey: listQueryKey });
      const previous = queryClient.getQueryData<ApiSuccess<CustomerListItem[]>>(listQueryKey);
      queryClient.setQueryData<ApiSuccess<CustomerListItem[]> | undefined>(listQueryKey, (old) =>
        old
          ? {
              ...old,
              data: old.data.map((row) =>
                row.id === customer.id
                  ? {
                      ...row,
                      name: form.name,
                      phone: form.phone ?? null,
                      address: form.address ?? null,
                      debtLimit: form.debtLimit ?? 0,
                      customerGroupId: form.customerGroupId ?? null,
                      groupName:
                        groups.find((g) => g.id === form.customerGroupId)?.name ?? null,
                    }
                  : row,
              ),
            }
          : old,
      );
      return { previous };
    },
    onSuccess: () => {
      toast({ title: isEdit ? "Đã lưu thay đổi" : "Đã thêm khách hàng" });
      onClose();
    },
    onError: (err, _vars, context) => {
      if (isEdit && context?.previous) {
        queryClient.setQueryData(listQueryKey, context.previous);
      }
      toast({
        variant: "destructive",
        title: isEdit ? "Không thể lưu" : "Không thể thêm",
        description: getApiErrorMessage(err),
      });
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["customers"] });
    },
  });

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? "Sửa khách hàng" : "Thêm khách hàng"}</DialogTitle>
        </DialogHeader>
        <div className="space-y-3">
          <div>
            <Label htmlFor="customer-name">Tên khách hàng</Label>
            <Input
              id="customer-name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
            />
          </div>
          <div>
            <Label htmlFor="customer-phone">Số điện thoại</Label>
            <Input
              id="customer-phone"
              value={form.phone ?? ""}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
            />
          </div>
          <div>
            <Label htmlFor="customer-address">Địa chỉ</Label>
            <Input
              id="customer-address"
              value={form.address ?? ""}
              onChange={(e) => setForm({ ...form, address: e.target.value })}
            />
          </div>
          <div>
            <Label htmlFor="customer-debt-limit">Hạn mức nợ</Label>
            <Input
              id="customer-debt-limit"
              type="number"
              value={form.debtLimit ?? 0}
              onChange={(e) => setForm({ ...form, debtLimit: Number(e.target.value) })}
            />
          </div>
          <div>
            <Label htmlFor="customer-group">Nhóm khách hàng</Label>
            <Select
              value={form.customerGroupId ? String(form.customerGroupId) : "none"}
              onValueChange={(v) => setForm({ ...form, customerGroupId: v === "none" ? null : Number(v) })}
            >
              <SelectTrigger id="customer-group">
                <SelectValue placeholder="Không có nhóm" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="none">Không có nhóm</SelectItem>
                {groups.map((g) => (
                  <SelectItem key={g.id} value={String(g.id)}>
                    {g.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Hủy
          </Button>
          <Button disabled={!form.name.trim() || mutation.isPending} onClick={() => mutation.mutate()}>
            Lưu
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export function SupplierFormDialog({
  supplier,
  listQueryKey,
  onClose,
}: {
  supplier?: Supplier;
  listQueryKey: QueryKey;
  onClose: () => void;
}) {
  const isEdit = !!supplier;
  const [form, setForm] = useState<SupplierRequest>(
    supplier
      ? { name: supplier.name, phone: supplier.phone, address: supplier.address }
      : { name: "", phone: "" },
  );
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: () => (isEdit ? updateSupplier(supplier.id, form) : createSupplier(form)),
    onMutate: async () => {
      if (!isEdit) return undefined;
      await queryClient.cancelQueries({ queryKey: listQueryKey });
      const previous = queryClient.getQueryData<ApiSuccess<Supplier[]>>(listQueryKey);
      queryClient.setQueryData<ApiSuccess<Supplier[]> | undefined>(listQueryKey, (old) =>
        old
          ? {
              ...old,
              data: old.data.map((row) =>
                row.id === supplier.id
                  ? { ...row, name: form.name, phone: form.phone ?? null, address: form.address ?? null }
                  : row,
              ),
            }
          : old,
      );
      return { previous };
    },
    onSuccess: () => {
      toast({ title: isEdit ? "Đã lưu thay đổi" : "Đã thêm nhà cung cấp" });
      onClose();
    },
    onError: (err, _vars, context) => {
      if (isEdit && context?.previous) {
        queryClient.setQueryData(listQueryKey, context.previous);
      }
      toast({
        variant: "destructive",
        title: isEdit ? "Không thể lưu" : "Không thể thêm",
        description: getApiErrorMessage(err),
      });
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["suppliers"] });
    },
  });

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? "Sửa nhà cung cấp" : "Thêm nhà cung cấp"}</DialogTitle>
        </DialogHeader>
        <div className="space-y-3">
          <div>
            <Label htmlFor="supplier-name">Tên nhà cung cấp</Label>
            <Input
              id="supplier-name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
            />
          </div>
          <div>
            <Label htmlFor="supplier-phone">Số điện thoại</Label>
            <Input
              id="supplier-phone"
              value={form.phone ?? ""}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
            />
          </div>
          <div>
            <Label htmlFor="supplier-address">Địa chỉ</Label>
            <Input
              id="supplier-address"
              value={form.address ?? ""}
              onChange={(e) => setForm({ ...form, address: e.target.value })}
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Hủy
          </Button>
          <Button disabled={!form.name.trim() || mutation.isPending} onClick={() => mutation.mutate()}>
            Lưu
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export function PartnersPage() {
  const location = useLocation();
  const tab: "customers" | "suppliers" =
    location.pathname === "/suppliers" ? "suppliers" : "customers";
  const [search, setSearch] = useState("");
  const [hasDebt, setHasDebt] = useState("all");
  const [groupId, setGroupId] = useState("all");
  const [page, setPage] = useState(0);
  const [showForm, setShowForm] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState<CustomerListItem | null>(null);
  const [editingSupplier, setEditingSupplier] = useState<Supplier | null>(null);
  const [showGroupManager, setShowGroupManager] = useState(false);
  const permissions = useAppSelector((state) => state.auth.permissions);
  const canManageGroups = permissions.includes("customer:create") || permissions.includes("customer:update");

  const groupsQuery = useQuery({
    queryKey: ["customer-groups"],
    queryFn: listCustomerGroups,
    enabled: tab === "customers",
  });
  const groups = groupsQuery.data ?? [];

  const debouncedSearch = useDebouncedValue(search);

  const customerParams = useMemo(
    () => ({
      search: debouncedSearch,
      page,
      size: PAGE_SIZE,
      hasDebt: hasDebt === "all" ? undefined : hasDebt === "yes",
      customerGroupId: groupId === "all" ? undefined : Number(groupId),
    }),
    [debouncedSearch, page, hasDebt, groupId],
  );

  const customersQueryKey = useMemo(() => ["customers", customerParams] as const, [customerParams]);
  const customersQuery = useQuery({
    queryKey: customersQueryKey,
    queryFn: () => listCustomersWithStats(customerParams),
    enabled: tab === "customers",
  });

  const supplierParams = useMemo(
    () => ({ search: debouncedSearch, page, size: PAGE_SIZE }),
    [debouncedSearch, page],
  );

  const suppliersQueryKey = useMemo(() => ["suppliers", supplierParams] as const, [supplierParams]);
  const suppliersQuery = useQuery({
    queryKey: suppliersQueryKey,
    queryFn: () => listSuppliersWithStats(supplierParams),
    enabled: tab === "suppliers",
  });

  const customerColumns: DataTableColumn<CustomerListItem>[] = [
    {
      key: "name",
      header: "Khách hàng",
      render: (row) => (
        <div>
          <div className="font-medium">{row.name}</div>
          <div className="text-xs text-muted-foreground">{row.phone}</div>
        </div>
      ),
    },
    {
      key: "address",
      header: "Địa chỉ",
      render: (row) => row.address ?? "—",
    },
    {
      key: "groupName",
      header: "Nhóm",
      render: (row) =>
        row.groupName ? (
          <span className="rounded-full bg-muted px-2.5 py-1 text-xs font-medium">{row.groupName}</span>
        ) : (
          <span className="text-muted-foreground">—</span>
        ),
    },
    {
      key: "totalPurchased",
      header: "Tổng mua",
      className: "text-right",
      render: (row) => <Money value={row.totalPurchased} />,
    },
    { key: "orderCount", header: "Số đơn", className: "text-right" },
    {
      key: "currentDebt",
      header: "Công nợ",
      className: "text-right",
      render: (row) => (
        <span className={row.currentDebt > 0 ? "font-semibold text-destructive" : ""}>
          <Money value={row.currentDebt} />
        </span>
      ),
    },
    {
      key: "debtLimit",
      header: "Hạn mức nợ",
      className: "text-right",
      render: (row) => <Money value={row.debtLimit} />,
    },
    {
      key: "lastPurchaseAt",
      header: "Lần mua cuối",
      render: (row) => formatDate(row.lastPurchaseAt),
    },
    {
      key: "actions",
      header: "",
      render: (row) => (
        <PermissionGate perm="customer:update">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-8 w-8">
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => setEditingCustomer(row)}>Sửa</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </PermissionGate>
      ),
    },
  ];

  const supplierColumns: DataTableColumn<Supplier>[] = [
    {
      key: "name",
      header: "Nhà cung cấp",
      render: (row) => (
        <div>
          <div className="font-medium">{row.name}</div>
          <div className="text-xs text-muted-foreground">{row.phone}</div>
        </div>
      ),
    },
    {
      key: "totalPurchased",
      header: "Tổng nhập",
      className: "text-right",
      render: (row) => <Money value={row.totalPurchased} />,
    },
    { key: "orderCount", header: "Số phiếu", className: "text-right" },
    {
      key: "outstandingDebt",
      header: "Công nợ",
      className: "text-right",
      render: (row) => (
        <span className={row.outstandingDebt > 0 ? "font-semibold text-destructive" : ""}>
          <Money value={row.outstandingDebt} />
        </span>
      ),
    },
    {
      key: "lastPurchaseAt",
      header: "Lần nhập cuối",
      render: (row) => formatDate(row.lastPurchaseAt),
    },
    {
      key: "actions",
      header: "",
      render: (row) => (
        <PermissionGate perm="supplier:manage">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-8 w-8">
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => setEditingSupplier(row)}>Sửa</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </PermissionGate>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold">
          {tab === "customers" ? "Khách hàng" : "Nhà cung cấp"}
        </h1>
        <div className="flex gap-2">
          {tab === "customers" && (
            <Button variant="outline" onClick={() => setShowGroupManager(true)}>
              <Users2 className="h-4 w-4" />
              Quản lý nhóm
            </Button>
          )}
          <Button size="lg" onClick={() => setShowForm(true)}>
            <Plus className="h-4 w-4" />
            {tab === "customers" ? "Thêm khách hàng" : "Thêm nhà cung cấp"}
          </Button>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Input
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
          placeholder={tab === "customers" ? "Tìm tên, SĐT khách hàng..." : "Tìm tên, SĐT NCC..."}
          className="w-64"
        />
        {tab === "customers" && (
          <>
            <Select
              value={hasDebt}
              onValueChange={(v) => {
                setHasDebt(v);
                setPage(0);
              }}
            >
              <SelectTrigger className="w-40">
                <SelectValue placeholder="Công nợ" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Công nợ: Tất cả</SelectItem>
                <SelectItem value="yes">Có công nợ</SelectItem>
                <SelectItem value="no">Không nợ</SelectItem>
              </SelectContent>
            </Select>
            <Select
              value={groupId}
              onValueChange={(v) => {
                setGroupId(v);
                setPage(0);
              }}
            >
              <SelectTrigger className="w-44">
                <SelectValue placeholder="Nhóm" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Nhóm: Tất cả</SelectItem>
                {groups.map((g) => (
                  <SelectItem key={g.id} value={String(g.id)}>
                    {g.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </>
        )}
      </div>

      {tab === "customers" ? (
        <DataTable
          columns={customerColumns}
          data={customersQuery.data?.data ?? []}
          rowKey={(row) => row.id}
          meta={customersQuery.data?.meta}
          loading={customersQuery.isLoading}
          error={customersQuery.isError ? getApiErrorMessage(customersQuery.error) : null}
          onPageChange={setPage}
          emptyMessage="Không tìm thấy khách hàng phù hợp"
        />
      ) : (
        <DataTable
          columns={supplierColumns}
          data={suppliersQuery.data?.data ?? []}
          rowKey={(row) => row.id}
          meta={suppliersQuery.data?.meta}
          loading={suppliersQuery.isLoading}
          error={suppliersQuery.isError ? getApiErrorMessage(suppliersQuery.error) : null}
          onPageChange={setPage}
          emptyMessage="Không tìm thấy nhà cung cấp phù hợp"
        />
      )}

      {showForm &&
        (tab === "customers" ? (
          <CustomerFormDialog
            groups={groups}
            listQueryKey={customersQueryKey}
            onClose={() => setShowForm(false)}
          />
        ) : (
          <SupplierFormDialog listQueryKey={suppliersQueryKey} onClose={() => setShowForm(false)} />
        ))}

      {editingCustomer && (
        <CustomerFormDialog
          groups={groups}
          customer={editingCustomer}
          listQueryKey={customersQueryKey}
          onClose={() => setEditingCustomer(null)}
        />
      )}

      {editingSupplier && (
        <SupplierFormDialog
          supplier={editingSupplier}
          listQueryKey={suppliersQueryKey}
          onClose={() => setEditingSupplier(null)}
        />
      )}

      {tab === "customers" && (
        <CustomerGroupManagerDialog
          open={showGroupManager}
          onOpenChange={setShowGroupManager}
          canManage={canManageGroups}
        />
      )}
    </div>
  );
}
