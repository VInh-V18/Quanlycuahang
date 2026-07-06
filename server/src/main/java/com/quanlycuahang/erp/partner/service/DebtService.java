package com.quanlycuahang.erp.partner.service;

import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.partner.dto.DebtHistoryEventResponse;
import com.quanlycuahang.erp.partner.dto.DebtPartnerAgingResponse;
import com.quanlycuahang.erp.partner.dto.DebtPaymentRequest;
import com.quanlycuahang.erp.partner.dto.DebtSummaryResponse;
import com.quanlycuahang.erp.partner.entity.Customer;
import com.quanlycuahang.erp.partner.entity.Debt;
import com.quanlycuahang.erp.partner.entity.DebtPayment;
import com.quanlycuahang.erp.partner.entity.Supplier;
import com.quanlycuahang.erp.partner.repository.CustomerRepository;
import com.quanlycuahang.erp.partner.repository.DebtPaymentRepository;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.partner.repository.SupplierRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cong no chi tiet (FH-12): tuoi no theo tung doi tac (0-7/8-30/>30 ngay, khac voi tong hop toan he
 * thong o ReportService/Phase 10), lich su doi chieu, va ghi nhan thanh toan (phan bo FIFO qua cac
 * Debt con du cua 1 doi tac — giong cach ReturnService da giam no khi tra hang, Phase 9).
 */
@Service
public class DebtService {

  private final DebtRepository debtRepository;
  private final DebtPaymentRepository debtPaymentRepository;
  private final CustomerRepository customerRepository;
  private final SupplierRepository supplierRepository;
  private final CurrentUserProvider currentUserProvider;

  public DebtService(
      DebtRepository debtRepository,
      DebtPaymentRepository debtPaymentRepository,
      CustomerRepository customerRepository,
      SupplierRepository supplierRepository,
      CurrentUserProvider currentUserProvider) {
    this.debtRepository = debtRepository;
    this.debtPaymentRepository = debtPaymentRepository;
    this.customerRepository = customerRepository;
    this.supplierRepository = supplierRepository;
    this.currentUserProvider = currentUserProvider;
  }

  @Transactional(readOnly = true)
  public DebtSummaryResponse summary() {
    DebtSummaryResponse response = new DebtSummaryResponse();

    List<Object[]> receivable = debtRepository.summaryByDirection("receivable");
    if (!receivable.isEmpty()) {
      response.setReceivableTotal((BigDecimal) receivable.get(0)[0]);
      response.setReceivableCount(((Number) receivable.get(0)[1]).longValue());
    } else {
      response.setReceivableTotal(BigDecimal.ZERO);
    }

    List<Object[]> payable = debtRepository.summaryByDirection("payable");
    if (!payable.isEmpty()) {
      response.setPayableTotal((BigDecimal) payable.get(0)[0]);
      response.setPayableCount(((Number) payable.get(0)[1]).longValue());
    } else {
      response.setPayableTotal(BigDecimal.ZERO);
    }

    BigDecimal overdue =
        debtRepository.findAgingByPartner("receivable").stream()
            .map(row -> (BigDecimal) row[5])
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    response.setOverdueReceivable(overdue);

    return response;
  }

  @Transactional(readOnly = true)
  public List<DebtPartnerAgingResponse> agingByPartner(String direction) {
    return debtRepository.findAgingByPartner(direction).stream()
        .map(
            row -> {
              DebtPartnerAgingResponse dto = new DebtPartnerAgingResponse();
              dto.setPartnerId(((Number) row[0]).longValue());
              dto.setPartnerName((String) row[1]);
              dto.setTotalDebt((BigDecimal) row[2]);
              dto.setBucket0to7((BigDecimal) row[3]);
              dto.setBucket8to30((BigDecimal) row[4]);
              dto.setBucketOver30((BigDecimal) row[5]);
              return dto;
            })
        .toList();
  }

  @Transactional(readOnly = true)
  public List<DebtHistoryEventResponse> history(String direction, Long partnerId) {
    Long customerId = "receivable".equals(direction) ? partnerId : null;
    Long supplierId = "payable".equals(direction) ? partnerId : null;
    return debtRepository.findHistory(direction, customerId, supplierId).stream()
        .map(
            row -> {
              DebtHistoryEventResponse dto = new DebtHistoryEventResponse();
              dto.setEventAt(toInstant(row[0]));
              dto.setLabel((String) row[1]);
              dto.setReferenceCode((String) row[2]);
              dto.setAmount((BigDecimal) row[3]);
              return dto;
            })
        .toList();
  }

  @Transactional
  public void recordPayment(DebtPaymentRequest request) {
    List<Debt> outstanding;
    if ("receivable".equals(request.getDirection())) {
      Customer customer =
          customerRepository
              .findById(request.getPartnerId())
              .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay khach hang"));
      outstanding =
          debtRepository.findByCustomerIdAndDirectionAndAmountGreaterThanOrderByCreatedAtAsc(
              customer.getId(), "receivable", BigDecimal.ZERO);
    } else if ("payable".equals(request.getDirection())) {
      Supplier supplier =
          supplierRepository
              .findById(request.getPartnerId())
              .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nha cung cap"));
      outstanding =
          debtRepository.findBySupplierIdAndDirectionAndAmountGreaterThanOrderByCreatedAtAsc(
              supplier.getId(), "payable", BigDecimal.ZERO);
    } else {
      throw new BusinessRuleException("DEBT_INVALID_DIRECTION", "Chieu cong no khong hop le");
    }

    if (outstanding.isEmpty()) {
      throw new BusinessRuleException("DEBT_NONE_OUTSTANDING", "Doi tac khong con cong no");
    }

    BigDecimal remaining = request.getAmount();
    for (Debt debt : outstanding) {
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }
      BigDecimal applied = remaining.min(debt.getAmount());
      debt.setAmount(debt.getAmount().subtract(applied));
      debtRepository.save(debt);

      DebtPayment payment = new DebtPayment();
      payment.setDebt(debt);
      payment.setAmount(applied);
      payment.setMethod(request.getMethod());
      payment.setNote(request.getNote());
      payment.setPaidAt(OffsetDateTime.now());
      currentUserProvider.getCurrentUser().ifPresent(payment::setCreatedBy);
      debtPaymentRepository.save(payment);

      remaining = remaining.subtract(applied);
    }

    if (remaining.compareTo(BigDecimal.ZERO) > 0) {
      throw new BusinessRuleException(
          "DEBT_PAYMENT_EXCEEDS_OUTSTANDING",
          "So tien thanh toan vuot qua tong cong no con du cua doi tac");
    }
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
}
