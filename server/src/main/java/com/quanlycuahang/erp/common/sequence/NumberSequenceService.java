package com.quanlycuahang.erp.common.sequence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Sinh so nguyen theo tung tenant (order_number, invoice_number, sku) qua bang tenant_sequences
 * (V14) - moi tenant co day so rieng, doc lap voi tenant khac (khong nhay coc), dam bao khong trung
 * so khi nhieu request tao don/hoa don/san pham dong thoi (POS nhieu quay). INSERT ... ON CONFLICT
 * DO UPDATE ... RETURNING la 1 cau lenh nguyen tu duy nhat - Postgres tu khoa dong xung dot, khong
 * can SELECT ... FOR UPDATE thu cong rieng.
 */
@Component
public class NumberSequenceService {

  public static final String ORDER_NUMBER_SEQ = "order_number_seq";
  public static final String INVOICE_NUMBER_SEQ = "invoice_number_seq";
  public static final String SKU_SEQ = "sku_seq";

  private static final String UPSERT_SQL =
      "INSERT INTO tenant_sequences (tenant_id, sequence_name, current_value) "
          + "VALUES (?, ?, 1) "
          + "ON CONFLICT (tenant_id, sequence_name) "
          + "DO UPDATE SET current_value = tenant_sequences.current_value + 1 "
          + "RETURNING current_value";

  private final JdbcTemplate jdbcTemplate;

  public NumberSequenceService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long nextValue(Long tenantId, String sequenceName) {
    Long value = jdbcTemplate.queryForObject(UPSERT_SQL, Long.class, tenantId, sequenceName);
    return value;
  }
}
