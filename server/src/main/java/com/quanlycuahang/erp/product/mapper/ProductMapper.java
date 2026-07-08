package com.quanlycuahang.erp.product.mapper;

import com.quanlycuahang.erp.product.dto.PriceHistoryResponse;
import com.quanlycuahang.erp.product.dto.ProductResponse;
import com.quanlycuahang.erp.product.entity.PriceHistory;
import com.quanlycuahang.erp.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

  @Mapping(target = "categoryId", source = "category.id")
  @Mapping(target = "categoryName", source = "category.name")
  ProductResponse toResponse(Product entity);

  @Mapping(target = "changedByName", source = "changedBy.fullName")
  PriceHistoryResponse toPriceHistoryResponse(PriceHistory entity);
}
