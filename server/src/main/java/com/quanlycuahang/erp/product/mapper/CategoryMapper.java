package com.quanlycuahang.erp.product.mapper;

import com.quanlycuahang.erp.product.dto.CategoryResponse;
import com.quanlycuahang.erp.product.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

  @Mapping(target = "parentId", source = "parent.id")
  CategoryResponse toResponse(Category entity);
}
