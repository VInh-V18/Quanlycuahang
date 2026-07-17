package com.quanlycuahang.erp.report.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.ValidationException;
import com.quanlycuahang.erp.report.dto.DebtAgingBucketResponse;
import com.quanlycuahang.erp.report.dto.EmployeePerformanceResponse;
import com.quanlycuahang.erp.report.dto.GrossProfitResponse;
import com.quanlycuahang.erp.report.dto.InventoryValueResponse;
import com.quanlycuahang.erp.report.dto.ReportFilter;
import com.quanlycuahang.erp.report.dto.RevenueBucketResponse;
import com.quanlycuahang.erp.report.dto.TopCustomerResponse;
import com.quanlycuahang.erp.report.dto.TopProductResponse;
import com.quanlycuahang.erp.report.excel.ReportExcelExporter;
import com.quanlycuahang.erp.report.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bao cao tong hop (Phase 10) — moi endpoint lay du lieu qua ReportService, xuat Excel qua
 * ReportExcelExporter dung chung. Bo loc ngay/chi nhanh dung chung 1 ReportFilter (D-chung).
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

  /** 2 nam - du cho doi chieu cung ky nam truoc, van chan duoc khoang ngay vo han. */
  private static final int MAX_REPORT_RANGE_DAYS = 731;

  private final ReportService reportService;
  private final ReportExcelExporter excelExporter;

  public ReportController(ReportService reportService, ReportExcelExporter excelExporter) {
    this.reportService = reportService;
    this.excelExporter = excelExporter;
  }

  @GetMapping("/revenue")
  @PreAuthorize("hasAuthority('report:revenue')")
  public ResponseEntity<ApiResponse<List<RevenueBucketResponse>>> revenue(
      @RequestParam LocalDate from,
      @RequestParam LocalDate to,
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "day") String groupBy) {
    return ResponseEntity.ok(
        ApiResponse.success(revenueByGroup(filter(from, to, branchId), groupBy)));
  }

  @GetMapping("/revenue/export")
  @PreAuthorize("hasAuthority('report:revenue') and hasAuthority('report:export')")
  public ResponseEntity<byte[]> exportRevenue(
      @RequestParam LocalDate from,
      @RequestParam LocalDate to,
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "day") String groupBy) {
    List<RevenueBucketResponse> data = revenueByGroup(filter(from, to, branchId), groupBy);
    byte[] file =
        excelExporter.export(
            "Doanh thu",
            List.of("Nhóm", "Doanh thu", "Số đơn", "Giá vốn hàng bán", "Lợi nhuận gộp"),
            data,
            row ->
                new Object[] {
                  row.getLabel(),
                  row.getRevenue(),
                  row.getOrderCount(),
                  row.getCostOfGoodsSold(),
                  row.getGrossProfit()
                });
    return excelExporter.toXlsxResponse(file, "doanh-thu.xlsx");
  }

  @GetMapping("/gross-profit")
  @PreAuthorize("hasAuthority('report:gross-profit')")
  public ResponseEntity<ApiResponse<GrossProfitResponse>> grossProfit(
      @RequestParam LocalDate from,
      @RequestParam LocalDate to,
      @RequestParam(required = false) Long branchId) {
    return ResponseEntity.ok(
        ApiResponse.success(reportService.grossProfit(filter(from, to, branchId))));
  }

  @GetMapping("/top-products")
  @PreAuthorize("hasAuthority('report:revenue')")
  public ResponseEntity<ApiResponse<List<TopProductResponse>>> topProducts(
      @RequestParam LocalDate from,
      @RequestParam LocalDate to,
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "10") int limit) {
    return ResponseEntity.ok(
        ApiResponse.success(reportService.topProducts(filter(from, to, branchId), limit)));
  }

  @GetMapping("/top-products/export")
  @PreAuthorize("hasAuthority('report:revenue') and hasAuthority('report:export')")
  public ResponseEntity<byte[]> exportTopProducts(
      @RequestParam LocalDate from,
      @RequestParam LocalDate to,
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "10") int limit) {
    List<TopProductResponse> data = reportService.topProducts(filter(from, to, branchId), limit);
    byte[] file =
        excelExporter.export(
            "Top san pham",
            List.of("SKU", "Sản phẩm", "SL bán", "Doanh thu"),
            data,
            row ->
                new Object[] {
                  row.getSku(), row.getProductName(), row.getQuantitySold(), row.getRevenue()
                });
    return excelExporter.toXlsxResponse(file, "top-san-pham.xlsx");
  }

  @GetMapping("/top-customers")
  @PreAuthorize("hasAuthority('report:revenue')")
  public ResponseEntity<ApiResponse<List<TopCustomerResponse>>> topCustomers(
      @RequestParam LocalDate from,
      @RequestParam LocalDate to,
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "10") int limit) {
    return ResponseEntity.ok(
        ApiResponse.success(reportService.topCustomers(filter(from, to, branchId), limit)));
  }

  @GetMapping("/top-customers/export")
  @PreAuthorize("hasAuthority('report:revenue') and hasAuthority('report:export')")
  public ResponseEntity<byte[]> exportTopCustomers(
      @RequestParam LocalDate from,
      @RequestParam LocalDate to,
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "10") int limit) {
    List<TopCustomerResponse> data = reportService.topCustomers(filter(from, to, branchId), limit);
    byte[] file =
        excelExporter.export(
            "Top khach hang",
            List.of("Khách hàng", "Số đơn", "Doanh thu"),
            data,
            row -> new Object[] {row.getCustomerName(), row.getOrderCount(), row.getRevenue()});
    return excelExporter.toXlsxResponse(file, "top-khach-hang.xlsx");
  }

  @GetMapping("/employee-performance")
  @PreAuthorize("hasAuthority('report:employee-performance')")
  public ResponseEntity<ApiResponse<List<EmployeePerformanceResponse>>> employeePerformance(
      @RequestParam LocalDate from,
      @RequestParam LocalDate to,
      @RequestParam(required = false) Long branchId) {
    return ResponseEntity.ok(
        ApiResponse.success(reportService.employeePerformance(filter(from, to, branchId))));
  }

  @GetMapping("/employee-performance/export")
  @PreAuthorize("hasAuthority('report:employee-performance') and hasAuthority('report:export')")
  public ResponseEntity<byte[]> exportEmployeePerformance(
      @RequestParam LocalDate from,
      @RequestParam LocalDate to,
      @RequestParam(required = false) Long branchId) {
    List<EmployeePerformanceResponse> data =
        reportService.employeePerformance(filter(from, to, branchId));
    byte[] file =
        excelExporter.export(
            "Hieu suat nhan vien",
            List.of("Thu ngân", "Số đơn", "Doanh thu", "Giá trị TB/đơn"),
            data,
            row ->
                new Object[] {
                  row.getCashierName(),
                  row.getOrderCount(),
                  row.getRevenue(),
                  row.getAverageOrderValue()
                });
    return excelExporter.toXlsxResponse(file, "hieu-suat-nhan-vien.xlsx");
  }

  @GetMapping("/inventory-value")
  @PreAuthorize("hasAuthority('report:inventory-value')")
  public ResponseEntity<ApiResponse<List<InventoryValueResponse>>> inventoryValue(
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "branch") String groupBy) {
    return ResponseEntity.ok(
        ApiResponse.success(reportService.inventoryValue(branchId, "category".equals(groupBy))));
  }

  @GetMapping("/inventory-value/export")
  @PreAuthorize("hasAuthority('report:inventory-value') and hasAuthority('report:export')")
  public ResponseEntity<byte[]> exportInventoryValue(
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "branch") String groupBy) {
    List<InventoryValueResponse> data =
        reportService.inventoryValue(branchId, "category".equals(groupBy));
    byte[] file =
        excelExporter.export(
            "Gia tri ton kho",
            List.of("Nhóm", "Giá trị tồn kho", "Số lượng"),
            data,
            row -> new Object[] {row.getLabel(), row.getTotalValue(), row.getTotalQuantity()});
    return excelExporter.toXlsxResponse(file, "gia-tri-ton-kho.xlsx");
  }

  @GetMapping("/debt-aging")
  @PreAuthorize("hasAuthority('debt:view')")
  public ResponseEntity<ApiResponse<List<DebtAgingBucketResponse>>> debtAging(
      @RequestParam String direction) {
    return ResponseEntity.ok(ApiResponse.success(reportService.debtAging(direction)));
  }

  @GetMapping("/debt-aging/export")
  @PreAuthorize("hasAuthority('debt:view') and hasAuthority('report:export')")
  public ResponseEntity<byte[]> exportDebtAging(@RequestParam String direction) {
    List<DebtAgingBucketResponse> data = reportService.debtAging(direction);
    byte[] file =
        excelExporter.export(
            "Cong no theo tuoi no",
            List.of("Tuổi nợ (ngày)", "Số khoản", "Tổng tiền"),
            data,
            row -> new Object[] {row.getBucket(), row.getDebtCount(), row.getTotalAmount()});
    String fileName =
        "receivable".equals(direction) ? "cong-no-phai-thu.xlsx" : "cong-no-phai-tra.xlsx";
    return excelExporter.toXlsxResponse(file, fileName);
  }

  private List<RevenueBucketResponse> revenueByGroup(ReportFilter filter, String groupBy) {
    return switch (groupBy) {
      case "branch" -> reportService.revenueByBranch(filter);
      case "cashier" -> reportService.revenueByCashier(filter);
      case "week", "month", "day" -> reportService.revenueByPeriod(filter, groupBy);
      default -> reportService.revenueByPeriod(filter, "day");
    };
  }

  /**
   * Kiem tra quyen truy cap chi nhanh chuyen sang ReportService (goi trong @Transactional) — goi
   * thang o Controller se lam vo lazy-load User.roles vi khong co Session dang mo.
   */
  private ReportFilter filter(LocalDate from, LocalDate to, Long branchId) {
    // Chan khoang ngay nguoc/vo han - truoc day from/to khong duoc kiem tra gi, 1 request voi
    // khoang vai chuc nam bat DB quet toan bo lich su giao dich (phat hien khi rieng soat).
    if (from.isAfter(to)) {
      throw new ValidationException("Khoảng ngày không hợp lệ: 'Từ ngày' phải trước 'Đến ngày'");
    }
    if (to.isAfter(from.plusDays(MAX_REPORT_RANGE_DAYS))) {
      throw new ValidationException(
          "Khoảng ngày báo cáo tối đa " + MAX_REPORT_RANGE_DAYS + " ngày, vui lòng thu hẹp lại");
    }
    ReportFilter filter = new ReportFilter();
    filter.setFrom(from);
    filter.setTo(to);
    filter.setBranchId(branchId);
    return filter;
  }
}
