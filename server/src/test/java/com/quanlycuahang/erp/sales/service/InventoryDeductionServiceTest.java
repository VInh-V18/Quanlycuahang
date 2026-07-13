package com.quanlycuahang.erp.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.entity.InventoryTransaction;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryTransactionRepository;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.sales.dto.OrderItemResponse;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.entity.OrderItem;
import com.quanlycuahang.erp.sales.pricing.OrderLineResult;
import com.quanlycuahang.erp.sales.pricing.OrderPricingResult;
import com.quanlycuahang.erp.sales.repository.OrderItemRepository;
import com.quanlycuahang.erp.system.entity.Branch;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Prompt #2, buoc 3: unit test InventoryDeductionService voi repository mock. */
@ExtendWith(MockitoExtension.class)
class InventoryDeductionServiceTest {

  @Mock private OrderItemRepository orderItemRepository;
  @Mock private InventoryRepository inventoryRepository;
  @Mock private InventoryTransactionRepository inventoryTransactionRepository;
  @Mock private CurrentUserProvider currentUserProvider;

  private InventoryDeductionService service;

  @BeforeEach
  void setUp() {
    service =
        new InventoryDeductionService(
            orderItemRepository,
            inventoryRepository,
            inventoryTransactionRepository,
            currentUserProvider);
    when(currentUserProvider.getCurrentUser()).thenReturn(Optional.empty());
    // Mo phong hanh vi that cua Hibernate: save() 1 entity moi se gan ID sinh tu DB len CHINH doi
    // tuong truyen vao - Mockito mac dinh KHONG lam viec nay, phai gia lap thu cong de
    // item.getId() khac null nhu san xuat that.
    when(orderItemRepository.save(any(OrderItem.class)))
        .thenAnswer(
            invocation -> {
              OrderItem item = invocation.getArgument(0);
              item.setId(100L);
              return item;
            });
  }

  @Test
  void deductsStockWritesOrderItemsAndBatchesInventoryTransactions() {
    Branch branch = new Branch();
    branch.setId(1L);
    Order order = new Order();
    order.setId(10L);
    order.setBranch(branch);
    order.setOrderNumber("HD-000001");

    Product product = new Product();
    product.setId(1L);
    product.setName("Táo");
    product.setVatRate(BigDecimal.ZERO);

    Inventory inventory = new Inventory();
    inventory.setStock(BigDecimal.valueOf(20));
    inventory.setCostPrice(BigDecimal.valueOf(30_000));

    OrderLineResult lineResult =
        new OrderLineResult(
            1L,
            BigDecimal.valueOf(50_000),
            BigDecimal.valueOf(3),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.valueOf(150_000));
    OrderPricingResult pricing =
        new OrderPricingResult(
            List.of(lineResult),
            BigDecimal.valueOf(150_000),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.valueOf(150_000),
            null);

    List<OrderItemResponse> responses =
        service.deductStockAndCreateItems(
            order, pricing, Map.of(1L, product), Map.of(1L, inventory));

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).getId()).isEqualTo(100L);
    assertThat(responses.get(0).getLineTotal()).isEqualByComparingTo(BigDecimal.valueOf(150_000));

    // Ton kho phai bi TRU dung so luong (20 - 3 = 17), saveAndFlush (khong phai save thuong) de
    // bat xung dot @Version ngay lap tuc.
    assertThat(inventory.getStock()).isEqualByComparingTo(BigDecimal.valueOf(17));
    verify(inventoryRepository).saveAndFlush(inventory);

    ArgumentCaptor<List<InventoryTransaction>> captor = ArgumentCaptor.forClass(List.class);
    verify(inventoryTransactionRepository).saveAll(captor.capture());
    List<InventoryTransaction> movements = captor.getValue();
    assertThat(movements).hasSize(1);
    assertThat(movements.get(0).getType()).isEqualTo("sale");
    // So luong ghi AM (xuat kho), khong phai duong.
    assertThat(movements.get(0).getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(-3));
    assertThat(movements.get(0).getReferenceType()).isEqualTo("order");
    assertThat(movements.get(0).getReferenceId()).isEqualTo(10L);
  }

  @Test
  void tagsInventoryTransactionWithCurrentCashierWhenPresent() {
    Branch branch = new Branch();
    branch.setId(1L);
    Order order = new Order();
    order.setId(11L);
    order.setBranch(branch);

    User cashier = new User();
    cashier.setId(7L);
    when(currentUserProvider.getCurrentUser()).thenReturn(Optional.of(cashier));

    Product product = new Product();
    product.setId(2L);
    product.setName("Cam");
    product.setVatRate(BigDecimal.ZERO);
    Inventory inventory = new Inventory();
    inventory.setStock(BigDecimal.valueOf(5));
    inventory.setCostPrice(BigDecimal.valueOf(10_000));

    OrderLineResult lineResult =
        new OrderLineResult(
            2L,
            BigDecimal.valueOf(20_000),
            BigDecimal.ONE,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.valueOf(20_000));
    OrderPricingResult pricing =
        new OrderPricingResult(
            List.of(lineResult),
            BigDecimal.valueOf(20_000),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.valueOf(20_000),
            null);

    service.deductStockAndCreateItems(order, pricing, Map.of(2L, product), Map.of(2L, inventory));

    ArgumentCaptor<List<InventoryTransaction>> captor = ArgumentCaptor.forClass(List.class);
    verify(inventoryTransactionRepository).saveAll(captor.capture());
    assertThat(captor.getValue().get(0).getCreatedBy()).isEqualTo(cashier);
  }
}
