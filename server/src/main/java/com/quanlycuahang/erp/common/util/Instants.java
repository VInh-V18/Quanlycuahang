package com.quanlycuahang.erp.common.util;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Chuyen gia tri thoi gian tho doc tu native query (Object[] row) ve Instant - tuy cot va phien ban
 * JDBC driver ma gia tri tra ve co the la Instant, OffsetDateTime hay java.sql.Timestamp. Truoc day
 * ham nay bi copy y het o 7 Service khac nhau (phat hien khi rieng soat) - moi noi doc ket qua
 * native query dung chung ham nay, khong tu che ban rieng.
 */
public final class Instants {

  private Instants() {}

  public static Instant toInstant(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof OffsetDateTime odt) {
      return odt.toInstant();
    }
    if (value instanceof java.sql.Timestamp ts) {
      return ts.toInstant();
    }
    throw new IllegalStateException("Khong the chuyen doi thoi gian: " + value.getClass());
  }
}
