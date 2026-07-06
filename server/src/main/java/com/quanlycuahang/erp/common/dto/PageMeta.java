package com.quanlycuahang.erp.common.dto;

import org.springframework.data.domain.Page;

/** Phan "meta" trong response D2 khi tra ve danh sach co phan trang (Spring Pageable). */
public class PageMeta {

  private int page;
  private int limit;
  private long total;

  public PageMeta() {}

  public PageMeta(int page, int limit, long total) {
    this.page = page;
    this.limit = limit;
    this.total = total;
  }

  public static PageMeta from(Page<?> page) {
    return new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements());
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }
}
