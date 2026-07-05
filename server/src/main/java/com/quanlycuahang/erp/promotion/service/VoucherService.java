package com.quanlycuahang.erp.promotion.service;

import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.promotion.entity.Voucher;
import com.quanlycuahang.erp.promotion.entity.VoucherUsage;
import com.quanlycuahang.erp.promotion.repository.VoucherRepository;
import com.quanlycuahang.erp.promotion.repository.VoucherUsageRepository;
import com.quanlycuahang.erp.sales.entity.Order;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verify hieu luc voucher va tinh so tien giam (UC-11). Goi ca tu FE (kiem tra som khi ap dung o
 * POS) lan Backend (tai kiem tra doc lap luc tao don — khong tin tuong FE, B4 UC-04).
 */
@Service
public class VoucherService {

  private final VoucherRepository voucherRepository;
  private final VoucherUsageRepository voucherUsageRepository;

  public VoucherService(
      VoucherRepository voucherRepository, VoucherUsageRepository voucherUsageRepository) {
    this.voucherRepository = voucherRepository;
    this.voucherUsageRepository = voucherUsageRepository;
  }

  @Transactional(readOnly = true)
  public VoucherValidationResult validate(String code, BigDecimal orderSubtotal) {
    Voucher voucher =
        voucherRepository
            .findByCode(code)
            .orElseThrow(
                () -> new BusinessRuleException("VOUCHER_INVALID", "Voucher khong ton tai"));

    if (!voucher.isActive()) {
      throw new BusinessRuleException("VOUCHER_INVALID", "Voucher khong con hieu luc");
    }
    if (voucher.getExpiresAt() != null && voucher.getExpiresAt().isBefore(OffsetDateTime.now())) {
      throw new BusinessRuleException("VOUCHER_INVALID", "Voucher da het han");
    }
    if (voucher.getUsedCount() >= voucher.getMaxUsage()) {
      throw new BusinessRuleException("VOUCHER_INVALID", "Voucher da het luot su dung");
    }
    if (orderSubtotal.compareTo(voucher.getMinOrderAmount()) < 0) {
      throw new BusinessRuleException(
          "VOUCHER_INVALID", "Don hang chua dat gia tri toi thieu de ap dung voucher");
    }

    BigDecimal discountAmount =
        "percentage".equals(voucher.getDiscountType())
            ? orderSubtotal
                .multiply(voucher.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            : voucher.getDiscountValue().setScale(0, RoundingMode.HALF_UP);

    return new VoucherValidationResult(voucher, discountAmount);
  }

  @Transactional
  public void recordUsage(Voucher voucher, Order order) {
    voucher.setUsedCount(voucher.getUsedCount() + 1);
    voucherRepository.save(voucher);

    VoucherUsage usage = new VoucherUsage();
    usage.setVoucher(voucher);
    usage.setOrder(order);
    usage.setUsedAt(OffsetDateTime.now());
    voucherUsageRepository.save(usage);
  }
}
