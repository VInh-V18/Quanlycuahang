package com.quanlycuahang.erp.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiAskRequest {

  @NotBlank
  @Size(max = 500, message = "Câu hỏi tối đa 500 ký tự")
  private String question;

  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }
}
