package com.quanlycuahang.erp.inventory.service;

import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.auth.security.TenantContext;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger log = LoggerFactory.getLogger(StockTakeService.class);

  /**
   * Nguong xem la "chenh lech lon" khi duyet phieu kiem ke (Prompt #8, P2 quan sat) - tuong doi
   * (>=20% so voi ton du kien) HOAC tuyet doi (>=50 don vi khi ton du kien qua nho/bang 0, tranh
   * chia cho 0 va bo lot truong hop "du kien 0, thuc te 1000"). Chua co cai dat rieng tung tenant
   * cho nguong nay - la 1 hang so co dinh, hop ly cho da so nganh hang ban le vua/nho.
   */
  private static final BigDecimal LARGE_DISCREPANCY_RATIO = BigDecimal.valueOf(0.2);

  private static final BigDecimal LARGE_DISCREPANCY_ABSOLUTE = BigDecimal.valueOf(50);

  private final StockTakeRepository stockTakeRepository;
  private final StockTakeItemRepository stockTakeItemRepository;
  private final InventoryRepository inventoryRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final BranchRepository branchRepository;
  private final StockTakeMapper stockTakeMapper;
  private final CurrentUserProvider currentUserProvider;
  private final BranchAccessGuard branchAccessGuard;

  public StockTakeService(
      StockTakeRepository stockTakeRepository,
      StockTakeItemRepository stockTakeItemRepository,
      InventoryRepository inventoryRepository,
      InventoryTransactionRepository inventoryTransactionRepository,
      BranchRepository branchRepository,
      StockTakeMapper stockTakeMapper,
      CurrentUserProvider currentUserProvider,
      BranchAccessGuard branchAccessGuard) {
    this.stockTakeRepository = stockTakeRepository;
    this.stockTakeItemRepository = stockTakeItemRepository;
    this.inventoryRepository = inventoryRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.branchRepository = branchRepository;
    this.stockTakeMapper = stockTakeMapper;
    this.currentUserProvider = currentUserProvider;
    this.branchAccessGuard = branchAccessGuard;
  }

  @Transactional
  public StockTakeResponse create(StockTakeCreateRequest request) {
    branchAccessGuard.assertAccess(request.getBranchId());
    Branch branch =
        branchRepository
            .findById(request.getBranchId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));

    StockTake stockTake = new StockTake();
    stockTake.setBranch(branch);
    stockTake.setStatus("draft");
    currentUserProvider.getCurrentUser().ifPresent(stockTake::setCreatedBy);
    stockTake = stockTakeRepository.save(stockTake);

    // saveAll 1 lan thay vi save() tung dong - danh muc lon (hang nghin SKU) truoc day tao tung
    // round-trip repository rieng le. Luu y: ID dang IDENTITY nen Hibernate van khong batch duoc
    // cau INSERT o tang JDBC (gioi han cua IDENTITY), saveAll chi giam chi phi tang repository;
    // hibernate.jdbc.batch_size trong application.yml chi co tac dung voi UPDATE/DELETE.
    List<Inventory> inventories = inventoryRepository.findByBranchId(branch.getId());
    List<StockTakeItem> items = new ArrayList<>(inventories.size());
    for (Inventory inventory : inventories) {
      StockTakeItem item = new StockTakeItem();
      item.setStockTake(stockTake);
      item.setProduct(inventory.getProduct());
      item.setExpectedQty(inventory.getStock());
      items.add(item);
    }
    stockTakeItemRepository.saveAll(items);

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
        throw new ResourceNotFoundException("Không tìm thấy dòng kiểm kê tương ứng");
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

    int largeDiscrepancyCount = 0;
    for (StockTakeItem item : items) {
      if (item.getActualQty() == null) {
        throw new ValidationException("Còn dòng kiểm kê chưa nhập số lượng thực tế");
      }
      BigDecimal diff = item.getActualQty().subtract(item.getExpectedQty());
      if (diff.compareTo(BigDecimal.ZERO) != 0
          && (item.getReason() == null || item.getReason().isBlank())) {
        throw new BusinessRuleException(
            "STOCK_TAKE_REASON_REQUIRED",
            "Cần nhập lý do cho sản phẩm có chênh lệch: " + item.getProduct().getName());
      }
      if (isLargeDiscrepancy(diff, item.getExpectedQty())) {
        largeDiscrepancyCount++;
      }
    }
    if (largeDiscrepancyCount > 0) {
      // Su kien nhay cam (Prompt #8, P2 quan sat) - chenh lech kiem ke lon co the la trom cap/that
      // thoat nghiem trong hoac loi nhap lieu, can theo doi rieng khoi cac phieu chenh lech nho
      // thong thuong.
      log.warn(
          "STOCK_TAKE_APPROVE_LARGE_DISCREPANCY stockTakeId={} branchId={} tenantId={}"
              + " largeDiscrepancyItemCount={}",
          stockTakeId,
          stockTake.getBranch().getId(),
          TenantContext.get(),
          largeDiscrepancyCount);
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
              .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tồn kho"));
      // Cong don CHENH LECH vao ton kho hien tai (vua khoa), khong gan thang actualQty - neu ban
      // hang/nhap kho xay ra giua luc tao phieu (snapshot expectedQty) va luc duyet (co the cach
      // nhau nhieu gio), gan thang se xoa sach thay doi do; cong don giu duoc thay doi ngoai y muon
      // trong khi van phan anh dung ket qua dem thuc te.
      inventory.setStock(inventory.getStock().add(diff));
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
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu kiểm kê"));
    branchAccessGuard.assertAccess(stockTake.getBranch().getId());
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
    branchAccessGuard.assertAccess(branchId);
    Page<StockTake> page =
        stockTakeRepository.findByBranchIdOrderByCreatedAtDesc(branchId, pageable);
    return com.quanlycuahang.erp.common.dto.ApiResponse.page(page.map(stockTakeMapper::toResponse));
  }

  private static boolean isLargeDiscrepancy(BigDecimal diff, BigDecimal expectedQty) {
    BigDecimal absDiff = diff.abs();
    if (absDiff.compareTo(LARGE_DISCREPANCY_ABSOLUTE) >= 0) {
      return true;
    }
    if (expectedQty.compareTo(BigDecimal.ZERO) <= 0) {
      return false;
    }
    BigDecimal ratio = absDiff.divide(expectedQty, 4, java.math.RoundingMode.HALF_UP);
    return ratio.compareTo(LARGE_DISCREPANCY_RATIO) >= 0;
  }

  private StockTake requireDraft(Long id) {
    StockTake stockTake =
        stockTakeRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu kiểm kê"));
    branchAccessGuard.assertAccess(stockTake.getBranch().getId());
    if (!"draft".equals(stockTake.getStatus())) {
      throw new BusinessRuleException("STOCK_TAKE_ALREADY_APPROVED", "Phiếu kiểm kê đã được duyệt");
    }
    return stockTake;
  }
}
