package com.quanlycuahang.erp.sales.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlycuahang.erp.common.dto.ApiError;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.web.IdempotencyService;
import com.quanlycuahang.erp.sales.dto.OrderResponse;
import com.quanlycuahang.erp.sales.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Kiem tra header Idempotency-Key TRUOC khi vao Controller (B4 edge case 7): request lap lai voi
 * cung key tra ve dung ket qua lan dau (khong tao don thu 2); request khac dang xu ly cung key
 * (chua xong) -> 409 tam thoi. Ket qua thanh cong duoc OrderService tu ghi vao Redis (xem
 * OrderService.createOrder) — Interceptor chi giai phong khoa khi that bai.
 */
@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

  private static final String HEADER = "Idempotency-Key";
  private static final String REQUEST_ATTR = "idempotencyKey";

  private final IdempotencyService idempotencyService;
  private final OrderService orderService;
  private final ObjectMapper objectMapper;

  public IdempotencyInterceptor(
      IdempotencyService idempotencyService, OrderService orderService, ObjectMapper objectMapper) {
    this.idempotencyService = idempotencyService;
    this.orderService = orderService;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    String key = request.getHeader(HEADER);
    if (key == null || key.isBlank()) {
      return true;
    }

    Optional<String> completed = idempotencyService.getCompletedResult(key);
    if (completed.isPresent()) {
      OrderResponse cached = orderService.getById(Long.valueOf(completed.get()));
      writeJson(response, HttpServletResponse.SC_OK, ApiResponse.success(cached));
      return false;
    }

    if (idempotencyService.isProcessing(key)) {
      writeJson(
          response,
          HttpServletResponse.SC_CONFLICT,
          ApiResponse.error(
              new ApiError(
                  "REQUEST_IN_PROGRESS",
                  "Yeu cau dang duoc xu ly, vui long doi trong giay lat",
                  Map.of())));
      return false;
    }

    if (!idempotencyService.tryClaim(key)) {
      writeJson(
          response,
          HttpServletResponse.SC_CONFLICT,
          ApiResponse.error(
              new ApiError(
                  "REQUEST_IN_PROGRESS",
                  "Yeu cau dang duoc xu ly, vui long doi trong giay lat",
                  Map.of())));
      return false;
    }

    request.setAttribute(REQUEST_ATTR, key);
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    String key = (String) request.getAttribute(REQUEST_ATTR);
    if (key == null) {
      return;
    }
    // OrderService.createOrder da tu "complete" key khi thanh cong; o day chi can giai phong
    // neu that bai (loi/exception) de client co the thu lai voi cung key.
    if (ex != null || response.getStatus() >= 400) {
      idempotencyService.release(key);
    }
  }

  private void writeJson(HttpServletResponse response, int status, Object body)
      throws java.io.IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), body);
  }
}
