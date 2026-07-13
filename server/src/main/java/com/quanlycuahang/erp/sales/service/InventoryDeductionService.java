package com.quanlycuahang.erp.sales.service;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Prompt #2 (refactor OrderService god-class): tach buoc ghi OrderItem + tru ton kho +
 * InventoryTransaction cua createOrder() — KHONG doi hanh vi (van saveAndFlush tung dong Inventory
 * de bat xung dot @Version NGAY, van batch InventoryTransaction bang 1 saveAll sau vong lap).
 *
 * <p>KHONG tai su dung cho ReturnService/StockTakeService du roadmap co goi y — 2 luong do la HOAN
 * kho (dau + thay vi -) va dung snapshot khac (costPriceSnapshot cua OrderItem cu, khong phai gia
 * hien tai), ep dung chung se phai them nhieu tham so dieu kien (cong/tru, nguon gia von) lam class
 * nay phuc tap hon la tach rieng — giu moi luong doc lap, it rui ro hon.
 */
@Service
public class InventoryDeductionService {

  private final OrderItemRepository orderItemRepository;
  private final InventoryRepository inventoryRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final CurrentUserProvider currentUserProvider;

  public InventoryDeductionService(
      OrderItemRepository orderItemRepository,
      InventoryRepository inventoryRepository,
      InventoryTransactionRepository inventoryTransactionRepository,
      CurrentUserProvider currentUserProvider) {
    this.orderItemRepository = orderItemRepository;
    this.inventoryRepository = inventoryRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.currentUserProvider = currentUserProvider;
  }

  public List<OrderItemResponse> deductStockAndCreateItems(
      Order order,
      OrderPricingResult pricing,
      Map<Long, Product> productsById,
      Map<Long, Inventory> inventoriesByProductId) {
    List<OrderItemResponse> itemResponses = new ArrayList<>();
    // Gom insert InventoryTransaction thanh 1 saveAll sau vong lap - chi Inventory moi can
    // saveAndFlush RIENG tung dong (phat hien xung dot @Version ngay lap tuc), transaction kho
    // khong can flush som.
    List<InventoryTransaction> stockMovements = new ArrayList<>();
    var cashier = currentUserProvider.getCurrentUser();
    for (OrderLineResult lineResult : pricing.getLines()) {
      Product product = productsById.get(lineResult.getProductId());
      Inventory inventory = inventoriesByProductId.get(lineResult.getProductId());

      OrderItem item = new OrderItem();
      item.setOrder(order);
      item.setProduct(product);
      item.setProductNameSnapshot(product.getName());
      item.setUnitPriceSnapshot(lineResult.getUnitPrice());
      item.setCostPriceSnapshot(inventory.getCostPrice());
      item.setVatRateSnapshot(product.getVatRate());
      item.setQuantity(lineResult.getQuantity());
      item.setDiscountAmount(lineResult.getDiscountAmount());
      item.setVatAmount(lineResult.getVatAmount());
      item.setLineTotal(lineResult.getLineTotal());
      orderItemRepository.save(item);

      // Chong oversell: @Version tren Inventory, saveAndFlush de phat hien xung dot NGAY, khong
      // cho doi den commit (B4 — chong oversell 2 lop).
      inventory.setStock(inventory.getStock().subtract(lineResult.getQuantity()));
      inventoryRepository.saveAndFlush(inventory);

      InventoryTransaction transaction = new InventoryTransaction();
      transaction.setProduct(product);
      transaction.setBranch(order.getBranch());
      transaction.setType("sale");
      transaction.setQuantity(lineResult.getQuantity().negate());
      transaction.setUnitCost(inventory.getCostPrice());
      transaction.setReferenceType("order");
      transaction.setReferenceId(order.getId());
      cashier.ifPresent(transaction::setCreatedBy);
      stockMovements.add(transaction);

      OrderItemResponse itemResponse = new OrderItemResponse();
      itemResponse.setId(item.getId());
      itemResponse.setProductId(product.getId());
      itemResponse.setProductName(product.getName());
      itemResponse.setUnitPrice(lineResult.getUnitPrice());
      itemResponse.setQuantity(lineResult.getQuantity());
      itemResponse.setDiscountAmount(lineResult.getDiscountAmount());
      itemResponse.setVatAmount(lineResult.getVatAmount());
      itemResponse.setLineTotal(lineResult.getLineTotal());
      itemResponses.add(itemResponse);
    }
    inventoryTransactionRepository.saveAll(stockMovements);
    return itemResponses;
  }
}
