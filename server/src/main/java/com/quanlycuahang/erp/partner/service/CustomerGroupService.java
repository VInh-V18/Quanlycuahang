package com.quanlycuahang.erp.partner.service;

import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.partner.dto.CustomerGroupRequest;
import com.quanlycuahang.erp.partner.dto.CustomerGroupResponse;
import com.quanlycuahang.erp.partner.entity.CustomerGroup;
import com.quanlycuahang.erp.partner.repository.CustomerGroupRepository;
import com.quanlycuahang.erp.partner.repository.CustomerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quan ly nhom khach hang (VIP/Than thiet/Doanh nghiep...) — dung de loc + gan cho khach hang o
 * trang Khach hang, khong co quy tac chiet khau rieng gan voi nhom (chiet khau nam o Voucher).
 */
@Service
public class CustomerGroupService {

  private final CustomerGroupRepository customerGroupRepository;
  private final CustomerRepository customerRepository;

  public CustomerGroupService(
      CustomerGroupRepository customerGroupRepository, CustomerRepository customerRepository) {
    this.customerGroupRepository = customerGroupRepository;
    this.customerRepository = customerRepository;
  }

  @Transactional(readOnly = true)
  public List<CustomerGroupResponse> list() {
    return customerGroupRepository.findAll().stream()
        .map(CustomerGroupService::toResponse)
        .toList();
  }

  @Transactional
  public CustomerGroupResponse create(CustomerGroupRequest request) {
    CustomerGroup group = new CustomerGroup();
    group.setName(request.getName());
    return toResponse(customerGroupRepository.save(group));
  }

  @Transactional
  public CustomerGroupResponse update(Long id, CustomerGroupRequest request) {
    CustomerGroup group =
        customerGroupRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm khách hàng"));
    group.setName(request.getName());
    return toResponse(customerGroupRepository.save(group));
  }

  @Transactional
  public void delete(Long id) {
    if (!customerGroupRepository.existsById(id)) {
      throw new ResourceNotFoundException("Không tìm thấy nhóm khách hàng");
    }
    if (customerRepository.existsByCustomerGroupId(id)) {
      throw new BusinessRuleException(
          "CUSTOMER_GROUP_HAS_CUSTOMERS",
          "Nhóm còn khách hàng, vui lòng chuyển khách sang nhóm khác trước khi xóa");
    }
    customerGroupRepository.deleteById(id);
  }

  private static CustomerGroupResponse toResponse(CustomerGroup group) {
    return new CustomerGroupResponse(group.getId(), group.getName());
  }
}
