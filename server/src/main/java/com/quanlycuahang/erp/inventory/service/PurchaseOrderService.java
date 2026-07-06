package com.quanlycuahang.erp.inventory.service;

import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderItemRequest;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderRequest;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderResponse;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.entity.InventoryBatch;
import com.quanlycuahang.erp.inventory.entity.InventoryTransaction;
import com.quanlycuahang.erp.inventory.entity.PurchaseOrder;
import com.quanlycuahang.erp.inventory.entity.PurchaseOrderItem;
import com.quanlycuahang.erp.inventory.mapper.PurchaseOrderMapper;
import com.quanlycuahang.erp.inventory.repository.InventoryBatchRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryTransactionRepository;
import com.quanlycuahang.erp.inventory.repository.PurchaseOrderItemRepository;
import com.quanlycuahang.erp.inventory.repository.PurchaseOrderRepository;
import com.quanlycuahang.erp.partner.entity.Debt;
import com.quanlycuahang.erp.partner.entity.Supplier;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.partner.repository.SupplierRepository;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import com.quanlycuahang.erp.system.entity.Branch;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhap kho (UC-05): tinh lai gia von binh quan gia quyen dung cong thuc B4 trong 1 transaction, ghi
 * cong no NCC neu mua thieu.
 */
@Service
public class PurchaseOrderService {

  private final PurchaseOrderRepository purchaseOrderRepository;
  private final PurchaseOrderItemRepository purchaseOrderItemRepository;
  private final InventoryRepository inventoryRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final InventoryBatchRepository inventoryBatchRepository;
  private final SupplierRepository supplierRepository;
  private final BranchRepository branchRepository;
  private final ProductRepository productRepository;
  private final DebtRepository debtRepository;
  private final PurchaseOrderMapper purchaseOrderMapper;
  private final CurrentUserProvider currentUserProvider;

  public PurchaseOrderService(
      PurchaseOrderRepository purchaseOrderRepository,
      PurchaseOrderItemRepository purchaseOrderItemRepository,
      InventoryRepository inventoryRepository,
      InventoryTransactionRepository inventoryTransactionRepository,
      InventoryBatchRepository inventoryBatchRepository,
      SupplierRepository supplierRepository,
      BranchRepository branchRepository,
      ProductRepository productRepository,
      DebtRepository debtRepository,
      PurchaseOrderMapper purchaseOrderMapper,
      CurrentUserProvider currentUserProvider) {
    this.purchaseOrderRepository = purchaseOrderRepository;
    this.purchaseOrderItemRepository = purchaseOrderItemRepository;
    this.inventoryRepository = inventoryRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.inventoryBatchRepository = inventoryBatchRepository;
    this.supplierRepository = supplierRepository;
    this.branchRepository = branchRepository;
    this.productRepository = productRepository;
    this.debtRepository = debtRepository;
    this.purchaseOrderMapper = purchaseOrderMapper;
    this.currentUserProvider = currentUserProvider;
  }

  @Transactional
  public PurchaseOrderResponse create(PurchaseOrderRequest request) {
    Supplier supplier =
        supplierRepository
            .findById(request.getSupplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nha cung cap"));
    Branch branch =
        branchRepository
            .findById(request.getBranchId())
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay chi nhanh"));

    PurchaseOrder purchaseOrder = new PurchaseOrder();
    purchaseOrder.setSupplier(supplier);
    purchaseOrder.setBranch(branch);
    purchaseOrder.setStatus("completed");
    purchaseOrder.setTotalAmount(BigDecimal.ZERO);
    currentUserProvider.getCurrentUser().ifPresent(purchaseOrder::setCreatedBy);
    purchaseOrder = purchaseOrderRepository.save(purchaseOrder);

    BigDecimal total = BigDecimal.ZERO;
    List<PurchaseOrderItem> savedItems = new ArrayList<>();

    for (PurchaseOrderItemRequest itemRequest : request.getItems()) {
      Product product =
          productRepository
              .findById(itemRequest.getProductId())
              .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham"));

      Inventory inventory =
          inventoryRepository
              .findByProductIdAndBranchIdForUpdate(product.getId(), branch.getId())
              .orElseGet(
                  () -> {
                    Inventory created = new Inventory();
                    created.setProduct(product);
                    created.setBranch(branch);
                    created.setStock(BigDecimal.ZERO);
                    created.setCostPrice(BigDecimal.ZERO);
                    return created;
                  });

      BigDecimal newCost =
          AverageCostService.calculateNewCost(
              inventory.getStock(),
              inventory.getCostPrice(),
              itemRequest.getQuantity(),
              itemRequest.getUnitPrice());
      inventory.setStock(inventory.getStock().add(itemRequest.getQuantity()));
      inventory.setCostPrice(newCost);
      inventoryRepository.save(inventory);

      PurchaseOrderItem item = new PurchaseOrderItem();
      item.setPurchaseOrder(purchaseOrder);
      item.setProduct(product);
      item.setQuantity(itemRequest.getQuantity());
      item.setUnitPrice(itemRequest.getUnitPrice());
      item = purchaseOrderItemRepository.save(item);
      savedItems.add(item);

      if (itemRequest.getBatchCode() != null && !itemRequest.getBatchCode().isBlank()) {
        InventoryBatch batch = new InventoryBatch();
        batch.setProduct(product);
        batch.setBranch(branch);
        batch.setPurchaseOrderItem(item);
        batch.setBatchCode(itemRequest.getBatchCode());
        batch.setExpiryDate(itemRequest.getExpiryDate());
        batch.setQuantity(itemRequest.getQuantity());
        batch.setCostPrice(itemRequest.getUnitPrice());
        inventoryBatchRepository.save(batch);
      }

      InventoryTransaction transaction = new InventoryTransaction();
      transaction.setProduct(product);
      transaction.setBranch(branch);
      transaction.setType("purchase");
      transaction.setQuantity(itemRequest.getQuantity());
      transaction.setUnitCost(itemRequest.getUnitPrice());
      transaction.setReferenceType("purchase_order");
      transaction.setReferenceId(purchaseOrder.getId());
      currentUserProvider.getCurrentUser().ifPresent(transaction::setCreatedBy);
      inventoryTransactionRepository.save(transaction);

      total = total.add(itemRequest.getQuantity().multiply(itemRequest.getUnitPrice()));
    }

    BigDecimal discount =
        request.getDiscountAmount() == null ? BigDecimal.ZERO : request.getDiscountAmount();
    purchaseOrder.setTotalAmount(total);
    purchaseOrder.setDiscountAmount(discount);
    purchaseOrder = purchaseOrderRepository.save(purchaseOrder);

    BigDecimal unpaid = total.subtract(discount).subtract(request.getPaidAmount());
    if (unpaid.compareTo(BigDecimal.ZERO) > 0) {
      Debt debt = new Debt();
      debt.setSupplier(supplier);
      debt.setDirection("payable");
      debt.setAmount(unpaid);
      debt.setOriginalAmount(unpaid);
      debt.setReferenceType("purchase_order");
      debt.setReferenceId(purchaseOrder.getId());
      debtRepository.save(debt);
    }

    PurchaseOrderResponse response = purchaseOrderMapper.toResponse(purchaseOrder);
    response.setItems(savedItems.stream().map(purchaseOrderMapper::toItemResponse).toList());
    return response;
  }

  @Transactional(readOnly = true)
  public ApiResponse<List<PurchaseOrderResponse>> list(Long branchId, Pageable pageable) {
    Page<PurchaseOrder> page =
        purchaseOrderRepository.findByBranchIdOrderByCreatedAtDesc(branchId, pageable);
    return ApiResponse.page(page.map(purchaseOrderMapper::toResponse));
  }

  @Transactional(readOnly = true)
  public PurchaseOrderResponse getById(Long id) {
    PurchaseOrder purchaseOrder =
        purchaseOrderRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phieu nhap"));
    PurchaseOrderResponse response = purchaseOrderMapper.toResponse(purchaseOrder);
    response.setItems(
        purchaseOrderItemRepository.findByPurchaseOrderId(id).stream()
            .map(purchaseOrderMapper::toItemResponse)
            .toList());
    return response;
  }
}
