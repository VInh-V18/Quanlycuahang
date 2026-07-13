package com.quanlycuahang.erp.platform.entity;

import com.quanlycuahang.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Nhat ky audit RIENG cho hanh dong cua Super Admin (tao/khoa tenant, CRUD tai khoan bat ky tenant
 * nao, dat lai mat khau...) - KHONG ke thua TenantScopedEntity (khac AuditLog thuong cua tenant
 * User): hanh dong Super Admin khong gan voi dung 1 tenant duy nhat (vd tao tenant moi), va request
 * cua Super Admin khong bao gio co TenantContext - neu dung chung AuditLog (tenant_id NOT NULL) thi
 * AuditAspect se luon bo qua khong ghi (day chinh la lo hong da phat hien khi rieng soat bao mat:
 * MOI hanh dong Super Admin truoc day hoan toan khong de lai dau vet). tenant_id o day la NULLABLE
 * va CHI de tham khao/loc, khong phai khoa loc du lieu.
 */
@Entity
@Table(name = "platform_audit_logs")
public class PlatformAuditLog extends BaseEntity {

  @Column(name = "platform_admin_username", nullable = false)
  private String platformAdminUsername;

  @Column(name = "action", nullable = false)
  private String action;

  @Column(name = "tenant_id")
  private Long tenantId;

  @Column(name = "target_description")
  private String targetDescription;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "detail", columnDefinition = "jsonb")
  private String detail;

  @Column(name = "client_ip")
  private String clientIp;

  @Column(name = "user_agent")
  private String userAgent;

  public String getPlatformAdminUsername() {
    return platformAdminUsername;
  }

  public void setPlatformAdminUsername(String platformAdminUsername) {
    this.platformAdminUsername = platformAdminUsername;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getTargetDescription() {
    return targetDescription;
  }

  public void setTargetDescription(String targetDescription) {
    this.targetDescription = targetDescription;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public String getClientIp() {
    return clientIp;
  }

  public void setClientIp(String clientIp) {
    this.clientIp = clientIp;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }
}
