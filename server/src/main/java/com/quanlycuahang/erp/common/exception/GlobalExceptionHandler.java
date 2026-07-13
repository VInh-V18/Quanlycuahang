package com.quanlycuahang.erp.common.exception;

import com.quanlycuahang.erp.common.dto.ApiError;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
    // Thong bao chung (khong noi rieng "san pham") - @Version (chong ghi de dong thoi) gio dung
    // tren nhieu Entity khac nhau (Inventory, Voucher, Shift...), khong chi ton kho; thong bao cu
    // ("San pham vua het hang") gay hieu nham khi xung dot thuc su la o dong ca lam viec/voucher.
    ApiError error =
        new ApiError(
            "CONCURRENT_UPDATE_CONFLICT",
            "Dữ liệu vừa được cập nhật bởi thao tác khác, vui lòng tải lại và thử lại",
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

  /**
   * Duong dan khong ton tai (endpoint da bi xoa/doi cho, hoac client goi sai URL) - truoc day roi
   * vao handleUnexpected() ben duoi, tra ve nham 500 INTERNAL_ERROR thay vi 404 dung ban chat (phat
   * hien khi rieng soat: sau khi chuyen 1 endpoint sang duong khac, goi lai duong cu tra ve 500).
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
    ApiError error = new ApiError("NOT_FOUND", "Không tìm thấy đường dẫn này", Map.of());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
  }

  /**
   * Goi dung method HTTP khong duoc ho tro cho duong dan nay (vd POST vao 1 endpoint chi nhan GET)
   * - cung truoc day roi vao handleUnexpected() thanh 500.
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex) {
    ApiError error =
        new ApiError(
            "METHOD_NOT_ALLOWED", "Phương thức không được hỗ trợ cho đường dẫn này", Map.of());
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.error(error));
  }

  /**
   * Body request khong phai JSON hop le (thieu, sai dinh dang, encoding loi...) - loi cua CLIENT
   * (400), khong phai loi he thong (500) nhu truoc day.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
      HttpMessageNotReadableException ex) {
    log.warn("Malformed request body: {}", ex.getMessage());
    ApiError error = new ApiError("MALFORMED_REQUEST", "Dữ liệu gửi lên không hợp lệ", Map.of());
    return ResponseEntity.badRequest().body(ApiResponse.error(error));
  }

  /**
   * EntityManager.getReference()/TenantAwareRepositoryImpl.getReferenceById() nem ra khi tham chieu
   * toi 1 dong khong ton tai (hoac thuoc tenant khac) - loi CLIENT (404), khong phai 500.
   */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(EntityNotFoundException ex) {
    ApiError error = new ApiError("NOT_FOUND", "Không tìm thấy dữ liệu", Map.of());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
    log.error("Unexpected error", ex);
    ApiError error =
        new ApiError("INTERNAL_ERROR", "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau", Map.of());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error));
  }
}
