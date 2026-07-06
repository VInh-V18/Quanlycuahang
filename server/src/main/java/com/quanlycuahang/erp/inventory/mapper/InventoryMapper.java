package com.quanlycuahang.erp.inventory.mapper;

import com.quanlycuahang.erp.inventory.dto.InventoryResponse;
import com.quanlycuahang.erp.inventory.dto.InventoryTransactionResponse;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.entity.InventoryTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

  @Mapping(target = "productId", source = "product.id")
  @Mapping(target = "productName", source = "product.name")
  @Mapping(target = "sku", source = "product.sku")
  @Mapping(target = "minStock", source = "product.minStock")
  InventoryResponse toResponse(Inventory entity);

  InventoryTransactionResponse toTransactionResponse(InventoryTransaction entity);
}
