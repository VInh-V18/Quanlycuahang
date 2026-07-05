package com.quanlycuahang.erp.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlycuahang.erp.common.dto.ApiError;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Tra ve dung format D2 khi da dang nhap nhung thieu quyen truy cap. */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ApiError error =
        new ApiError("PERMISSION_DENIED", "Ban khong co quyen thuc hien hanh dong nay", Map.of());
    objectMapper.writeValue(response.getWriter(), ApiResponse.error(error));
  }
}
