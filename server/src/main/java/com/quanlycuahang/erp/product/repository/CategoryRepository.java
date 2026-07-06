package com.quanlycuahang.erp.product.repository;

import com.quanlycuahang.erp.product.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

  List<Category> findAllByOrderByDisplayOrderAsc();

  boolean existsByParentId(Long parentId);
}
