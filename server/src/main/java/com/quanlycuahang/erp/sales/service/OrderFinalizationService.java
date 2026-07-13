package com.quanlycuahang.erp.sales.service;

import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.operation.entity.Invoice;
import com.quanlycuahang.erp.operation.invoice.InvoiceCreatedEvent;
import com.quanlycuahang.erp.operation.repository.InvoiceRepository;
import com.quanlycuahang.erp.promotion.entity.Voucher;
import com.quanlycuahang.erp.promotion.service.VoucherService;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.system.service.SettingsService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Prompt #2 (refactor OrderService god-class): tach 2 buoc "chot don" cuoi cung cua createOrder() —
 * ghi nhan da dung voucher + xuat hoa don (kem VietQR neu co bank_transfer) — KHONG doi hanh vi.
 *
 * <p>5 dependency (vuot goi y "≤4 dependency" cua roadmap Prompt #2): VoucherService,
 * InvoiceRepository, VietQrService, SettingsService, ApplicationEventPublisher deu can thiet cho
 * dung 2 buoc "chot don" gan lien nhau nay — tach nho hon nua se tao 1-2 class chi con vai dong
 * logic that (con lai la delegate thuan), uu tien co ket logic theo dung mo ta cua roadmap hon la
 * dat cung so dem tuyet doi. Da ghi ro trong bao cao truoc/sau (docs/PROJECT_STATE.md).
 */
@Service
public class OrderFinalizationService {

  private final VoucherService voucherService;
  private final InvoiceRepository invoiceRepository;
  private final VietQrService vietQrService;
  private final SettingsService settingsService;
  private final ApplicationEventPublisher eventPublisher;

  public OrderFinalizationService(
      VoucherService voucherService,
      InvoiceRepository invoiceRepository,
      VietQrService vietQrService,
      SettingsService settingsService,
      ApplicationEventPublisher eventPublisher) {
    this.voucherService = voucherService;
    this.invoiceRepository = invoiceRepository;
    this.vietQrService = vietQrService;
    this.settingsService = settingsService;
    this.eventPublisher = eventPublisher;
  }

  public void recordVoucherUsageIfAny(Voucher voucher, Order order) {
    if (voucher == null) {
      return;
    }
    try {
      voucherService.recordUsage(voucher, order);
    } catch (OptimisticLockingFailureException ex) {
      // @Version tren Voucher phat hien 2 don dung cung 1 voucher gan het luot cung luc — dich
      // thanh loi ro rang thay vi de GlobalExceptionHandler map chung thanh loi ton kho (sai ngu
      // canh, gay hieu lam la ton kho chu khong phai voucher).
      throw new BusinessRuleException(
          "VOUCHER_INVALID",
          "Voucher vừa hết lượt sử dụng do có đơn khác dùng cùng lúc, vui lòng tải lại giỏ hàng");
    }
  }

  /**
   * invoiceNumber duoc orchestrator (OrderService) tinh san qua NumberSequenceService — giu nguyen
   * tach biet voi service nay de khong phai them NumberSequenceService lam dependency thu 6.
   */
  public Invoice issueInvoice(
      Order order, String invoiceNumber, BigDecimal grandTotal, boolean hasBankTransfer) {
    Invoice invoice = new Invoice();
    invoice.setOrder(order);
    invoice.setInvoiceNumber(invoiceNumber);
    // UUID DAY DU (128 bit), khong cat con 8 ky tu (32 bit) nhu truoc - endpoint tra cuu hoa don
    // cong khai (khong dang nhap) chi dua vao do kho doan cua ma nay, xem V18__invoice_lookup_code_
    // entropy.sql. Khong can logic retry khi trung: xac suat trung UUID day du la khong dang ke.
    invoice.setLookupCode(UUID.randomUUID().toString().replace("-", "").toUpperCase());
    invoice.setIssuedAt(OffsetDateTime.now());
    if (hasBankTransfer) {
      invoice.setQrPayload(
          vietQrService.generatePayload(
              settingsService.getValue(null, SettingsService.KEY_BANK_BIN, ""),
              settingsService.getValue(null, SettingsService.KEY_BANK_ACCOUNT_NUMBER, ""),
              settingsService.getValue(null, SettingsService.KEY_BANK_ACCOUNT_NAME, ""),
              null,
              grandTotal,
              "Thanh toan " + order.getOrderNumber()));
    }
    invoice = invoiceRepository.save(invoice);
    eventPublisher.publishEvent(new InvoiceCreatedEvent(invoice.getId()));
    return invoice;
  }
}
