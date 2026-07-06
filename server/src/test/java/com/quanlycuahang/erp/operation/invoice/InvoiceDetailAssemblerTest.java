package com.quanlycuahang.erp.operation.invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.operation.dto.InvoiceDetailResponse;
import com.quanlycuahang.erp.operation.entity.Invoice;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.entity.OrderItem;
import com.quanlycuahang.erp.sales.entity.OrderPayment;
import com.quanlycuahang.erp.system.entity.Branch;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * "Snapshot" cua don hang that da verify qua API o Phase 8 (HD-000021: 3 SP, CK dong, CK don, tien
 * thua) — dam bao InvoiceDetailAssembler tai tao dung tung dong tien khi doc lai hoa don cu, dac
 * biet la buoc gop VAT theo thue suat (Gate Phase 9).
 */
class InvoiceDetailAssemblerTest {

  @Test
  void assemblesFullInvoiceMatchingOriginalOrderExactly() {
    Branch branch = new Branch();
    branch.setName("Chi nhanh Quan 1");
    branch.setAddress("12 Nguyen Hue, Quan 1, TP.HCM");
    branch.setPhone("0281234567");

    User cashier = new User();
    cashier.setFullName("Nguyen Van Chu");

    Order order = new Order();
    order.setOrderNumber("HD-000021");
    order.setBranch(branch);
    order.setCashier(cashier);
    order.setStatus("completed");
    order.setSubtotalAmount(new BigDecimal("144000"));
    order.setDiscountAmount(new BigDecimal("5000"));
    order.setVatAmount(new BigDecimal("9962"));
    order.setRoundingAdjustment(BigDecimal.ZERO);
    order.setTotalAmount(new BigDecimal("139000"));
    order.setCashReceived(new BigDecimal("150000"));
    order.setChangeAmount(new BigDecimal("11000"));

    OrderItem item1 =
        lineItem("Coca-Cola lon 330ml", "Lon", "10000", "5", "3667", "10", "4212", "46333");
    OrderItem item2 =
        lineItem("Sua tuoi Vinamilk hop 1 lit", "Hop", "32000", "2", "2222", "5", "2942", "61778");
    OrderItem item3 =
        lineItem(
            "Nuoc tang luc Red Bull lon 250ml", "Lon", "11000", "3", "2111", "10", "2808", "30889");
    List<OrderItem> items = List.of(item1, item2, item3);

    OrderPayment payment = new OrderPayment();
    payment.setMethod("cash");
    payment.setAmount(new BigDecimal("150000"));

    Invoice invoice = new Invoice();
    invoice.setInvoiceNumber("INV-000021");
    invoice.setIssuedAt(OffsetDateTime.parse("2026-07-05T19:34:52+07:00"));
    invoice.setLookupCode("0997B6B1");

    InvoiceDetailResponse response =
        InvoiceDetailAssembler.assemble(
            invoice,
            order,
            items,
            List.of(payment),
            "Cua hang Quan Ly",
            "",
            "Nguyen Van Chu",
            null,
            null,
            null);

    assertEquals("INV-000021", response.getInvoiceNumber());
    assertEquals("0997B6B1", response.getLookupCode());
    assertEquals("HD-000021", response.getOrderNumber());
    assertEquals("completed", response.getOrderStatus());
    assertEquals("Cua hang Quan Ly", response.getStoreName());
    assertEquals("Chi nhanh Quan 1", response.getBranchName());
    assertEquals("Nguyen Van Chu", response.getCashierName());
    assertNull(response.getCustomerName());

    assertEquals(3, response.getLines().size());
    assertEquals(new BigDecimal("46333"), response.getLines().get(0).getLineTotal());

    assertEquals(new BigDecimal("144000"), response.getSubtotalAmount());
    assertEquals(new BigDecimal("5000"), response.getDiscountAmount());
    assertEquals(new BigDecimal("9962"), response.getVatAmount());
    assertEquals(new BigDecimal("139000"), response.getTotalAmount());
    assertEquals(new BigDecimal("150000"), response.getCashReceived());
    assertEquals(new BigDecimal("11000"), response.getChangeAmount());

    assertEquals(1, response.getPayments().size());
    assertEquals("cash", response.getPayments().get(0).getMethod());
    assertEquals(new BigDecimal("150000"), response.getPayments().get(0).getAmount());

    // VAT boc tach theo thue suat: 5% gop dong 2, 10% gop dong 1+3 — tinh tay tung dong.
    assertEquals(2, response.getVatBreakdown().size());
    var vat5 = response.getVatBreakdown().get(0);
    assertEquals(new BigDecimal("5"), vat5.getVatRate());
    assertEquals(new BigDecimal("58836"), vat5.getTaxableAmount()); // 61778 - 2942
    assertEquals(new BigDecimal("2942"), vat5.getVatAmount());

    var vat10 = response.getVatBreakdown().get(1);
    assertEquals(new BigDecimal("10"), vat10.getVatRate());
    assertEquals(new BigDecimal("70202"), vat10.getTaxableAmount()); // (46333-4212) + (30889-2808)
    assertEquals(new BigDecimal("7020"), vat10.getVatAmount()); // 4212 + 2808

    // Tong taxable + VAT phai khop chinh xac tong tien don goc — khong lech 1 dong.
    BigDecimal totalTaxable =
        response.getVatBreakdown().stream()
            .map(v -> v.getTaxableAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalVat =
        response.getVatBreakdown().stream()
            .map(v -> v.getVatAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertEquals(response.getTotalAmount(), totalTaxable.add(totalVat));
  }

  private static OrderItem lineItem(
      String name,
      String unit,
      String unitPrice,
      String qty,
      String discount,
      String vatRate,
      String vatAmount,
      String lineTotal) {
    Product product = new Product();
    product.setUnit(unit);

    OrderItem item = new OrderItem();
    item.setProduct(product);
    item.setProductNameSnapshot(name);
    item.setUnitPriceSnapshot(new BigDecimal(unitPrice));
    item.setQuantity(new BigDecimal(qty));
    item.setDiscountAmount(new BigDecimal(discount));
    item.setVatRateSnapshot(new BigDecimal(vatRate));
    item.setVatAmount(new BigDecimal(vatAmount));
    item.setLineTotal(new BigDecimal(lineTotal));
    return item;
  }
}
