package com.quanlycuahang.erp.sales.service;

import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.entity.InventoryTransaction;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryTransactionRepository;
import com.quanlycuahang.erp.partner.entity.Debt;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.sales.dto.ReturnItemRequest;
import com.quanlycuahang.erp.sales.dto.ReturnItemResponse;
import com.quanlycuahang.erp.sales.dto.ReturnRequest;
import com.quanlycuahang.erp.sales.dto.ReturnResponse;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.entity.OrderItem;
import com.quanlycuahang.erp.sales.repository.OrderItemRepository;
import com.quanlycuahang.erp.sales.repository.OrderRepository;
import com.quanlycuahang.erp.sales.repository.ReturnItemRepository;
import com.quanlycuahang.erp.sales.repository.ReturnRepository;
import com.quanlycuahang.erp.sales.statemachine.OrderStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tra hang theo hoa don goc (UC-12, B4): toi da = SL da mua - da tra; hoan tien theo don gia thuc
 * tra (sau moi CK da phan bo ve dong); nhap lai kho voi gia von tai thoi diem ban (snapshot tren
 * OrderItem, khong lay gia Product hien tai).
 */
@Service
public class ReturnService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final ReturnRepository returnRepository;
  private final ReturnItemRepository returnItemRepository;
  private final InventoryRepository inventoryRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final DebtRepository debtRepository;
  private final CurrentUserProvider currentUserProvider;
  private final BranchAccessGuard branchAccessGuard;

  public ReturnService(
      OrderRepository orderRepository,
      OrderItemRepository orderItemRepository,
      ReturnRepository returnRepository,
      ReturnItemRepository returnItemRepository,
      InventoryRepository inventoryRepository,
      InventoryTransactionRepository inventoryTransactionRepository,
      DebtRepository debtRepository,
      CurrentUserProvider currentUserProvider,
      BranchAccessGuard branchAccessGuard) {
    this.orderRepository = orderRepository;
    this.orderItemRepository = orderItemRepository;
    this.returnRepository = returnRepository;
    this.returnItemRepository = returnItemRepository;
    this.inventoryRepository = inventoryRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.debtRepository = debtRepository;
    this.currentUserProvider = currentUserProvider;
    this.branchAccessGuard = branchAccessGuard;
  }

  @Transactional
  public ReturnResponse createReturn(ReturnRequest request) {
    Order order =
        orderRepository
            .findById(request.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don hang"));
    branchAccessGuard.assertAccess(order.getBranch().getId());

    OrderStatus currentStatus = OrderStatus.fromValue(order.getStatus());
    if (currentStatus != OrderStatus.COMPLETED && currentStatus != OrderStatus.PARTIALLY_RETURNED) {
      throw new BusinessRuleException(
          "ORDER_NOT_RETURNABLE", "Don hang khong o trang thai co the tra hang");
    }

    com.quanlycuahang.erp.sales.entity.Return returnEntity =
        new com.quanlycuahang.erp.sales.entity.Return();
    returnEntity.setOrder(order);
    returnEntity.setRefundMethod(request.getRefundMethod());
    returnEntity.setTotalRefund(BigDecimal.ZERO);
    currentUserProvider.getCurrentUser().ifPresent(returnEntity::setCreatedBy);
    returnEntity = returnRepository.save(returnEntity);

    BigDecimal totalRefund = BigDecimal.ZERO;
    List<ReturnItemResponse> itemResponses = new ArrayList<>();

    for (ReturnItemRequest itemRequest : request.getItems()) {
      OrderItem orderItem =
          orderItemRepository
              .findById(itemRequest.getOrderItemId())
              .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay dong don hang"));
      if (!orderItem.getOrder().getId().equals(order.getId())) {
        throw new ResourceNotFoundException("Dong hang khong thuoc don nay");
      }

      BigDecimal remaining = orderItem.getQuantity().subtract(orderItem.getReturnedQuantity());
      if (itemRequest.getQuantity().compareTo(remaining) > 0) {
        throw new BusinessRuleException(
            "RETURN_QUANTITY_EXCEEDED",
            "So luong tra vuot qua so luong con lai cua " + orderItem.getProductNameSnapshot());
      }

      BigDecimal unitEffectivePrice =
          orderItem.getLineTotal().divide(orderItem.getQuantity(), 4, RoundingMode.HALF_UP);
      BigDecimal refundAmount =
          unitEffectivePrice.multiply(itemRequest.getQuantity()).setScale(0, RoundingMode.HALF_UP);

      orderItem.setReturnedQuantity(orderItem.getReturnedQuantity().add(itemRequest.getQuantity()));
      orderItemRepository.save(orderItem);

      Inventory inventory =
          inventoryRepository
              .findByProductIdAndBranchId(orderItem.getProduct().getId(), order.getBranch().getId())
              .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ton kho"));
      inventory.setStock(inventory.getStock().add(itemRequest.getQuantity()));
      inventoryRepository.saveAndFlush(inventory);

      InventoryTransaction transaction = new InventoryTransaction();
      transaction.setProduct(orderItem.getProduct());
      transaction.setBranch(order.getBranch());
      transaction.setType("customer_return");
      transaction.setQuantity(itemRequest.getQuantity());
      transaction.setUnitCost(orderItem.getCostPriceSnapshot());
      transaction.setReferenceType("return");
      transaction.setReferenceId(returnEntity.getId());
      currentUserProvider.getCurrentUser().ifPresent(transaction::setCreatedBy);
      inventoryTransactionRepository.save(transaction);

      com.quanlycuahang.erp.sales.entity.ReturnItem returnItem =
          new com.quanlycuahang.erp.sales.entity.ReturnItem();
      returnItem.setReturnEntity(returnEntity);
      returnItem.setOrderItem(orderItem);
      returnItem.setQuantity(itemRequest.getQuantity());
      returnItem.setRefundAmount(refundAmount);
      returnItemRepository.save(returnItem);

      totalRefund = totalRefund.add(refundAmount);

      ReturnItemResponse itemResponse = new ReturnItemResponse();
      itemResponse.setOrderItemId(orderItem.getId());
      itemResponse.setProductName(orderItem.getProductNameSnapshot());
      itemResponse.setQuantity(itemRequest.getQuantity());
      itemResponse.setRefundAmount(refundAmount);
      itemResponses.add(itemResponse);
    }

    returnEntity.setTotalRefund(totalRefund);
    returnRepository.save(returnEntity);

    boolean allFullyReturned =
        orderItemRepository.findByOrderId(order.getId()).stream()
            .allMatch(oi -> oi.getReturnedQuantity().compareTo(oi.getQuantity()) >= 0);
    OrderStatus newStatus =
        allFullyReturned ? OrderStatus.FULLY_RETURNED : OrderStatus.PARTIALLY_RETURNED;
    if (newStatus != currentStatus) {
      order.setStatus(newStatus.getValue());
      orderRepository.save(order);
    }

    // Neu don goc ban no (co Debt receivable gan voi don), giam cong no truoc; phan con lai
    // (neu co) coi nhu hoan tien mat/CK thuc te ngoai he thong theo request.refundMethod.
    List<Debt> relatedDebts =
        debtRepository.findByReferenceTypeAndReferenceId("order", order.getId());
    BigDecimal remainingRefund = totalRefund;
    for (Debt debt : relatedDebts) {
      if (remainingRefund.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }
      BigDecimal reduceBy = remainingRefund.min(debt.getAmount());
      debt.setAmount(debt.getAmount().subtract(reduceBy));
      debtRepository.save(debt);
      remainingRefund = remainingRefund.subtract(reduceBy);
    }

    ReturnResponse response = new ReturnResponse();
    response.setId(returnEntity.getId());
    response.setOrderId(order.getId());
    response.setTotalRefund(totalRefund);
    response.setRefundMethod(request.getRefundMethod());
    response.setItems(itemResponses);
    return response;
  }
}
