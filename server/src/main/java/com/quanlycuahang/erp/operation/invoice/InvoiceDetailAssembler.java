package com.quanlycuahang.erp.operation.invoice;

import com.quanlycuahang.erp.operation.dto.InvoiceDetailResponse;
import com.quanlycuahang.erp.operation.dto.InvoiceLineResponse;
import com.quanlycuahang.erp.operation.dto.InvoicePaymentResponse;
import com.quanlycuahang.erp.operation.dto.VatBreakdownResponse;
import com.quanlycuahang.erp.operation.entity.Invoice;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.entity.OrderItem;
import com.quanlycuahang.erp.sales.entity.OrderPayment;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Lap rap InvoiceDetailResponse tu cac Entity da tai san — Java thuan, khong phu thuoc Spring/DB,
 * de unit test truc tiep (Gate Phase 9: "endpoint JSON hoa don co test snapshot") ma khong can
 * Testcontainers/ApplicationContext, cung phong cach voi OrderPricingService (Phase 8).
 */
public final class InvoiceDetailAssembler {

  private InvoiceDetailAssembler() {}

  public static InvoiceDetailResponse assemble(
      Invoice invoice,
      Order order,
      List<OrderItem> items,
      List<OrderPayment> payments,
      String storeName,
      String storeTaxCode,
      String cashierName,
      String customerName,
      String customerPhone,
      String customerEmail) {

    InvoiceDetailResponse response = new InvoiceDetailResponse();
    response.setInvoiceNumber(invoice.getInvoiceNumber());
    response.setIssuedAt(invoice.getIssuedAt());
    response.setLookupCode(invoice.getLookupCode());
    response.setQrPayload(invoice.getQrPayload());

    response.setOrderNumber(order.getOrderNumber());
    response.setOrderStatus(order.getStatus());

    response.setStoreName(storeName);
    response.setStoreTaxCode(storeTaxCode);
    response.setBranchName(order.getBranch().getName());
    response.setBranchAddress(order.getBranch().getAddress());
    response.setBranchPhone(order.getBranch().getPhone());

    response.setCashierName(cashierName);
    response.setCustomerName(customerName);
    response.setCustomerPhone(customerPhone);
    response.setCustomerEmail(customerEmail);

    response.setLines(items.stream().map(InvoiceDetailAssembler::toLine).toList());
    response.setVatBreakdown(buildVatBreakdown(items));
    response.setPayments(
        payments.stream()
            .map(p -> new InvoicePaymentResponse(p.getMethod(), p.getAmount()))
            .toList());

    response.setSubtotalAmount(order.getSubtotalAmount());
    response.setDiscountAmount(order.getDiscountAmount());
    response.setVatAmount(order.getVatAmount());
    response.setRoundingAdjustment(order.getRoundingAdjustment());
    response.setTotalAmount(order.getTotalAmount());
    response.setCashReceived(order.getCashReceived());
    response.setChangeAmount(order.getChangeAmount());

    return response;
  }

  private static InvoiceLineResponse toLine(OrderItem item) {
    InvoiceLineResponse line = new InvoiceLineResponse();
    line.setProductName(item.getProductNameSnapshot());
    line.setUnit(item.getProduct().getUnit());
    line.setUnitPrice(item.getUnitPriceSnapshot());
    line.setQuantity(item.getQuantity());
    line.setDiscountAmount(item.getDiscountAmount());
    line.setVatRate(item.getVatRateSnapshot());
    line.setVatAmount(item.getVatAmount());
    line.setLineTotal(item.getLineTotal());
    return line;
  }

  /**
   * Gop cac dong theo thue suat — dung hoa don VN thuong tach rieng tung muc thue thay vi 1 tong
   * VAT duy nhat. taxableAmount = lineTotal - vatAmount (gia da gom VAT, B4).
   */
  private static List<VatBreakdownResponse> buildVatBreakdown(List<OrderItem> items) {
    Map<BigDecimal, BigDecimal[]> byRate = new TreeMap<>(Comparator.naturalOrder());
    for (OrderItem item : items) {
      BigDecimal rate = item.getVatRateSnapshot();
      BigDecimal taxable = item.getLineTotal().subtract(item.getVatAmount());
      BigDecimal[] totals =
          byRate.computeIfAbsent(rate, r -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
      totals[0] = totals[0].add(taxable);
      totals[1] = totals[1].add(item.getVatAmount());
    }
    return byRate.entrySet().stream()
        .map(e -> new VatBreakdownResponse(e.getKey(), e.getValue()[0], e.getValue()[1]))
        .toList();
  }
}
