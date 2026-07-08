package com.quanlycuahang.erp.partner.mapper;

import com.quanlycuahang.erp.partner.dto.CustomerResponse;
import com.quanlycuahang.erp.partner.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

  @Mapping(target = "customerGroupId", source = "customerGroup.id")
  CustomerResponse toResponse(Customer entity);
}
