package com.quanlycuahang.erp.product.service;

import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.product.dto.PriceHistoryResponse;
import com.quanlycuahang.erp.product.dto.ProductRequest;
import com.quanlycuahang.erp.product.dto.ProductResponse;
import com.quanlycuahang.erp.product.entity.Category;
import com.quanlycuahang.erp.product.entity.PriceHistory;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.product.mapper.ProductMapper;
import com.quanlycuahang.erp.product.repository.CategoryRepository;
import com.quanlycuahang.erp.product.repository.PriceHistoryRepository;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import com.quanlycuahang.erp.system.service.SettingsService;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD san pham (UC-03): SKU tu sinh, snapshot lich su gia, soft delete tu dong qua @SQLDelete. */
@Service
public class ProductService {

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;
  private final PriceHistoryRepository priceHistoryRepository;
  private final ProductMapper productMapper;
  private final SettingsService settingsService;
  private final CurrentUserProvider currentUserProvider;

  public ProductService(
      ProductRepository productRepository,
      CategoryRepository categoryRepository,
      PriceHistoryRepository priceHistoryRepository,
      ProductMapper productMapper,
      SettingsService settingsService,
      CurrentUserProvider currentUserProvider) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
    this.priceHistoryRepository = priceHistoryRepository;
    this.productMapper = productMapper;
    this.settingsService = settingsService;
    this.currentUserProvider = currentUserProvider;
  }

  @Transactional(readOnly = true)
  public ApiResponse<java.util.List<ProductResponse>> search(String search, Pageable pageable) {
    Page<Product> page = productRepository.search(search == null ? "" : search.trim(), pageable);
    return ApiResponse.page(page.map(productMapper::toResponse));
  }

  @Transactional(readOnly = true)
  public ProductResponse getById(Long id) {
    return productMapper.toResponse(
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham")));
  }

  @Transactional
  public ProductResponse create(ProductRequest request) {
    Product product = new Product();
    applyRequest(product, request);
    product.setSku(
        (request.getSku() == null || request.getSku().isBlank())
            ? generateSku()
            : request.getSku());
    if (productRepository.existsBySku(product.getSku())) {
      throw new BusinessRuleException("PRODUCT_DUPLICATE_SKU", "SKU da ton tai");
    }
    if (product.getBarcode() != null && productRepository.existsByBarcode(product.getBarcode())) {
      throw new BusinessRuleException("PRODUCT_DUPLICATE_BARCODE", "Barcode da ton tai");
    }
    return productMapper.toResponse(productRepository.save(product));
  }

  @Transactional
  public ProductResponse update(Long id, ProductRequest request) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham"));
    BigDecimal oldPrice = product.getSellPrice();
    applyRequest(product, request);

    if (oldPrice.compareTo(request.getSellPrice()) != 0) {
      PriceHistory history = new PriceHistory();
      history.setProduct(product);
      history.setOldPrice(oldPrice);
      history.setNewPrice(request.getSellPrice());
      currentUserProvider.getCurrentUser().ifPresent(history::setChangedBy);
      priceHistoryRepository.save(history);
    }
    return productMapper.toResponse(productRepository.save(product));
  }

  @Transactional
  public void delete(Long id) {
    if (!productRepository.existsById(id)) {
      throw new ResourceNotFoundException("Khong tim thay san pham");
    }
    // Soft delete tu dong qua @SQLDelete tren Product — lich su giao dich giu nguyen (B4).
    productRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public ApiResponse<java.util.List<PriceHistoryResponse>> priceHistory(
      Long productId, Pageable pageable) {
    Page<PriceHistory> page =
        priceHistoryRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
    return ApiResponse.page(page.map(productMapper::toPriceHistoryResponse));
  }

  private void applyRequest(Product product, ProductRequest request) {
    product.setName(request.getName());
    product.setUnit(request.getUnit());
    product.setSellPrice(request.getSellPrice());
    product.setPriceIncludesVat(request.isPriceIncludesVat());
    product.setVatRate(request.getVatRate());
    product.setMinStock(request.getMinStock());
    product.setImageUrl(request.getImageUrl());
    product.setBarcode(request.getBarcode());
    if (request.getCategoryId() != null) {
      Category category =
          categoryRepository
              .findById(request.getCategoryId())
              .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay danh muc"));
      product.setCategory(category);
    } else {
      product.setCategory(null);
    }
  }

  private String generateSku() {
    String prefix = settingsService.getValue(null, SettingsService.KEY_SKU_PREFIX, "SP-");
    long sequence = productRepository.count() + 1;
    String candidate = prefix + String.format("%06d", sequence);
    while (productRepository.existsBySku(candidate)) {
      sequence++;
      candidate = prefix + String.format("%06d", sequence);
    }
    return candidate;
  }
}
