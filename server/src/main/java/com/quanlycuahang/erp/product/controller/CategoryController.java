package com.quanlycuahang.erp.product.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.product.dto.CategoryRequest;
import com.quanlycuahang.erp.product.dto.CategoryResponse;
import com.quanlycuahang.erp.product.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('category:view')")
  public ResponseEntity<ApiResponse<List<CategoryResponse>>> list() {
    return ResponseEntity.ok(ApiResponse.success(categoryService.list()));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('category:create')")
  public ResponseEntity<ApiResponse<CategoryResponse>> create(
      @Valid @RequestBody CategoryRequest request) {
    return ResponseEntity.ok(ApiResponse.success(categoryService.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('category:update')")
  public ResponseEntity<ApiResponse<CategoryResponse>> update(
      @PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
    return ResponseEntity.ok(ApiResponse.success(categoryService.update(id, request)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('category:delete')")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
    categoryService.delete(id);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
