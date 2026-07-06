package com.quanlycuahang.erp.partner.service;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.partner.dto.CustomerRequest;
import com.quanlycuahang.erp.partner.dto.CustomerResponse;
import com.quanlycuahang.erp.partner.entity.Customer;
import com.quanlycuahang.erp.partner.entity.CustomerGroup;
import com.quanlycuahang.erp.partner.mapper.CustomerMapper;
import com.quanlycuahang.erp.partner.repository.CustomerGroupRepository;
import com.quanlycuahang.erp.partner.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD khach hang (UC-07). */
@Service
public class CustomerService {

  private final CustomerRepository customerRepository;
  private final CustomerGroupRepository customerGroupRepository;
  private final CustomerMapper customerMapper;

  public CustomerService(
      CustomerRepository customerRepository,
      CustomerGroupRepository customerGroupRepository,
      CustomerMapper customerMapper) {
    this.customerRepository = customerRepository;
    this.customerGroupRepository = customerGroupRepository;
    this.customerMapper = customerMapper;
  }

  @Transactional(readOnly = true)
  public ApiResponse<java.util.List<CustomerResponse>> search(String search, Pageable pageable) {
    Page<Customer> page = customerRepository.search(search == null ? "" : search.trim(), pageable);
    return ApiResponse.page(page.map(customerMapper::toResponse));
  }

  @Transactional(readOnly = true)
  public CustomerResponse getById(Long id) {
    return customerMapper.toResponse(
        customerRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay khach hang")));
  }

  @Transactional
  public CustomerResponse create(CustomerRequest request) {
    if (request.getPhone() != null && customerRepository.existsByPhone(request.getPhone())) {
      throw new BusinessRuleException("CUSTOMER_DUPLICATE_PHONE", "So dien thoai da ton tai");
    }
    Customer customer = new Customer();
    applyRequest(customer, request);
    return customerMapper.toResponse(customerRepository.save(customer));
  }

  @Transactional
  public CustomerResponse update(Long id, CustomerRequest request) {
    Customer customer =
        customerRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay khach hang"));
    applyRequest(customer, request);
    return customerMapper.toResponse(customerRepository.save(customer));
  }

  private void applyRequest(Customer customer, CustomerRequest request) {
    customer.setName(request.getName());
    customer.setPhone(request.getPhone());
    customer.setAddress(request.getAddress());
    customer.setEmail(request.getEmail());
    customer.setDebtLimit(request.getDebtLimit());
    if (request.getCustomerGroupId() != null) {
      CustomerGroup group =
          customerGroupRepository
              .findById(request.getCustomerGroupId())
              .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhom khach hang"));
      customer.setCustomerGroup(group);
    } else {
      customer.setCustomerGroup(null);
    }
  }
}
