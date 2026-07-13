package com.quanlycuahang.erp.sales.service;

import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.common.metrics.BusinessMetrics;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.partner.entity.Customer;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.sales.dto.OrderLineRequest;
import com.quanlycuahang.erp.sales.pricing.OrderLineInput;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Prompt #2 (refactor OrderService god-class): tach toan bo dieu kien chan/validate cua
 * createOrder() ra khoi orchestrator — KHONG doi hanh vi, chi doi vi tri (xem docs/PROJECT_STATE.md
 * muc Prompt #2 cho so do truoc/sau). Moi phuong thuc giu dung nguyen van logic + thong diep loi
 * nhu OrderService cu.
 */
@Service
public class OrderValidationService {

  private static final Logger log = LoggerFactory.getLogger(OrderValidationService.class);

  private final BranchAccessGuard branchAccessGuard;
  private final DebtRepository debtRepository;
  private final BusinessMetrics businessMetrics;

  public OrderValidationService(
      BranchAccessGuard branchAccessGuard,
      DebtRepository debtRepository,
      BusinessMetrics businessMetrics) {
    this.branchAccessGuard = branchAccessGuard;
    this.debtRepository = debtRepository;
    this.businessMetrics = businessMetrics;
  }

  public void assertBranchAccess(Long branchId) {
    branchAccessGuard.assertAccess(branchId);
  }

  /**
   * Gop tong so luong yeu cau THEO productId truoc khi kiem tra ton kho (chan tach dong de qua kiem
   * tra), roi tra ve danh sach OrderLineInput dung cho OrderPricingService — giu NGUYEN 1 vong lap
   * duy nhat nhu ban goc de khong doi thu tu nem loi (NOT_FOUND truoc hay OUT_OF_STOCK truoc) giua
   * cac dong.
   */
  public List<OrderLineInput> buildPricingLinesAndAssertStock(
      List<OrderLineRequest> lines,
      Map<Long, Product> productsById,
      Map<Long, Inventory> inventoriesByProductId,
      boolean allowNegativeStock) {
    Map<Long, BigDecimal> totalQuantityByProductId =
        lines.stream()
            .collect(
                Collectors.groupingBy(
                    OrderLineRequest::getProductId,
                    Collectors.reducing(
                        BigDecimal.ZERO, OrderLineRequest::getQuantity, BigDecimal::add)));

    List<OrderLineInput> pricingLines = new ArrayList<>();
    for (OrderLineRequest lineRequest : lines) {
      Product product = productsById.get(lineRequest.getProductId());
      if (product == null) {
        throw new ResourceNotFoundException("Không tìm thấy sản phẩm");
      }
      Inventory inventory = inventoriesByProductId.get(product.getId());
      if (inventory == null) {
        throw new BusinessRuleException(
            "PRODUCT_OUT_OF_STOCK",
            "Sản phẩm " + product.getName() + " chưa có tồn kho tại chi nhánh này");
      }

      BigDecimal totalRequestedQuantity = totalQuantityByProductId.get(product.getId());
      if (!allowNegativeStock && inventory.getStock().compareTo(totalRequestedQuantity) < 0) {
        throw new BusinessRuleException(
            "PRODUCT_OUT_OF_STOCK",
            "Sản phẩm " + product.getName() + " chỉ còn " + inventory.getStock() + " trong kho");
      }

      pricingLines.add(
          new OrderLineInput(
              product.getId(),
              product.getSellPrice(),
              lineRequest.getQuantity(),
              lineRequest.getLineDiscountAmount(),
              product.getVatRate()));
    }
    return pricingLines;
  }

  public void assertOrderReductionWithinSubtotal(
      BigDecimal orderLevelReduction, BigDecimal roughSubtotal) {
    if (orderLevelReduction.compareTo(roughSubtotal) > 0) {
      throw new BusinessRuleException(
          "ORDER_DISCOUNT_EXCEEDS_SUBTOTAL",
          "Tổng chiết khấu (đơn hàng + voucher) không được vượt quá giá trị đơn hàng");
    }
  }

  public void assertPricingMatchesExpected(BigDecimal grandTotal, BigDecimal expectedTotalAmount) {
    if (grandTotal.compareTo(expectedTotalAmount) != 0) {
      // Su kien nhay cam (Prompt #8, P2 quan sat) - FE/BE tinh tien lech nhau co the la bug tinh
      // gia thuc su (khong chi FE gui cu) - can theo doi tan suat qua metric, khong chi bao loi
      // 400 roi thoi.
      Long tenantId = TenantContext.get();
      log.warn(
          "ORDER_PRICE_MISMATCH expected={} actual={} tenantId={}",
          expectedTotalAmount,
          grandTotal,
          tenantId);
      businessMetrics.recordPriceMismatch(tenantId);
      throw new BusinessRuleException(
          "ORDER_PRICE_MISMATCH",
          "Số tiền tính toán không khớp, vui lòng tải lại giỏ hàng và thử lại");
    }
  }

  public void assertUnpaidRequiresCustomer(BigDecimal unpaid, Customer customer) {
    if (unpaid.compareTo(BigDecimal.ZERO) > 0 && customer == null) {
      throw new BusinessRuleException(
          "ORDER_UNPAID_REQUIRES_CUSTOMER", "Bán nợ phải chọn khách hàng cụ thể");
    }
  }

  /**
   * debtLimit = 0 nghia la chua duoc cau hinh han muc (mac dinh khi tao khach hang) -> khong chan,
   * giu nguyen hanh vi cu; chi enforce khi nguoi dung da chu dong dat han muc > 0 cho khach hang
   * do.
   */
  public void assertWithinDebtLimit(Customer customer, BigDecimal unpaid) {
    if (unpaid.compareTo(BigDecimal.ZERO) > 0
        && customer.getDebtLimit() != null
        && customer.getDebtLimit().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal existingDebt = debtRepository.sumOutstandingByCustomerId(customer.getId());
      if (existingDebt.add(unpaid).compareTo(customer.getDebtLimit()) > 0) {
        throw new BusinessRuleException(
            "CUSTOMER_DEBT_LIMIT_EXCEEDED", "Khách hàng đã vượt hạn mức nợ cho phép");
      }
    }
  }
}
