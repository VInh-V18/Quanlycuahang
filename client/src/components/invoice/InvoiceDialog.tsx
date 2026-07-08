import { Dialog, DialogContent } from "@/components/ui/dialog";
import { InvoiceViewer } from "@/components/invoice/InvoiceViewer";

/** Hien hoa don ngay tren trang hien tai (VD: trang ban hang luc thanh toan xong) thay vi mo tab
 * moi — DialogContent mac dinh dung position:fixed + transform de can giua man hinh, 2 thu nay
 * in ra rat khong on dinh (Chrome hay cat/lech trang khi in phan tu fixed), nen luc in phai tro ve
 * static/khong transform (cac class print:...) de ban in dung nhu 1 khoi noi dung binh thuong. */
export function InvoiceDialog({
  invoiceId,
  onClose,
}: {
  invoiceId: number;
  onClose: () => void;
}) {
  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent
        className="max-w-3xl max-h-[90vh] overflow-y-auto print:static print:left-auto print:top-auto print:z-auto print:max-h-none print:max-w-none print:translate-x-0 print:translate-y-0 print:overflow-visible print:border-0 print:bg-transparent print:p-0 print:shadow-none"
      >
        <InvoiceViewer invoiceId={invoiceId} />
      </DialogContent>
    </Dialog>
  );
}
