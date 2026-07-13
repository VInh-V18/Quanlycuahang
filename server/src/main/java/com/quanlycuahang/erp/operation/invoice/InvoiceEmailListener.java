package com.quanlycuahang.erp.operation.invoice;

import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.operation.service.InvoiceEmailService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Gui email hoa don SAU KHI transaction ban hang commit thanh cong (D4) — neu don huy/rollback
 * truoc khi commit, event khong bao gio duoc xu ly nen khong gui nham email cho don khong ton tai.
 *
 * <p>Chi ghi lai tenantId roi giao het cho InvoiceEmailService.sendInvoiceEmailAsync() (@Async,
 * chay tren thread khac) — TenantContext PHAI doc o day (con tren thread request goc), khong the
 * doc lai ben trong ham @Async vi ThreadLocal khong tu ke thua sang thread moi.
 */
@Component
public class InvoiceEmailListener {

  private final InvoiceEmailService invoiceEmailService;

  public InvoiceEmailListener(InvoiceEmailService invoiceEmailService) {
    this.invoiceEmailService = invoiceEmailService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onInvoiceCreated(InvoiceCreatedEvent event) {
    invoiceEmailService.sendInvoiceEmailAsync(event.invoiceId(), TenantContext.get());
  }
}
