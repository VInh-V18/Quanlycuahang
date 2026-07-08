package com.quanlycuahang.erp.common.exception;

import com.quanlycuahang.erp.common.dto.ApiError;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Xu ly tap trung moi exception thanh response chuan D2. Khong de exception tho lot ra ngoai — moi
 * nhanh deu tra ve ApiResponse.error(...) voi ma loi UPPER_SNAKE on dinh.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(AppException.class)
  public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
    log.warn("Business exception: code={}, message={}", ex.getCode(), ex.getMessage());
    ApiError error = new ApiError(ex.getCode(), ex.getMessage(), ex.getDetails());
    return ResponseEntity.status(ex.getHttpStatus()).body(ApiResponse.error(error));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, Object> fieldErrors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
    ApiError error = new ApiError("VALIDATION_ERROR", "Dữ liệu không hợp lệ", fieldErrors);
    return ResponseEntity.badRequest().body(ApiResponse.error(error));
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(
      OptimisticLockingFailureException ex) {
    log.warn("Optimistic lock conflict: {}", ex.getMessage());
    ApiError error =
        new ApiError(
            "PRODUCT_OUT_OF_STOCK",
            "Sản phẩm vừa hết hàng hoặc đang được cập nhật, vui lòng thử lại",
            Map.of());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    log.warn("Data integrity violation: {}", ex.getMessage());
    ApiError error = new ApiError("CONFLICT", "Dữ liệu bị trùng hoặc vi phạm ràng buộc", Map.of());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
    ApiError error =
        new ApiError("PERMISSION_DENIED", "Bạn không có quyền thực hiện hành động này", Map.of());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(error));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
    ApiError error =
        new ApiError("AUTH_UNAUTHENTICATED", "Chưa đăng nhập hoặc phiên đã hết hạn", Map.of());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(error));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
    log.error("Unexpected error", ex);
    ApiError error =
        new ApiError("INTERNAL_ERROR", "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau", Map.of());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error));
  }
}
