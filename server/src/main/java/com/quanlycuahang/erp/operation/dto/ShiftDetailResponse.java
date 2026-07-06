package com.quanlycuahang.erp.operation.dto;

import java.math.BigDecimal;
import java.util.List;

public class ShiftDetailResponse extends ShiftSummaryResponse {

  private BigDecimal cashSalesTotal;
  private BigDecimal bankTransferSalesTotal;
  private BigDecimal cardSalesTotal;
  private BigDecimal cashRefundTotal;
  private BigDecimal cashInTotal;
  private BigDecimal cashOutTotal;
  private BigDecimal expectedCash;
  private long orderCount;
  private List<CashTransactionResponse> cashTransactions;

  public BigDecimal getCashSalesTotal() {
    return cashSalesTotal;
  }

  public void setCashSalesTotal(BigDecimal cashSalesTotal) {
    this.cashSalesTotal = cashSalesTotal;
  }

  public BigDecimal getBankTransferSalesTotal() {
    return bankTransferSalesTotal;
  }

  public void setBankTransferSalesTotal(BigDecimal bankTransferSalesTotal) {
    this.bankTransferSalesTotal = bankTransferSalesTotal;
  }

  public BigDecimal getCardSalesTotal() {
    return cardSalesTotal;
  }

  public void setCardSalesTotal(BigDecimal cardSalesTotal) {
    this.cardSalesTotal = cardSalesTotal;
  }

  public BigDecimal getCashRefundTotal() {
    return cashRefundTotal;
  }

  public void setCashRefundTotal(BigDecimal cashRefundTotal) {
    this.cashRefundTotal = cashRefundTotal;
  }

  public BigDecimal getCashInTotal() {
    return cashInTotal;
  }

  public void setCashInTotal(BigDecimal cashInTotal) {
    this.cashInTotal = cashInTotal;
  }

  public BigDecimal getCashOutTotal() {
    return cashOutTotal;
  }

  public void setCashOutTotal(BigDecimal cashOutTotal) {
    this.cashOutTotal = cashOutTotal;
  }

  public BigDecimal getExpectedCash() {
    return expectedCash;
  }

  public void setExpectedCash(BigDecimal expectedCash) {
    this.expectedCash = expectedCash;
  }

  public long getOrderCount() {
    return orderCount;
  }

  public void setOrderCount(long orderCount) {
    this.orderCount = orderCount;
  }

  public List<CashTransactionResponse> getCashTransactions() {
    return cashTransactions;
  }

  public void setCashTransactions(List<CashTransactionResponse> cashTransactions) {
    this.cashTransactions = cashTransactions;
  }
}
