package com.quanlycuahang.erp.partner.service;

import static com.quanlycuahang.erp.common.util.Instants.toInstant;

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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    List<Long> supplierIds = response.getData().stream().map(SupplierResponse::getId).toList();
    if (!supplierIds.isEmpty()) {
      Map<Long, BigDecimal> outstandingBySupplierId =
          debtRepository.sumOutstandingBySupplierIds(supplierIds).stream()
              .collect(Collectors.toMap(row -> (Long) row[0], row -> (BigDecimal) row[1]));
      for (SupplierResponse supplier : response.getData()) {
        supplier.setOutstandingDebt(
            outstandingBySupplierId.getOrDefault(supplier.getId(), BigDecimal.ZERO));
      }
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
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp"));
    applyRequest(supplier, request);
    return supplierMapper.toResponse(supplierRepository.save(supplier));
  }

  private void applyRequest(Supplier supplier, SupplierRequest request) {
    supplier.setName(request.getName());
    supplier.setPhone(request.getPhone());
    supplier.setAddress(request.getAddress());
  }
}
