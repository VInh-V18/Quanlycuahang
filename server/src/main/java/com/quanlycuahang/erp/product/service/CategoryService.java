package com.quanlycuahang.erp.product.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.product.dto.CategoryRequest;
import com.quanlycuahang.erp.product.dto.CategoryResponse;
import com.quanlycuahang.erp.product.entity.Category;
import com.quanlycuahang.erp.product.mapper.CategoryMapper;
import com.quanlycuahang.erp.product.repository.CategoryRepository;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quan ly cay danh muc 2 cap (UC-02, B4). Cay danh muc doc rat nhieu (moi lan mo POS/trang san
 * pham) nhung thay doi hiem (tao/sua/xoa danh muc la thao tac quan tri, khong phai luong nghiep vu
 * hang ngay) - cache Caffeine TRONG TIEN TRINH (khong qua mang nhu Redis cua SettingsService, vi
 * khong can chia se giua nhieu instance/tenant) theo tenantId, TTL ngan + xoa chu dong khi ghi
 * (Prompt #7, P2 hieu nang).
 */
@Service
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;
  private final CategoryMapper categoryMapper;
  private final Cache<Long, List<CategoryResponse>> cache =
      Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(120)).maximumSize(1000).build();

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
    return cache.get(
        TenantContext.get(),
        tenantId ->
            categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(categoryMapper::toResponse)
                .toList());
  }

  @Transactional
  public CategoryResponse create(CategoryRequest request) {
    Category category = new Category();
    category.setName(request.getName());
    category.setDisplayOrder(request.getDisplayOrder());
    applyParent(category, request.getParentId());
    CategoryResponse response = categoryMapper.toResponse(categoryRepository.save(category));
    cache.invalidate(TenantContext.get());
    return response;
  }

  @Transactional
  public CategoryResponse update(Long id, CategoryRequest request) {
    Category category =
        categoryRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
    category.setName(request.getName());
    category.setDisplayOrder(request.getDisplayOrder());
    applyParent(category, request.getParentId());
    CategoryResponse response = categoryMapper.toResponse(categoryRepository.save(category));
    cache.invalidate(TenantContext.get());
    return response;
  }

  @Transactional
  public void delete(Long id) {
    if (!categoryRepository.existsById(id)) {
      throw new ResourceNotFoundException("Không tìm thấy danh mục");
    }
    if (categoryRepository.existsByParentId(id)) {
      throw new BusinessRuleException(
          "CATEGORY_HAS_CHILDREN", "Danh mục còn danh mục con, không thể xóa");
    }
    if (productRepository.existsByCategoryId(id)) {
      throw new BusinessRuleException(
          "CATEGORY_HAS_PRODUCTS", "Danh mục còn sản phẩm, vui lòng chuyển sản phẩm trước khi xóa");
    }
    categoryRepository.deleteById(id);
    cache.invalidate(TenantContext.get());
  }

  private void applyParent(Category category, Long parentId) {
    if (parentId == null) {
      category.setParent(null);
      return;
    }
    Category parent =
        categoryRepository
            .findById(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục cha"));
    if (parent.getParent() != null) {
      throw new BusinessRuleException(
          "CATEGORY_MAX_DEPTH_EXCEEDED", "Chỉ hỗ trợ cây danh mục 2 cấp");
    }
    category.setParent(parent);
  }
}
