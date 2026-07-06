package com.quanlycuahang.erp.report.dto;

import java.math.BigDecimal;

/**
 * 1 dong doanh thu — label la ngay/tuan/thang (dang chuoi ISO) hoac ten chi nhanh/thu ngan tuy
 * groupBy, de FE dung chung 1 hinh dang du lieu cho moi cach nhom. grossProfit = revenue - COGS,
 * dung de ve chart 2 chuoi Doanh thu/Loi nhuan gop tren trang Bao cao (FH-15).
 */
public class RevenueBucketResponse {

  private String label;
  private BigDecimal revenue;
  private long orderCount;
  private BigDecimal costOfGoodsSold;
  private BigDecimal grossProfit;

  public RevenueBucketResponse() {}

  public RevenueBucketResponse(
      String label, BigDecimal revenue, long orderCount, BigDecimal costOfGoodsSold) {
    this.label = label;
    this.revenue = revenue;
    this.orderCount = orderCount;
    this.costOfGoodsSold = costOfGoodsSold;
    this.grossProfit = revenue.subtract(costOfGoodsSold);
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public BigDecimal getRevenue() {
    return revenue;
  }

  public void setRevenue(BigDecimal revenue) {
    this.revenue = revenue;
  }

  public long getOrderCount() {
    return orderCount;
  }

  public void setOrderCount(long orderCount) {
    this.orderCount = orderCount;
  }

  public BigDecimal getCostOfGoodsSold() {
    return costOfGoodsSold;
  }

  public void setCostOfGoodsSold(BigDecimal costOfGoodsSold) {
    this.costOfGoodsSold = costOfGoodsSold;
  }

  public BigDecimal getGrossProfit() {
    return grossProfit;
  }

  public void setGrossProfit(BigDecimal grossProfit) {
    this.grossProfit = grossProfit;
  }
}
