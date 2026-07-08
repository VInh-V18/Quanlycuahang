package com.quanlycuahang.erp.operation.service;

import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.operation.dto.InvoiceDetailResponse;
import com.quanlycuahang.erp.operation.dto.InvoiceListItemResponse;
import com.quanlycuahang.erp.operation.entity.Invoice;
import com.quanlycuahang.erp.operation.invoice.InvoiceDetailAssembler;
import com.quanlycuahang.erp.operation.repository.InvoiceRepository;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.repository.OrderItemRepository;
import com.quanlycuahang.erp.sales.repository.OrderPaymentRepository;
import com.quanlycuahang.erp.sales.service.VietQrService;
import com.quanlycuahang.erp.system.service.SettingsService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Doc du lieu hoa don day du de tra JSON (Phase 9) — Backend khong render HTML/PDF (B3). */
@Service
public class InvoiceDetailService {

  private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final InvoiceRepository invoiceRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderPaymentRepository orderPaymentRepository;
  private final SettingsService settingsService;
  private final VietQrService vietQrService;
  private final BranchAccessGuard branchAccessGuard;

  public InvoiceDetailService(
      InvoiceRepository invoiceRepository,
      OrderItemRepository orderItemRepository,
      OrderPaymentRepository orderPaymentRepository,
      SettingsService settingsService,
      VietQrService vietQrService,
      BranchAccessGuard branchAccessGuard) {
    this.invoiceRepository = invoiceRepository;
    this.orderItemRepository = orderItemRepository;
    this.orderPaymentRepository = orderPaymentRepository;
    this.settingsService = settingsService;
    this.vietQrService = vietQrService;
    this.branchAccessGuard = branchAccessGuard;
  }

  @Transactional(readOnly = true)
  public ApiResponse<List<InvoiceListItemResponse>> list(
      Long branchId, LocalDate from, LocalDate to, String search, Pageable pageable) {
    branchAccessGuard.assertAccess(branchId);
    OffsetDateTime fromDateTime =
        from == null ? null : from.atStartOfDay(APP_ZONE).toOffsetDateTime();
    OffsetDateTime toDateTime =
        to == null ? null : to.plusDays(1).atStartOfDay(APP_ZONE).toOffsetDateTime();
    Page<Object[]> page =
        invoiceRepository.search(
            branchId,
            fromDateTime,
            toDateTime,
            search == null ? "" : search.trim(),
            TenantContext.get(),
            pageable);
    return ApiResponse.page(page.map(InvoiceDetailService::toListItem));
  }

  private static InvoiceListItemResponse toListItem(Object[] row) {
    InvoiceListItemResponse response = new InvoiceListItemResponse();
    response.setId(((Number) row[0]).longValue());
    response.setInvoiceNumber((String) row[1]);
    response.setIssuedAt(toInstant(row[2]));
    response.setOrderNumber((String) row[3]);
    response.setTotalAmount((BigDecimal) row[4]);
    response.setCustomerName((String) row[5]);
    response.setCustomerPhone((String) row[6]);
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

  @Transactional(readOnly = true)
  public InvoiceDetailResponse getById(Long id) {
    Invoice invoice =
        invoiceRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay hoa don"));
    return buildResponse(invoice);
  }

  @Transactional(readOnly = true)
  public InvoiceDetailResponse getByLookupCode(String lookupCode) {
    Invoice invoice =
        invoiceRepository
            .findByLookupCode(lookupCode)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay hoa don"));
    return buildResponse(invoice);
  }

  private InvoiceDetailResponse buildResponse(Invoice invoice) {
    Order order = invoice.getOrder();
    String bankAccountName = settingsService.getValue(null, SettingsService.KEY_BANK_ACCOUNT_NAME, "");
    String bankAccountNumber =
        settingsService.getValue(null, SettingsService.KEY_BANK_ACCOUNT_NUMBER, "");
    String bankBin = settingsService.getValue(null, SettingsService.KEY_BANK_BIN, "");

    InvoiceDetailResponse response =
        InvoiceDetailAssembler.assemble(
            invoice,
            order,
            orderItemRepository.findByOrderId(order.getId()),
            orderPaymentRepository.findByOrderId(order.getId()),
            settingsService.getValue(null, SettingsService.KEY_STORE_NAME, ""),
            settingsService.getValue(null, SettingsService.KEY_STORE_TAX_CODE, ""),
            settingsService.getValue(null, SettingsService.KEY_STORE_PHONE, ""),
            bankAccountName,
            bankAccountNumber,
            settingsService.getValue(null, SettingsService.KEY_BANK_NAME, ""),
            settingsService.getValue(null, SettingsService.KEY_BANK_QR_IMAGE_URL, ""),
            order.getCashier().getFullName(),
            order.getCustomer() == null ? null : order.getCustomer().getName(),
            order.getCustomer() == null ? null : order.getCustomer().getPhone(),
            order.getCustomer() == null ? null : order.getCustomer().getEmail(),
            order.getCustomer() == null ? null : order.getCustomer().getAddress());

    // Luon sinh lai QR "song" theo tong tien hoa don hien tai (thay vi dung qrPayload da luu luc
    // tao don, chi khop khi khach chon dung "Chuyen khoan") — de hoa don nao cung quet duoc, dung
    // so tien, du khach tra tien mat hay chuyen khoan (theo yeu cau FH). Khong sinh neu cua hang
    // chua cau hinh so TK nhan tien (tranh QR tro ve tai khoan mac dinh khong thuoc ve ai).
    if (!bankAccountNumber.isBlank()) {
      response.setQrPayload(
          vietQrService.generatePayload(
              bankBin,
              bankAccountNumber,
              bankAccountName,
              null,
              order.getTotalAmount(),
              "Thanh toan " + invoice.getInvoiceNumber()));
    } else {
      response.setQrPayload(null);
    }
    return response;
  }
}
