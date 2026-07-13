package com.quanlycuahang.erp.product.service;

import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.common.sequence.NumberSequenceService;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final InventoryRepository inventoryRepository;
  private final ProductMapper productMapper;
  private final SettingsService settingsService;
  private final CurrentUserProvider currentUserProvider;
  private final NumberSequenceService numberSequenceService;

  public ProductService(
      ProductRepository productRepository,
      CategoryRepository categoryRepository,
      PriceHistoryRepository priceHistoryRepository,
      InventoryRepository inventoryRepository,
      ProductMapper productMapper,
      SettingsService settingsService,
      CurrentUserProvider currentUserProvider,
      NumberSequenceService numberSequenceService) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
    this.priceHistoryRepository = priceHistoryRepository;
    this.inventoryRepository = inventoryRepository;
    this.productMapper = productMapper;
    this.settingsService = settingsService;
    this.currentUserProvider = currentUserProvider;
    this.numberSequenceService = numberSequenceService;
  }

  @Transactional(readOnly = true)
  public ApiResponse<java.util.List<ProductResponse>> search(
      String search,
      Long categoryId,
      Boolean active,
      String originCountry,
      Long branchId,
      Pageable pageable) {
    Page<Product> page =
        productRepository.search(
            search == null ? "" : search.trim(),
            categoryId,
            active,
            originCountry,
            TenantContext.get(),
            pageable);
    ApiResponse<java.util.List<ProductResponse>> response =
        ApiResponse.page(page.map(productMapper::toResponse));
    if (branchId != null) {
      enrichWithBranchStock(response.getData(), branchId);
    }
    return response;
  }

  /**
   * Gan ton kho + gia von theo chi nhanh vao moi dong san pham (FH-5) — 1 truy van cho ca trang.
   */
  private void enrichWithBranchStock(List<ProductResponse> rows, Long branchId) {
    if (rows.isEmpty()) {
      return;
    }
    List<Long> productIds = rows.stream().map(ProductResponse::getId).toList();
    Map<Long, Inventory> byProductId =
        inventoryRepository.findByBranchIdAndProductIdIn(branchId, productIds).stream()
            .collect(Collectors.toMap(i -> i.getProduct().getId(), Function.identity()));
    for (ProductResponse row : rows) {
      Inventory inventory = byProductId.get(row.getId());
      if (inventory != null) {
        row.setStock(inventory.getStock());
        row.setCostPrice(inventory.getCostPrice());
      }
    }
  }

  @Transactional(readOnly = true)
  public ProductResponse getById(Long id) {
    return productMapper.toResponse(
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm")));
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
      throw new BusinessRuleException("PRODUCT_DUPLICATE_SKU", "SKU đã tồn tại");
    }
    if (product.getBarcode() != null && productRepository.existsByBarcode(product.getBarcode())) {
      throw new BusinessRuleException("PRODUCT_DUPLICATE_BARCODE", "Barcode đã tồn tại");
    }
    return productMapper.toResponse(productRepository.save(product));
  }

  @Transactional
  public ProductResponse update(Long id, ProductRequest request) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
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
      throw new ResourceNotFoundException("Không tìm thấy sản phẩm");
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
    product.setOriginCountry(request.getOriginCountry());
    product.setOriginRegion(request.getOriginRegion());
    if (request.getCategoryId() != null) {
      Category category =
          categoryRepository
              .findById(request.getCategoryId())
              .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
      product.setCategory(category);
    } else {
      product.setCategory(null);
    }
  }

  private String generateSku() {
    String prefix = settingsService.getValue(null, SettingsService.KEY_SKU_PREFIX, "SP-");
    long sequence = numberSequenceService.nextValue(NumberSequenceService.SKU_SEQ);
    return prefix + String.format("%06d", sequence);
  }
}
