package com.quanlycuahang.erp.partner.service;

import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.partner.dto.SupplierRequest;
import com.quanlycuahang.erp.partner.dto.SupplierResponse;
import com.quanlycuahang.erp.partner.entity.Supplier;
import com.quanlycuahang.erp.partner.mapper.SupplierMapper;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.partner.repository.SupplierRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
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

  @Transactional(readOnly = true)
  public ApiResponse<List<SupplierResponse>> listWithStats(String search, Pageable pageable) {
    Page<Object[]> page =
        supplierRepository.searchWithStats(
            search == null ? "" : search.trim(), TenantContext.get(), pageable);
    return ApiResponse.page(page.map(SupplierService::toListItem));
  }

  private static SupplierResponse toListItem(Object[] row) {
    SupplierResponse response = new SupplierResponse();
    response.setId(((Number) row[0]).longValue());
    response.setName((String) row[1]);
    response.setPhone((String) row[2]);
    response.setAddress((String) row[3]);
    response.setTotalPurchased((BigDecimal) row[4]);
    response.setOrderCount(((Number) row[5]).longValue());
    response.setLastPurchaseAt(toInstant(row[6]));
    response.setOutstandingDebt((BigDecimal) row[7]);
    return response;
  }

  private static Instant toInstant(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof OffsetDateTime odt) {
      return odt.toInstant();
    }
    if (value instanceof java.sql.Timestamp ts) {
      return ts.toInstant();
    }
    throw new IllegalStateException("Khong the chuyen doi thoi gian: " + value.getClass());
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
