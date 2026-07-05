package com.quanlycuahang.erp.common.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Loi validate du lieu dau vao (400) — dung khi validate thu cong ngoai @Valid. */
public class ValidationException extends AppException {

  public ValidationException(String message) {
    super("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, message);
  }

  public ValidationException(String message, Map<String, Object> details) {
    super("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, message, details);
  }
}
