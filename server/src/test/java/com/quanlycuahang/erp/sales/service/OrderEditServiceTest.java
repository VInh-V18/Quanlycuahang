package com.quanlycuahang.erp.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryTransactionRepository;
import com.quanlycuahang.erp.partner.entity.Debt;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import com.quanlycuahang.erp.sales.dto.EditOrderLineRequest;
import com.quanlycuahang.erp.sales.dto.EditOrderRequest;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.entity.OrderItem;
import com.quanlycuahang.erp.sales.repository.OrderItemRepository;
import com.quanlycuahang.erp.sales.repository.OrderPaymentRepository;
import com.quanlycuahang.erp.sales.repository.OrderRepository;
import com.quanlycuahang.erp.system.entity.Branch;
import com.quanlycuahang.erp.system.service.SettingsService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Sua don da hoan tat (order:edit, tinh nang moi) — unit test cho OrderEditService, mirror phong
 * cach OrderValidationServiceTest/OrderServiceCancelTest (mock repository/guard).
 */
@ExtendWith(MockitoExtension.class)
class OrderEditServiceTest {

  @Mock private OrderRepository orderRepository;
  @Mock private OrderItemRepository orderItemRepository;
  @Mock private OrderPaymentRepository orderPaymentRepository;
  @Mock private ProductRepository productRepository;
  @Mock private InventoryRepository inventoryRepository;
  @Mock private InventoryTransactionRepository inventoryTransactionRepository;
  @Mock private DebtRepository debtRepository;
  @Mock private SettingsService settingsService;
  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private BranchAccessGuard branchAccessGuard;
  @Mock private OrderValidationService orderValidationService;

  private OrderEditService service;

  @BeforeEach
  void setUp() {
    service =
        new OrderEditService(
            orderRepository,
            orderItemRepository,
            orderPaymentRepository,
            productRepository,
            inventoryRepository,
            inventoryTransactionRepository,
            debtRepository,
            settingsService,
            currentUserProvider,
            branchAccessGuard,
            orderValidationService);
    org.mockito.Mockito.lenient()
        .when(settingsService.getBoolean(eq(10L), anyString(), anyBoolean()))
        .thenAnswer(inv -> inv.getArgument(2));
    org.mockito.Mockito.lenient()
        .when(settingsService.getBigDecimal(eq(10L), anyString(), any()))
        .thenAnswer(inv -> inv.getArgument(2));
  }

  private Order completedOrder() {
    Order order = new Order();
    order.setId(1L);
    Branch branch = new Branch();
    branch.setId(10L);
    order.setBranch(branch);
    order.setStatus("completed");
    order.setShippingFee(BigDecimal.ZERO);
    return order;
  }

  private Product product(Long id, BigDecimal vatRate) {
    Product product = new Product();
    product.setId(id);
    product.setName("SP " + id);
    product.setVatRate(vatRate);
    return product;
  }

  private Inventory inventory(BigDecimal stock, BigDecimal costPrice) {
    Inventory inventory = new Inventory();
    inventory.setStock(stock);
    inventory.setCostPrice(costPrice);
    return inventory;
  }

  private OrderItem existingItem(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
    OrderItem item = new OrderItem();
    Product product = product(productId, BigDecimal.ZERO);
    item.setProduct(product);
    item.setProductNameSnapshot(product.getName());
    item.setQuantity(quantity);
    item.setUnitPriceSnapshot(unitPrice);
    item.setCostPriceSnapshot(BigDecimal.valueOf(5_000));
    item.setLineTotal(unitPrice.multiply(quantity));
    item.setReturnedQuantity(BigDecimal.ZERO);
    return item;
  }

  private EditOrderLineRequest line(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
    EditOrderLineRequest line = new EditOrderLineRequest();
    line.setProductId(productId);
    line.setQuantity(quantity);
    line.setUnitPrice(unitPrice);
    line.setLineDiscountAmount(BigDecimal.ZERO);
    return line;
  }

  @Test
  void editOrderThrowsWhenOrderNotCompleted() {
    Order order = completedOrder();
    order.setStatus("partially_returned");
    when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

    EditOrderRequest request = new EditOrderRequest();
    request.setLines(List.of(line(2L, BigDecimal.ONE, BigDecimal.valueOf(10_000))));

    assertThatThrownBy(() -> service.editOrder(1L, request))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("hoàn tất");
  }

  @Test
  void editOrderThrowsWhenDuplicateProductInRequest() {
    Order order = completedOrder();
    when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

    EditOrderRequest request = new EditOrderRequest();
    request.setLines(
        List.of(
            line(2L, BigDecimal.ONE, BigDecimal.valueOf(10_000)),
            line(2L, BigDecimal.valueOf(2), BigDecimal.valueOf(10_000))));

    assertThatThrownBy(() -> service.editOrder(1L, request))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("1 dòng");
  }

  @Test
  void editOrderThrowsWhenRelatedDebtAlreadyPartiallyPaid() {
    Order order = completedOrder();
    when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
    Debt debt = new Debt();
    debt.setAmount(BigDecimal.valueOf(50_000));
    debt.setOriginalAmount(BigDecimal.valueOf(100_000));
    when(debtRepository.findByReferenceTypeAndReferenceIdOrderByIdAsc("order", 1L))
        .thenReturn(List.of(debt));

    EditOrderRequest request = new EditOrderRequest();
    request.setLines(List.of(line(2L, BigDecimal.ONE, BigDecimal.valueOf(10_000))));

    assertThatThrownBy(() -> service.editOrder(1L, request))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("công nợ");
  }

  @Test
  void editOrderThrowsWhenIncreasedQuantityExceedsAvailableStock() {
    Order order = completedOrder();
    when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
    when(debtRepository.findByReferenceTypeAndReferenceIdOrderByIdAsc("order", 1L))
        .thenReturn(List.of());
    OrderItem existing = existingItem(2L, BigDecimal.valueOf(2), BigDecimal.valueOf(10_000));
    when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(existing));
    Product product = product(2L, BigDecimal.ZERO);
    when(productRepository.findAllById(List.of(2L))).thenReturn(List.of(product));
    Inventory inv = inventory(BigDecimal.valueOf(1), BigDecimal.valueOf(5_000));
    inv.setProduct(product);
    when(inventoryRepository.findByBranchIdAndProductIdIn(10L, List.of(2L)))
        .thenReturn(List.of(inv));

    EditOrderRequest request = new EditOrderRequest();
    // tang tu 2 len 10, kho chi con 1 -> thieu
    request.setLines(List.of(line(2L, BigDecimal.valueOf(10), BigDecimal.valueOf(10_000))));

    assertThatThrownBy(() -> service.editOrder(1L, request))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("chỉ còn");
  }

  @Test
  void editOrderDeductsAdditionalStockWhenQuantityIncreases() {
    Order order = completedOrder();
    when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
    when(debtRepository.findByReferenceTypeAndReferenceIdOrderByIdAsc("order", 1L))
        .thenReturn(List.of());
    OrderItem existing = existingItem(2L, BigDecimal.valueOf(2), BigDecimal.valueOf(10_000));
    when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(existing));
    Product product = product(2L, BigDecimal.ZERO);
    when(productRepository.findAllById(List.of(2L))).thenReturn(List.of(product));
    Inventory inv = inventory(BigDecimal.valueOf(20), BigDecimal.valueOf(5_000));
    inv.setProduct(product);
    when(inventoryRepository.findByBranchIdAndProductIdIn(10L, List.of(2L)))
        .thenReturn(List.of(inv));
    when(inventoryRepository.findByProductIdAndBranchId(2L, 10L)).thenReturn(Optional.of(inv));
    when(orderPaymentRepository.findByOrderId(1L)).thenReturn(List.of());

    EditOrderRequest request = new EditOrderRequest();
    request.setLines(List.of(line(2L, BigDecimal.valueOf(5), BigDecimal.valueOf(10_000))));

    service.editOrder(1L, request);

    // tang tu 2 len 5 -> tru them 3
    assertThat(inv.getStock()).isEqualByComparingTo(BigDecimal.valueOf(17));
    assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50_000));
  }

  @Test
  void editOrderRestocksWhenQuantityDecreases() {
    Order order = completedOrder();
    when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
    when(debtRepository.findByReferenceTypeAndReferenceIdOrderByIdAsc("order", 1L))
        .thenReturn(List.of());
    OrderItem existing = existingItem(2L, BigDecimal.valueOf(5), BigDecimal.valueOf(10_000));
    when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(existing));
    Product product = product(2L, BigDecimal.ZERO);
    when(productRepository.findAllById(List.of(2L))).thenReturn(List.of(product));
    Inventory inv = inventory(BigDecimal.valueOf(10), BigDecimal.valueOf(5_000));
    inv.setProduct(product);
    when(inventoryRepository.findByBranchIdAndProductIdIn(10L, List.of(2L)))
        .thenReturn(List.of(inv));
    when(inventoryRepository.findByProductIdAndBranchId(2L, 10L)).thenReturn(Optional.of(inv));
    when(orderPaymentRepository.findByOrderId(1L)).thenReturn(List.of());

    EditOrderRequest request = new EditOrderRequest();
    request.setLines(List.of(line(2L, BigDecimal.valueOf(2), BigDecimal.valueOf(10_000))));

    service.editOrder(1L, request);

    // giam tu 5 xuong 2 -> hoan lai 3
    assertThat(inv.getStock()).isEqualByComparingTo(BigDecimal.valueOf(13));
    assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(20_000));
  }

  @Test
  void editOrderCreatesDebtWhenNewTotalExceedsAlreadyPaidAmount() {
    Order order = completedOrder();
    when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
    when(debtRepository.findByReferenceTypeAndReferenceIdOrderByIdAsc("order", 1L))
        .thenReturn(List.of());
    OrderItem existing = existingItem(2L, BigDecimal.valueOf(1), BigDecimal.valueOf(10_000));
    when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(existing));
    Product product = product(2L, BigDecimal.ZERO);
    when(productRepository.findAllById(List.of(2L))).thenReturn(List.of(product));
    Inventory inv = inventory(BigDecimal.valueOf(10), BigDecimal.valueOf(5_000));
    inv.setProduct(product);
    when(inventoryRepository.findByBranchIdAndProductIdIn(10L, List.of(2L)))
        .thenReturn(List.of(inv));
    when(inventoryRepository.findByProductIdAndBranchId(2L, 10L)).thenReturn(Optional.of(inv));
    // khach da tra du 10.000 luc mua (1 x 10.000), gio tang len 3 x 10.000 = 30.000 -> con no
    // 20.000
    com.quanlycuahang.erp.sales.entity.OrderPayment payment =
        new com.quanlycuahang.erp.sales.entity.OrderPayment();
    payment.setAmount(BigDecimal.valueOf(10_000));
    when(orderPaymentRepository.findByOrderId(1L)).thenReturn(List.of(payment));

    EditOrderRequest request = new EditOrderRequest();
    request.setLines(List.of(line(2L, BigDecimal.valueOf(3), BigDecimal.valueOf(10_000))));

    service.editOrder(1L, request);

    org.mockito.ArgumentCaptor<Debt> captor = org.mockito.ArgumentCaptor.forClass(Debt.class);
    org.mockito.Mockito.verify(debtRepository, org.mockito.Mockito.atLeastOnce())
        .save(captor.capture());
    Debt savedDebt = captor.getValue();
    assertThat(savedDebt.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(20_000));
  }
}
