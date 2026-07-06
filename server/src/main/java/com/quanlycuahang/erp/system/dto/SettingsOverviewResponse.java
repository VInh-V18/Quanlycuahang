package com.quanlycuahang.erp.system.dto;

import java.util.Map;

/**
 * settings: cau hinh co the sua qua PUT /settings. systemInfo: thong tin he thong CHI DOC (vd TTL
 * JWT doc tu application.yml) — hien thi de tham khao trong tab Bao mat & he thong nhung khong gui
 * len PUT vi doi can sua config + restart, khong phai settings dong (FH-16).
 */
public class SettingsOverviewResponse {

  private Map<String, String> settings;
  private Map<String, String> systemInfo;

  public SettingsOverviewResponse() {}

  public SettingsOverviewResponse(Map<String, String> settings, Map<String, String> systemInfo) {
    this.settings = settings;
    this.systemInfo = systemInfo;
  }

  public Map<String, String> getSettings() {
    return settings;
  }

  public void setSettings(Map<String, String> settings) {
    this.settings = settings;
  }

  public Map<String, String> getSystemInfo() {
    return systemInfo;
  }

  public void setSystemInfo(Map<String, String> systemInfo) {
    this.systemInfo = systemInfo;
  }
}
