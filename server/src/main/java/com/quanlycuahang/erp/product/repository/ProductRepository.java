package com.quanlycuahang.erp.product.repository;

import com.quanlycuahang.erp.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

  boolean existsBySku(String sku);

  boolean existsByBarcode(String barcode);

  boolean existsByCategoryId(Long categoryId);

  /**
   * Tim khong dau + gan dung (B3): immutable_unaccent + ILIKE cho "chua", pg_trgm % cho gan dung
   * (vd "ca phe" ra "Cà phê"). Bo qua khi search rong.
   */
  @Query(
      value =
          "SELECT * FROM products p WHERE p.deleted_at IS NULL AND p.tenant_id = :tenantId "
              + "AND (:search = '' "
              + "     OR immutable_unaccent(lower(p.name)) ILIKE '%' || immutable_unaccent(lower(:search)) || '%' "
              + "     OR immutable_unaccent(lower(p.name)) % immutable_unaccent(lower(:search)) "
              + "     OR p.sku ILIKE '%' || :search || '%' "
              + "     OR p.barcode = :search) "
              + "AND (:categoryId IS NULL OR p.category_id = :categoryId) "
              + "AND (:active IS NULL OR p.is_active = :active) "
              + "AND (:originCountry IS NULL OR p.origin_country = :originCountry) "
              + "ORDER BY p.name",
      countQuery =
          "SELECT count(*) FROM products p WHERE p.deleted_at IS NULL AND p.tenant_id = :tenantId "
              + "AND (:search = '' "
              + "     OR immutable_unaccent(lower(p.name)) ILIKE '%' || immutable_unaccent(lower(:search)) || '%' "
              + "     OR immutable_unaccent(lower(p.name)) % immutable_unaccent(lower(:search)) "
              + "     OR p.sku ILIKE '%' || :search || '%' "
              + "     OR p.barcode = :search) "
              + "AND (:categoryId IS NULL OR p.category_id = :categoryId) "
              + "AND (:active IS NULL OR p.is_active = :active) "
              + "AND (:originCountry IS NULL OR p.origin_country = :originCountry)",
      nativeQuery = true)
  Page<Product> search(
      @Param("search") String search,
      @Param("categoryId") Long categoryId,
      @Param("active") Boolean active,
      @Param("originCountry") String originCountry,
      @Param("tenantId") Long tenantId,
      Pageable pageable);
}
