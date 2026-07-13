package com.quanlycuahang.erp.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Prompt #2 (refactor OrderService god-class), buoc 3: unit test rieng cho collaborator moi tach —
 * mock repository/guard thay vi can Spring context/DB that (khac han IT test cua Prompt #1), vi gio
 * da tach nho du de test doc lap tung dieu kien.
 */
@ExtendWith(MockitoExtension.class)
class OrderValidationServiceTest {

  @Mock private BranchAccessGuard branchAccessGuard;
  @Mock private DebtRepository debtRepository;
  @Mock private BusinessMetrics businessMetrics;

  private OrderValidationService service;

  @BeforeEach
  void setUp() {
    service = new OrderValidationService(branchAccessGuard, debtRepository, businessMetrics);
  }

  @Test
  void assertBranchAccessDelegatesToGuard() {
    service.assertBranchAccess(5L);
    verify(branchAccessGuard).assertAccess(5L);
  }

  private OrderLineRequest line(Long productId, BigDecimal quantity) {
    OrderLineRequest line = new OrderLineRequest();
    line.setProductId(productId);
    line.setQuantity(quantity);
    line.setLineDiscountAmount(BigDecimal.ZERO);
    return line;
  }

  private Product product(Long id, BigDecimal sellPrice) {
    Product product = new Product();
    product.setId(id);
    product.setName("SP test " + id);
    product.setSellPrice(sellPrice);
    product.setVatRate(BigDecimal.ZERO);
    return product;
  }

  private Inventory inventory(BigDecimal stock) {
    Inventory inventory = new Inventory();
    inventory.setStock(stock);
    inventory.setCostPrice(BigDecimal.TEN);
    return inventory;
  }

  @Test
  void buildPricingLinesGroupsQuantityAcrossTwoLinesOfSameProductBeforeCheckingStock() {
    Product product = product(1L, BigDecimal.valueOf(10_000));
    Map<Long, Product> productsById = Map.of(1L, product);
    Map<Long, Inventory> inventoriesByProductId = Map.of(1L, inventory(BigDecimal.valueOf(10)));

    List<OrderLineRequest> lines =
        List.of(line(1L, BigDecimal.valueOf(8)), line(1L, BigDecimal.valueOf(8)));

    assertThatThrownBy(
            () ->
                service.buildPricingLinesAndAssertStock(
                    lines, productsById, inventoriesByProductId, false))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("chỉ còn");
  }

  @Test
  void buildPricingLinesReturnsInputsWhenStockSufficient() {
    Product product = product(1L, BigDecimal.valueOf(10_000));
    Map<Long, Product> productsById = Map.of(1L, product);
    Map<Long, Inventory> inventoriesByProductId = Map.of(1L, inventory(BigDecimal.valueOf(10)));
    List<OrderLineRequest> lines = List.of(line(1L, BigDecimal.valueOf(3)));

    List<OrderLineInput> result =
        service.buildPricingLinesAndAssertStock(lines, productsById, inventoriesByProductId, false);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(3));
  }

  @Test
  void buildPricingLinesThrowsResourceNotFoundWhenProductMissing() {
    List<OrderLineRequest> lines = List.of(line(99L, BigDecimal.ONE));

    assertThatThrownBy(
            () -> service.buildPricingLinesAndAssertStock(lines, Map.of(), Map.of(), false))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void buildPricingLinesAllowsExceedingStockWhenNegativeStockAllowed() {
    Product product = product(1L, BigDecimal.valueOf(10_000));
    Map<Long, Product> productsById = Map.of(1L, product);
    Map<Long, Inventory> inventoriesByProductId = Map.of(1L, inventory(BigDecimal.valueOf(2)));
    List<OrderLineRequest> lines = List.of(line(1L, BigDecimal.valueOf(5)));

    List<OrderLineInput> result =
        service.buildPricingLinesAndAssertStock(lines, productsById, inventoriesByProductId, true);

    assertThat(result).hasSize(1);
  }

  @Test
  void assertOrderReductionWithinSubtotalThrowsWhenReductionExceeds() {
    assertThatThrownBy(
            () ->
                service.assertOrderReductionWithinSubtotal(
                    BigDecimal.valueOf(150), BigDecimal.valueOf(100)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("chiết khấu");
  }

  @Test
  void assertOrderReductionWithinSubtotalPassesWhenEqual() {
    service.assertOrderReductionWithinSubtotal(BigDecimal.valueOf(100), BigDecimal.valueOf(100));
  }

  @Test
  void assertPricingMatchesExpectedThrowsWhenMismatch() {
    assertThatThrownBy(
            () ->
                service.assertPricingMatchesExpected(
                    BigDecimal.valueOf(100_000), BigDecimal.valueOf(99_999)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("không khớp");
  }

  @Test
  void assertUnpaidRequiresCustomerThrowsWhenNoCustomer() {
    assertThatThrownBy(() -> service.assertUnpaidRequiresCustomer(BigDecimal.valueOf(10_000), null))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("khách hàng");
  }

  @Test
  void assertUnpaidRequiresCustomerPassesWhenFullyPaid() {
    service.assertUnpaidRequiresCustomer(BigDecimal.ZERO, null);
  }

  @Test
  void assertWithinDebtLimitThrowsWhenExceedsConfiguredLimit() {
    Customer customer = new Customer();
    customer.setId(1L);
    customer.setDebtLimit(BigDecimal.valueOf(100_000));
    when(debtRepository.sumOutstandingByCustomerId(1L)).thenReturn(BigDecimal.valueOf(80_000));

    assertThatThrownBy(() -> service.assertWithinDebtLimit(customer, BigDecimal.valueOf(50_000)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("hạn mức nợ");
  }

  @Test
  void assertWithinDebtLimitDoesNotCheckWhenLimitNotConfigured() {
    Customer customer = new Customer();
    customer.setId(1L);
    customer.setDebtLimit(BigDecimal.ZERO);

    service.assertWithinDebtLimit(customer, BigDecimal.valueOf(999_999_999));

    verify(debtRepository, org.mockito.Mockito.never()).sumOutstandingByCustomerId(any());
  }

  @Test
  void assertWithinDebtLimitPassesWhenUnderLimit() {
    Customer customer = new Customer();
    customer.setId(2L);
    customer.setDebtLimit(BigDecimal.valueOf(500_000));
    when(debtRepository.sumOutstandingByCustomerId(eq(2L))).thenReturn(BigDecimal.valueOf(100_000));

    service.assertWithinDebtLimit(customer, BigDecimal.valueOf(200_000));
  }
}
