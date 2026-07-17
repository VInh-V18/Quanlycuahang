package com.quanlycuahang.erp.inventory.controller;

import com.quanlycuahang.erp.common.audit.Audited;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.ValidationException;
import com.quanlycuahang.erp.inventory.dto.InventoryCostPriceUpdateRequest;
import com.quanlycuahang.erp.inventory.dto.InventoryResponse;
import com.quanlycuahang.erp.inventory.dto.InventoryTransactionResponse;
import com.quanlycuahang.erp.inventory.service.InventoryService;
import com.quanlycuahang.erp.report.excel.ReportExcelExporter;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

  // Nhan tieng Viet cho InventoryResponse.status ("near_expiry"/"low_stock"/"ok") — chinh nguon
  // tinh toan nguong/uu tien da chuyen het vao InventoryService.computeStatus(), Controller chi
  // con dich sang tieng Viet de xuat Excel (man hinh Ton kho tu dich rieng, xem InventoryPage.tsx).
  private static final Map<String, String> STATUS_LABELS =
      Map.of(
          "near_expiry", "Cận hạn — xả 30%",
          "low_stock", "Dưới định mức",
          "ok", "Đủ hàng");

  private final InventoryService inventoryService;
  private final ReportExcelExporter excelExporter;

  public InventoryController(InventoryService inventoryService, ReportExcelExporter excelExporter) {
    this.inventoryService = inventoryService;
    this.excelExporter = excelExporter;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('inventory:view')")
  public ResponseEntity<ApiResponse<List<InventoryResponse>>> listByBranch(
      @RequestParam Long branchId,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Integer expiryThresholdDays,
      Pageable pageable) {
    return ResponseEntity.ok(
        inventoryService.search(branchId, false, search, expiryThresholdDays, pageable));
  }

  @GetMapping("/low-stock")
  @PreAuthorize("hasAuthority('inventory:view')")
  public ResponseEntity<ApiResponse<List<InventoryResponse>>> lowStock(
      @RequestParam Long branchId,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Integer expiryThresholdDays,
      Pageable pageable) {
    return ResponseEntity.ok(
        inventoryService.search(branchId, true, search, expiryThresholdDays, pageable));
  }

  /**
   * Xuat toan bo bang ton kho chi tiet (dung cot voi trang Ton kho) — truoc day nut "Xuat Excel" o
   * trang nay vo tinh goi nham /reports/inventory-value/export (chi tra ve tong gia tri gop theo
   * chi nhanh/danh muc, khong phai bang chi tiet tung san pham dang hien tren man hinh).
   */
  @GetMapping("/export")
  @PreAuthorize("hasAuthority('inventory:view') and hasAuthority('report:export')")
  public ResponseEntity<byte[]> export(
      @RequestParam Long branchId, @RequestParam(defaultValue = "false") boolean lowStockOnly) {
    Pageable allRows = PageRequest.of(0, 10_000);
    ApiResponse<List<InventoryResponse>> page =
        lowStockOnly
            ? inventoryService.lowStockByBranch(branchId, allRows)
            : inventoryService.listByBranch(branchId, allRows);
    // Xem chu thich tuong tu o OrderController.export() — canh bao ro rang thay vi xuat lang le
    // thieu dong khi vuot 10.000 dong/lan.
    if (page.getMeta().getTotal() > 10_000) {
      throw new ValidationException(
          "Có "
              + page.getMeta().getTotal()
              + " sản phẩm, vượt quá 10.000 dòng cho phép xuất 1 lần — vui lòng lọc bớt (vd chỉ hàng dưới định mức)");
    }
    List<InventoryResponse> data = page.getData();
    byte[] file =
        excelExporter.export(
            "Ton kho",
            List.of(
                "SKU", "Sản phẩm", "Tồn", "Tối thiểu", "Lô", "HSD", "Giá trị tồn", "Trạng thái"),
            data,
            row ->
                new Object[] {
                  row.getSku(),
                  row.getProductName(),
                  row.getStock(),
                  row.getMinStock(),
                  row.getNearestBatchCode(),
                  row.getNearestExpiryDate(),
                  row.getStockValue(),
                  STATUS_LABELS.getOrDefault(row.getStatus(), row.getStatus())
                });
    return excelExporter.toXlsxResponse(file, "ton-kho.xlsx");
  }

  @GetMapping("/products/{productId}/transactions")
  @PreAuthorize("hasAuthority('inventory:view')")
  public ResponseEntity<ApiResponse<List<InventoryTransactionResponse>>> transactionHistory(
      @PathVariable Long productId, @RequestParam Long branchId, Pageable pageable) {
    return ResponseEntity.ok(inventoryService.transactionHistory(productId, branchId, pageable));
  }

  @PatchMapping("/products/{productId}/cost-price")
  @PreAuthorize("hasAuthority('inventory:cost-price-override')")
  @Audited(action = "INVENTORY_COST_PRICE_OVERRIDE", entityName = "Inventory")
  public ResponseEntity<ApiResponse<Void>> updateCostPrice(
      @PathVariable Long productId,
      @RequestParam Long branchId,
      @Valid @RequestBody InventoryCostPriceUpdateRequest request) {
    inventoryService.overrideCostPrice(productId, branchId, request.getCostPrice());
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
