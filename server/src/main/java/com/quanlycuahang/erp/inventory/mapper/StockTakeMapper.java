package com.quanlycuahang.erp.inventory.mapper;

import com.quanlycuahang.erp.inventory.dto.StockTakeItemResponse;
import com.quanlycuahang.erp.inventory.dto.StockTakeResponse;
import com.quanlycuahang.erp.inventory.entity.StockTake;
import com.quanlycuahang.erp.inventory.entity.StockTakeItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockTakeMapper {

  @Mapping(target = "branchId", source = "branch.id")
  @Mapping(target = "items", ignore = true)
  StockTakeResponse toResponse(StockTake entity);

  @Mapping(target = "productId", source = "product.id")
  @Mapping(target = "productName", source = "product.name")
  StockTakeItemResponse toItemResponse(StockTakeItem entity);
}
