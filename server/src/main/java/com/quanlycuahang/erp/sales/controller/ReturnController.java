package com.quanlycuahang.erp.sales.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.sales.dto.ReturnRequest;
import com.quanlycuahang.erp.sales.dto.ReturnResponse;
import com.quanlycuahang.erp.sales.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/returns")
public class ReturnController {

  private final ReturnService returnService;

  public ReturnController(ReturnService returnService) {
    this.returnService = returnService;
  }

  @PostMapping
  @PreAuthorize("hasAuthority('return:create')")
  public ResponseEntity<ApiResponse<ReturnResponse>> create(
      @Valid @RequestBody ReturnRequest request) {
    return ResponseEntity.ok(ApiResponse.success(returnService.createReturn(request)));
  }
}
