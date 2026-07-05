package com.quanlycuahang.erp.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Lop cha chung cho moi Entity nghiep vu: id tu sinh, createdAt/updatedAt tu dong qua Spring Data
 * JPA Auditing, deletedAt cho co che soft delete (moi Entity con phai tu khai bao @SQLDelete
 * + @Where tro ve cot nay).
 *
 * <p>createdAt/updatedAt dung Instant (khong phai OffsetDateTime) — da xac minh qua test thuc te:
 * Spring Data Commons' DefaultAuditableBeanWrapperFactory (ban dung voi Spring Boot 3.3.5) NEM
 * IllegalArgumentException voi OffsetDateTime ("Cannot convert unsupported date type... Supported
 * types are [LocalDateTime, LocalDate, LocalTime, Instant, Date, Long, long]"). B3 cho phep ca
 * OffsetDateTime lan Instant — chon Instant de tuong thich voi @CreatedDate/@LastModifiedDate.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public OffsetDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(OffsetDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }
}
