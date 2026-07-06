package com.quanlycuahang.erp.common.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Goc cua toan bo exception nghiep vu. Moi exception con mang theo ma loi UPPER_SNAKE on dinh (D2),
 * HTTP status tuong ung, va details tuy chon de FE hien thi chi tiet (vd danh sach field loi
 * validation).
 */
public abstract class AppException extends RuntimeException {

  private final String code;
  private final HttpStatus httpStatus;
  private final Map<String, Object> details;

  protected AppException(String code, HttpStatus httpStatus, String message) {
    this(code, httpStatus, message, Map.of());
  }

  protected AppException(
      String code, HttpStatus httpStatus, String message, Map<String, Object> details) {
    super(message);
    this.code = code;
    this.httpStatus = httpStatus;
    this.details = details;
  }

  public String getCode() {
    return code;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public Map<String, Object> getDetails() {
    return details;
  }
}
