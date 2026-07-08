package com.quanlycuahang.erp.operation.invoice;

import com.quanlycuahang.erp.operation.dto.InvoiceDetailResponse;
import com.quanlycuahang.erp.operation.service.InvoiceDetailService;
import com.quanlycuahang.erp.operation.service.InvoiceEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Gui email hoa don SAU KHI transaction ban hang commit thanh cong (D4) — neu don huy/rollback
 * truoc khi commit, event khong bao gio duoc xu ly nen khong gui nham email cho don khong ton tai.
 */
@Component
public class InvoiceEmailListener {

  private static final Logger log = LoggerFactory.getLogger(InvoiceEmailListener.class);

  private final InvoiceDetailService invoiceDetailService;
  private final InvoiceEmailService invoiceEmailService;

  public InvoiceEmailListener(
      InvoiceDetailService invoiceDetailService, InvoiceEmailService invoiceEmailService) {
    this.invoiceDetailService = invoiceDetailService;
    this.invoiceEmailService = invoiceEmailService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onInvoiceCreated(InvoiceCreatedEvent event) {
    try {
      InvoiceDetailResponse invoice = invoiceDetailService.getById(event.invoiceId());
      if (invoice.getCustomerEmail() == null || invoice.getCustomerEmail().isBlank()) {
        return;
      }
      invoiceEmailService.sendInvoiceEmail(invoice, invoice.getCustomerEmail());
    } catch (Exception ex) {
      log.error(
          "Xu ly InvoiceCreatedEvent that bai cho invoiceId={}: {}",
          event.invoiceId(),
          ex.getMessage(),
          ex);
    }
  }
}
