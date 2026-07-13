package com.quanlycuahang.erp.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.TestDataFactory;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.partner.entity.Customer;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import com.quanlycuahang.erp.sales.dto.OrderCreateRequest;
import com.quanlycuahang.erp.sales.dto.OrderLineRequest;
import com.quanlycuahang.erp.sales.dto.OrderPaymentRequest;
import com.quanlycuahang.erp.sales.dto.OrderResponse;
import com.quanlycuahang.erp.sales.dto.ReturnItemRequest;
import com.quanlycuahang.erp.sales.dto.ReturnRequest;
import com.quanlycuahang.erp.sales.dto.ReturnResponse;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.repository.OrderRepository;
import com.quanlycuahang.erp.sales.service.OrderService;
import com.quanlycuahang.erp.sales.service.ReturnService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test Prompt #1 (P0): ReturnService.createReturn(). Dung that OrderService de tao don
 * goc (tai su dung code da duoc kiem chung o OrderServiceCreateOrderIT) roi moi goi ReturnService -
 * kiem tra dung luong UC-04 -> UC-12 that thay vi dung du lieu Order/OrderItem dung tay (de sai
 * lech voi thuc te OrderService tao ra).
 */
class ReturnServiceCreateReturnIT extends AbstractIntegrationTest {

  @Autowired private OrderService orderService;
  @Autowired private ReturnService returnService;
  @Autowired private TestDataFactory testDataFactory;
  @Autowired private InventoryRepository inventoryRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private OrderRepository orderRepository;
  @Autowired private DebtRepository debtRepository;

  private OrderResponse createCompletedOrder(
      TestDataFactory.TestTenant tenant, Product product, BigDecimal quantity, Customer customer) {
    OrderCreateRequest request = new OrderCreateRequest();
    request.setBranchId(tenant.branchA().getId());
    if (customer != null) {
      request.setCustomerId(customer.getId());
    }
    OrderLineRequest line = new OrderLineRequest();
    line.setProductId(product.getId());
    line.setQuantity(quantity);
    line.setLineDiscountAmount(BigDecimal.ZERO);
    request.setLines(List.of(line));
    BigDecimal total = product.getSellPrice().multiply(quantity);
    request.setExpectedTotalAmount(total);
    if (customer == null) {
      OrderPaymentRequest payment = new OrderPaymentRequest();
      payment.setMethod("cash");
      payment.setAmount(total);
      request.setPayments(List.of(payment));
    } else {
      request.setPayments(List.of()); // ban no toan bo
    }
    return orderService.createOrder(request, "idem-setup-" + System.nanoTime());
  }

  @Test
  void blocksReturningMoreThanRemainingQuantity() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantReturnExceed");
    actingAsUser(tenant.owner());
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(50_000),
            BigDecimal.valueOf(30_000),
            BigDecimal.valueOf(10));
    OrderResponse order = createCompletedOrder(tenant, product, BigDecimal.valueOf(3), null);
    Long orderItemId = order.getItems().get(0).getId();

    ReturnRequest returnRequest = new ReturnRequest();
    returnRequest.setOrderId(order.getId());
    ReturnItemRequest item = new ReturnItemRequest();
    item.setOrderItemId(orderItemId);
    item.setQuantity(BigDecimal.valueOf(4)); // vuot 3 da mua
    returnRequest.setItems(List.of(item));
    returnRequest.setRefundMethod("cash");

    assertThatThrownBy(() -> returnService.createReturn(returnRequest))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("vượt quá");
  }

  @Test
  void refundUsesPriceSnapshotNotCurrentProductPrice() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantReturnSnapshot");
    actingAsUser(tenant.owner());
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(100_000),
            BigDecimal.valueOf(60_000),
            BigDecimal.valueOf(10));
    OrderResponse order = createCompletedOrder(tenant, product, BigDecimal.valueOf(2), null);
    Long orderItemId = order.getItems().get(0).getId();

    // Doi gia ban SAU khi da tao don - hoan tra PHAI dung gia luc BAN (100.000), khong phai gia
    // moi (200.000).
    product.setSellPrice(BigDecimal.valueOf(200_000));
    productRepository.save(product);

    ReturnRequest returnRequest = new ReturnRequest();
    returnRequest.setOrderId(order.getId());
    ReturnItemRequest item = new ReturnItemRequest();
    item.setOrderItemId(orderItemId);
    item.setQuantity(BigDecimal.ONE);
    returnRequest.setItems(List.of(item));
    returnRequest.setRefundMethod("cash");

    ReturnResponse response = returnService.createReturn(returnRequest);

    assertThat(response.getTotalRefund()).isEqualByComparingTo(BigDecimal.valueOf(100_000));

    Inventory inventory =
        inventoryRepository
            .findByProductIdAndBranchId(product.getId(), tenant.branchA().getId())
            .orElseThrow();
    // Ton kho: mua 10, ban 2 (con 8), tra 1 (con 9).
    assertThat(inventory.getStock()).isEqualByComparingTo(BigDecimal.valueOf(9));
  }

  @Test
  void partialReturnSetsOrderStatusPartiallyReturnedFullReturnSetsFullyReturned() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantReturnStatus");
    actingAsUser(tenant.owner());
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(50_000),
            BigDecimal.valueOf(30_000),
            BigDecimal.valueOf(10));
    OrderResponse order = createCompletedOrder(tenant, product, BigDecimal.valueOf(4), null);
    Long orderItemId = order.getItems().get(0).getId();

    ReturnRequest partial = new ReturnRequest();
    partial.setOrderId(order.getId());
    ReturnItemRequest partialItem = new ReturnItemRequest();
    partialItem.setOrderItemId(orderItemId);
    partialItem.setQuantity(BigDecimal.valueOf(2));
    partial.setItems(List.of(partialItem));
    partial.setRefundMethod("cash");
    returnService.createReturn(partial);

    Order afterPartial = orderRepository.findById(order.getId()).orElseThrow();
    assertThat(afterPartial.getStatus()).isEqualTo("partially_returned");

    ReturnRequest rest = new ReturnRequest();
    rest.setOrderId(order.getId());
    ReturnItemRequest restItem = new ReturnItemRequest();
    restItem.setOrderItemId(orderItemId);
    restItem.setQuantity(BigDecimal.valueOf(2));
    rest.setItems(List.of(restItem));
    rest.setRefundMethod("cash");
    returnService.createReturn(rest);

    Order afterFull = orderRepository.findById(order.getId()).orElseThrow();
    assertThat(afterFull.getStatus()).isEqualTo("fully_returned");
  }

  @Test
  void returnOnUnpaidOrderReducesDebtBeforeExternalRefund() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantReturnDebt");
    actingAsUser(tenant.owner());
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(100_000),
            BigDecimal.valueOf(60_000),
            BigDecimal.valueOf(10));
    Customer customer =
        testDataFactory.createCustomerWithDebtLimit(tenant.tenant(), BigDecimal.ZERO);
    // Ban no toan bo 2 don vi = 200.000.
    OrderResponse order = createCompletedOrder(tenant, product, BigDecimal.valueOf(2), customer);
    Long orderItemId = order.getItems().get(0).getId();

    ReturnRequest returnRequest = new ReturnRequest();
    returnRequest.setOrderId(order.getId());
    ReturnItemRequest item = new ReturnItemRequest();
    item.setOrderItemId(orderItemId);
    item.setQuantity(BigDecimal.ONE); // tra 1/2 -> hoan 100.000
    returnRequest.setItems(List.of(item));
    returnRequest.setRefundMethod("cash");
    returnService.createReturn(returnRequest);

    // Dung finder KHONG khoa (khong @Lock) de doc lai ket qua trong test - phuong thuc co @Lock
    // PESSIMISTIC_WRITE (dung that trong ReturnService) doi hoi transaction dang mo, se nem
    // TransactionRequiredException neu goi truc tiep tu test (ngoai transaction cua service).
    var debts = debtRepository.findByReferenceTypeAndReferenceId("order", order.getId());
    assertThat(debts).hasSize(1);
    // No goc 200.000, hoan 100.000 giam thang vao no -> con du 100.000 (khong phai hoan tien mat
    // ngoai he thong vi da tru vao no truoc).
    assertThat(debts.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
  }
}
