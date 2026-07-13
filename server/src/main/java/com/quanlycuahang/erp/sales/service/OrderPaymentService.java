package com.quanlycuahang.erp.sales.service;

import com.quanlycuahang.erp.partner.entity.Customer;
import com.quanlycuahang.erp.partner.entity.Debt;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.sales.dto.OrderPaymentRequest;
import com.quanlycuahang.erp.sales.dto.OrderPaymentResponse;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.entity.OrderPayment;
import com.quanlycuahang.erp.sales.repository.OrderPaymentRepository;
import com.quanlycuahang.erp.system.service.SettingsService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Prompt #2 (refactor OrderService god-class): tach buoc ghi OrderPayment (kem sinh VietQR khi
 * bank_transfer) va ghi Debt khi ban no cua createOrder() — KHONG doi hanh vi.
 */
@Service
public class OrderPaymentService {

  private final OrderPaymentRepository orderPaymentRepository;
  private final VietQrService vietQrService;
  private final SettingsService settingsService;
  private final DebtRepository debtRepository;

  public OrderPaymentService(
      OrderPaymentRepository orderPaymentRepository,
      VietQrService vietQrService,
      SettingsService settingsService,
      DebtRepository debtRepository) {
    this.orderPaymentRepository = orderPaymentRepository;
    this.vietQrService = vietQrService;
    this.settingsService = settingsService;
    this.debtRepository = debtRepository;
  }

  /** Ket qua buoc ghi thanh toan cua createOrder — dung chung giua OrderService va service nay. */
  public record PaymentCapture(List<OrderPaymentResponse> responses, boolean hasBankTransfer) {}

  public PaymentCapture capturePayments(Order order, List<OrderPaymentRequest> paymentRequests) {
    String bankBin = settingsService.getValue(null, SettingsService.KEY_BANK_BIN, "");
    String bankAccountNumber =
        settingsService.getValue(null, SettingsService.KEY_BANK_ACCOUNT_NUMBER, "");
    String bankAccountName =
        settingsService.getValue(null, SettingsService.KEY_BANK_ACCOUNT_NAME, "");

    List<OrderPaymentResponse> paymentResponses = new ArrayList<>();
    boolean hasBankTransfer = false;
    for (OrderPaymentRequest paymentRequest : paymentRequests) {
      OrderPayment payment = new OrderPayment();
      payment.setOrder(order);
      payment.setMethod(paymentRequest.getMethod());
      payment.setAmount(paymentRequest.getAmount());
      if ("bank_transfer".equals(paymentRequest.getMethod())) {
        hasBankTransfer = true;
        payment.setQrPayload(
            vietQrService.generatePayload(
                bankBin,
                bankAccountNumber,
                bankAccountName,
                null,
                paymentRequest.getAmount(),
                "Thanh toan " + order.getOrderNumber()));
      }
      orderPaymentRepository.save(payment);

      OrderPaymentResponse paymentResponse = new OrderPaymentResponse();
      paymentResponse.setMethod(payment.getMethod());
      paymentResponse.setAmount(payment.getAmount());
      paymentResponses.add(paymentResponse);
    }
    return new PaymentCapture(paymentResponses, hasBankTransfer);
  }

  public void recordDebtIfUnpaid(Order order, Customer customer, BigDecimal unpaid) {
    if (unpaid.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }
    Debt debt = new Debt();
    debt.setCustomer(customer);
    debt.setDirection("receivable");
    debt.setAmount(unpaid);
    debt.setOriginalAmount(unpaid);
    debt.setReferenceType("order");
    debt.setReferenceId(order.getId());
    debtRepository.save(debt);
  }
}
