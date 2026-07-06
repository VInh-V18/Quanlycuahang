package com.quanlycuahang.erp.report.dto;

import java.math.BigDecimal;

/**
 * 1 dong doanh thu — label la ngay/tuan/thang (dang chuoi ISO) hoac ten chi nhanh/thu ngan tuy
 * groupBy, de FE dung chung 1 hinh dang du lieu cho moi cach nhom.
 */
public class RevenueBucketResponse {

  private String label;
  private BigDecimal revenue;
  private long orderCount;

  public RevenueBucketResponse() {}

  public RevenueBucketResponse(String label, BigDecimal revenue, long orderCount) {
    this.label = label;
    this.revenue = revenue;
    this.orderCount = orderCount;
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
}
