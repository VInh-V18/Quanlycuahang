import type { ReactNode } from "react";
import { ArrowDown, ArrowUp, ArrowUpDown, ChevronLeft, ChevronRight, SearchX } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";
import type { PageMeta } from "@/types/api";

export interface SortState {
  key: string;
  direction: "asc" | "desc";
}

export interface DataTableColumn<T> {
  key: string;
  header: string;
  sortable?: boolean;
  className?: string;
  render?: (row: T) => ReactNode;
}

export interface DataTableProps<T> {
  columns: DataTableColumn<T>[];
  data: T[];
  rowKey: (row: T) => string | number;
  meta?: PageMeta;
  loading?: boolean;
  error?: string | null;
  sort?: SortState | null;
  onSortChange?: (sort: SortState | null) => void;
  onPageChange?: (page: number) => void;
  searchValue?: string;
  onSearchChange?: (value: string) => void;
  searchPlaceholder?: string;
  emptyMessage?: string;
}

/** DataTable dùng chung mọi màn danh sách — đọc `meta.page/limit/total` đúng shape
 * Spring `Pageable` response (D2), hỗ trợ sắp xếp (click header), lọc (ô tìm kiếm rời rạc
 * với fetch, debounce do nơi gọi tự quyết định), 3 trạng thái chuẩn loading/empty/error
 * (Phase 4 ui-states.md). */
export function DataTable<T>({
  columns,
  data,
  rowKey,
  meta,
  loading = false,
  error = null,
  sort = null,
  onSortChange,
  onPageChange,
  searchValue,
  onSearchChange,
  searchPlaceholder = "Tìm kiếm...",
  emptyMessage = "Không có dữ liệu",
}: DataTableProps<T>) {
  const totalPages = meta ? Math.max(1, Math.ceil(meta.total / Math.max(meta.limit, 1))) : 1;
  const currentPage = meta ? meta.page : 0;

  function handleSort(column: DataTableColumn<T>) {
    if (!column.sortable || !onSortChange) return;
    if (sort?.key !== column.key) {
      onSortChange({ key: column.key, direction: "asc" });
    } else if (sort.direction === "asc") {
      onSortChange({ key: column.key, direction: "desc" });
    } else {
      onSortChange(null);
    }
  }

  return (
    <div className="space-y-4">
      {onSearchChange && (
        <Input
          value={searchValue ?? ""}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder={searchPlaceholder}
          className="max-w-sm"
        />
      )}

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              {columns.map((column) => (
                <TableHead
                  key={column.key}
                  className={cn(column.sortable && "cursor-pointer select-none", column.className)}
                  onClick={() => handleSort(column)}
                >
                  <span className="inline-flex items-center gap-1">
                    {column.header}
                    {column.sortable &&
                      (sort?.key === column.key ? (
                        sort.direction === "asc" ? (
                          <ArrowUp className="h-3.5 w-3.5" />
                        ) : (
                          <ArrowDown className="h-3.5 w-3.5" />
                        )
                      ) : (
                        <ArrowUpDown className="h-3.5 w-3.5 opacity-40" />
                      ))}
                  </span>
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={`skeleton-${i}`}>
                  {columns.map((column) => (
                    <TableCell key={column.key}>
                      <Skeleton className="h-5 w-full" />
                    </TableCell>
                  ))}
                </TableRow>
              ))
            ) : error ? (
              <TableRow>
                <TableCell colSpan={columns.length} className="h-32 text-center text-destructive">
                  {error}
                </TableCell>
              </TableRow>
            ) : data.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length} className="h-32 text-center text-muted-foreground">
                  <div className="flex flex-col items-center gap-2">
                    <SearchX className="h-8 w-8 opacity-50" />
                    {emptyMessage}
                  </div>
                </TableCell>
              </TableRow>
            ) : (
              data.map((row) => (
                <TableRow key={rowKey(row)}>
                  {columns.map((column) => (
                    <TableCell key={column.key} className={column.className}>
                      {column.render ? column.render(row) : String((row as Record<string, unknown>)[column.key] ?? "")}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {meta && onPageChange && !loading && !error && data.length > 0 && (
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>
            Trang {currentPage + 1}/{totalPages} — tổng {meta.total} bản ghi
          </span>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage <= 0}
              onClick={() => onPageChange(currentPage - 1)}
            >
              <ChevronLeft className="h-4 w-4" />
              Trước
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage + 1 >= totalPages}
              onClick={() => onPageChange(currentPage + 1)}
            >
              Sau
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
