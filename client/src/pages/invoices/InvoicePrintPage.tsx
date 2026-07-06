import { useState } from "react";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Printer } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { InvoiceA4 } from "@/components/invoice/InvoiceA4";
import { InvoiceK80 } from "@/components/invoice/InvoiceK80";
import { getInvoiceById } from "@/lib/api/invoices";
import { getApiErrorMessage } from "@/lib/http/errors";

type Format = "k80" | "a4";

const PAGE_CSS: Record<Format, string> = {
  k80: "@page { size: 80mm auto; margin: 0; }",
  a4: "@page { size: A4; margin: 0; }",
};

/** In lại hóa đơn (K80/A4) — Gate Phase 9: "in thử K80 đúng khổ, số liệu khớp đơn gốc từng đồng".
 * Backend chỉ trả JSON, toàn bộ layout in dựng ở FE (B3). */
export function InvoicePrintPage() {
  const { id } = useParams<{ id: string }>();
  const [format, setFormat] = useState<Format>("k80");

  const { data: invoice, isLoading, isError, error } = useQuery({
    queryKey: ["invoice", id],
    queryFn: () => getInvoiceById(id!),
    enabled: !!id,
  });

  const lookupUrl = invoice ? `${window.location.origin}/tra-cuu/${invoice.lookupCode}` : "";

  return (
    <div>
      <style>{`@media print { ${PAGE_CSS[format]} .no-print { display: none !important; } }`}</style>

      <div className="no-print mb-6 flex items-center justify-between">
        <Tabs value={format} onValueChange={(v) => setFormat(v as Format)}>
          <TabsList>
            <TabsTrigger value="k80">Khổ K80</TabsTrigger>
            <TabsTrigger value="a4">Khổ A4</TabsTrigger>
          </TabsList>
        </Tabs>
        <Button onClick={() => window.print()} disabled={!invoice}>
          <Printer className="mr-2 h-4 w-4" />
          In hóa đơn
        </Button>
      </div>

      {isLoading && <Skeleton className="mx-auto h-96 w-full max-w-2xl" />}
      {isError && <p className="text-center text-destructive">{getApiErrorMessage(error)}</p>}
      {invoice &&
        (format === "k80" ? (
          <InvoiceK80 invoice={invoice} lookupUrl={lookupUrl} />
        ) : (
          <InvoiceA4 invoice={invoice} lookupUrl={lookupUrl} />
        ))}
    </div>
  );
}
