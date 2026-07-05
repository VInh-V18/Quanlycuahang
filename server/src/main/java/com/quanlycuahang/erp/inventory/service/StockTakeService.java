package com.quanlycuahang.erp.inventory.service;

import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.common.exception.ValidationException;
import com.quanlycuahang.erp.inventory.dto.StockTakeCreateRequest;
import com.quanlycuahang.erp.inventory.dto.StockTakeItemCountRequest;
import com.quanlycuahang.erp.inventory.dto.StockTakeResponse;
import com.quanlycuahang.erp.inventory.dto.StockTakeSubmitRequest;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.entity.InventoryTransaction;
import com.quanlycuahang.erp.inventory.entity.StockTake;
import com.quanlycuahang.erp.inventory.entity.StockTakeItem;
import com.quanlycuahang.erp.inventory.mapper.StockTakeMapper;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryTransactionRepository;
import com.quanlycuahang.erp.inventory.repository.StockTakeItemRepository;
import com.quanlycuahang.erp.inventory.repository.StockTakeRepository;
import com.quanlycuahang.erp.system.entity.Branch;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kiem ke kho (UC-16): chot snapshot ton he thong -> nhap so dem thuc te -> duyet sinh phieu can
 * bang chenh lech kem ly do bat buoc (B4).
 */
@Service
public class StockTakeService {

  private final StockTakeRepository stockTakeRepository;
  private final StockTakeItemRepository stockTakeItemRepository;
  private final InventoryRepository inventoryRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final BranchRepository branchRepository;
  private final StockTakeMapper stockTakeMapper;
  private final CurrentUserProvider currentUserProvider;

  public StockTakeService(
      StockTakeRepository stockTakeRepository,
      StockTakeItemRepository stockTakeItemRepository,
      InventoryRepository inventoryRepository,
      InventoryTransactionRepository inventoryTransactionRepository,
      BranchRepository branchRepository,
      StockTakeMapper stockTakeMapper,
      CurrentUserProvider currentUserProvider) {
    this.stockTakeRepository = stockTakeRepository;
    this.stockTakeItemRepository = stockTakeItemRepository;
    this.inventoryRepository = inventoryRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.branchRepository = branchRepository;
    this.stockTakeMapper = stockTakeMapper;
    this.currentUserProvider = currentUserProvider;
  }

  @Transactional
  public StockTakeResponse create(StockTakeCreateRequest request) {
    Branch branch =
        branchRepository
            .findById(request.getBranchId())
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay chi nhanh"));

    StockTake stockTake = new StockTake();
    stockTake.setBranch(branch);
    stockTake.setStatus("draft");
    currentUserProvider.getCurrentUser().ifPresent(stockTake::setCreatedBy);
    stockTake = stockTakeRepository.save(stockTake);

    List<Inventory> inventories = inventoryRepository.findByBranchId(branch.getId());
    for (Inventory inventory : inventories) {
      StockTakeItem item = new StockTakeItem();
      item.setStockTake(stockTake);
      item.setProduct(inventory.getProduct());
      item.setExpectedQty(inventory.getStock());
      stockTakeItemRepository.save(item);
    }

    return getById(stockTake.getId());
  }

  @Transactional
  public StockTakeResponse submitCounts(Long stockTakeId, StockTakeSubmitRequest request) {
    StockTake stockTake = requireDraft(stockTakeId);
    Map<Long, StockTakeItem> itemsById =
        stockTakeItemRepository.findByStockTakeId(stockTakeId).stream()
            .collect(java.util.stream.Collectors.toMap(StockTakeItem::getId, i -> i));

    for (StockTakeItemCountRequest countRequest : request.getItems()) {
      StockTakeItem item = itemsById.get(countRequest.getStockTakeItemId());
      if (item == null) {
        throw new ResourceNotFoundException("Khong tim thay dong kiem ke tuong ung");
      }
      item.setActualQty(countRequest.getActualQty());
      item.setReason(countRequest.getReason());
      stockTakeItemRepository.save(item);
    }
    return getById(stockTake.getId());
  }

  @Transactional
  public StockTakeResponse approve(Long stockTakeId) {
    StockTake stockTake = requireDraft(stockTakeId);
    List<StockTakeItem> items = stockTakeItemRepository.findByStockTakeId(stockTakeId);

    for (StockTakeItem item : items) {
      if (item.getActualQty() == null) {
        throw new ValidationException("Con dong kiem ke chua nhap so luong thuc te");
      }
      BigDecimal diff = item.getActualQty().subtract(item.getExpectedQty());
      if (diff.compareTo(BigDecimal.ZERO) != 0
          && (item.getReason() == null || item.getReason().isBlank())) {
        throw new BusinessRuleException(
            "STOCK_TAKE_REASON_REQUIRED",
            "Can nhap ly do cho san pham co chenh lech: " + item.getProduct().getName());
      }
    }

    for (StockTakeItem item : items) {
      BigDecimal diff = item.getActualQty().subtract(item.getExpectedQty());
      if (diff.compareTo(BigDecimal.ZERO) == 0) {
        continue;
      }
      Inventory inventory =
          inventoryRepository
              .findByProductIdAndBranchIdForUpdate(
                  item.getProduct().getId(), stockTake.getBranch().getId())
              .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ton kho"));
      inventory.setStock(item.getActualQty());
      inventoryRepository.save(inventory);

      InventoryTransaction transaction = new InventoryTransaction();
      transaction.setProduct(item.getProduct());
      transaction.setBranch(stockTake.getBranch());
      transaction.setType("stock_take");
      transaction.setQuantity(diff);
      transaction.setReferenceType("stock_take");
      transaction.setReferenceId(stockTakeId);
      transaction.setNote(item.getReason());
      currentUserProvider.getCurrentUser().ifPresent(transaction::setCreatedBy);
      inventoryTransactionRepository.save(transaction);
    }

    stockTake.setStatus("approved");
    stockTake.setApprovedAt(OffsetDateTime.now());
    currentUserProvider.getCurrentUser().ifPresent(stockTake::setApprovedBy);
    stockTakeRepository.save(stockTake);

    return getById(stockTakeId);
  }

  @Transactional(readOnly = true)
  public StockTakeResponse getById(Long id) {
    StockTake stockTake =
        stockTakeRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phieu kiem ke"));
    StockTakeResponse response = stockTakeMapper.toResponse(stockTake);
    response.setItems(
        stockTakeItemRepository.findByStockTakeId(id).stream()
            .map(stockTakeMapper::toItemResponse)
            .toList());
    return response;
  }

  @Transactional(readOnly = true)
  public com.quanlycuahang.erp.common.dto.ApiResponse<List<StockTakeResponse>> list(
      Long branchId, Pageable pageable) {
    Page<StockTake> page =
        stockTakeRepository.findByBranchIdOrderByCreatedAtDesc(branchId, pageable);
    return com.quanlycuahang.erp.common.dto.ApiResponse.page(page.map(stockTakeMapper::toResponse));
  }

  private StockTake requireDraft(Long id) {
    StockTake stockTake =
        stockTakeRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phieu kiem ke"));
    if (!"draft".equals(stockTake.getStatus())) {
      throw new BusinessRuleException("STOCK_TAKE_ALREADY_APPROVED", "Phieu kiem ke da duoc duyet");
    }
    return stockTake;
  }
}
