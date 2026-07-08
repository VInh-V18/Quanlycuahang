package com.quanlycuahang.erp.dashboard.service;

import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.dashboard.dto.DashboardSummaryResponse;
import com.quanlycuahang.erp.dashboard.dto.RecentOrderResponse;
import com.quanlycuahang.erp.inventory.dto.InventoryResponse;
import com.quanlycuahang.erp.inventory.service.InventoryService;
import com.quanlycuahang.erp.report.dto.GrossProfitResponse;
import com.quanlycuahang.erp.report.dto.ReportFilter;
import com.quanlycuahang.erp.report.dto.RevenueBucketResponse;
import com.quanlycuahang.erp.report.dto.TopProductResponse;
import com.quanlycuahang.erp.report.service.ReportService;
import com.quanlycuahang.erp.sales.repository.OrderRepository;
import com.quanlycuahang.erp.sales.repository.ReturnRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tong hop so lieu cho trang Tong quan (FH-3) — tai su dung ReportService cho doanh thu/lai gop/top
 * san pham (cung 1 cong thuc voi module Bao cao, Phase 10), chi them truy van rieng cho don gan day
 * + so don tra hang trong ngay (chua co o ReportService).
 */
@Service
public class DashboardService {

  private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final ReportService reportService;
  private final OrderRepository orderRepository;
  private final ReturnRepository returnRepository;
  private final InventoryService inventoryService;
  private final BranchAccessGuard branchAccessGuard;

  public DashboardService(
      ReportService reportService,
      OrderRepository orderRepository,
      ReturnRepository returnRepository,
      InventoryService inventoryService,
      BranchAccessGuard branchAccessGuard) {
    this.reportService = reportService;
    this.orderRepository = orderRepository;
    this.returnRepository = returnRepository;
    this.inventoryService = inventoryService;
    this.branchAccessGuard = branchAccessGuard;
  }

  @Transactional(readOnly = true)
  public DashboardSummaryResponse summary(Long branchId) {
    branchAccessGuard.assertAccess(branchId);
    LocalDate today = LocalDate.now(APP_ZONE);
    LocalDate weekAgo = today.minusDays(6);

    List<RevenueBucketResponse> queried =
        reportService.revenueByPeriod(filter(weekAgo, today, branchId), "day");
    List<RevenueBucketResponse> last7Days = fillMissingDays(queried, weekAgo, today);
    RevenueBucketResponse todayBucket = findByLabel(last7Days, today);
    RevenueBucketResponse yesterdayBucket = findByLabel(last7Days, today.minusDays(1));

    BigDecimal todayRevenue = revenueOf(todayBucket);
    long todayOrderCount = orderCountOf(todayBucket);
    BigDecimal yesterdayRevenue = revenueOf(yesterdayBucket);
    long yesterdayOrderCount = orderCountOf(yesterdayBucket);

    ReportFilter todayFilter = filter(today, today, branchId);
    GrossProfitResponse grossProfit = reportService.grossProfit(todayFilter);
    List<TopProductResponse> topProducts = reportService.topProducts(todayFilter, 5);

    OffsetDateTime todayStart = today.atStartOfDay(APP_ZONE).toOffsetDateTime();
    OffsetDateTime todayEnd = today.plusDays(1).atStartOfDay(APP_ZONE).toOffsetDateTime();
    List<Object[]> returnRows =
        returnRepository.countAndSumReturns(todayStart, todayEnd, branchId, TenantContext.get());
    long returnCountToday = 0;
    BigDecimal refundAmountToday = BigDecimal.ZERO;
    if (!returnRows.isEmpty()) {
      Object[] row = returnRows.get(0);
      returnCountToday = ((Number) row[0]).longValue();
      refundAmountToday = (BigDecimal) row[1];
    }

    List<RecentOrderResponse> recentOrders =
        orderRepository.findRecentOrders(branchId, 5, TenantContext.get()).stream()
            .map(
                row ->
                    new RecentOrderResponse(
                        (String) row[0],
                        (String) row[1],
                        (BigDecimal) row[2],
                        statusLabel((String) row[3], (Boolean) row[4])))
            .toList();

    List<InventoryResponse> lowStock =
        branchId != null
            ? inventoryService.lowStockByBranch(branchId, PageRequest.of(0, 5)).getData()
            : List.of();

    DashboardSummaryResponse response = new DashboardSummaryResponse();
    response.setTodayRevenue(todayRevenue);
    response.setRevenueChangePercent(percentChange(yesterdayRevenue, todayRevenue));
    response.setTodayOrderCount(todayOrderCount);
    response.setOrderCountDelta(todayOrderCount - yesterdayOrderCount);
    response.setAverageOrderValue(averageOf(todayRevenue, todayOrderCount));
    response.setGrossProfitToday(grossProfit.getGrossProfit());
    response.setGrossProfitMarginPercent(
        percentOfRevenue(grossProfit.getGrossProfit(), todayRevenue));
    response.setReturnCountToday(returnCountToday);
    response.setRefundAmountToday(refundAmountToday);
    response.setLast7Days(last7Days);
    response.setTopProducts(topProducts);
    response.setRecentOrders(recentOrders);
    response.setLowStock(lowStock);
    return response;
  }

  private static ReportFilter filter(LocalDate from, LocalDate to, Long branchId) {
    ReportFilter filter = new ReportFilter();
    filter.setFrom(from);
    filter.setTo(to);
    filter.setBranchId(branchId);
    return filter;
  }

  /**
   * ReportService.revenueByPeriod chi tra ve nhung ngay THAT SU co don (GROUP BY khong sinh hang
   * cho ngay 0 don) — Dashboard can bieu do 7 ngay lien tuc (dung mockup), nen dien them 0 cho
   * nhung ngay bi thieu.
   */
  private static List<RevenueBucketResponse> fillMissingDays(
      List<RevenueBucketResponse> queried, LocalDate from, LocalDate to) {
    List<RevenueBucketResponse> filled = new java.util.ArrayList<>();
    for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
      RevenueBucketResponse existing = findByLabel(queried, date);
      filled.add(
          existing != null
              ? existing
              : new RevenueBucketResponse(
                  date.format(LABEL_FORMAT), BigDecimal.ZERO, 0, BigDecimal.ZERO));
    }
    return filled;
  }

  private static RevenueBucketResponse findByLabel(
      List<RevenueBucketResponse> buckets, LocalDate date) {
    String label = date.format(LABEL_FORMAT);
    return buckets.stream().filter(b -> label.equals(b.getLabel())).findFirst().orElse(null);
  }

  private static BigDecimal revenueOf(RevenueBucketResponse bucket) {
    return bucket != null ? bucket.getRevenue() : BigDecimal.ZERO;
  }

  private static long orderCountOf(RevenueBucketResponse bucket) {
    return bucket != null ? bucket.getOrderCount() : 0;
  }

  private static BigDecimal averageOf(BigDecimal revenue, long orderCount) {
    if (orderCount == 0) {
      return BigDecimal.ZERO;
    }
    return revenue.divide(BigDecimal.valueOf(orderCount), 0, RoundingMode.HALF_UP);
  }

  private static BigDecimal percentChange(BigDecimal previous, BigDecimal current) {
    if (previous == null || previous.signum() == 0) {
      return null;
    }
    return current
        .subtract(previous)
        .multiply(BigDecimal.valueOf(100))
        .divide(previous, 1, RoundingMode.HALF_UP);
  }

  private static BigDecimal percentOfRevenue(BigDecimal amount, BigDecimal revenue) {
    if (revenue == null || revenue.signum() == 0) {
      return BigDecimal.ZERO;
    }
    return amount.multiply(BigDecimal.valueOf(100)).divide(revenue, 1, RoundingMode.HALF_UP);
  }

  private static String statusLabel(String status, Boolean hasDebt) {
    if (Boolean.TRUE.equals(hasDebt)) {
      return "debt";
    }
    return status;
  }
}
