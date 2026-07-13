package com.quanlycuahang.erp.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class AiSettingsUpdateRequest {

  @NotBlank(message = "Khoá API không được để trống")
  private String apiKey;

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
}
