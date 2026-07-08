package com.quanlycuahang.erp.common.entity;

import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.system.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Lop cha chung cho MOI Entity thuoc ve 1 Tenant cu the (tat ca Entity nghiep vu — tru
 * Role/Permission/RolePermission la dinh nghia quyen dung chung moi tenant, va Tenant/PlatformAdmin
 * tu than). @Filter tu dong loc tenant_id cho MOI truy van JPQL/Criteria — giong co che
 * @Where(deleted_at IS NULL) da co san o BaseEntity, ke thua tu MappedSuperclass xuong moi Entity
 * con — nhung KHONG ap dung cho native query (@Query(nativeQuery = true)), nhung cho do PHAI tu
 * them dieu kien tenant_id thu cong.
 *
 * <p>Anh xa "kep": truong tenantId (cot that su, ghi duoc truc tiep) + truong tenant (chi doc, tien
 * dieu huong quan he) CUNG tro ve 1 cot tenant_id — nho vay @PrePersist gan tenantId = TenantContext
 * hien tai (neu chua gan tay) MA KHONG can EntityManager (Entity khong duoc Spring inject) va KHONG
 * bat buoc MOI noi tao Entity moi phai tu goi setTenant(...) — tranh sot 1 trong hang chuc cho tao
 * moi Order/Product/Customer... rai rac khap Service, giong cach createdAt/updatedAt da tu dong qua
 * BaseEntity truoc do.
 *
 * <p>TenantFilter (Servlet Filter) bat "tenantFilter" voi tenantId cua nguoi dang nhap ngay dau moi
 * request — Entity con KHONG can khai bao gi them ngoai extends lop nay.
 */
@MappedSuperclass
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantScopedEntity extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
  private Tenant tenant;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @PrePersist
  private void assignTenantIfMissing() {
    if (tenantId == null) {
      tenantId = TenantContext.get();
    }
  }

  public Tenant getTenant() {
    return tenant;
  }

  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
    this.tenantId = tenant == null ? null : tenant.getId();
  }

  public Long getTenantId() {
    return tenantId;
  }
}
