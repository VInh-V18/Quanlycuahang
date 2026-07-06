import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Printer } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { InvoiceA4 } from "@/components/invoice/InvoiceA4";
import { getInvoiceByLookupCode } from "@/lib/api/invoices";
import { getApiErrorMessage } from "@/lib/http/errors";

/** Trang tra cứu hóa đơn công khai (khách quét QR trên hóa đơn giấy) — KHÔNG cần đăng nhập,
 * chỉ đọc được đúng 1 hóa đơn qua lookupCode (không liệt kê/duyệt được hóa đơn khác). */
export function InvoiceLookupPage() {
  const { code } = useParams<{ code: string }>();

  const { data: invoice, isLoading, isError, error } = useQuery({
    queryKey: ["invoice-lookup", code],
    queryFn: () => getInvoiceByLookupCode(code!),
    enabled: !!code,
    retry: false,
  });

  const lookupUrl = window.location.href;

  return (
    <div className="min-h-svh bg-muted py-8">
      <style>{`@media print { @page { size: A4; margin: 0; } .no-print { display: none !important; } }`}</style>

      <div className="no-print mx-auto mb-4 flex max-w-[210mm] justify-end">
        <Button onClick={() => window.print()} disabled={!invoice}>
          <Printer className="mr-2 h-4 w-4" />
          In / Lưu PDF
        </Button>
      </div>

      {isLoading && <Skeleton className="mx-auto h-96 w-full max-w-2xl" />}
      {isError && (
        <p className="text-center text-destructive">
          {getApiErrorMessage(error, "Không tìm thấy hóa đơn với mã tra cứu này")}
        </p>
      )}
      {invoice && <InvoiceA4 invoice={invoice} lookupUrl={lookupUrl} />}
    </div>
  );
}
