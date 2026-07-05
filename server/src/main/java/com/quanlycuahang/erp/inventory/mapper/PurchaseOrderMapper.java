package com.quanlycuahang.erp.inventory.mapper;

import com.quanlycuahang.erp.inventory.dto.PurchaseOrderItemResponse;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderResponse;
import com.quanlycuahang.erp.inventory.entity.PurchaseOrder;
import com.quanlycuahang.erp.inventory.entity.PurchaseOrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PurchaseOrderMapper {

  @Mapping(target = "supplierId", source = "supplier.id")
  @Mapping(target = "supplierName", source = "supplier.name")
  @Mapping(target = "branchId", source = "branch.id")
  @Mapping(target = "items", ignore = true)
  PurchaseOrderResponse toResponse(PurchaseOrder entity);

  @Mapping(target = "productId", source = "product.id")
  @Mapping(target = "productName", source = "product.name")
  PurchaseOrderItemResponse toItemResponse(PurchaseOrderItem entity);
}
