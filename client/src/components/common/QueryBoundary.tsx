import type { ReactNode } from "react";
import { RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { getApiErrorMessage, isNotFoundError } from "@/lib/http/errors";

export interface QueryBoundaryProps<T> {
  isLoading: boolean;
  isError: boolean;
  error?: unknown;
  data: T | null | undefined;
  onRetry?: () => void;
  notFoundMessage?: string;
  /** Tuỳ biến khung skeleton lúc tải (mặc định 3 thanh, đủ dùng cho hầu hết trang chi tiết). */
  loadingFallback?: ReactNode;
  children: (data: T) => ReactNode;
}

/** Bọc 3 trạng thái chuẩn (loading skeleton / lỗi kèm nút thử lại / 404 trung lập) cho MỌI trang
 * chi tiết dùng useQuery theo :id — tránh lặp lại cùng 1 khối if/else ở từng trang (Prompt #5, P1).
 * 404 và "khác tenant" cố ý gộp chung 1 thông điệp vì Backend cũng cố ý trả cùng 1 lỗi cho cả 2
 * trường hợp (không tiết lộ bản ghi có tồn tại ở nơi khác hay không — xem
 * TenantAwareRepositoryImpl phía server). */
export function QueryBoundary<T>({
  isLoading,
  isError,
  error,
  data,
  onRetry,
  notFoundMessage = "Không tìm thấy bản ghi này — có thể đã bị xoá hoặc bạn không có quyền xem.",
  loadingFallback,
  children,
}: QueryBoundaryProps<T>) {
  if (isLoading) {
    return (
      loadingFallback ?? (
        <div className="space-y-3">
          <Skeleton className="h-8 w-1/3" />
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
        </div>
      )
    );
  }

  if (isError) {
    const notFound = isNotFoundError(error);
    return (
      <div className="flex flex-col items-center gap-3 rounded-md border border-dashed p-10 text-center">
        <p className="text-sm text-destructive">
          {notFound ? notFoundMessage : getApiErrorMessage(error)}
        </p>
        {!notFound && onRetry && (
          <Button variant="outline" size="sm" onClick={onRetry}>
            <RefreshCw className="h-4 w-4" />
            Thử lại
          </Button>
        )}
      </div>
    );
  }

  if (data == null) {
    return null;
  }

  return <>{children(data)}</>;
}
