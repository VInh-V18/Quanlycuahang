package com.quanlycuahang.erp.common.exception;

import org.springframework.http.HttpStatus;

/** Thieu quyen thuc hien hanh dong (403) — dung khi kiem tra quyen thu cong ngoai @PreAuthorize. */
public class PermissionDeniedException extends AppException {

  public PermissionDeniedException(String message) {
    super("PERMISSION_DENIED", HttpStatus.FORBIDDEN, message);
  }
}
