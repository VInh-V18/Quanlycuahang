package com.quanlycuahang.erp.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.operation.entity.Invoice;
import com.quanlycuahang.erp.operation.invoice.InvoiceCreatedEvent;
import com.quanlycuahang.erp.operation.repository.InvoiceRepository;
import com.quanlycuahang.erp.promotion.entity.Voucher;
import com.quanlycuahang.erp.promotion.service.VoucherService;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.system.service.SettingsService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;

/** Prompt #2, buoc 3: unit test OrderFinalizationService voi repository/service mock. */
@ExtendWith(MockitoExtension.class)
class OrderFinalizationServiceTest {

  @Mock private VoucherService voucherService;
  @Mock private InvoiceRepository invoiceRepository;
  @Mock private VietQrService vietQrService;
  @Mock private SettingsService settingsService;
  @Mock private ApplicationEventPublisher eventPublisher;

  private OrderFinalizationService service;

  @BeforeEach
  void setUp() {
    service =
        new OrderFinalizationService(
            voucherService, invoiceRepository, vietQrService, settingsService, eventPublisher);
  }

  @Test
  void recordVoucherUsageIfAnyDoesNothingWhenVoucherNull() {
    service.recordVoucherUsageIfAny(null, new Order());
    verify(voucherService, never()).recordUsage(any(), any());
  }

  @Test
  void recordVoucherUsageIfAnyDelegatesWhenVoucherPresent() {
    Voucher voucher = new Voucher();
    Order order = new Order();
    service.recordVoucherUsageIfAny(voucher, order);
    verify(voucherService).recordUsage(voucher, order);
  }

  @Test
  void recordVoucherUsageTranslatesOptimisticLockFailureToVoucherInvalidError() {
    Voucher voucher = new Voucher();
    Order order = new Order();
    doThrow(new OptimisticLockingFailureException("conflict"))
        .when(voucherService)
        .recordUsage(voucher, order);

    assertThatThrownBy(() -> service.recordVoucherUsageIfAny(voucher, order))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("hết lượt sử dụng");
  }

  @Test
  void issueInvoiceGeneratesQrOnlyWhenHasBankTransfer() {
    Order order = new Order();
    order.setId(1L);
    order.setOrderNumber("HD-000001");
    when(settingsService.getValue(any(), anyString(), anyString())).thenReturn("");
    when(vietQrService.generatePayload(any(), any(), any(), any(), any(), any()))
        .thenReturn("QR_PAYLOAD");
    when(invoiceRepository.save(any(Invoice.class)))
        .thenAnswer(
            invocation -> {
              Invoice invoice = invocation.getArgument(0);
              invoice.setId(500L);
              return invoice;
            });

    Invoice invoice = service.issueInvoice(order, "INV-000001", BigDecimal.valueOf(200_000), true);

    assertThat(invoice.getId()).isEqualTo(500L);
    assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-000001");
    assertThat(invoice.getQrPayload()).isEqualTo("QR_PAYLOAD");
    assertThat(invoice.getLookupCode()).hasSize(32); // UUID 128-bit khong dau gach ngang

    ArgumentCaptor<InvoiceCreatedEvent> eventCaptor =
        ArgumentCaptor.forClass(InvoiceCreatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().invoiceId()).isEqualTo(500L);
  }

  @Test
  void issueInvoiceSkipsQrWhenNoBankTransfer() {
    Order order = new Order();
    order.setId(2L);
    order.setOrderNumber("HD-000002");
    when(invoiceRepository.save(any(Invoice.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Invoice invoice = service.issueInvoice(order, "INV-000002", BigDecimal.valueOf(50_000), false);

    assertThat(invoice.getQrPayload()).isNull();
    verify(vietQrService, never()).generatePayload(any(), any(), any(), any(), any(), any());
    verify(settingsService, never()).getValue(any(), anyString(), anyString());
  }
}
