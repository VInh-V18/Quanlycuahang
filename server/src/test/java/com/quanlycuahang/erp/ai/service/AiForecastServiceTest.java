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
 * Prompt #12 - {@link AiForecastService} goi sang ml-service (Python) qua RestClient. Vi khong co
 * ml-service that chay trong unit test, dung {@code base-url} tro toi 1 port CHAC CHAN khong co ai
 * lang nghe ({@code http://localhost:1} - port 0/1 luon bi tu choi ngay lap tuc, khong can cho
 * timeout that su) de CHAC CHAN kich hoat nhanh nhanh du phong (fallback) ve {@link
 * AiPurchaseSuggestionService} - day CHINH LA hanh vi can kiem chung: ml-service khong kha dung
 * KHONG duoc lam vo tinh nang, phai tu dong quay ve cong thuc don gian.
 */
@ExtendWith(MockitoExtension.class)
class AiForecastServiceTest {

  private static final Long TENANT_ID = 42L;
  private static final Long BRANCH_ID = 1L;

  @Mock private AiPurchaseSuggestionRepository repository;
  @Mock private AiQueryService aiQueryService;
  @Mock private AiPurchaseSuggestionService fallbackService;
  @Mock private BranchAccessGuard branchAccessGuard;

  private AiForecastService service;

  @BeforeEach
  void setUp() {
    service =
        new AiForecastService(
            repository,
            aiQueryService,
            fallbackService,
            branchAccessGuard,
            "http://localhost:1",
            1);
    TenantContext.set(TENANT_ID);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private static Object[] row(
      long productId, String name, String sku, String stock, String minStock) {
    return new Object[] {
      productId, name, sku, new BigDecimal(stock), new BigDecimal(minStock), BigDecimal.ZERO
    };
  }

  @Test
  void suggestAdvancedAssertsBranchAccess() {
    when(repository.findVelocityDataForBranch(BRANCH_ID, TENANT_ID)).thenReturn(List.of());

    service.suggestAdvanced(BRANCH_ID);

    verify(branchAccessGuard).assertAccess(BRANCH_ID);
  }

  @Test
  void suggestAdvancedReturnsEmptyWithoutCallingMlServiceWhenNoProducts() {
    when(repository.findVelocityDataForBranch(BRANCH_ID, TENANT_ID)).thenReturn(List.of());

    List<PurchaseSuggestionResponse> result = service.suggestAdvanced(BRANCH_ID);

    assertThat(result).isEmpty();
  }

  @Test
  void suggestAdvancedFallsBackToSimpleAlgorithmWhenMlServiceUnavailable() {
    when(repository.findVelocityDataForBranch(BRANCH_ID, TENANT_ID))
        .thenReturn(List.<Object[]>of(row(1L, "Táo", "SKU-1", "5", "20")));
    when(aiQueryService.getDailySales(anyLong(), anyLong(), org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(List.of());
    PurchaseSuggestionResponse fallbackSuggestion =
        new PurchaseSuggestionResponse(
            1L, "Táo", "SKU-1", new BigDecimal("5"), new BigDecimal("20"), new BigDecimal("15"));
    when(fallbackService.suggest(BRANCH_ID)).thenReturn(List.of(fallbackSuggestion));

    List<PurchaseSuggestionResponse> result = service.suggestAdvanced(BRANCH_ID);

    assertThat(result).containsExactly(fallbackSuggestion);
    verify(fallbackService).suggest(BRANCH_ID);
  }
}
