package com.quanlycuahang.erp.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Danh dau method Service nhay cam (sua gia, xoa, hoan tien, huy don, ket ca, doi phan quyen...) de
 * AuditAspect tu dong ghi vao audit_logs (D3) — khong goi tay AuditLogRepository o tung noi.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

  /** Ma hanh dong, vd PRODUCT_PRICE_UPDATE, ORDER_CANCEL, SHIFT_CLOSE. */
  String action();

  /** Ten Entity lien quan (tuy chon, chi de ghi chu). */
  String entityName() default "";
}
