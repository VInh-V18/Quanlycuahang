import { QRCodeSVG } from "qrcode.react";
import { Money } from "@/components/common/Money";
import type { InvoiceDetail } from "@/lib/api/invoices";
import { formatInvoiceDate, formatPrintedAt } from "@/components/invoice/format";

export interface InvoiceA4Props {
  invoice: InvoiceDetail;
}

function phoneLine(branchPhone: string, storePhone: string): string {
  if (storePhone && storePhone !== branchPhone) return `${branchPhone} - ${storePhone}`;
  return branchPhone;
}

/** Template in hóa đơn khổ A4 — dùng cho hóa đơn gửi khách doanh nghiệp/lưu trữ giấy. */
export function InvoiceA4({ invoice }: InvoiceA4Props) {
  return (
    <div className="mx-auto max-w-[210mm] bg-white p-[15mm] text-xs text-black">
      <div className="text-center">
        <div className="text-base font-bold uppercase">{invoice.storeName}</div>
        {invoice.storeTaxCode && <div>Mã số thuế: {invoice.storeTaxCode}</div>}
        <div>Địa chỉ: {invoice.branchAddress}</div>
        <div>Điện thoại: {phoneLine(invoice.branchPhone, invoice.storePhone)}</div>
      </div>

      <h1 className="my-4 text-center text-lg font-bold uppercase">Hóa đơn bán hàng</h1>
      <div className="mb-4 text-center">
        <div>Số HĐ: {invoice.invoiceNumber}</div>
        <div>{formatInvoiceDate(invoice.issuedAt)}</div>
      </div>

      <div className="mb-4 grid grid-cols-2 gap-x-8">
        <div>
          <div>Khách hàng: {invoice.customerName ?? "Khách lẻ"}</div>
          {invoice.customerAddress && <div>Địa chỉ: {invoice.customerAddress}</div>}
          {invoice.customerPhone && <div>Điện thoại: {invoice.customerPhone}</div>}
          <div>Ghi chú: {invoice.note ?? ""}</div>
        </div>
        <div className="text-right">
          <div>TN: {invoice.cashierName}</div>
          <div>In lúc: {formatPrintedAt(new Date().toISOString())}</div>
        </div>
      </div>

      <table className="w-full border-collapse">
        <thead>
          <tr className="border-b-2 border-black text-left">
            <th className="py-1">TT</th>
            <th>Sản phẩm</th>
            <th className="text-right">ĐVT</th>
            <th className="text-right">SL</th>
            <th className="text-right">Đơn giá</th>
            <th className="text-right">Thành tiền</th>
          </tr>
        </thead>
        <tbody>
          {invoice.lines.map((line, i) => (
            <tr key={i} className="border-b border-gray-300">
              <td className="py-1">{i + 1}</td>
              <td>{line.productName}</td>
              <td className="text-right">{line.unit}</td>
              <td className="text-right">{line.quantity}</td>
              <td className="text-right">{line.unitPrice.toLocaleString("vi-VN")}</td>
              <td className="text-right">{line.lineTotal.toLocaleString("vi-VN")}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="mt-4 flex justify-between">
        <div className="flex items-center gap-2">
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
        </div>

        <table className="w-72">
          <tbody>
            <tr>
              <td>Tổng số lượng</td>
              <td className="text-right">{invoice.totalQuantity}</td>
            </tr>
            <tr>
              <td>Tổng tiền hàng</td>
              <td className="text-right">
                <Money value={invoice.subtotalAmount} />
              </td>
            </tr>
            <tr>
              <td>Phí Ship</td>
              <td className="text-right">
                <Money value={invoice.shippingFee} />
              </td>
            </tr>
            <tr>
              <td>Chiết khấu</td>
              <td className="text-right">
                <Money value={invoice.discountAmount} />
              </td>
            </tr>
            {invoice.roundingAdjustment !== 0 && (
              <tr>
                <td>Làm tròn</td>
                <td className="text-right">
                  <Money value={invoice.roundingAdjustment} showSign />
                </td>
              </tr>
            )}
            <tr className="border-t border-black text-sm font-bold">
              <td>TỔNG</td>
              <td className="text-right">
                <Money value={invoice.totalAmount} />
              </td>
            </tr>
            {invoice.cashReceived != null && (
              <tr>
                <td>Khách đưa</td>
                <td className="text-right">
                  <Money value={invoice.cashReceived} />
                </td>
              </tr>
            )}
            {invoice.changeAmount != null && (
              <tr>
                <td>Tiền thừa</td>
                <td className="text-right">
                  <Money value={invoice.changeAmount} />
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-4 text-center text-gray-600">
        <div>Quý khách vui lòng kiểm tra trước khi nhận hàng</div>
        <div>Không đổi, trả khi đã nhận hàng. Xin cảm ơn quý khách!</div>
      </div>

      <div className="mt-12 grid grid-cols-3 gap-4 text-center">
        <div>
          <div className="font-semibold">KHÁCH HÀNG</div>
      
        </div>
        <div>
          <div className="font-semibold">NGƯỜI GIAO</div>
          
        </div>
        <div>
          <div className="font-semibold">THỦ KHO</div>
          
        </div>
      </div>
    </div>
  );
}
