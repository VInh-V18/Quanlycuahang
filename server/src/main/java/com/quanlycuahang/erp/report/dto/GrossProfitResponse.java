package com.quanlycuahang.erp.report.dto;

import java.math.BigDecimal;

/** loi_nhuan_gop = doanh_thu - COGS - anh_huong_hoan_tra (B4/Phase 10). */
public class GrossProfitResponse {

  private BigDecimal revenue;
  private BigDecimal costOfGoodsSold;
  private BigDecimal returnImpact;
  private BigDecimal grossProfit;

  public GrossProfitResponse() {}

  public GrossProfitResponse(
      BigDecimal revenue, BigDecimal costOfGoodsSold, BigDecimal returnImpact) {
    this.revenue = revenue;
    this.costOfGoodsSold = costOfGoodsSold;
    this.returnImpact = returnImpact;
    this.grossProfit = revenue.subtract(costOfGoodsSold).subtract(returnImpact);
  }

  public BigDecimal getRevenue() {
    return revenue;
  }

  public void setRevenue(BigDecimal revenue) {
    this.revenue = revenue;
  }

  public BigDecimal getCostOfGoodsSold() {
    return costOfGoodsSold;
  }

  public void setCostOfGoodsSold(BigDecimal costOfGoodsSold) {
    this.costOfGoodsSold = costOfGoodsSold;
  }

  public BigDecimal getReturnImpact() {
    return returnImpact;
  }

  public void setReturnImpact(BigDecimal returnImpact) {
    this.returnImpact = returnImpact;
  }

  public BigDecimal getGrossProfit() {
    return grossProfit;
  }

  public void setGrossProfit(BigDecimal grossProfit) {
    this.grossProfit = grossProfit;
  }
}
