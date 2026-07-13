package com.quanlycuahang.erp.ai.dto;

/**
 * KHONG BAO GIO chua gia tri khoa API that (da ma hoa hay chua) - chi bao "da cau hinh" hay chua,
 * xem Javadoc AiSettingsService.
 */
public class AiSettingsStatusResponse {

  private boolean configured;

  public AiSettingsStatusResponse(boolean configured) {
    this.configured = configured;
  }

  public boolean isConfigured() {
    return configured;
  }
}
