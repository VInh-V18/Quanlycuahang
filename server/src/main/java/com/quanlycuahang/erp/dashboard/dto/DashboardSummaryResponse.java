package com.quanlycuahang.erp.dashboard.dto;

import com.quanlycuahang.erp.inventory.dto.InventoryResponse;
import com.quanlycuahang.erp.report.dto.RevenueBucketResponse;
import com.quanlycuahang.erp.report.dto.TopProductResponse;
import java.math.BigDecimal;
import java.util.List;

public class DashboardSummaryResponse {

  private BigDecimal todayRevenue;
  private BigDecimal revenueChangePercent;
  private long todayOrderCount;
  private long orderCountDelta;
  private BigDecimal averageOrderValue;
  private BigDecimal grossProfitToday;
  private BigDecimal grossProfitMarginPercent;
  private long returnCountToday;
  private BigDecimal refundAmountToday;
  private List<RevenueBucketResponse> last7Days;
  private List<TopProductResponse> topProducts;
  private List<RecentOrderResponse> recentOrders;
  private List<InventoryResponse> lowStock;

  public BigDecimal getTodayRevenue() {
    return todayRevenue;
  }

  public void setTodayRevenue(BigDecimal todayRevenue) {
    this.todayRevenue = todayRevenue;
  }

  public BigDecimal getRevenueChangePercent() {
    return revenueChangePercent;
  }

  public void setRevenueChangePercent(BigDecimal revenueChangePercent) {
    this.revenueChangePercent = revenueChangePercent;
  }

  public long getTodayOrderCount() {
    return todayOrderCount;
  }

  public void setTodayOrderCount(long todayOrderCount) {
    this.todayOrderCount = todayOrderCount;
  }

  public long getOrderCountDelta() {
    return orderCountDelta;
  }

  public void setOrderCountDelta(long orderCountDelta) {
    this.orderCountDelta = orderCountDelta;
  }

  public BigDecimal getAverageOrderValue() {
    return averageOrderValue;
  }

  public void setAverageOrderValue(BigDecimal averageOrderValue) {
    this.averageOrderValue = averageOrderValue;
  }

  public BigDecimal getGrossProfitToday() {
    return grossProfitToday;
  }

  public void setGrossProfitToday(BigDecimal grossProfitToday) {
    this.grossProfitToday = grossProfitToday;
  }

  public BigDecimal getGrossProfitMarginPercent() {
    return grossProfitMarginPercent;
  }

  public void setGrossProfitMarginPercent(BigDecimal grossProfitMarginPercent) {
    this.grossProfitMarginPercent = grossProfitMarginPercent;
  }

  public long getReturnCountToday() {
    return returnCountToday;
  }

  public void setReturnCountToday(long returnCountToday) {
    this.returnCountToday = returnCountToday;
  }

  public BigDecimal getRefundAmountToday() {
    return refundAmountToday;
  }

  public void setRefundAmountToday(BigDecimal refundAmountToday) {
    this.refundAmountToday = refundAmountToday;
  }

  public List<RevenueBucketResponse> getLast7Days() {
    return last7Days;
  }

  public void setLast7Days(List<RevenueBucketResponse> last7Days) {
    this.last7Days = last7Days;
  }

  public List<TopProductResponse> getTopProducts() {
    return topProducts;
  }

  public void setTopProducts(List<TopProductResponse> topProducts) {
    this.topProducts = topProducts;
  }

  public List<RecentOrderResponse> getRecentOrders() {
    return recentOrders;
  }

  public void setRecentOrders(List<RecentOrderResponse> recentOrders) {
    this.recentOrders = recentOrders;
  }

  public List<InventoryResponse> getLowStock() {
    return lowStock;
  }

  public void setLowStock(List<InventoryResponse> lowStock) {
    this.lowStock = lowStock;
  }
}
