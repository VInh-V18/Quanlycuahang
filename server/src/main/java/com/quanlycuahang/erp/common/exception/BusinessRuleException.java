package com.quanlycuahang.erp.common.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Vi pham quy tac nghiep vu (422) — vd PRODUCT_OUT_OF_STOCK, ORDER_PRICE_MISMATCH, VOUCHER_INVALID,
 * RETURN_QUANTITY_EXCEEDED. Ma loi (code) do noi goi truyen vao, thuoc 1 trong cac nhom da chuan
 * hoa D2.
 */
public class BusinessRuleException extends AppException {

  public BusinessRuleException(String code, String message) {
    super(code, HttpStatus.UNPROCESSABLE_ENTITY, message);
  }

  public BusinessRuleException(String code, String message, Map<String, Object> details) {
    super(code, HttpStatus.UNPROCESSABLE_ENTITY, message, details);
  }
}
