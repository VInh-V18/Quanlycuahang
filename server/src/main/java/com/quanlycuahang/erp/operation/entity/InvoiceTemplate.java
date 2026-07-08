package com.quanlycuahang.erp.operation.entity;

import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import com.quanlycuahang.erp.system.entity.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

/**
 * Mau in hoa don. paperSize: K80 (mac dinh), K58, A4 (B3 — Backend chi tra JSON, khong sinh HTML).
 */
@Entity
@Table(name = "invoice_templates")
@SQLDelete(sql = "UPDATE invoice_templates SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class InvoiceTemplate extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "branch_id")
  private Branch branch;

  @Column(name = "paper_size", nullable = false)
  private String paperSize = "K80";

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "template_config", columnDefinition = "jsonb")
  private String templateConfig;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault;

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }

  public String getPaperSize() {
    return paperSize;
  }

  public void setPaperSize(String paperSize) {
    this.paperSize = paperSize;
  }

  public String getTemplateConfig() {
    return templateConfig;
  }

  public void setTemplateConfig(String templateConfig) {
    this.templateConfig = templateConfig;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public void setDefault(boolean isDefault) {
    this.isDefault = isDefault;
  }
}
