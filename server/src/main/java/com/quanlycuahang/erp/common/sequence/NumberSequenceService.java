package com.quanlycuahang.erp.common.sequence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Sinh so nguyen tu tu PostgreSQL SEQUENCE (order_number_seq, invoice_number_seq, sku_seq) — dam
 * bao khong trung so khi nhieu request tao don/hoa don/san pham dong thoi (POS nhieu quay).
 */
@Component
public class NumberSequenceService {

  public static final String ORDER_NUMBER_SEQ = "order_number_seq";
  public static final String INVOICE_NUMBER_SEQ = "invoice_number_seq";
  public static final String SKU_SEQ = "sku_seq";

  private final JdbcTemplate jdbcTemplate;

  public NumberSequenceService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long nextValue(String sequenceName) {
    Long value = jdbcTemplate.queryForObject("SELECT nextval(?)", Long.class, sequenceName);
    return value;
  }
}
