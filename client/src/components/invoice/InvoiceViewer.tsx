import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Download, Printer } from "lucide-react";
import { QueryBoundary } from "@/components/common/QueryBoundary";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useToast } from "@/components/ui/use-toast";
import { InvoiceA4 } from "@/components/invoice/InvoiceA4";
import { InvoiceK58 } from "@/components/invoice/InvoiceK58";
import { InvoiceK80 } from "@/components/invoice/InvoiceK80";
import { getInvoiceById } from "@/lib/api/invoices";

type Format = "k58" | "k80" | "a4";

const PAPER_WIDTH_MM: Record<"k58" | "k80", number> = { k58: 58, k80: 80 };
const PX_PER_MM = 96 / 25.4;
const A4_WIDTH_PX = 210 * PX_PER_MM;
const A4_HEIGHT_PX = 297 * PX_PER_MM;

// Nho kho giay da chon lan gan nhat (thay vi luon mo lai defaultFormat cua noi goi) - nguoi dung
// yeu cau khong phai bam lai "Khổ A4"/"Khổ K80" moi lan mo hoa don neu ho quen dung 1 kho co dinh.
const FORMAT_STORAGE_KEY = "fruithouse:invoice-format";

function readStoredFormat(fallback: Format): Format {
  if (typeof window === "undefined") return fallback;
  const stored = window.localStorage.getItem(FORMAT_STORAGE_KEY);
  return stored === "k58" || stored === "k80" || stored === "a4" ? stored : fallback;
}

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
  const [format, setFormatState] = useState<Format>(() => readStoredFormat(defaultFormat));
  const [receiptHeightMm, setReceiptHeightMm] = useState(200);
  const contentRef = useRef<HTMLDivElement>(null);
  const frameRef = useRef<HTMLDivElement>(null);
  const pdfSourceRef = useRef<HTMLDivElement>(null);
  const [a4Scale, setA4Scale] = useState(1);
  const [downloading, setDownloading] = useState(false);
  const { toast } = useToast();

  function setFormat(next: Format) {
    setFormatState(next);
    window.localStorage.setItem(FORMAT_STORAGE_KEY, next);
  }

  // Thu nho khổ A4 vua man hinh may tinh (ca be rong khung xem LAN chieu cao cua so trinh duyet,
  // khong bao gio phong to qua 100%) — giong dung cach trinh duyet tu thu nho trang de vua khung xem
  // truoc khi in. Dung thang window.innerHeight (khong do qua cay DOM cha) - da thu do "to cuon gan
  // nhat" truoc do va sai tren trang doc lap /invoices/:id/print vi tim nham to cuon ngoai, lam hoa
  // don co lai qua nho khong ro ly do (xem lich su sua doi). Luc in that, CSS o duoi reset transform
  // ve binh thuong nen ban in van dung 100% khổ A4 that.
  useEffect(() => {
    if (format !== "a4") return;
    function updateScale() {
      if (!frameRef.current) return;
      // p-8 = 32px moi ben, tru ra de co dung khong gian con lai cho noi dung ben trong.
      const availableWidth = frameRef.current.getBoundingClientRect().width - 64;
      // Tru them ~180px cho thanh tab + nut + khoang cach tren/duoi trang - uoc luong, khong can
      // chinh xac tuyet doi vi luon con `overflow-y-auto` cua trang/Dialog lam luoi an toan cuoi.
      const availableHeight = window.innerHeight - 180;
      setA4Scale(Math.min(1, availableWidth / A4_WIDTH_PX, availableHeight / A4_HEIGHT_PX));
    }
    updateScale();
    window.addEventListener("resize", updateScale);
    return () => window.removeEventListener("resize", updateScale);
  }, [format]);

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

  // Tai PDF truc tiep (khong qua hop thoai in cua trinh duyet) - chup lai DOM hoa don thanh anh roi
  // nhung vao 1 file PDF co kich thuoc dung khổ giay dang chon. Chup tu `pdfSourceRef` (ban an,
  // dat ngoai man hinh, LUON o ty le that 100%) thay vi `contentRef` (ban dang hien thi, co the
  // dang bi CSS transform thu nho de vua man hinh xem A4) — tung thu tam tra scale=1 roi chup lai
  // contentRef nhung bi cat mat 1 phan noi dung ben phai (rat co the do doi transform chua kip ap
  // dung xong luc chup, xem lich su sua doi), dung ban rieng nay de tranh hoan toan van de do.
  async function handleDownloadPdf() {
    if (!invoice || !pdfSourceRef.current) return;
    setDownloading(true);
    try {
      const [{ default: html2canvas }, { jsPDF }] = await Promise.all([
        import("html2canvas"),
        import("jspdf"),
      ]);
      const source = pdfSourceRef.current;
      const canvas = await html2canvas(source, {
        scale: 2,
        backgroundColor: "#ffffff",
        // Neu khong ep scrollX/scrollY/window* thi html2canvas tu doan theo cua so trinh duyet hien
        // tai — voi phan tu dat ngoai vung nhin thay (xem comment o cho render ben duoi), doan sai se
        // lam canvas bi CAT MAT o canh phai/duoi du element khong he display:none (da gap loi nay
        // thuc te, xem lich su sua doi lan truoc chua het loi vi thieu doan nay).
        scrollX: 0,
        scrollY: 0,
        windowWidth: source.scrollWidth,
        windowHeight: source.scrollHeight,
      });
      const widthMm = format === "a4" ? 210 : PAPER_WIDTH_MM[format];
      const contentHeightMm = (canvas.height * widthMm) / canvas.width;
      // Khổ A4 dung CO DINH 210x297mm (giong het `@page { size: A4 }` luc in that) — neu noi dung
      // ngan hon 1 trang, phan con lai la khoang trang o DUOI trang giong ban in that, khong co dinh
      // chieu cao trang theo noi dung nhu truoc (lam trang PDF bi "cat ngan" thay vi giu du 1 to A4).
      // Khổ nhiet K58/K80 van giu chieu cao trang = chieu cao noi dung, dung nhu `pageCss` da tinh.
      const heightMm = format === "a4" ? 297 : contentHeightMm;
      const pdf = new jsPDF({
        unit: "mm",
        format: [widthMm, heightMm],
        orientation: "portrait",
      });
      pdf.addImage(canvas.toDataURL("image/png"), "PNG", 0, 0, widthMm, contentHeightMm);
      pdf.save(`hoa-don-${invoice.invoiceNumber}.pdf`);
    } catch {
      toast({ variant: "destructive", title: "Không thể tải PDF, vui lòng thử lại" });
    } finally {
      setDownloading(false);
    }
  }

  return (
    <div>
      <style>{`@media print {
        ${pageCss}
        .no-print { display: none !important; }
        .invoice-a4-scale-frame, .invoice-a4-scale-frame > div {
          transform: none !important;
          width: auto !important;
          height: auto !important;
        }
      }`}</style>

      <div className="no-print mb-4 flex items-center justify-between">
        <Tabs value={format} onValueChange={(v) => setFormat(v as Format)}>
          <TabsList>
            <TabsTrigger value="k58">Khổ K58</TabsTrigger>
            <TabsTrigger value="k80">Khổ K80</TabsTrigger>
            <TabsTrigger value="a4">Khổ A4</TabsTrigger>
          </TabsList>
        </Tabs>
        <div className="flex gap-2">
          <Button variant="outline" onClick={handleDownloadPdf} disabled={!invoice || downloading}>
            <Download className="mr-2 h-4 w-4" />
            {downloading ? "Đang tải..." : "Tải PDF"}
          </Button>
          <Button onClick={() => window.print()} disabled={!invoice}>
            <Printer className="mr-2 h-4 w-4" />
            In hóa đơn
          </Button>
        </div>
      </div>

      <div ref={frameRef} className="rounded-lg bg-muted p-8 print:bg-transparent print:p-0">
        <QueryBoundary
          isLoading={isLoading}
          isError={isError}
          error={error}
          data={invoice}
          onRetry={() => refetch()}
          notFoundMessage="Không tìm thấy hóa đơn này — có thể đã bị xoá hoặc bạn không có quyền xem."
          loadingFallback={<Skeleton className="mx-auto h-96 w-full max-w-2xl" />}
        >
          {(invoice) =>
            format === "a4" ? (
              // Khung ngoai la hinh chu nhat TRANG (bg-white + shadow) dung dung ty le 1 trang A4
              // that (297mm), khong chi vua khit theo chieu cao noi dung — neu hoa don ngan hon 1
              // trang, phan con lai la khoang trang TRANG (khong phai mau xam cua nen page) giong
              // dung ban xem truoc khi in cua trinh duyet, khong phai khoi noi dung troi lung lo.
              <div
                className="invoice-a4-scale-frame mx-auto overflow-hidden bg-white shadow-xl print:shadow-none"
                style={{ width: A4_WIDTH_PX * a4Scale, height: A4_HEIGHT_PX * a4Scale }}
              >
                <div
                  ref={contentRef}
                  style={{
                    width: A4_WIDTH_PX,
                    transform: `scale(${a4Scale})`,
                    transformOrigin: "top left",
                  }}
                >
                  <InvoiceA4 invoice={invoice} />
                </div>
              </div>
            ) : (
              <div ref={contentRef} className="shadow-xl print:shadow-none">
                {format === "k58" ? (
                  <InvoiceK58 invoice={invoice} />
                ) : (
                  <InvoiceK80 invoice={invoice} />
                )}
              </div>
            )
          }
        </QueryBoundary>
      </div>

      {/* Ban an danh rieng cho Tai PDF - luon o ty le that 100% (khong bao gio bi CSS transform thu
          nho nhu ban dang hien thi tren man hinh). Dat tai top:0/left:0 (TRONG vung nhin thay cua
          cua so, khong day ra xa bang left am rat lon) roi an bang opacity:0 + pointer-events:none —
          tung dung `left: -99999px` nhung html2canvas doan kich thuoc "cua so" de nhan ban trang
          theo cua so trinh duyet hien tai, phan tu nam qua xa ngoai vung do bi CAT MAT luc chup du
          khong he display:none (loi thuc te da gap, xem lich su sua doi). z-index am de khong che
          noi dung that phia tren du opacity da la 0. */}
      {invoice && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            opacity: 0,
            pointerEvents: "none",
            zIndex: -1,
          }}
        >
          <div
            ref={pdfSourceRef}
            style={{ width: format === "a4" ? A4_WIDTH_PX : PAPER_WIDTH_MM[format] * PX_PER_MM }}
          >
            {format === "k58" ? (
              <InvoiceK58 invoice={invoice} />
            ) : format === "k80" ? (
              <InvoiceK80 invoice={invoice} />
            ) : (
              <InvoiceA4 invoice={invoice} />
            )}
          </div>
        </div>
      )}
    </div>
  );
}
