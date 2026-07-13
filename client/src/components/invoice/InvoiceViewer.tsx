import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Printer } from "lucide-react";
import { QueryBoundary } from "@/components/common/QueryBoundary";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { InvoiceA4 } from "@/components/invoice/InvoiceA4";
import { InvoiceK58 } from "@/components/invoice/InvoiceK58";
import { InvoiceK80 } from "@/components/invoice/InvoiceK80";
import { getInvoiceById } from "@/lib/api/invoices";

type Format = "k58" | "k80" | "a4";

const PAPER_WIDTH_MM: Record<"k58" | "k80", number> = { k58: 58, k80: 80 };
const PX_PER_MM = 96 / 25.4;

/** Noi dung xem/in hoa don dung chung — dung truc tiep tren trang /invoices/:id/print (route rieng)
 * lan nhung ben trong InvoiceDialog (hien ngay tren trang ban hang luc thanh toan). Tach rieng de
 * logic do chieu cao khổ nhiệt K58/K80 (@page size) chi ton tai 1 noi, tranh lech khi sua sau nay. */
export function InvoiceViewer({
  invoiceId,
  defaultFormat = "k80",
}: {
  invoiceId: number | string;
  defaultFormat?: Format;
}) {
  const [format, setFormat] = useState<Format>(defaultFormat);
  const [receiptHeightMm, setReceiptHeightMm] = useState(200);
  const contentRef = useRef<HTMLDivElement>(null);

  const {
    data: invoice,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["invoice", invoiceId],
    queryFn: () => getInvoiceById(invoiceId),
  });

  // Do chieu cao thuc te cua noi dung roi ghi thang so mm vao @page, KHONG dung tu khoa `auto` —
  // 1 so may in nhiet/driver ao (VD tien ich mo rong cua trinh duyet) khong hieu `size: 58mm auto`
  // nen tu dong roi ve A4 doc mac dinh. Ghi chieu cao cu the loai bo hoan toan phu thuoc do.
  useEffect(() => {
    if (format === "a4" || !invoice) return;
    function measure() {
      if (!contentRef.current) return;
      const heightPx = contentRef.current.getBoundingClientRect().height;
      setReceiptHeightMm(Math.ceil(heightPx / PX_PER_MM) + 5);
    }
    measure();
    window.addEventListener("beforeprint", measure);
    return () => window.removeEventListener("beforeprint", measure);
  }, [format, invoice]);

  const pageCss =
    format === "a4"
      ? "@page { size: A4; margin: 0; }"
      : `@page { size: ${PAPER_WIDTH_MM[format]}mm ${receiptHeightMm}mm; margin: 0; }`;

  return (
    <div>
      <style>{`@media print { ${pageCss} .no-print { display: none !important; } }`}</style>

      <div className="no-print mb-4 flex items-center justify-between">
        <Tabs value={format} onValueChange={(v) => setFormat(v as Format)}>
          <TabsList>
            <TabsTrigger value="k58">Khổ K58</TabsTrigger>
            <TabsTrigger value="k80">Khổ K80</TabsTrigger>
            <TabsTrigger value="a4">Khổ A4</TabsTrigger>
          </TabsList>
        </Tabs>
        <Button onClick={() => window.print()} disabled={!invoice}>
          <Printer className="mr-2 h-4 w-4" />
          In hóa đơn
        </Button>
      </div>

      <div className="rounded-lg bg-muted p-8 print:bg-transparent print:p-0">
        <QueryBoundary
          isLoading={isLoading}
          isError={isError}
          error={error}
          data={invoice}
          onRetry={() => refetch()}
          notFoundMessage="Không tìm thấy hóa đơn này — có thể đã bị xoá hoặc bạn không có quyền xem."
          loadingFallback={<Skeleton className="mx-auto h-96 w-full max-w-2xl" />}
        >
          {(invoice) => (
            <div ref={contentRef} className="shadow-xl print:shadow-none">
              {format === "k58" ? (
                <InvoiceK58 invoice={invoice} />
              ) : format === "k80" ? (
                <InvoiceK80 invoice={invoice} />
              ) : (
                <InvoiceA4 invoice={invoice} />
              )}
            </div>
          )}
        </QueryBoundary>
      </div>
    </div>
  );
}
