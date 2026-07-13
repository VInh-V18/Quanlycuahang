import { AxiosError, AxiosHeaders } from "axios";
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { QueryBoundary } from "@/components/common/QueryBoundary";

function notFoundError(): AxiosError {
  return new AxiosError(
    "Not Found",
    "404",
    undefined,
    undefined,
    {
      status: 404,
      statusText: "Not Found",
      headers: new AxiosHeaders(),
      config: { headers: new AxiosHeaders() },
      data: { error: { code: "NOT_FOUND", message: "Không tìm thấy" } },
    } as never,
  );
}

function serverError(): AxiosError {
  return new AxiosError(
    "Server Error",
    "500",
    undefined,
    undefined,
    {
      status: 500,
      statusText: "Internal Server Error",
      headers: new AxiosHeaders(),
      config: { headers: new AxiosHeaders() },
      data: { error: { code: "INTERNAL_ERROR", message: "Lỗi hệ thống, vui lòng thử lại" } },
    } as never,
  );
}

/** Prompt #5, mục 3: kiểm tra 3 trạng thái chuẩn cho component dùng chung mọi trang chi tiết
 * (PurchaseOrderDetailPage/StockTakeDetailPage/InvoiceViewer/ShiftDetailDialog) — test 1 lần ở
 * đây thay vì lặp lại ở từng trang tiêu thụ. */
describe("QueryBoundary", () => {
  it("hiện skeleton khi đang tải", () => {
    const { container } = render(
      <QueryBoundary isLoading isError={false} data={undefined}>
        {() => <div>Nội dung</div>}
      </QueryBoundary>,
    );
    expect(container.querySelector(".animate-pulse")).toBeInTheDocument();
    expect(screen.queryByText("Nội dung")).not.toBeInTheDocument();
  });

  it("404 hiện thông điệp trung lập, KHÔNG hiện nút thử lại", () => {
    const onRetry = vi.fn();
    render(
      <QueryBoundary isLoading={false} isError error={notFoundError()} data={undefined} onRetry={onRetry}>
        {() => <div>Nội dung</div>}
      </QueryBoundary>,
    );
    expect(
      screen.getByText(/Không tìm thấy bản ghi này/),
    ).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Thử lại/ })).not.toBeInTheDocument();
  });

  it("lỗi khác 404 hiện message + nút Thử lại, bấm gọi onRetry", () => {
    const onRetry = vi.fn();
    render(
      <QueryBoundary isLoading={false} isError error={serverError()} data={undefined} onRetry={onRetry}>
        {() => <div>Nội dung</div>}
      </QueryBoundary>,
    );
    expect(screen.getByText("Lỗi hệ thống, vui lòng thử lại")).toBeInTheDocument();
    const retryButton = screen.getByRole("button", { name: /Thử lại/ });
    fireEvent.click(retryButton);
    expect(onRetry).toHaveBeenCalledOnce();
  });

  it("có dữ liệu thì render children với đúng data", () => {
    render(
      <QueryBoundary isLoading={false} isError={false} data={{ name: "Test" }}>
        {(data) => <div>Xin chào {data.name}</div>}
      </QueryBoundary>,
    );
    expect(screen.getByText("Xin chào Test")).toBeInTheDocument();
  });
});
