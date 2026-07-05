package com.quanlycuahang.erp.sales.mapper;

import com.quanlycuahang.erp.sales.dto.ParkedOrderResponse;
import com.quanlycuahang.erp.sales.entity.ParkedOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParkedOrderMapper {

  @Mapping(target = "branchId", source = "branch.id")
  ParkedOrderResponse toResponse(ParkedOrder entity);
}
