package com.quanlycuahang.erp.partner.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.partner.dto.CustomerGroupResponse;
import com.quanlycuahang.erp.partner.repository.CustomerGroupRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Danh sach nhom khach hang (VIP/Than thiet/Doanh nghiep/Moi...) — dung cho bo loc + form o trang
 * Khach hang (FH-11). Chua co UC quan ly nhom rieng nen chi doc, chua CRUD.
 */
@RestController
@RequestMapping("/api/v1/customer-groups")
public class CustomerGroupController {

  private final CustomerGroupRepository customerGroupRepository;

  public CustomerGroupController(CustomerGroupRepository customerGroupRepository) {
    this.customerGroupRepository = customerGroupRepository;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('customer:view')")
  public ResponseEntity<ApiResponse<List<CustomerGroupResponse>>> list() {
    List<CustomerGroupResponse> groups =
        customerGroupRepository.findAll().stream()
            .map(g -> new CustomerGroupResponse(g.getId(), g.getName()))
            .toList();
    return ResponseEntity.ok(ApiResponse.success(groups));
  }
}
