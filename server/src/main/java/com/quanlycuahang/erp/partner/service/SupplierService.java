package com.quanlycuahang.erp.partner.service;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.partner.dto.SupplierRequest;
import com.quanlycuahang.erp.partner.dto.SupplierResponse;
import com.quanlycuahang.erp.partner.entity.Supplier;
import com.quanlycuahang.erp.partner.mapper.SupplierMapper;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.partner.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {

  private final SupplierRepository supplierRepository;
  private final DebtRepository debtRepository;
  private final SupplierMapper supplierMapper;

  public SupplierService(
      SupplierRepository supplierRepository,
      DebtRepository debtRepository,
      SupplierMapper supplierMapper) {
    this.supplierRepository = supplierRepository;
    this.debtRepository = debtRepository;
    this.supplierMapper = supplierMapper;
  }

  @Transactional(readOnly = true)
  public ApiResponse<java.util.List<SupplierResponse>> list(Pageable pageable) {
    Page<Supplier> page = supplierRepository.findAll(pageable);
    ApiResponse<java.util.List<SupplierResponse>> response =
        ApiResponse.page(page.map(supplierMapper::toResponse));
    for (SupplierResponse supplier : response.getData()) {
      supplier.setOutstandingDebt(debtRepository.sumOutstandingBySupplierId(supplier.getId()));
    }
    return response;
  }

  @Transactional
  public SupplierResponse create(SupplierRequest request) {
    Supplier supplier = new Supplier();
    applyRequest(supplier, request);
    return supplierMapper.toResponse(supplierRepository.save(supplier));
  }

  @Transactional
  public SupplierResponse update(Long id, SupplierRequest request) {
    Supplier supplier =
        supplierRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nha cung cap"));
    applyRequest(supplier, request);
    return supplierMapper.toResponse(supplierRepository.save(supplier));
  }

  private void applyRequest(Supplier supplier, SupplierRequest request) {
    supplier.setName(request.getName());
    supplier.setPhone(request.getPhone());
    supplier.setAddress(request.getAddress());
  }
}
