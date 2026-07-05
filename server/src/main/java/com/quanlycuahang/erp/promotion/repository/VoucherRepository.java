package com.quanlycuahang.erp.promotion.repository;

import com.quanlycuahang.erp.promotion.entity.Voucher;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

  Optional<Voucher> findByCode(String code);
}
