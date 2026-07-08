package com.quanlycuahang.erp.common.exception;

import org.springframework.http.HttpStatus;

/** Khong tim thay resource theo id/dieu kien (404). */
public class ResourceNotFoundException extends AppException {

  public ResourceNotFoundException(String message) {
    super("NOT_FOUND", HttpStatus.NOT_FOUND, message);
  }

  public ResourceNotFoundException(String code, String message) {
    super(code, HttpStatus.NOT_FOUND, message);
  }
}
