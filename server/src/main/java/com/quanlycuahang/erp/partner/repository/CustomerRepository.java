package com.quanlycuahang.erp.partner.repository;

import com.quanlycuahang.erp.partner.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

  boolean existsByPhone(String phone);

  @Query(
      value =
          "SELECT * FROM customers c WHERE c.deleted_at IS NULL AND (:search = '' "
              + "OR immutable_unaccent(lower(c.name)) ILIKE '%' || immutable_unaccent(lower(:search)) || '%' "
              + "OR c.phone ILIKE '%' || :search || '%') ORDER BY c.name",
      countQuery =
          "SELECT count(*) FROM customers c WHERE c.deleted_at IS NULL AND (:search = '' "
              + "OR immutable_unaccent(lower(c.name)) ILIKE '%' || immutable_unaccent(lower(:search)) || '%' "
              + "OR c.phone ILIKE '%' || :search || '%')",
      nativeQuery = true)
  Page<Customer> search(@Param("search") String search, Pageable pageable);
}
