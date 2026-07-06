package com.quanlycuahang.erp.operation.invoice;

/**
 * Phat sinh ngay sau khi Invoice duoc luu trong transaction ban hang — lang nghe qua
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} de gui email SAU KHI commit, khong gui
 * trong transaction ban hang (D4: khong goi IO ngoai/email trong @Transactional).
 */
public record InvoiceCreatedEvent(Long invoiceId) {}
