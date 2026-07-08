package com.quanlycuahang.erp.operation.entity;

import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import com.quanlycuahang.erp.sales.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/** Hoa don — Backend chi tra JSON, khong sinh HTML/PDF (B3). 1-1 voi Order. */
@Entity
@Table(name = "invoices")
@SQLDelete(sql = "UPDATE invoices SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Invoice extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false, unique = true)
  private Order order;

  @Column(name = "invoice_number", nullable = false, unique = true)
  private String invoiceNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_template_id")
  private InvoiceTemplate invoiceTemplate;

  @Column(name = "qr_payload", columnDefinition = "text")
  private String qrPayload;

  @Column(name = "lookup_code")
  private String lookupCode;

  @Column(name = "issued_at", nullable = false)
  private OffsetDateTime issuedAt;

  public Order getOrder() {
    return order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public String getInvoiceNumber() {
    return invoiceNumber;
  }

  public void setInvoiceNumber(String invoiceNumber) {
    this.invoiceNumber = invoiceNumber;
  }

  public InvoiceTemplate getInvoiceTemplate() {
    return invoiceTemplate;
  }

  public void setInvoiceTemplate(InvoiceTemplate invoiceTemplate) {
    this.invoiceTemplate = invoiceTemplate;
  }

  public String getQrPayload() {
    return qrPayload;
  }

  public void setQrPayload(String qrPayload) {
    this.qrPayload = qrPayload;
  }

  public String getLookupCode() {
    return lookupCode;
  }

  public void setLookupCode(String lookupCode) {
    this.lookupCode = lookupCode;
  }

  public OffsetDateTime getIssuedAt() {
    return issuedAt;
  }

  public void setIssuedAt(OffsetDateTime issuedAt) {
    this.issuedAt = issuedAt;
  }
}
