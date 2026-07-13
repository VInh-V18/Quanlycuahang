import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";
import { FolderCog, MoreHorizontal, Plus, Search } from "lucide-react";
import { useAppSelector } from "@/store/hooks";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CategoryManagerDialog } from "@/components/products/CategoryManagerDialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
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
import { listCategories } from "@/lib/api/categories";
import {
  deleteProduct,
  originFlag,
  searchProducts,
  type Product,
} from "@/lib/api/products";
import { useCurrentBranchId } from "@/lib/hooks/useCurrentBranchId";
import { useDebouncedValue } from "@/lib/hooks/useDebouncedValue";
import { getApiErrorMessage } from "@/lib/http/errors";

const PAGE_SIZE = 20;

export function ProductsPage() {
  const branchId = useCurrentBranchId();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [categoryId, setCategoryId] = useState<string>("all");
  const [active, setActive] = useState<string>("all");
  const [deleteTarget, setDeleteTarget] = useState<Product | null>(null);
  const [showCategoryManager, setShowCategoryManager] = useState(false);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const permissions = useAppSelector((state) => state.auth.permissions);
  const canManageCategories =
    permissions.includes("category:create") ||
    permissions.includes("category:update") ||
    permissions.includes("category:delete");

  const debouncedSearch = useDebouncedValue(search);

  const params = useMemo(
    () => ({
      page,
      size: PAGE_SIZE,
      search: debouncedSearch,
      categoryId: categoryId === "all" ? undefined : Number(categoryId),
      active: active === "all" ? undefined : active === "active",
      branchId,
    }),
    [page, debouncedSearch, categoryId, active, branchId],
  );

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["products", params],
    queryFn: () => searchProducts(params),
  });

  const categoriesQuery = useQuery({ queryKey: ["categories"], queryFn: listCategories });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteProduct(id),
    onSuccess: () => {
      toast({ title: "Đã xóa sản phẩm" });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      setDeleteTarget(null);
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể xóa", description: getApiErrorMessage(err) });
    },
  });

  const columns: DataTableColumn<Product>[] = [
    {
      key: "name",
      header: "Sản phẩm",
      render: (row) => (
        <div>
          <div className="font-medium">
            {row.name} {originFlag(row.originCountry)}
          </div>
          <div className="flex gap-2 text-xs text-muted-foreground">
            <span>{row.sku}</span>
            {row.barcode && <span>{row.barcode}</span>}
          </div>
        </div>
      ),
    },
    { key: "categoryName", header: "Danh mục" },
    {
      key: "costPrice",
      header: "Giá vốn",
      className: "text-right",
      render: (row) => (row.costPrice != null ? <Money value={row.costPrice} /> : "—"),
    },
    {
      key: "sellPrice",
      header: "Giá bán",
      className: "text-right",
      render: (row) => (
        <span>
          <Money value={row.sellPrice} />/{row.unit}
        </span>
      ),
    },
    {
      key: "vatRate",
      header: "VAT",
      className: "text-right",
      render: (row) => `${row.vatRate}%`,
    },
    {
      key: "stock",
      header: "Tồn",
      className: "text-right",
      render: (row) =>
        row.stock != null ? (
          <span className={row.stock <= row.minStock ? "font-semibold text-destructive" : ""}>
            {row.stock} {row.unit}
          </span>
        ) : (
          "—"
        ),
    },
    {
      key: "active",
      header: "Trạng thái",
      render: (row) => (
        <Badge variant={row.active ? "success" : "secondary"}>
          {row.active ? "Đang bán" : "Ngừng bán"}
        </Badge>
      ),
    },
    {
      key: "id",
      header: "",
      render: (row) => (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" className="h-8 w-8">
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => navigate(`/products/${row.id}/edit`)}>
              Sửa
            </DropdownMenuItem>
            <DropdownMenuItem className="text-destructive" onClick={() => setDeleteTarget(row)}>
              Xóa
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Sản phẩm</h1>
          <p className="text-sm text-muted-foreground">
            Hàng hóa / Sản phẩm · {data?.meta?.total ?? 0} SKU
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setShowCategoryManager(true)}>
            <FolderCog className="h-4 w-4" />
            Quản lý danh mục
          </Button>
          <Button asChild size="lg">
            <Link to="/products/new">
              <Plus className="h-4 w-4" />
              Thêm sản phẩm
            </Link>
          </Button>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <div className="relative w-64">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            placeholder="Tìm tên / SKU / barcode"
            className="pl-8"
          />
        </div>
        <Select
          value={categoryId}
          onValueChange={(v) => {
            setCategoryId(v);
            setPage(0);
          }}
        >
          <SelectTrigger className="w-48">
            <SelectValue placeholder="Danh mục" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Danh mục: Tất cả</SelectItem>
            {categoriesQuery.data?.map((c) => (
              <SelectItem key={c.id} value={String(c.id)}>
                {c.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select
          value={active}
          onValueChange={(v) => {
            setActive(v);
            setPage(0);
          }}
        >
          <SelectTrigger className="w-44">
            <SelectValue placeholder="Trạng thái" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Trạng thái: Tất cả</SelectItem>
            <SelectItem value="active">Đang bán</SelectItem>
            <SelectItem value="inactive">Ngừng bán</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <DataTable
        columns={columns}
        data={data?.data ?? []}
        rowKey={(row) => row.id}
        meta={data?.meta}
        loading={isLoading}
        error={isError ? getApiErrorMessage(error) : null}
        onPageChange={setPage}
        emptyMessage="Không tìm thấy sản phẩm phù hợp"
      />

      <Dialog open={!!deleteTarget} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Xóa sản phẩm</DialogTitle>
            <DialogDescription>
              Bạn có chắc muốn xóa "{deleteTarget?.name}"? Lịch sử giao dịch liên quan vẫn được giữ
              lại.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>
              Hủy
            </Button>
            <Button
              variant="destructive"
              disabled={deleteMutation.isPending}
              onClick={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
            >
              Xóa
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <CategoryManagerDialog
        open={showCategoryManager}
        onOpenChange={setShowCategoryManager}
        canManage={canManageCategories}
      />
    </div>
  );
}
