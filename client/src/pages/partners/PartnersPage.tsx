import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useLocation } from "react-router-dom";
import { Plus, Users2 } from "lucide-react";
import { useAppSelector } from "@/store/hooks";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
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
import { useToast } from "@/components/ui/use-toast";
import { CustomerGroupManagerDialog } from "@/components/partners/CustomerGroupManagerDialog";
import {
  createCustomer,
  listCustomersWithStats,
  type CustomerListItem,
  type CustomerRequest,
} from "@/lib/api/customers";
import { listCustomerGroups, type CustomerGroup } from "@/lib/api/customerGroups";
import {
  createSupplier,
  listSuppliersWithStats,
  type Supplier,
  type SupplierRequest,
} from "@/lib/api/suppliers";
import { getApiErrorMessage } from "@/lib/http/errors";

const PAGE_SIZE = 20;

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("vi-VN");
}

function CustomerFormDialog({
  groups,
  onClose,
}: {
  groups: CustomerGroup[];
  onClose: () => void;
}) {
  const [form, setForm] = useState<CustomerRequest>({
    name: "",
    phone: "",
    address: "",
    debtLimit: 0,
    customerGroupId: null,
  });
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: () => createCustomer(form),
    onSuccess: () => {
      toast({ title: "Đã thêm khách hàng" });
      queryClient.invalidateQueries({ queryKey: ["customers"] });
      onClose();
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể thêm", description: getApiErrorMessage(err) });
    },
  });

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Thêm khách hàng</DialogTitle>
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

function SupplierFormDialog({ onClose }: { onClose: () => void }) {
  const [form, setForm] = useState<SupplierRequest>({ name: "", phone: "" });
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: () => createSupplier(form),
    onSuccess: () => {
      toast({ title: "Đã thêm nhà cung cấp" });
      queryClient.invalidateQueries({ queryKey: ["suppliers"] });
      onClose();
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể thêm", description: getApiErrorMessage(err) });
    },
  });

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Thêm nhà cung cấp</DialogTitle>
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
  const [showGroupManager, setShowGroupManager] = useState(false);
  const permissions = useAppSelector((state) => state.auth.permissions);
  const canManageGroups = permissions.includes("customer:create") || permissions.includes("customer:update");

  const groupsQuery = useQuery({
    queryKey: ["customer-groups"],
    queryFn: listCustomerGroups,
    enabled: tab === "customers",
  });
  const groups = groupsQuery.data ?? [];

  const customerParams = useMemo(
    () => ({
      search,
      page,
      size: PAGE_SIZE,
      hasDebt: hasDebt === "all" ? undefined : hasDebt === "yes",
      customerGroupId: groupId === "all" ? undefined : Number(groupId),
    }),
    [search, page, hasDebt, groupId],
  );

  const customersQuery = useQuery({
    queryKey: ["customers", customerParams],
    queryFn: () => listCustomersWithStats(customerParams),
    enabled: tab === "customers",
  });

  const supplierParams = useMemo(() => ({ search, page, size: PAGE_SIZE }), [search, page]);

  const suppliersQuery = useQuery({
    queryKey: ["suppliers", supplierParams],
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
          <CustomerFormDialog groups={groups} onClose={() => setShowForm(false)} />
        ) : (
          <SupplierFormDialog onClose={() => setShowForm(false)} />
        ))}

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
