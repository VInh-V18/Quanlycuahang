package com.quanlycuahang.erp.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.mapper.InventoryMapper;
import com.quanlycuahang.erp.inventory.repository.InventoryBatchRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryTransactionRepository;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Ghi de gia von (inventory:cost-price-override, tinh nang moi) — unit test cho
 * InventoryService.overrideCostPrice(), mirror phong cach OrderEditServiceTest (mock
 * repository/guard).
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceCostPriceTest {

  @Mock private InventoryRepository inventoryRepository;
  @Mock private InventoryTransactionRepository inventoryTransactionRepository;
  @Mock private InventoryBatchRepository inventoryBatchRepository;
  @Mock private InventoryMapper inventoryMapper;
  @Mock private BranchAccessGuard branchAccessGuard;
  @Mock private ProductRepository productRepository;

  private InventoryService service;

  @BeforeEach
  void setUp() {
    service =
        new InventoryService(
            inventoryRepository,
            inventoryTransactionRepository,
            inventoryBatchRepository,
            inventoryMapper,
            branchAccessGuard,
            productRepository);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private Inventory inventory(BigDecimal costPrice) {
    Inventory inventory = new Inventory();
    inventory.setStock(BigDecimal.TEN);
    inventory.setCostPrice(costPrice);
    return inventory;
  }

  @Test
  void overrideCostPriceThrowsWhenNegative() {
    assertThatThrownBy(() -> service.overrideCostPrice(1L, 10L, BigDecimal.valueOf(-1)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("âm");
  }

  @Test
  void overrideCostPriceThrowsWhenProductNotFound() {
    when(productRepository.existsById(1L)).thenReturn(false);

    assertThatThrownBy(() -> service.overrideCostPrice(1L, 10L, BigDecimal.valueOf(5_000)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void overrideCostPriceOverwritesExistingInventoryRow() {
    when(productRepository.existsById(1L)).thenReturn(true);
    Inventory existing = inventory(BigDecimal.valueOf(3_000));
    when(inventoryRepository.findByProductIdAndBranchIdForUpdate(1L, 10L))
        .thenReturn(Optional.of(existing));

    service.overrideCostPrice(1L, 10L, BigDecimal.valueOf(5_500));

    assertThat(existing.getCostPrice()).isEqualByComparingTo(BigDecimal.valueOf(5_500));
    verify(inventoryRepository).save(existing);
    verify(inventoryRepository, never()).initializeIfAbsent(anyLong(), anyLong(), anyLong());
  }

  @Test
  void overrideCostPriceInitializesInventoryRowWhenAbsent() {
    TenantContext.set(99L);
    when(productRepository.existsById(1L)).thenReturn(true);
    Inventory created = inventory(BigDecimal.ZERO);
    when(inventoryRepository.findByProductIdAndBranchIdForUpdate(1L, 10L))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(created));

    service.overrideCostPrice(1L, 10L, BigDecimal.valueOf(7_000));

    verify(inventoryRepository).initializeIfAbsent(99L, 1L, 10L);
    verify(inventoryRepository, times(2)).findByProductIdAndBranchIdForUpdate(1L, 10L);
    assertThat(created.getCostPrice()).isEqualByComparingTo(BigDecimal.valueOf(7_000));
    verify(inventoryRepository).save(created);
  }
}
