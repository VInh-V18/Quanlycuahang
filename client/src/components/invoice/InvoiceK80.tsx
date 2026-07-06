import { QRCodeSVG } from "qrcode.react";
import { Money } from "@/components/common/Money";
import type { InvoiceDetail } from "@/lib/api/invoices";

export interface InvoiceK80Props {
  invoice: InvoiceDetail;
  lookupUrl: string;
}

/** Template in hóa đơn khổ K80 (giấy in nhiệt 80mm, phổ biến máy in bill POS) — chỉ hiển thị khi
 * in (`@page` do trang cha `InvoicePrintPage` quản lý theo format đang chọn). */
export function InvoiceK80({ invoice, lookupUrl }: InvoiceK80Props) {
  return (
    <div className="mx-auto w-[80mm] bg-white p-2 font-mono text-[11px] leading-tight text-black">
      <div className="text-center">
        <div className="text-sm font-bold">{invoice.storeName}</div>
        {invoice.storeTaxCode && <div>MST: {invoice.storeTaxCode}</div>}
        <div>{invoice.branchName}</div>
        <div>{invoice.branchAddress}</div>
        <div>ĐT: {invoice.branchPhone}</div>
      </div>
      <hr className="my-1 border-dashed border-black" />
      <div>Số HĐ: {invoice.invoiceNumber}</div>
      <div>Đơn: {invoice.orderNumber}</div>
      <div>Ngày: {new Date(invoice.issuedAt).toLocaleString("vi-VN")}</div>
      <div>Thu ngân: {invoice.cashierName}</div>
      {invoice.customerName && <div>Khách hàng: {invoice.customerName}</div>}
      <hr className="my-1 border-dashed border-black" />
      <table className="w-full">
        <thead>
          <tr>
            <th className="text-left">SP</th>
            <th className="text-right">SL</th>
            <th className="text-right">TT</th>
          </tr>
        </thead>
        <tbody>
          {invoice.lines.map((line, i) => (
            <tr key={i}>
              <td colSpan={3}>{line.productName}</td>
            </tr>
          ))}
          {invoice.lines.map((line, i) => (
            <tr key={`qty-${i}`}>
              <td>{line.unitPrice.toLocaleString("vi-VN")} x</td>
              <td className="text-right">{line.quantity}</td>
              <td className="text-right">{line.lineTotal.toLocaleString("vi-VN")}</td>
            </tr>
          ))}
        </tbody>
      </table>
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
      <div className="flex justify-between font-bold">
        <span>TỔNG</span>
        <Money value={invoice.totalAmount} />
      </div>
      {invoice.cashReceived != null && (
        <div className="flex justify-between">
          <span>Khách đưa</span>
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
      <div className="flex flex-col items-center gap-1 py-2">
        <QRCodeSVG value={lookupUrl} size={80} />
        <div>Mã tra cứu: {invoice.lookupCode}</div>
      </div>
      <div className="text-center">Cảm ơn quý khách!</div>
    </div>
  );
}
