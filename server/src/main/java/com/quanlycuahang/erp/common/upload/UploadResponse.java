package com.quanlycuahang.erp.common.upload;

public class UploadResponse {

  private final String fileName;
  private final String url;

  public UploadResponse(String fileName, String url) {
    this.fileName = fileName;
    this.url = url;
  }

  public String getFileName() {
    return fileName;
  }

  public String getUrl() {
    return url;
  }
}
