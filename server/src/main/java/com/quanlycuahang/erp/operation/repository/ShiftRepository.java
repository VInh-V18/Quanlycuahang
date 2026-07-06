package com.quanlycuahang.erp.operation.repository;

import com.quanlycuahang.erp.operation.entity.Shift;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

  Optional<Shift> findFirstByOpenedByIdAndStatusOrderByOpenedAtDesc(Long openedById, String status);

  @Query(
      value =
          "SELECT s FROM Shift s JOIN FETCH s.branch JOIN FETCH s.openedBy "
              + "WHERE (:status IS NULL OR s.status = :status) ORDER BY s.openedAt DESC",
      countQuery = "SELECT COUNT(s) FROM Shift s WHERE (:status IS NULL OR s.status = :status)")
  Page<Shift> search(@Param("status") String status, Pageable pageable);

  /**
   * Lich su ca chi cua 1 nhan vien (permission-matrix.md: shift:view cua cashier la "ca cua minh",
   * khong phai toan bo lich su moi ca moi chi nhanh).
   */
  @Query(
      value =
          "SELECT s FROM Shift s JOIN FETCH s.branch JOIN FETCH s.openedBy "
              + "WHERE s.openedBy.id = :openedById AND (:status IS NULL OR s.status = :status) "
              + "ORDER BY s.openedAt DESC",
      countQuery =
          "SELECT COUNT(s) FROM Shift s WHERE s.openedBy.id = :openedById "
              + "AND (:status IS NULL OR s.status = :status)")
  Page<Shift> searchByOpenedBy(
      @Param("openedById") Long openedById, @Param("status") String status, Pageable pageable);
}
