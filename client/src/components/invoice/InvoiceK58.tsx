import { QRCodeSVG } from "qrcode.react";
import { Money } from "@/components/common/Money";
import type { InvoiceDetail } from "@/lib/api/invoices";
import { formatInvoiceDate, formatPrintedAt } from "@/components/invoice/format";

export interface InvoiceK58Props {
  invoice: InvoiceDetail;
}

function phoneLine(branchPhone: string, storePhone: string): string {
  if (storePhone && storePhone !== branchPhone) return `${branchPhone} - ${storePhone}`;
  return branchPhone;
}

/** Template in hóa đơn khổ K58 (giấy in nhiệt 58mm, máy in bill mini) — cùng cấu trúc dữ liệu với
 * InvoiceK80 nhưng bề rộng/cỡ chữ nhỏ hơn để vừa khổ giấy hẹp hơn. */
export function InvoiceK58({ invoice }: InvoiceK58Props) {
  return (
    <div className="mx-auto w-[58mm] bg-white p-1.5 font-mono text-[9px] leading-tight text-black">
      <div className="text-center">
        <div className="text-xs font-bold uppercase">{invoice.storeName}</div>
        {invoice.storeTaxCode && <div>MST: {invoice.storeTaxCode}</div>}
        <div>{invoice.branchAddress}</div>
        <div>ĐT: {phoneLine(invoice.branchPhone, invoice.storePhone)}</div>
      </div>
      <hr className="my-1 border-dashed border-black" />
      <div className="text-center font-bold">HÓA ĐƠN BÁN HÀNG</div>
      <div>Số HĐ: {invoice.invoiceNumber}</div>
      <div>{formatInvoiceDate(invoice.issuedAt)}</div>
      <hr className="my-1 border-dashed border-black" />
      <div className="flex items-start justify-between gap-2">
        <div>
          <div>KH: {invoice.customerName ?? "Khách lẻ"}</div>
          {invoice.customerAddress && <div>Địa chỉ: {invoice.customerAddress}</div>}
          {invoice.customerPhone && <div>ĐT KH: {invoice.customerPhone}</div>}
          {invoice.note && <div>Ghi chú: {invoice.note}</div>}
        </div>
        <div className="shrink-0 text-right">
          <div>TN: {invoice.cashierName}</div>
          <div>In lúc: {formatPrintedAt(new Date().toISOString())}</div>
        </div>
      </div>
      <hr className="my-1 border-dashed border-black" />
      {invoice.lines.map((line, i) => (
        <div key={i} className="mb-0.5">
          <div>
            {i + 1}. {line.productName}
          </div>
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
        <span>Tổng SL</span>
        <span>{invoice.totalQuantity}</span>
      </div>
      <div className="flex justify-between">
        <span>Tổng hàng</span>
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
      <div className="flex justify-between border-t border-black pt-0.5 text-[10px] font-bold">
        <span>TỔNG</span>
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
      <div className="flex items-center justify-center gap-2 py-1">
        {invoice.qrPayload ? (
          <QRCodeSVG value={invoice.qrPayload} size={64} />
        ) : (
          invoice.bankQrImageUrl && (
            <img
              src={invoice.bankQrImageUrl}
              alt="QR chuyển khoản"
              className="h-16 w-16 object-contain"
            />
          )
        )}
        <div>Mã tra cứu: {invoice.lookupCode}</div>
      </div>
      <div className="text-center">Vui lòng kiểm tra hàng trước khi nhận.</div>
      <div className="text-center font-bold">Cảm ơn quý khách!</div>
      
    </div>
  );
}
