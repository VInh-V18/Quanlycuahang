package com.quanlycuahang.erp.report.service;

import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.report.dto.DebtAgingBucketResponse;
import com.quanlycuahang.erp.report.dto.EmployeePerformanceResponse;
import com.quanlycuahang.erp.report.dto.GrossProfitResponse;
import com.quanlycuahang.erp.report.dto.InventoryValueResponse;
import com.quanlycuahang.erp.report.dto.ReportFilter;
import com.quanlycuahang.erp.report.dto.RevenueBucketResponse;
import com.quanlycuahang.erp.report.dto.TopCustomerResponse;
import com.quanlycuahang.erp.report.dto.TopProductResponse;
import com.quanlycuahang.erp.sales.repository.OrderItemRepository;
import com.quanlycuahang.erp.sales.repository.OrderRepository;
import com.quanlycuahang.erp.sales.repository.ReturnRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tong hop bao cao (Phase 10) — moi truy van aggregate deu dung @Query(nativeQuery = true) (GROUP
 * BY/date_trunc PostgreSQL) truc tiep tren orders/order_items/returns/debts/inventory, chua can
 * bang tong hop daily_sales_summary vi quy mo du lieu (500-20000 SKU, 50-500 don/ngay) chay truc
 * tiep du nhanh — de ngo neu sau nay can toi uu (ghi trong docs/phase10).
 */
@Service
public class ReportService {

  private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final ReturnRepository returnRepository;
  private final DebtRepository debtRepository;
  private final InventoryRepository inventoryRepository;

  public ReportService(
      OrderRepository orderRepository,
      OrderItemRepository orderItemRepository,
      ReturnRepository returnRepository,
      DebtRepository debtRepository,
      InventoryRepository inventoryRepository) {
    this.orderRepository = orderRepository;
    this.orderItemRepository = orderItemRepository;
    this.returnRepository = returnRepository;
    this.debtRepository = debtRepository;
    this.inventoryRepository = inventoryRepository;
  }

  @Transactional(readOnly = true)
  public List<RevenueBucketResponse> revenueByPeriod(ReportFilter filter, String unit) {
    OffsetDateTime from = startOfDay(filter.getFrom());
    OffsetDateTime to = endOfDay(filter.getTo());
    return orderRepository.findRevenueByPeriod(unit, from, to, filter.getBranchId()).stream()
        .map(ReportService::toRevenueBucket)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<RevenueBucketResponse> revenueByBranch(ReportFilter filter) {
    OffsetDateTime from = startOfDay(filter.getFrom());
    OffsetDateTime to = endOfDay(filter.getTo());
    return orderRepository.findRevenueByBranch(from, to).stream()
        .map(ReportService::toRevenueBucket)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<RevenueBucketResponse> revenueByCashier(ReportFilter filter) {
    OffsetDateTime from = startOfDay(filter.getFrom());
    OffsetDateTime to = endOfDay(filter.getTo());
    return orderRepository.findRevenueByCashier(from, to, filter.getBranchId()).stream()
        .map(ReportService::toRevenueBucket)
        .toList();
  }

  @Transactional(readOnly = true)
  public GrossProfitResponse grossProfit(ReportFilter filter) {
    OffsetDateTime from = startOfDay(filter.getFrom());
    OffsetDateTime to = endOfDay(filter.getTo());
    BigDecimal revenue = orderRepository.sumRevenue(from, to, filter.getBranchId());
    BigDecimal cogs = orderItemRepository.sumCostOfGoodsSold(from, to, filter.getBranchId());
    BigDecimal returnImpact = returnRepository.sumReturnImpact(from, to, filter.getBranchId());
    return new GrossProfitResponse(revenue, cogs, returnImpact);
  }

  @Transactional(readOnly = true)
  public List<TopProductResponse> topProducts(ReportFilter filter, int limit) {
    OffsetDateTime from = startOfDay(filter.getFrom());
    OffsetDateTime to = endOfDay(filter.getTo());
    return orderItemRepository.findTopProducts(from, to, filter.getBranchId(), limit).stream()
        .map(
            row ->
                new TopProductResponse(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    (BigDecimal) row[3],
                    (BigDecimal) row[4]))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<TopCustomerResponse> topCustomers(ReportFilter filter, int limit) {
    OffsetDateTime from = startOfDay(filter.getFrom());
    OffsetDateTime to = endOfDay(filter.getTo());
    return orderRepository.findTopCustomers(from, to, filter.getBranchId(), limit).stream()
        .map(
            row ->
                new TopCustomerResponse(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    ((Number) row[2]).longValue(),
                    (BigDecimal) row[3]))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<EmployeePerformanceResponse> employeePerformance(ReportFilter filter) {
    OffsetDateTime from = startOfDay(filter.getFrom());
    OffsetDateTime to = endOfDay(filter.getTo());
    return orderRepository.findRevenueByCashier(from, to, filter.getBranchId()).stream()
        .map(
            row ->
                new EmployeePerformanceResponse(
                    null, (String) row[0], ((Number) row[2]).longValue(), (BigDecimal) row[1]))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<InventoryValueResponse> inventoryValue(Long branchId, boolean byCategory) {
    List<Object[]> rows =
        byCategory
            ? inventoryRepository.findInventoryValueByCategory(branchId)
            : inventoryRepository.findInventoryValueByBranch(branchId);
    return rows.stream()
        .map(
            row ->
                new InventoryValueResponse(
                    (String) row[0], (BigDecimal) row[1], (BigDecimal) row[2]))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<DebtAgingBucketResponse> debtAging(String direction) {
    return debtRepository.findAgingBuckets(direction).stream()
        .map(
            row ->
                new DebtAgingBucketResponse(
                    (String) row[0], (BigDecimal) row[1], ((Number) row[2]).longValue()))
        .toList();
  }

  private static RevenueBucketResponse toRevenueBucket(Object[] row) {
    return new RevenueBucketResponse(
        (String) row[0], (BigDecimal) row[1], ((Number) row[2]).longValue(), (BigDecimal) row[3]);
  }

  private static OffsetDateTime startOfDay(LocalDate date) {
    return date.atStartOfDay(APP_ZONE).toOffsetDateTime();
  }

  private static OffsetDateTime endOfDay(LocalDate date) {
    return date.plusDays(1).atStartOfDay(APP_ZONE).toOffsetDateTime();
  }
}
