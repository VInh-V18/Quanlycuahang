package com.quanlycuahang.erp.common.repository;

import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

/**
 * Base class thay cho SimpleJpaRepository mac dinh cho MOI JpaRepository trong he thong (dang ky
 * qua @EnableJpaRepositories(repositoryBaseClass=...) o RepositoryConfig) - va lo hong
 * Hibernate @Filter KHONG ap dung cho cac truy van "theo ID truc tiep":
 * findById/existsById/findAllById/ getReferenceById deu dich ra SQL rieng (toi uu
 * load-by-primary-key), KHONG di qua duong sinh SQL thuong nen @Filter da bat tren Session khong
 * loc duoc - day la gioi han da biet cua ban than Hibernate, khong phai loi cau
 * hinh @Filter/TenantFilter. Phat hien khi kiem chung Giai doan 4: tenant B GET/PUT thang
 * /products/{id thuoc tenant A} van thanh cong (200) du danh sach (findAll/search) da loc dung.
 *
 * <p>Chi loc them cho Entity ke thua TenantScopedEntity - Entity global (Role/Permission/Tenant/
 * PlatformAdmin) khong bi anh huong (instanceof tra false, di qua nguyen ban khong loc gi them).
 * Khi TenantContext dang null (vd luong Super Admin) cung khong loc gi - dung y (Super Admin can
 * thao tac tren moi tenant).
 */
public class TenantAwareRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> {

  private final Class<T> domainClass;
  private final JpaEntityInformation<T, ?> entityInformation;
  private final EntityManager entityManager;

  public TenantAwareRepositoryImpl(
      JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
    super(entityInformation, entityManager);
    this.domainClass = entityInformation.getJavaType();
    this.entityInformation = entityInformation;
    this.entityManager = entityManager;
  }

  @Override
  public Optional<T> findById(ID id) {
    return super.findById(id).filter(this::belongsToCurrentTenant);
  }

  /**
   * COUNT truc tiep tren DB thay vi findById(id).isPresent() nhu truoc - phien ban cu load NGUYEN
   * dong du lieu (moi cot) chi de kiem tra ton tai (phat hien khi rieng soat hieu nang). Dieu kien
   * tenant_id duoc dua thang vao SQL cho Entity tenant-scoped (thay cho post-filter trong bo nho),
   * giu nguyen ngu nghia cu: TenantContext null (Super Admin) thi khong loc tenant.
   */
  @Override
  public boolean existsById(ID id) {
    SingularAttribute<? super T, ?> idAttribute = entityInformation.getIdAttribute();
    if (idAttribute == null) {
      // Khoa phuc hop (khong co trong he thong nay) - giu hanh vi loc cu cho an toan.
      return findById(id).isPresent();
    }
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> query = cb.createQuery(Long.class);
    Root<T> root = query.from(domainClass);
    Predicate byId = cb.equal(root.get(idAttribute.getName()), id);
    Long tenantId = TenantContext.get();
    if (tenantId != null && TenantScopedEntity.class.isAssignableFrom(domainClass)) {
      query.where(cb.and(byId, cb.equal(root.get("tenantId"), tenantId)));
    } else {
      query.where(byId);
    }
    query.select(cb.count(root));
    return entityManager.createQuery(query).getSingleResult() > 0;
  }

  @Override
  public List<T> findAllById(Iterable<ID> ids) {
    return super.findAllById(ids).stream().filter(this::belongsToCurrentTenant).toList();
  }

  /**
   * getReferenceById() nguyen ban tra ve proxy "luoi" (chua cham DB) - doi thanh tra ve NGAY (goi
   * findById() da duoc loc o tren) vi day la noi duy nhat co the ro ri tham chieu sang Entity (vd
   * Branch) cua tenant khac ma khong bao gio bi phat hien tai thoi diem gan FK. Danh doi 1 truy van
   * DB ngay lap tuc thay vi hoan lai luc truy cap sau - chap nhan duoc vi cac noi dùng
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
