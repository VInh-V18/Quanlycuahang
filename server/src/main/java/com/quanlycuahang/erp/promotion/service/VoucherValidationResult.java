package com.quanlycuahang.erp.promotion.service;

import com.quanlycuahang.erp.promotion.entity.Voucher;
import java.math.BigDecimal;

public record VoucherValidationResult(Voucher voucher, BigDecimal discountAmount) {}
