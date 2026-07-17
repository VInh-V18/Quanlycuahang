package com.quanlycuahang.erp.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.metrics.BusinessMetrics;
import com.quanlycuahang.erp.common.sequence.NumberSequenceService;
import com.quanlycuahang.erp.common.web.IdempotencyService;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryTransactionRepository;
import com.quanlycuahang.erp.operation.repository.InvoiceRepository;
import com.quanlycuahang.erp.operation.repository.ShiftRepository;
import com.quanlycuahang.erp.partner.entity.Debt;
import com.quanlycuahang.erp.partner.repository.CustomerRepository;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import com.quanlycuahang.erp.promotion.service.VoucherService;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.entity.OrderItem;
import com.quanlycuahang.erp.sales.repository.OrderItemRepository;
import com.quanlycuahang.erp.sales.repository.OrderPaymentRepository;
import com.quanlycuahang.erp.sales.repository.OrderRepository;
import com.quanlycuahang.erp.system.entity.Branch;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import com.quanlycuahang.erp.system.service.SettingsService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Huy don (order:void, phuc dung tu ban truoc Prompt #2 bi xoa nham la dead code) — unit test rieng
 * cho cancelOrder(), mirror phong cach OrderValidationServiceTest (mock repository/guard, khong can
 * Spring context/DB that).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceCancelTest {

  @Mock private OrderRepository orderRepository;
  @Mock private OrderItemRepository orderItemRepository;
  @Mock private OrderPaymentRepository orderPaymentRepository;
  @Mock private ProductRepository productRepository;
  @Mock private InventoryRepository inventoryRepository;
  @Mock private InventoryTransactionRepository inventoryTransactionRepository;
  @Mock private DebtRepository debtRepository;
  @Mock private BranchRepository branchRepository;
  @Mock private CustomerRepository customerRepository;
  @Mock private ShiftRepository shiftRepository;
  @Mock private InvoiceRepository invoiceRepository;
  @Mock private VoucherService voucherService;
  @Mock private SettingsService settingsService;
  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private IdempotencyService idempotencyService;
  @Mock private NumberSequenceService numberSequenceService;
  @Mock private BranchAccessGuard branchAccessGuard;
  @Mock private OrderValidationService orderValidationService;
  @Mock private InventoryDeductionService inventoryDeductionService;
  @Mock private OrderPaymentService orderPaymentService;
  @Mock private OrderFinalizationService orderFinalizationService;
  @Mock private BusinessMetrics businessMetrics;

  private OrderService service;

  @BeforeEach
  void setUp() {
    service =
        new OrderService(
            orderRepository,
            orderItemRepository,
            orderPaymentRepository,
            productRepository,
            inventoryRepository,
            inventoryTransactionRepository,
            debtRepository,
            branchRepository,
            customerRepository,
            shiftRepository,
            invoiceRepository,
            voucherService,
            settingsService,
            currentUserProvider,
            idempotencyService,
            numberSequenceService,
            branchAccessGuard,
            orderValidationService,
            inventoryDeductionService,
            orderPaymentService,
            orderFinalizationService,
            businessMetrics);
  }

  private Order order(String status, Instant createdAt) {
    Order order = new Order();
    order.setId(1L);
    Branch branch = new Branch();
    branch.setId(10L);
    order.setBranch(branch);
    order.setStatus(status);
    order.setCreatedAt(createdAt);
    return order;
  }

  private Instant todayInAppZone() {
    return OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
  }

  private OrderItem item(Long productId, BigDecimal quantity, BigDecimal returnedQuantity) {
    OrderItem item = new OrderItem();
    Product product = new Product();
    product.setId(productId);
    item.setProduct(product);
    item.setQuantity(quantity);
    item.setReturnedQuantity(returnedQuantity);
    item.setCostPriceSnapshot(BigDecimal.valueOf(5_000));
    return item;
  }

  @Test
  void cancelOrderRestocksInventoryAndSetsStatusCancelled() {
    Order order = order("completed", todayInAppZone());
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(debtRepository.findByReferenceTypeAndReferenceIdOrderByIdAsc("order", 1L))
        .thenReturn(List.of());
    OrderItem item = item(2L, BigDecimal.valueOf(3), BigDecimal.ZERO);
    when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(item));
    Inventory inventory = new Inventory();
    inventory.setProduct(item.getProduct());
    inventory.setStock(BigDecimal.valueOf(7));
    when(inventoryRepository.findByBranchIdAndProductIdIn(10L, List.of(2L)))
        .thenReturn(List.of(inventory));

    var response = service.cancelOrder(1L);

    assertThat(response.getStatus()).isEqualTo("cancelled");
    assertThat(inventory.getStock()).isEqualByComparingTo(BigDecimal.valueOf(10));
    assertThat(order.getStatus()).isEqualTo("cancelled");
  }

  @Test
  void cancelOrderOnlyRestocksRemainingQuantityNotAlreadyReturned() {
    Order order = order("completed", todayInAppZone());
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(debtRepository.findByReferenceTypeAndReferenceIdOrderByIdAsc("order", 1L))
        .thenReturn(List.of());
    OrderItem item = item(2L, BigDecimal.valueOf(5), BigDecimal.valueOf(2));
    when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(item));
    Inventory inventory = new Inventory();
    inventory.setProduct(item.getProduct());
    inventory.setStock(BigDecimal.ZERO);
    when(inventoryRepository.findByBranchIdAndProductIdIn(10L, List.of(2L)))
        .thenReturn(List.of(inventory));

    service.cancelOrder(1L);

    assertThat(inventory.getStock()).isEqualByComparingTo(BigDecimal.valueOf(3));
  }

  @Test
  void cancelOrderThrowsWhenOrderCreatedOnPreviousDay() {
    Instant yesterday = todayInAppZone().minus(java.time.Duration.ofDays(2));
    Order order = order("completed", yesterday);
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> service.cancelOrder(1L))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("trong ngày");
  }

  @Test
  void cancelOrderThrowsWhenStatusNotCancellable() {
    Order order = order("fully_returned", todayInAppZone());
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> service.cancelOrder(1L))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("trạng thái");
  }

  @Test
  void cancelOrderThrowsWhenRelatedDebtAlreadyPartiallyPaid() {
    Order order = order("completed", todayInAppZone());
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    Debt debt = new Debt();
    debt.setAmount(BigDecimal.valueOf(50_000));
    debt.setOriginalAmount(BigDecimal.valueOf(100_000));
    when(debtRepository.findByReferenceTypeAndReferenceIdOrderByIdAsc("order", 1L))
        .thenReturn(List.of(debt));

    assertThatThrownBy(() -> service.cancelOrder(1L))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("công nợ");

    assertThat(debt.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50_000));
  }

  @Test
  void cancelOrderZeroesUnpaidDebtWhenNoPartialPaymentMade() {
    Order order = order("completed", todayInAppZone());
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    Debt debt = new Debt();
    debt.setAmount(BigDecimal.valueOf(100_000));
    debt.setOriginalAmount(BigDecimal.valueOf(100_000));
    when(debtRepository.findByReferenceTypeAndReferenceIdOrderByIdAsc("order", 1L))
        .thenReturn(List.of(debt));
    when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());

    service.cancelOrder(1L);

    assertThat(debt.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
  }
}
