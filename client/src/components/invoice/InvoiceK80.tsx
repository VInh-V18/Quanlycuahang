import { QRCodeSVG } from "qrcode.react";
import { Money } from "@/components/common/Money";
import type { InvoiceDetail } from "@/lib/api/invoices";
import { formatInvoiceDate, formatPrintedAt } from "@/components/invoice/format";

export interface InvoiceK80Props {
  invoice: InvoiceDetail;
}

function phoneLine(branchPhone: string, storePhone: string): string {
  if (storePhone && storePhone !== branchPhone) return `${branchPhone} - ${storePhone}`;
  return branchPhone;
}

/** Template in hóa đơn khổ K80 (giấy in nhiệt 80mm, phổ biến máy in bill POS) — chỉ hiển thị khi
 * in (`@page` do trang cha `InvoicePrintPage` quản lý theo format đang chọn). */
export function InvoiceK80({ invoice }: InvoiceK80Props) {
  return (
    <div className="mx-auto w-[80mm] bg-white p-2 font-mono text-[11px] leading-tight text-black">
      <div className="text-center">
        <div className="text-sm font-bold uppercase">{invoice.storeName}</div>
        {invoice.storeTaxCode && <div>MST: {invoice.storeTaxCode}</div>}
        <div>Địa chỉ: {invoice.branchAddress}</div>
        <div>Điện thoại: {phoneLine(invoice.branchPhone, invoice.storePhone)}</div>
      </div>
      <hr className="my-1 border-dashed border-black" />
      <div className="text-center font-bold">HOÁ ĐƠN BÁN HÀNG</div>
      <div>Số HĐ: {invoice.invoiceNumber}</div>
      <div>{formatInvoiceDate(invoice.issuedAt)}</div>
      <hr className="my-1 border-dashed border-black" />
      <div className="flex items-start justify-between gap-2">
        <div>
          <div>Khách hàng: {invoice.customerName ?? "Khách lẻ"}</div>
          {invoice.customerAddress && <div>Địa chỉ: {invoice.customerAddress}</div>}
          {invoice.customerPhone && <div>Điện thoại: {invoice.customerPhone}</div>}
          <div>Ghi chú: {invoice.note ?? ""}</div>
        </div>
        <div className="shrink-0 text-right">
          <div>TN: {invoice.cashierName}</div>
          <div>In lúc: {formatPrintedAt(new Date().toISOString())}</div>
        </div>
      </div>
      <hr className="my-1 border-dashed border-black" />
      <table className="w-full">
        <thead>
          <tr>
            <th className="text-left">TT</th>
            <th className="text-left">Sản phẩm</th>
            <th className="text-right">SL</th>
            <th className="text-right">TT</th>
          </tr>
        </thead>
        <tbody>
          {invoice.lines.map((line, i) => (
            <tr key={i}>
              <td colSpan={4} className="pt-1">
                {i + 1}. {line.productName}
              </td>
            </tr>
          ))}
          {invoice.lines.map((line, i) => (
            <tr key={`qty-${i}`}>
              <td />
              <td>{line.unitPrice.toLocaleString("vi-VN")} x</td>
              <td className="text-right">{line.quantity}</td>
              <td className="text-right">{line.lineTotal.toLocaleString("vi-VN")}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <hr className="my-1 border-dashed border-black" />
      <div className="flex justify-between">
        <span>Tổng số lượng</span>
        <span>{invoice.totalQuantity}</span>
      </div>
      <div className="flex justify-between">
        <span>Tổng tiền hàng</span>
        <Money value={invoice.subtotalAmount} />
      </div>
      <div className="flex justify-between">
        <span>Phí Ship</span>
        <Money value={invoice.shippingFee} />
      </div>
      <div className="flex justify-between">
        <span>Chiết khấu</span>
        <Money value={invoice.discountAmount} />
      </div>
      {invoice.vatAmount > 0 &&
        invoice.vatBreakdown.map((vat) => (
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
      <div className="flex items-center justify-center gap-2 py-1">
        {invoice.qrPayload ? (
          <QRCodeSVG value={invoice.qrPayload} size={80} />
        ) : (
          invoice.bankQrImageUrl && (
            <img
              src={invoice.bankQrImageUrl}
              alt="QR chuyển khoản"
              className="h-20 w-20 object-contain"
            />
          )
        )}
        <div className="text-center text-[9px]">Mã tra cứu: {invoice.lookupCode}</div>
      </div>
      <hr className="my-1 border-dashed border-black" />
      <div className="text-center text-[10px]">
        <div>Quý khách vui lòng kiểm tra trước khi nhận hàng</div>
        <div>Không đổi, trả khi đã nhận hàng. Xin cảm ơn quý khách!</div>
      </div>
      <div className="mt-4 grid grid-cols-3 gap-1 text-center text-[10px] font-semibold">
        <div>KHÁCH HÀNG</div>
        <div>NGƯỜI GIAO</div>
        <div>THỦ KHO</div>
      </div>
    </div>
  );
}
