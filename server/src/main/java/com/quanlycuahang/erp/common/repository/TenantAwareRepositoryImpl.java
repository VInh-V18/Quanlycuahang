package com.quanlycuahang.erp.common.repository;

import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

/**
 * Base class thay cho SimpleJpaRepository mac dinh cho MOI JpaRepository trong he thong (dang ky
 * qua @EnableJpaRepositories(repositoryBaseClass=...) o RepositoryConfig) - va lo hong Hibernate
 * @Filter KHONG ap dung cho cac truy van "theo ID truc tiep": findById/existsById/findAllById/
 * getReferenceById deu dich ra SQL rieng (toi uu load-by-primary-key), KHONG di qua duong sinh SQL
 * thuong nen @Filter da bat tren Session khong loc duoc - day la gioi han da biet cua ban than
 * Hibernate, khong phai loi cau hinh @Filter/TenantFilter. Phat hien khi kiem chung Giai doan 4:
 * tenant B GET/PUT thang /products/{id thuoc tenant A} van thanh cong (200) du danh sach
 * (findAll/search) da loc dung.
 *
 * <p>Chi loc them cho Entity ke thua TenantScopedEntity - Entity global (Role/Permission/Tenant/
 * PlatformAdmin) khong bi anh huong (instanceof tra false, di qua nguyen ban khong loc gi them).
 * Khi TenantContext dang null (vd luong Super Admin) cung khong loc gi - dung y (Super Admin can
 * thao tac tren moi tenant).
 */
public class TenantAwareRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> {

  private final Class<T> domainClass;

  public TenantAwareRepositoryImpl(
      JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
    super(entityInformation, entityManager);
    this.domainClass = entityInformation.getJavaType();
  }

  @Override
  public Optional<T> findById(ID id) {
    return super.findById(id).filter(this::belongsToCurrentTenant);
  }

  @Override
  public boolean existsById(ID id) {
    return findById(id).isPresent();
  }

  @Override
  public List<T> findAllById(Iterable<ID> ids) {
    return super.findAllById(ids).stream().filter(this::belongsToCurrentTenant).toList();
  }

  /**
   * getReferenceById() nguyen ban tra ve proxy "luoi" (chua cham DB) - doi thanh tra ve NGAY (goi
   * findById() da duoc loc o tren) vi day la noi duy nhat co the ro ri tham chieu sang Entity
   * (vd Branch) cua tenant khac ma khong bao gio bi phat hien tai thoi diem gan FK. Danh doi 1 truy
   * van DB ngay lap tuc thay vi hoan lai luc truy cap sau - chap nhan duoc vi cac noi dùng
   * getReferenceById trong he thong deu la thao tac quan tri hiem khi goi, khong phai POS hot-path.
   */
  @Override
  public T getReferenceById(ID id) {
    return findById(id)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Khong tim thay " + domainClass.getSimpleName() + " id=" + id));
  }

  private boolean belongsToCurrentTenant(T entity) {
    if (!(entity instanceof TenantScopedEntity tenantScoped)) {
      return true;
    }
    Long tenantId = TenantContext.get();
    return tenantId == null || tenantId.equals(tenantScoped.getTenantId());
  }
}
