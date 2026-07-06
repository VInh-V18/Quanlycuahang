package com.quanlycuahang.erp.operation.service;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.operation.dto.CashTransactionRequest;
import com.quanlycuahang.erp.operation.dto.CashTransactionResponse;
import com.quanlycuahang.erp.operation.dto.CloseShiftRequest;
import com.quanlycuahang.erp.operation.dto.OpenShiftRequest;
import com.quanlycuahang.erp.operation.dto.ShiftDetailResponse;
import com.quanlycuahang.erp.operation.dto.ShiftSummaryResponse;
import com.quanlycuahang.erp.operation.entity.CashTransaction;
import com.quanlycuahang.erp.operation.entity.Shift;
import com.quanlycuahang.erp.operation.repository.CashTransactionRepository;
import com.quanlycuahang.erp.operation.repository.ShiftRepository;
import com.quanlycuahang.erp.sales.repository.OrderPaymentRepository;
import com.quanlycuahang.erp.sales.repository.OrderRepository;
import com.quanlycuahang.erp.sales.repository.ReturnRepository;
import com.quanlycuahang.erp.system.entity.Branch;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ca & ket tien (FH-14): mo/dong ca, ghi thu/chi tien mat ngoai don, doi chieu ket tien khi dong
 * ca. Tien mat du kien = tien dau ca + tong thanh toan tien mat cac don thuoc ca (order.shift_id) -
 * tien hoan tra bang tien mat trong cua so thoi gian cua ca (returns khong co shift_id rieng tu
 * Phase 9 nen doi chieu theo [openedAt, closedAt-hoac-hien-tai)) + thu tien mat - chi tien mat.
 */
@Service
public class ShiftService {

  private final ShiftRepository shiftRepository;
  private final CashTransactionRepository cashTransactionRepository;
  private final OrderPaymentRepository orderPaymentRepository;
  private final OrderRepository orderRepository;
  private final ReturnRepository returnRepository;
  private final CurrentUserProvider currentUserProvider;

  public ShiftService(
      ShiftRepository shiftRepository,
      CashTransactionRepository cashTransactionRepository,
      OrderPaymentRepository orderPaymentRepository,
      OrderRepository orderRepository,
      ReturnRepository returnRepository,
      CurrentUserProvider currentUserProvider) {
    this.shiftRepository = shiftRepository;
    this.cashTransactionRepository = cashTransactionRepository;
    this.orderPaymentRepository = orderPaymentRepository;
    this.orderRepository = orderRepository;
    this.returnRepository = returnRepository;
    this.currentUserProvider = currentUserProvider;
  }

  @Transactional
  public ShiftDetailResponse open(OpenShiftRequest request) {
    User currentUser = currentUserProvider.requireCurrentUser();

    shiftRepository
        .findFirstByOpenedByIdAndStatusOrderByOpenedAtDesc(currentUser.getId(), "open")
        .ifPresent(
            s -> {
              throw new BusinessRuleException(
                  "SHIFT_ALREADY_OPEN", "Ban dang co 1 ca chua dong, vui long dong ca do truoc");
            });

    Branch branch =
        currentUser.getBranches().stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "SHIFT_NO_BRANCH", "Tai khoan chua duoc gan chi nhanh nao"));

    Shift shift = new Shift();
    shift.setBranch(branch);
    shift.setOpenedBy(currentUser);
    shift.setOpeningCash(request.getOpeningCash());
    shift.setNote(request.getNote());
    shift.setStatus("open");
    shift.setOpenedAt(OffsetDateTime.now());

    return toDetail(shiftRepository.save(shift));
  }

  @Transactional(readOnly = true)
  public ShiftDetailResponse getCurrent() {
    User currentUser = currentUserProvider.requireCurrentUser();
    return shiftRepository
        .findFirstByOpenedByIdAndStatusOrderByOpenedAtDesc(currentUser.getId(), "open")
        .map(this::toDetail)
        .orElse(null);
  }

  @Transactional
  public ShiftDetailResponse close(Long id, CloseShiftRequest request) {
    Shift shift = requireShift(id);
    if (!"open".equals(shift.getStatus())) {
      throw new BusinessRuleException(
          "SHIFT_ALREADY_CLOSED", "Ca lam viec nay da duoc dong truoc do");
    }

    OffsetDateTime closedAt = OffsetDateTime.now();
    BigDecimal expectedCash = computeExpectedCash(shift, closedAt);

    shift.setActualCash(request.getActualCash());
    shift.setDiscrepancy(request.getActualCash().subtract(expectedCash));
    shift.setClosedAt(closedAt);
    shift.setStatus("closed");
    if (request.getNote() != null && !request.getNote().isBlank()) {
      shift.setNote(
          shift.getNote() == null || shift.getNote().isBlank()
              ? request.getNote()
              : shift.getNote() + " | " + request.getNote());
    }

    return toDetail(shiftRepository.save(shift));
  }

  @Transactional(readOnly = true)
  public ApiResponse<List<ShiftSummaryResponse>> history(String status, Pageable pageable) {
    Page<Shift> page = shiftRepository.search(status, pageable);
    return ApiResponse.page(page.map(ShiftService::toSummary));
  }

  @Transactional(readOnly = true)
  public ShiftDetailResponse getDetail(Long id) {
    return toDetail(requireShift(id));
  }

  @Transactional
  public CashTransactionResponse addCashTransaction(Long shiftId, CashTransactionRequest request) {
    Shift shift = requireShift(shiftId);
    if (!"open".equals(shift.getStatus())) {
      throw new BusinessRuleException(
          "SHIFT_ALREADY_CLOSED", "Khong the ghi thu/chi tien mat cho ca da dong");
    }
    if (!"cash_in".equals(request.getType()) && !"cash_out".equals(request.getType())) {
      throw new BusinessRuleException(
          "CASH_TRANSACTION_INVALID_TYPE", "Loai giao dich phai la cash_in hoac cash_out");
    }

    CashTransaction tx = new CashTransaction();
    tx.setShift(shift);
    tx.setType(request.getType());
    tx.setAmount(request.getAmount());
    tx.setNote(request.getNote());
    currentUserProvider.getCurrentUser().ifPresent(tx::setCreatedBy);

    return toResponse(cashTransactionRepository.save(tx));
  }

  private Shift requireShift(Long id) {
    return shiftRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ca lam viec"));
  }

  private BigDecimal computeExpectedCash(Shift shift, OffsetDateTime asOf) {
    BigDecimal cashSales = orderPaymentRepository.sumByShiftIdAndMethod(shift.getId(), "cash");
    BigDecimal cashRefunds =
        returnRepository.sumCashRefundsInWindow(
            shift.getBranch().getId(), shift.getOpenedAt(), asOf);
    BigDecimal cashIn = cashTransactionRepository.sumByShiftIdAndType(shift.getId(), "cash_in");
    BigDecimal cashOut = cashTransactionRepository.sumByShiftIdAndType(shift.getId(), "cash_out");
    return shift
        .getOpeningCash()
        .add(cashSales)
        .subtract(cashRefunds)
        .add(cashIn)
        .subtract(cashOut);
  }

  private ShiftDetailResponse toDetail(Shift shift) {
    ShiftDetailResponse dto = new ShiftDetailResponse();
    fillSummary(dto, shift);

    OffsetDateTime asOf = shift.getClosedAt() != null ? shift.getClosedAt() : OffsetDateTime.now();
    BigDecimal cashSales = orderPaymentRepository.sumByShiftIdAndMethod(shift.getId(), "cash");
    BigDecimal bankSales =
        orderPaymentRepository.sumByShiftIdAndMethod(shift.getId(), "bank_transfer");
    BigDecimal cardSales = orderPaymentRepository.sumByShiftIdAndMethod(shift.getId(), "card");
    BigDecimal cashRefunds =
        returnRepository.sumCashRefundsInWindow(
            shift.getBranch().getId(), shift.getOpenedAt(), asOf);
    BigDecimal cashIn = cashTransactionRepository.sumByShiftIdAndType(shift.getId(), "cash_in");
    BigDecimal cashOut = cashTransactionRepository.sumByShiftIdAndType(shift.getId(), "cash_out");

    dto.setCashSalesTotal(cashSales);
    dto.setBankTransferSalesTotal(bankSales);
    dto.setCardSalesTotal(cardSales);
    dto.setCashRefundTotal(cashRefunds);
    dto.setCashInTotal(cashIn);
    dto.setCashOutTotal(cashOut);
    dto.setExpectedCash(
        shift.getOpeningCash().add(cashSales).subtract(cashRefunds).add(cashIn).subtract(cashOut));
    dto.setOrderCount(orderRepository.countByShiftId(shift.getId()));
    dto.setCashTransactions(
        cashTransactionRepository.findByShiftIdOrderByCreatedAtDesc(shift.getId()).stream()
            .map(ShiftService::toResponse)
            .toList());
    return dto;
  }

  private static void fillSummary(ShiftSummaryResponse dto, Shift shift) {
    dto.setId(shift.getId());
    dto.setBranchName(shift.getBranch().getName());
    dto.setCashierName(shift.getOpenedBy().getFullName());
    dto.setOpeningCash(shift.getOpeningCash());
    dto.setActualCash(shift.getActualCash());
    dto.setDiscrepancy(shift.getDiscrepancy());
    dto.setNote(shift.getNote());
    dto.setStatus(shift.getStatus());
    dto.setOpenedAt(shift.getOpenedAt() == null ? null : shift.getOpenedAt().toInstant());
    dto.setClosedAt(shift.getClosedAt() == null ? null : shift.getClosedAt().toInstant());
  }

  private static ShiftSummaryResponse toSummary(Shift shift) {
    ShiftSummaryResponse dto = new ShiftSummaryResponse();
    fillSummary(dto, shift);
    return dto;
  }

  private static CashTransactionResponse toResponse(CashTransaction tx) {
    CashTransactionResponse dto = new CashTransactionResponse();
    dto.setId(tx.getId());
    dto.setType(tx.getType());
    dto.setAmount(tx.getAmount());
    dto.setNote(tx.getNote());
    dto.setCreatedByName(tx.getCreatedBy() == null ? null : tx.getCreatedBy().getFullName());
    dto.setCreatedAt(tx.getCreatedAt());
    return dto;
  }
}
