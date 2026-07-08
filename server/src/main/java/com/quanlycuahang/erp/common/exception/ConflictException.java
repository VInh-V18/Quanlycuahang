package com.quanlycuahang.erp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Xung dot du lieu (409) — vd trung SKU/barcode (DataIntegrityViolationException bat duoc o
 * GlobalExceptionHandler), hoac xung dot nghiep vu tu dinh nghia khac oversell (oversell dung rieng
 * OptimisticLockException, xem GlobalExceptionHandler).
 */
public class ConflictException extends AppException {

  public ConflictException(String code, String message) {
    super(code, HttpStatus.CONFLICT, message);
  }
}
