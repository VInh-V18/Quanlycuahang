package com.quanlycuahang.erp.common.dto;

import java.util.Map;

/** Phan "error" trong response D2 khi success = false. */
public class ApiError {

  private String code;
  private String message;
  private Map<String, Object> details;

  public ApiError() {}

  public ApiError(String code, String message, Map<String, Object> details) {
    this.code = code;
    this.message = message;
    this.details = details;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Map<String, Object> getDetails() {
    return details;
  }

  public void setDetails(Map<String, Object> details) {
    this.details = details;
  }
}
