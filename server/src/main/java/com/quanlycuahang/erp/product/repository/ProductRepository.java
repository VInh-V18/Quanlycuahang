package com.quanlycuahang.erp.product.repository;

import com.quanlycuahang.erp.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

  boolean existsBySku(String sku);
}
