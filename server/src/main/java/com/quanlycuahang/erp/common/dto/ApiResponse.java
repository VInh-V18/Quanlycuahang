package com.quanlycuahang.erp.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Bao boc response thong nhat cho toan bo API (D2): { "success": true, "data": {}, "meta": {...} }
 * hoac { "success": false, "error": {...} }.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

  private boolean success;
  private T data;
  private PageMeta meta;
  private ApiError error;

  private ApiResponse() {}

  public static <T> ApiResponse<T> success(T data) {
    ApiResponse<T> response = new ApiResponse<>();
    response.success = true;
    response.data = data;
    return response;
  }

  public static <T> ApiResponse<T> success(T data, PageMeta meta) {
    ApiResponse<T> response = new ApiResponse<>();
    response.success = true;
    response.data = data;
    response.meta = meta;
    return response;
  }

  public static <T> ApiResponse<List<T>> page(Page<T> page) {
    ApiResponse<List<T>> response = new ApiResponse<>();
    response.success = true;
    response.data = page.getContent();
    response.meta = PageMeta.from(page);
    return response;
  }

  public static ApiResponse<Void> error(ApiError error) {
    ApiResponse<Void> response = new ApiResponse<>();
    response.success = false;
    response.error = error;
    return response;
  }

  public boolean isSuccess() {
    return success;
  }

  public T getData() {
    return data;
  }

  public PageMeta getMeta() {
    return meta;
  }

  public ApiError getError() {
    return error;
  }
}
