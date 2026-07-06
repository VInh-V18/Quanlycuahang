import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { DataTable, type DataTableColumn, type SortState } from "@/components/common/DataTable";
import { Money } from "@/components/common/Money";
import { searchProducts, type Product } from "@/lib/api/products";
import { getApiErrorMessage } from "@/lib/http/errors";

const PAGE_SIZE = 10;

/** Demo DataTable thật với API Backend (Gate Phase 5.3: phân trang/sắp xếp/lọc).
 * Ghi chú: Backend `/products` dùng native query có ORDER BY cố định theo tên (chưa hỗ trợ
 * Pageable.sort động — xem docs/phase5/frontend-foundation.md) nên sắp xếp ở demo này thực hiện
 * phía client trên trang dữ liệu hiện tại; phân trang & lọc (search) là gọi API thật. */
export function ProductsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState<SortState | null>(null);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["products", page, search],
    queryFn: () => searchProducts({ page, size: PAGE_SIZE, search }),
  });

  const rows = useMemo(() => {
    const list = data?.data ?? [];
    if (!sort) return list;
    const factor = sort.direction === "asc" ? 1 : -1;
    return [...list].sort((a, b) => {
      const av = a[sort.key as keyof Product];
      const bv = b[sort.key as keyof Product];
      if (typeof av === "number" && typeof bv === "number") return (av - bv) * factor;
      return String(av).localeCompare(String(bv)) * factor;
    });
  }, [data, sort]);

  const columns: DataTableColumn<Product>[] = [
    { key: "sku", header: "SKU", sortable: true },
    { key: "name", header: "Tên sản phẩm", sortable: true },
    { key: "categoryName", header: "Danh mục" },
    { key: "unit", header: "Đơn vị" },
    {
      key: "sellPrice",
      header: "Giá bán",
      sortable: true,
      className: "text-right",
      render: (row) => <Money value={row.sellPrice} />,
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
  ];

  function handlePageChange(nextPage: number) {
    setPage(nextPage);
  }

  function handleSearchChange(value: string) {
    setSearch(value);
    setPage(0);
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Sản phẩm</CardTitle>
      </CardHeader>
      <CardContent>
        <DataTable
          columns={columns}
          data={rows}
          rowKey={(row) => row.id}
          meta={data?.meta}
          loading={isLoading}
          error={isError ? getApiErrorMessage(error) : null}
          sort={sort}
          onSortChange={setSort}
          onPageChange={handlePageChange}
          searchValue={search}
          onSearchChange={handleSearchChange}
          searchPlaceholder="Tìm theo tên, SKU, barcode..."
          emptyMessage="Không tìm thấy sản phẩm phù hợp"
        />
      </CardContent>
    </Card>
  );
}
