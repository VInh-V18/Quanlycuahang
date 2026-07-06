package com.quanlycuahang.erp.partner.mapper;

import com.quanlycuahang.erp.partner.dto.SupplierResponse;
import com.quanlycuahang.erp.partner.entity.Supplier;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

  SupplierResponse toResponse(Supplier entity);
}
