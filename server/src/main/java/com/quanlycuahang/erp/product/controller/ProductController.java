package com.quanlycuahang.erp.product.controller;

import com.quanlycuahang.erp.common.audit.Audited;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.product.dto.PriceHistoryResponse;
import com.quanlycuahang.erp.product.dto.ProductRequest;
import com.quanlycuahang.erp.product.dto.ProductResponse;
import com.quanlycuahang.erp.product.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('product:view')")
  public ResponseEntity<ApiResponse<List<ProductResponse>>> search(
      @RequestParam(required = false, defaultValue = "") String search,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) String originCountry,
      @RequestParam(required = false) Long branchId,
      Pageable pageable) {
    return ResponseEntity.ok(
        productService.search(search, categoryId, active, originCountry, branchId, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('product:view')")
  public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(productService.getById(id)));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('product:create')")
  public ResponseEntity<ApiResponse<ProductResponse>> create(
      @Valid @RequestBody ProductRequest request) {
    return ResponseEntity.ok(ApiResponse.success(productService.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('product:update')")
  @Audited(action = "PRODUCT_UPDATE", entityName = "Product")
  public ResponseEntity<ApiResponse<ProductResponse>> update(
      @PathVariable Long id, @Valid @RequestBody ProductRequest request) {
    return ResponseEntity.ok(ApiResponse.success(productService.update(id, request)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('product:delete')")
  @Audited(action = "PRODUCT_DELETE", entityName = "Product")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
    productService.delete(id);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @GetMapping("/{id}/price-history")
  @PreAuthorize("hasAuthority('product:view')")
  public ResponseEntity<ApiResponse<List<PriceHistoryResponse>>> priceHistory(
      @PathVariable Long id, Pageable pageable) {
    return ResponseEntity.ok(productService.priceHistory(id, pageable));
  }
}
