package com.quanlycuahang.erp.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quanlycuahang.erp.partner.entity.Customer;
import com.quanlycuahang.erp.partner.entity.Debt;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.sales.dto.OrderPaymentRequest;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.repository.OrderPaymentRepository;
import com.quanlycuahang.erp.system.service.SettingsService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Prompt #2, buoc 3: unit test OrderPaymentService voi repository/service mock. */
@ExtendWith(MockitoExtension.class)
class OrderPaymentServiceTest {

  @Mock private OrderPaymentRepository orderPaymentRepository;
  @Mock private VietQrService vietQrService;
  @Mock private SettingsService settingsService;
  @Mock private DebtRepository debtRepository;

  private OrderPaymentService service;

  @BeforeEach
  void setUp() {
    service =
        new OrderPaymentService(
            orderPaymentRepository, vietQrService, settingsService, debtRepository);
  }

  private OrderPaymentRequest payment(String method, BigDecimal amount) {
    OrderPaymentRequest request = new OrderPaymentRequest();
    request.setMethod(method);
    request.setAmount(amount);
    return request;
  }

  @Test
  void capturePaymentsGeneratesVietQrOnlyForBankTransferMethod() {
    Order order = new Order();
    order.setId(1L);
    order.setOrderNumber("HD-000001");
    when(settingsService.getValue(any(), anyString(), anyString())).thenReturn("");
    when(vietQrService.generatePayload(any(), any(), any(), any(), any(), any()))
        .thenReturn("QR_PAYLOAD");

    OrderPaymentService.PaymentCapture capture =
        service.capturePayments(
            order,
            List.of(
                payment("cash", BigDecimal.valueOf(50_000)),
                payment("bank_transfer", BigDecimal.valueOf(100_000))));

    assertThat(capture.hasBankTransfer()).isTrue();
    assertThat(capture.responses()).hasSize(2);
    verify(vietQrService, org.mockito.Mockito.times(1))
        .generatePayload(any(), any(), any(), any(), any(), any());
    verify(orderPaymentRepository, org.mockito.Mockito.times(2)).save(any());
  }

  @Test
  void capturePaymentsSkipsVietQrWhenAllCash() {
    Order order = new Order();
    order.setId(2L);
    order.setOrderNumber("HD-000002");

    OrderPaymentService.PaymentCapture capture =
        service.capturePayments(order, List.of(payment("cash", BigDecimal.valueOf(50_000))));

    assertThat(capture.hasBankTransfer()).isFalse();
    verify(vietQrService, never()).generatePayload(any(), any(), any(), any(), any(), any());
  }

  @Test
  void recordDebtIfUnpaidDoesNothingWhenFullyPaid() {
    Order order = new Order();
    order.setId(3L);
    service.recordDebtIfUnpaid(order, new Customer(), BigDecimal.ZERO);
    verify(debtRepository, never()).save(any());
  }

  @Test
  void recordDebtIfUnpaidSavesReceivableDebtWhenUnpaidPositive() {
    Order order = new Order();
    order.setId(4L);
    Customer customer = new Customer();
    customer.setId(9L);

    service.recordDebtIfUnpaid(order, customer, BigDecimal.valueOf(75_000));

    ArgumentCaptor<Debt> captor = ArgumentCaptor.forClass(Debt.class);
    verify(debtRepository).save(captor.capture());
    Debt debt = captor.getValue();
    assertThat(debt.getDirection()).isEqualTo("receivable");
    assertThat(debt.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(75_000));
    assertThat(debt.getOriginalAmount()).isEqualByComparingTo(BigDecimal.valueOf(75_000));
    assertThat(debt.getReferenceType()).isEqualTo("order");
    assertThat(debt.getReferenceId()).isEqualTo(4L);
  }
}
