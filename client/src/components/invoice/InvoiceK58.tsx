import { QRCodeSVG } from "qrcode.react";
import { Money } from "@/components/common/Money";
import type { InvoiceDetail } from "@/lib/api/invoices";

export interface InvoiceK58Props {
  invoice: InvoiceDetail;
  lookupUrl: string;
}

/** Template in hóa đơn khổ K58 (giấy in nhiệt 58mm, máy in bill mini) — cùng cấu trúc dữ liệu với
 * InvoiceK80 nhưng bề rộng/cỡ chữ nhỏ hơn để vừa khổ giấy hẹp hơn. */
export function InvoiceK58({ invoice, lookupUrl }: InvoiceK58Props) {
  return (
    <div className="mx-auto w-[58mm] bg-white p-1.5 font-mono text-[9px] leading-tight text-black">
      <div className="text-center">
        <div className="text-xs font-bold">{invoice.storeName}</div>
        {invoice.storeTaxCode && <div>MST: {invoice.storeTaxCode}</div>}
        <div>{invoice.branchName}</div>
        <div>{invoice.branchAddress}</div>
        <div>ĐT: {invoice.branchPhone}</div>
      </div>
      <hr className="my-1 border-dashed border-black" />
      <div>HĐ: {invoice.invoiceNumber}</div>
      <div>Đơn: {invoice.orderNumber}</div>
      <div>{new Date(invoice.issuedAt).toLocaleString("vi-VN")}</div>
      <div>TN: {invoice.cashierName}</div>
      {invoice.customerName && <div>KH: {invoice.customerName}</div>}
      <hr className="my-1 border-dashed border-black" />
      {invoice.lines.map((line, i) => (
        <div key={i} className="mb-0.5">
          <div>{line.productName}</div>
          <div className="flex justify-between">
            <span>
              {line.quantity} x {line.unitPrice.toLocaleString("vi-VN")}
            </span>
            <span>{line.lineTotal.toLocaleString("vi-VN")}</span>
          </div>
        </div>
      ))}
      <hr className="my-1 border-dashed border-black" />
      <div className="flex justify-between">
        <span>Tổng hàng</span>
        <Money value={invoice.subtotalAmount} />
      </div>
      <div className="flex justify-between">
        <span>Chiết khấu</span>
        <Money value={invoice.discountAmount} />
      </div>
      {invoice.vatBreakdown.map((vat) => (
        <div className="flex justify-between" key={vat.vatRate}>
          <span>VAT {vat.vatRate}%</span>
          <Money value={vat.vatAmount} />
        </div>
      ))}
      <div className="flex justify-between border-t border-black pt-0.5 text-[10px] font-bold">
        <span>TỔNG TT</span>
        <Money value={invoice.totalAmount} />
      </div>
      {invoice.cashReceived != null && (
        <div className="flex justify-between">
          <span>Tiền mặt</span>
          <Money value={invoice.cashReceived} />
        </div>
      )}
      {invoice.changeAmount != null && (
        <div className="flex justify-between">
          <span>Tiền thừa</span>
          <Money value={invoice.changeAmount} />
        </div>
      )}
      <hr className="my-1 border-dashed border-black" />
      <div className="flex flex-col items-center gap-1 py-1">
        <QRCodeSVG value={lookupUrl} size={64} />
        <div>Mã tra cứu: {invoice.lookupCode}</div>
      </div>
      <div className="text-center font-bold">Cảm ơn quý khách!</div>
    </div>
  );
}
