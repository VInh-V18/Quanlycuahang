package com.quanlycuahang.erp.inventory.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.inventory.dto.InventoryResponse;
import com.quanlycuahang.erp.inventory.dto.InventoryTransactionResponse;
import com.quanlycuahang.erp.inventory.service.InventoryService;
import com.quanlycuahang.erp.report.excel.ReportExcelExporter;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

  private final InventoryService inventoryService;
  private final ReportExcelExporter excelExporter;

  public InventoryController(InventoryService inventoryService, ReportExcelExporter excelExporter) {
    this.inventoryService = inventoryService;
    this.excelExporter = excelExporter;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('inventory:view')")
  public ResponseEntity<ApiResponse<List<InventoryResponse>>> listByBranch(
      @RequestParam Long branchId, Pageable pageable) {
    return ResponseEntity.ok(inventoryService.listByBranch(branchId, pageable));
  }

  @GetMapping("/low-stock")
  @PreAuthorize("hasAuthority('inventory:view')")
  public ResponseEntity<ApiResponse<List<InventoryResponse>>> lowStock(
      @RequestParam Long branchId, Pageable pageable) {
    return ResponseEntity.ok(inventoryService.lowStockByBranch(branchId, pageable));
  }

  /** Xuat toan bo bang ton kho chi tiet (dung cot voi trang Ton kho) — truoc day nut "Xuat Excel"
   * o trang nay vo tinh goi nham /reports/inventory-value/export (chi tra ve tong gia tri gop
   * theo chi nhanh/danh muc, khong phai bang chi tiet tung san pham dang hien tren man hinh). */
  @GetMapping("/export")
  @PreAuthorize("hasAuthority('inventory:view') and hasAuthority('report:export')")
  public ResponseEntity<byte[]> export(
      @RequestParam Long branchId, @RequestParam(defaultValue = "false") boolean lowStockOnly) {
    Pageable allRows = PageRequest.of(0, 10_000);
    List<InventoryResponse> data =
        (lowStockOnly
                ? inventoryService.lowStockByBranch(branchId, allRows)
                : inventoryService.listByBranch(branchId, allRows))
            .getData();
    byte[] file =
        excelExporter.export(
            "Ton kho",
            List.of("SKU", "Sản phẩm", "Tồn", "Tối thiểu", "Lô", "HSD", "Giá trị tồn"),
            data,
            row ->
                new Object[] {
                  row.getSku(),
                  row.getProductName(),
                  row.getStock(),
                  row.getMinStock(),
                  row.getNearestBatchCode(),
                  row.getNearestExpiryDate(),
                  row.getStock().multiply(row.getCostPrice())
                });
    return ResponseEntity.ok()
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ton-kho.xlsx\"")
        .body(file);
  }

  @GetMapping("/products/{productId}/transactions")
  @PreAuthorize("hasAuthority('inventory:view')")
  public ResponseEntity<ApiResponse<List<InventoryTransactionResponse>>> transactionHistory(
      @PathVariable Long productId, @RequestParam Long branchId, Pageable pageable) {
    return ResponseEntity.ok(inventoryService.transactionHistory(productId, branchId, pageable));
  }
}
