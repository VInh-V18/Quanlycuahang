package com.quanlycuahang.erp.inventory.service;

import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.inventory.dto.InventoryResponse;
import com.quanlycuahang.erp.inventory.dto.InventoryTransactionResponse;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.entity.InventoryTransaction;
import com.quanlycuahang.erp.inventory.mapper.InventoryMapper;
import com.quanlycuahang.erp.inventory.repository.InventoryBatchRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryTransactionRepository;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ton kho theo chi nhanh + the kho (UC-06). Ton hien tai luon tinh lai duoc tu InventoryTransaction
 * (B4).
 */
@Service
public class InventoryService {

  private final InventoryRepository inventoryRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final InventoryBatchRepository inventoryBatchRepository;
  private final InventoryMapper inventoryMapper;
  private final BranchAccessGuard branchAccessGuard;
  private final ProductRepository productRepository;

  public InventoryService(
      InventoryRepository inventoryRepository,
      InventoryTransactionRepository inventoryTransactionRepository,
      InventoryBatchRepository inventoryBatchRepository,
      InventoryMapper inventoryMapper,
      BranchAccessGuard branchAccessGuard,
      ProductRepository productRepository) {
    this.inventoryRepository = inventoryRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.inventoryBatchRepository = inventoryBatchRepository;
    this.inventoryMapper = inventoryMapper;
    this.branchAccessGuard = branchAccessGuard;
    this.productRepository = productRepository;
  }

  @Transactional(readOnly = true)
  public ApiResponse<List<InventoryResponse>> listByBranch(Long branchId, Pageable pageable) {
    branchAccessGuard.assertAccess(branchId);
    Page<Inventory> page = inventoryRepository.findByBranchId(branchId, pageable);
    ApiResponse<List<InventoryResponse>> response =
        ApiResponse.page(page.map(inventoryMapper::toResponse));
    enrichWithNearestBatch(response.getData(), branchId);
    return response;
  }

  @Transactional(readOnly = true)
  public ApiResponse<List<InventoryResponse>> lowStockByBranch(Long branchId, Pageable pageable) {
    branchAccessGuard.assertAccess(branchId);
    Page<Inventory> page = inventoryRepository.findLowStockByBranchId(branchId, pageable);
    ApiResponse<List<InventoryResponse>> response =
        ApiResponse.page(page.map(inventoryMapper::toResponse));
    enrichWithNearestBatch(response.getData(), branchId);
    return response;
  }

  /**
   * Tim/loc tai server thay vi FE tu loc tren 1 trang da fetch (xem InventoryRepository.search()) -
   * dung chung cho ca "tat ca" va "chi hang duoi dinh muc" qua tham so onlyLowStock.
   */
  @Transactional(readOnly = true)
  public ApiResponse<List<InventoryResponse>> search(
      Long branchId,
      boolean onlyLowStock,
      String search,
      Integer expiryThresholdDays,
      Pageable pageable) {
    branchAccessGuard.assertAccess(branchId);
    String normalizedSearch = search == null ? "" : search.trim().toLowerCase();
    LocalDate expiryThreshold =
        expiryThresholdDays == null ? null : LocalDate.now().plusDays(expiryThresholdDays);
    Page<Inventory> page =
        onlyLowStock
            ? inventoryRepository.searchLowStock(
                branchId, normalizedSearch, expiryThreshold, pageable)
            : inventoryRepository.search(branchId, normalizedSearch, expiryThreshold, pageable);
    ApiResponse<List<InventoryResponse>> response =
        ApiResponse.page(page.map(inventoryMapper::toResponse));
    enrichWithNearestBatch(response.getData(), branchId);
    return response;
  }

  // Nguong canh HSD (ngay) + nhan trang thai — 1 nguon DUY NHAT dung chung cho man hinh Ton kho
  // (InventoryController/export) va truoc day ca client/src/pages/inventory/InventoryPage.tsx tu
  // tinh lai doc lap (phat hien khi rieng soat, 2 noi co the lech nhau neu sua nguong 1 cho quen
  // cho kia).
  private static final int EXPIRY_WARNING_DAYS = 7;

  /**
   * Gan them Lo/HSD gan nhat vao moi dong ton kho (FH-4/FH-7) — 1 truy van cho ca trang. Nhan tien
   * the tinh luon stockValue/status vi ca 2 deu can nearestExpiryDate da gan xong o day.
   */
  private void enrichWithNearestBatch(List<InventoryResponse> rows, Long branchId) {
    if (rows.isEmpty()) {
      return;
    }
    Map<Long, Object[]> byProductId = new HashMap<>();
    for (Object[] row :
        inventoryBatchRepository.findNearestBatchPerProduct(branchId, TenantContext.get())) {
      byProductId.put(((Number) row[0]).longValue(), row);
    }
    for (InventoryResponse row : rows) {
      Object[] batch = byProductId.get(row.getProductId());
      if (batch != null) {
        row.setNearestBatchCode((String) batch[1]);
        row.setNearestExpiryDate(toLocalDate(batch[2]));
      }
      row.setStockValue(row.getStock().multiply(row.getCostPrice()));
      row.setStatus(computeStatus(row));
    }
  }

  private static String computeStatus(InventoryResponse row) {
    LocalDate expiry = row.getNearestExpiryDate();
    if (expiry != null && !expiry.isAfter(LocalDate.now().plusDays(EXPIRY_WARNING_DAYS))) {
      return "near_expiry";
    }
    if (row.getStock().compareTo(row.getMinStock()) <= 0) {
      return "low_stock";
    }
    return "ok";
  }

  private static LocalDate toLocalDate(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof LocalDate localDate) {
      return localDate;
    }
    return ((java.sql.Date) value).toLocalDate();
  }

  /**
   * The kho theo san pham + chi nhanh (FH-7). branchId bat buoc (khong con lay xuyen suot moi chi
   * nhanh nhu truoc) — phat hien khi rieng soat: endpoint cu khong loc theo chi nhanh, lo ca so
   * luong lan gia von (unitCost) cua chi nhanh khac ma nguoi dung khong duoc gan.
   */
  @Transactional(readOnly = true)
  public ApiResponse<List<InventoryTransactionResponse>> transactionHistory(
      Long productId, Long branchId, Pageable pageable) {
    branchAccessGuard.assertAccess(branchId);
    Page<InventoryTransaction> page =
        inventoryTransactionRepository.findByProductIdAndBranchIdOrderByCreatedAtDesc(
            productId, branchId, pageable);
    return ApiResponse.page(page.map(inventoryMapper::toTransactionResponse));
  }

  /**
   * Ghi de truc tiep gia von hien tai (inventory:cost-price-override, tinh nang moi) — KHAC voi
   * bien dong binh quan gia quyen tu dong qua AverageCostService khi nhan hang nhap: day la ghi de
   * thu cong, khong tinh lai tu lich su. Khoa pessimistic truoc khi set (mirror
   * StockTakeService.approve()) de tranh doi voi 1 phieu nhap dang nhan hang cung luc. KHONG ghi
   * InventoryTransaction — bang do chi ghi nhan bien dong SO LUONG (quantity NOT NULL), ghi de gia
   * von khong co delta so luong nen khong hop ban chat "the kho"; audit trail dua vao @Audited o
   * Controller.
   */
  @Transactional
  public void overrideCostPrice(Long productId, Long branchId, BigDecimal newCostPrice) {
    branchAccessGuard.assertAccess(branchId);
    if (newCostPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessRuleException("INVENTORY_COST_PRICE_NEGATIVE", "Giá vốn không được âm");
    }
    if (!productRepository.existsById(productId)) {
      throw new ResourceNotFoundException("Không tìm thấy sản phẩm");
    }
    Inventory inventory =
        inventoryRepository.findByProductIdAndBranchIdForUpdate(productId, branchId).orElse(null);
    if (inventory == null) {
      inventoryRepository.initializeIfAbsent(TenantContext.get(), productId, branchId);
      inventory =
          inventoryRepository
              .findByProductIdAndBranchIdForUpdate(productId, branchId)
              .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tồn kho"));
    }
    inventory.setCostPrice(newCostPrice);
    inventoryRepository.save(inventory);
  }
}
