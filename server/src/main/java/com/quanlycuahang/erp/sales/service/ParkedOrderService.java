package com.quanlycuahang.erp.sales.service;

import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.sales.dto.ParkedOrderRequest;
import com.quanlycuahang.erp.sales.dto.ParkedOrderResponse;
import com.quanlycuahang.erp.sales.entity.ParkedOrder;
import com.quanlycuahang.erp.sales.mapper.ParkedOrderMapper;
import com.quanlycuahang.erp.sales.repository.ParkedOrderRepository;
import com.quanlycuahang.erp.system.entity.Branch;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Treo/mo lai don POS (UC-18) — chua tru kho, chua phai Order chinh thuc. */
@Service
public class ParkedOrderService {

  private final ParkedOrderRepository parkedOrderRepository;
  private final BranchRepository branchRepository;
  private final ParkedOrderMapper parkedOrderMapper;
  private final CurrentUserProvider currentUserProvider;
  private final BranchAccessGuard branchAccessGuard;

  public ParkedOrderService(
      ParkedOrderRepository parkedOrderRepository,
      BranchRepository branchRepository,
      ParkedOrderMapper parkedOrderMapper,
      CurrentUserProvider currentUserProvider,
      BranchAccessGuard branchAccessGuard) {
    this.parkedOrderRepository = parkedOrderRepository;
    this.branchRepository = branchRepository;
    this.parkedOrderMapper = parkedOrderMapper;
    this.currentUserProvider = currentUserProvider;
    this.branchAccessGuard = branchAccessGuard;
  }

  @Transactional
  public ParkedOrderResponse park(ParkedOrderRequest request) {
    branchAccessGuard.assertAccess(request.getBranchId());
    Branch branch =
        branchRepository
            .findById(request.getBranchId())
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay chi nhanh"));
    ParkedOrder parkedOrder = new ParkedOrder();
    parkedOrder.setBranch(branch);
    parkedOrder.setCartSnapshot(request.getCartSnapshot());
    parkedOrder.setNote(request.getNote());
    parkedOrder.setParkedAt(OffsetDateTime.now());
    currentUserProvider.getCurrentUser().ifPresent(parkedOrder::setCreatedBy);
    return parkedOrderMapper.toResponse(parkedOrderRepository.save(parkedOrder));
  }

  @Transactional(readOnly = true)
  public List<ParkedOrderResponse> listByBranch(Long branchId) {
    branchAccessGuard.assertAccess(branchId);
    return parkedOrderRepository.findByBranchIdOrderByParkedAtDesc(branchId).stream()
        .map(parkedOrderMapper::toResponse)
        .toList();
  }

  @Transactional
  public ParkedOrderResponse resume(Long id) {
    ParkedOrder parkedOrder =
        parkedOrderRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don treo"));
    branchAccessGuard.assertAccess(parkedOrder.getBranch().getId());
    ParkedOrderResponse response = parkedOrderMapper.toResponse(parkedOrder);
    parkedOrderRepository.delete(parkedOrder);
    return response;
  }
}
