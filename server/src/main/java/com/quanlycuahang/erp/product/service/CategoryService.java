package com.quanlycuahang.erp.product.service;

import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.product.dto.CategoryRequest;
import com.quanlycuahang.erp.product.dto.CategoryResponse;
import com.quanlycuahang.erp.product.entity.Category;
import com.quanlycuahang.erp.product.mapper.CategoryMapper;
import com.quanlycuahang.erp.product.repository.CategoryRepository;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quan ly cay danh muc 2 cap (UC-02, B4). */
@Service
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;
  private final CategoryMapper categoryMapper;

  public CategoryService(
      CategoryRepository categoryRepository,
      ProductRepository productRepository,
      CategoryMapper categoryMapper) {
    this.categoryRepository = categoryRepository;
    this.productRepository = productRepository;
    this.categoryMapper = categoryMapper;
  }

  @Transactional(readOnly = true)
  public List<CategoryResponse> list() {
    return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
        .map(categoryMapper::toResponse)
        .toList();
  }

  @Transactional
  public CategoryResponse create(CategoryRequest request) {
    Category category = new Category();
    category.setName(request.getName());
    category.setDisplayOrder(request.getDisplayOrder());
    applyParent(category, request.getParentId());
    return categoryMapper.toResponse(categoryRepository.save(category));
  }

  @Transactional
  public CategoryResponse update(Long id, CategoryRequest request) {
    Category category =
        categoryRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay danh muc"));
    category.setName(request.getName());
    category.setDisplayOrder(request.getDisplayOrder());
    applyParent(category, request.getParentId());
    return categoryMapper.toResponse(categoryRepository.save(category));
  }

  @Transactional
  public void delete(Long id) {
    if (!categoryRepository.existsById(id)) {
      throw new ResourceNotFoundException("Khong tim thay danh muc");
    }
    if (categoryRepository.existsByParentId(id)) {
      throw new BusinessRuleException(
          "CATEGORY_HAS_CHILDREN", "Danh muc con danh muc con, khong the xoa");
    }
    if (productRepository.existsByCategoryId(id)) {
      throw new BusinessRuleException(
          "CATEGORY_HAS_PRODUCTS", "Danh muc con san pham, vui long chuyen san pham truoc khi xoa");
    }
    categoryRepository.deleteById(id);
  }

  private void applyParent(Category category, Long parentId) {
    if (parentId == null) {
      category.setParent(null);
      return;
    }
    Category parent =
        categoryRepository
            .findById(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay danh muc cha"));
    if (parent.getParent() != null) {
      throw new BusinessRuleException(
          "CATEGORY_MAX_DEPTH_EXCEEDED", "Chi ho tro cay danh muc 2 cap");
    }
    category.setParent(parent);
  }
}
