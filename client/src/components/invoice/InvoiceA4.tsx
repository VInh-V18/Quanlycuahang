import { QRCodeSVG } from "qrcode.react";
import { Money } from "@/components/common/Money";
import type { InvoiceDetail } from "@/lib/api/invoices";

export interface InvoiceA4Props {
  invoice: InvoiceDetail;
  lookupUrl: string;
}

/** Template in hóa đơn khổ A4 — dùng cho hóa đơn gửi khách doanh nghiệp/lưu trữ giấy. */
export function InvoiceA4({ invoice, lookupUrl }: InvoiceA4Props) {
  return (
    <div className="mx-auto max-w-[210mm] bg-white p-[15mm] text-black">
      <div className="flex items-start justify-between border-b border-black pb-4">
        <div>
          <div className="text-xl font-bold">{invoice.storeName}</div>
          {invoice.storeTaxCode && <div>Mã số thuế: {invoice.storeTaxCode}</div>}
          <div>{invoice.branchName}</div>
          <div>{invoice.branchAddress}</div>
          <div>Điện thoại: {invoice.branchPhone}</div>
        </div>
        <QRCodeSVG value={lookupUrl} size={96} />
      </div>

      <h1 className="my-4 text-center text-2xl font-bold uppercase">Hóa đơn bán hàng</h1>

      <div className="mb-4 grid grid-cols-2 gap-x-8 text-sm">
        <div>
          <div>Số hóa đơn: {invoice.invoiceNumber}</div>
          <div>Mã đơn hàng: {invoice.orderNumber}</div>
          <div>Ngày: {new Date(invoice.issuedAt).toLocaleString("vi-VN")}</div>
          <div>Thu ngân: {invoice.cashierName}</div>
        </div>
        <div>
          {invoice.customerName && <div>Khách hàng: {invoice.customerName}</div>}
          {invoice.customerPhone && <div>Điện thoại: {invoice.customerPhone}</div>}
          <div>Mã tra cứu: {invoice.lookupCode}</div>
        </div>
      </div>

      <table className="w-full border-collapse text-sm">
        <thead>
          <tr className="border-b-2 border-black text-left">
            <th className="py-1">Sản phẩm</th>
            <th className="text-right">ĐVT</th>
            <th className="text-right">SL</th>
            <th className="text-right">Đơn giá</th>
            <th className="text-right">CK</th>
            <th className="text-right">VAT</th>
            <th className="text-right">Thành tiền</th>
          </tr>
        </thead>
        <tbody>
          {invoice.lines.map((line, i) => (
            <tr key={i} className="border-b border-gray-300">
              <td className="py-1">{line.productName}</td>
              <td className="text-right">{line.unit}</td>
              <td className="text-right">{line.quantity}</td>
              <td className="text-right">{line.unitPrice.toLocaleString("vi-VN")}</td>
              <td className="text-right">{line.discountAmount.toLocaleString("vi-VN")}</td>
              <td className="text-right">{line.vatRate}%</td>
              <td className="text-right">{line.lineTotal.toLocaleString("vi-VN")}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="mt-4 flex justify-end">
        <table className="w-72 text-sm">
          <tbody>
            <tr>
              <td>Tổng hàng</td>
              <td className="text-right">
                <Money value={invoice.subtotalAmount} />
              </td>
            </tr>
            <tr>
              <td>Chiết khấu</td>
              <td className="text-right">
                <Money value={invoice.discountAmount} />
              </td>
            </tr>
            {invoice.vatBreakdown.map((vat) => (
              <tr key={vat.vatRate}>
                <td>Thuế GTGT {vat.vatRate}% (trên {vat.taxableAmount.toLocaleString("vi-VN")})</td>
                <td className="text-right">
                  <Money value={vat.vatAmount} />
                </td>
              </tr>
            ))}
            {invoice.roundingAdjustment !== 0 && (
              <tr>
                <td>Làm tròn</td>
                <td className="text-right">
                  <Money value={invoice.roundingAdjustment} showSign />
                </td>
              </tr>
            )}
            <tr className="border-t border-black text-base font-bold">
              <td>Tổng thanh toán</td>
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

      <div className="mt-6 text-sm">
        Phương thức thanh toán: {invoice.payments.map((p) => p.method).join(", ")}
      </div>

      <div className="mt-12 flex justify-between text-center text-sm">
        <div>
          <div className="font-semibold">Khách hàng</div>
          <div className="mt-12">(Ký, ghi rõ họ tên)</div>
        </div>
        <div>
          <div className="font-semibold">Người bán hàng</div>
          <div className="mt-12">(Ký, ghi rõ họ tên)</div>
        </div>
      </div>
    </div>
  );
}
