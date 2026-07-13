package com.quanlycuahang.erp.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Yeu cau "AI giai thich" tren ReportsPage (Prompt #12) - {@code dataContext} PHAI la so lieu TONG
 * HOP (vd {"doanhThuThang6": 12000000, "topSanPham": [...]}) ma FE da tu tinh san tu du lieu report
 * hien co tren man hinh, KHONG phai toan bo dong du lieu tho (tranh gui qua nhieu du lieu khong can
 * thiet len AI + giu dung nguyen tac "AI chi doc du lieu da qua Backend xu ly", khong tu truy van).
 */
public class AiExplainRequest {

  @NotBlank
  @Size(max = 500, message = "Câu hỏi tối đa 500 ký tự")
  private String question;

  @NotNull private Map<String, Object> dataContext;

  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }

  public Map<String, Object> getDataContext() {
    return dataContext;
  }

  public void setDataContext(Map<String, Object> dataContext) {
    this.dataContext = dataContext;
  }
}
