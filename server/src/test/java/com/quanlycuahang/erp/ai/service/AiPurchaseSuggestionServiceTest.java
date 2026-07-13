package com.quanlycuahang.erp.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quanlycuahang.erp.ai.dto.PurchaseSuggestionResponse;
import com.quanlycuahang.erp.ai.repository.AiPurchaseSuggestionRepository;
import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.TenantContext;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Prompt #11 (tinh nang dot 1b) - kiem chung cong thuc xac dinh (deterministic, KHONG goi AI/LLM -
 * xem Javadoc AiPurchaseSuggestionService): target_stock = MAX(min_stock, toc_do_ban_ngay x 30),
 * chi tra ve dong co goi y > 0, va sap xep theo do khan cap (thieu nhieu nhat truoc).
 */
@ExtendWith(MockitoExtension.class)
class AiPurchaseSuggestionServiceTest {

  private static final Long BRANCH_ID = 1L;
  private static final Long TENANT_ID = 99L;

  @Mock private AiPurchaseSuggestionRepository repository;
  @Mock private BranchAccessGuard branchAccessGuard;

  private AiPurchaseSuggestionService service;

  @BeforeEach
  void setUp() {
    service = new AiPurchaseSuggestionService(repository, branchAccessGuard);
    TenantContext.set(TENANT_ID);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private static Object[] row(
      long productId,
      String name,
      String sku,
      String currentStock,
      String minStock,
      String qtySold30d) {
    return new Object[] {
      productId,
      name,
      sku,
      new BigDecimal(currentStock),
      new BigDecimal(minStock),
      new BigDecimal(qtySold30d)
    };
  }

  @Test
  void assertsBranchAccessBeforeQuerying() {
    when(repository.findVelocityDataForBranch(BRANCH_ID, TENANT_ID)).thenReturn(List.of());

    service.suggest(BRANCH_ID);

    verify(branchAccessGuard).assertAccess(BRANCH_ID);
  }

  @Test
  void suggestsReorderQuantityWhenProjectedStockBelowTarget() {
    // toc do ban = 60/30 ngay = 2/ngay, target = 2*30 = 60, ton hien tai 10 -> goi y = 50
    when(repository.findVelocityDataForBranch(BRANCH_ID, TENANT_ID))
        .thenReturn(List.<Object[]>of(row(1L, "Tao", "SKU-1", "10", "5", "60")));

    List<PurchaseSuggestionResponse> result = service.suggest(BRANCH_ID);

    assertThat(result).hasSize(1);
    PurchaseSuggestionResponse suggestion = result.get(0);
    assertThat(suggestion.getProductId()).isEqualTo(1L);
    assertThat(suggestion.getSuggestedQty()).isEqualByComparingTo("50");
  }

  @Test
  void usesMinStockAsFloorWhenVelocityIsLow() {
    // toc do ban rat thap (0/ngay) nhung dinh muc toi thieu la 20, ton hien tai 5 -> goi y = 15
    when(repository.findVelocityDataForBranch(BRANCH_ID, TENANT_ID))
        .thenReturn(List.<Object[]>of(row(1L, "Cam", "SKU-2", "5", "20", "0")));

    List<PurchaseSuggestionResponse> result = service.suggest(BRANCH_ID);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getSuggestedQty()).isEqualByComparingTo("15");
  }

  @Test
  void omitsProductsWithNoReorderNeeded() {
    // ton hien tai (100) da vuot xa target (60) -> KHONG duoc xuat hien trong ket qua
    when(repository.findVelocityDataForBranch(BRANCH_ID, TENANT_ID))
        .thenReturn(List.<Object[]>of(row(1L, "Xoai", "SKU-3", "100", "5", "60")));

    List<PurchaseSuggestionResponse> result = service.suggest(BRANCH_ID);

    assertThat(result).isEmpty();
  }

  @Test
  void sortsByMostUrgentDeficitFirst() {
    // SKU-A: ton - min = 10 - 5 = 5 (it khan cap hon)
    // SKU-B: ton - min = 2 - 20 = -18 (khan cap nhat, phai xep truoc)
    when(repository.findVelocityDataForBranch(BRANCH_ID, TENANT_ID))
        .thenReturn(
            List.of(
                row(1L, "SKU-A", "SKU-A", "10", "5", "60"),
                row(2L, "SKU-B", "SKU-B", "2", "20", "30")));

    List<PurchaseSuggestionResponse> result = service.suggest(BRANCH_ID);

    assertThat(result).extracting(PurchaseSuggestionResponse::getProductId).containsExactly(2L, 1L);
  }

  @Test
  void readsCurrentTenantFromTenantContext() {
    when(repository.findVelocityDataForBranch(anyLong(), anyLong())).thenReturn(List.of());

    service.suggest(BRANCH_ID);

    verify(repository).findVelocityDataForBranch(BRANCH_ID, TENANT_ID);
  }
}
