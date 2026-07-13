package com.quanlycuahang.erp.ai.service;

import com.quanlycuahang.erp.ai.dto.PurchaseSuggestionResponse;
import com.quanlycuahang.erp.ai.repository.AiPurchaseSuggestionRepository;
import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Goi y nhap hang NANG CAO (Prompt #12, tinh nang dot 2) - goi sang 1 dich vu Python rieng (FastAPI
 * + scikit-learn IsolationForest, xem ml-service/) de loc "ngay ban bat thuong" (vd 1 ngay sale dot
 * bien) truoc khi tinh toc do ban trung binh, thay vi cong thuc don gian cua {@link
 * AiPurchaseSuggestionService} (Prompt #11 - van GIU NGUYEN, dung lam PHUONG AN DU PHONG duoi day).
 *
 * <p><b>Vi sao PHAI co du phong (fallback) khi ML service loi/cham/khong kha dung</b>: day la 1
 * dich vu PHU tro giup quyet dinh nhap hang tot hon, KHONG phai luong nghiep vu chinh - nguoi dung
 * KHONG duoc thay loi 500 chi vi 1 container Python rieng bi treo/chua khoi dong kip; ngay khi ML
 * service tra loi loi HTTP, timeout, hoac mat ket noi, tu dong quay ve dung cong thuc xac dinh don
 * gian (van hop ly, chi khong loc duoc outlier) - nguoi dung van co goi y de dung, chi kem chinh
 * xac hon 1 chut.
 *
 * <p><b>Metadata san pham (ten/sku/ton/dinh muc) van lay qua {@link AiPurchaseSuggestionRepository}
 * (DataSource CHINH)</b>, CHI rieng chuoi doanh so 90 ngay/ngay moi lay qua {@link AiQueryService}
 * (pool doc-rieng, view {@code v_ai_daily_sales}, V32) - giu dung nguyen tac "AI chi doc qua view
 * whitelist", trong khi metadata san pham khong nhay cam hon nhung du lieu da dung cho tinh nang
 * dot 1 (khong can tach pool cho phan nay).
 */
@Service
public class AiForecastService {

  private static final Logger log = LoggerFactory.getLogger(AiForecastService.class);
  private static final int LOOKBACK_DAYS = 90;

  private final AiPurchaseSuggestionRepository repository;
  private final AiQueryService aiQueryService;
  private final AiPurchaseSuggestionService fallbackService;
  private final BranchAccessGuard branchAccessGuard;
  private final RestClient mlServiceRestClient;

  public AiForecastService(
      AiPurchaseSuggestionRepository repository,
      AiQueryService aiQueryService,
      AiPurchaseSuggestionService fallbackService,
      BranchAccessGuard branchAccessGuard,
      @Value("${app.ai.ml-service.base-url}") String baseUrl,
      @Value("${app.ai.ml-service.timeout-seconds:5}") int timeoutSeconds) {
    this.repository = repository;
    this.aiQueryService = aiQueryService;
    this.fallbackService = fallbackService;
    this.branchAccessGuard = branchAccessGuard;
    this.mlServiceRestClient =
        RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(
                new SimpleClientHttpRequestFactory() {
                  {
                    // Timeout NGAN (5s mac dinh, khac 30s cua ClaudeAiProvider) - day la dich vu
                    // NOI BO cung docker network, khong phai goi API ben ngoai qua Internet; neu
                    // khong tra loi trong vai giay thi coi nhu "khong kha dung" va fallback ngay,
                    // khong bat nguoi dung cho lau cho 1 tinh nang phu.
                    setConnectTimeout((int) Duration.ofSeconds(timeoutSeconds).toMillis());
                    setReadTimeout((int) Duration.ofSeconds(timeoutSeconds).toMillis());
                  }
                })
            .build();
  }

  @Transactional(readOnly = true)
  public List<PurchaseSuggestionResponse> suggestAdvanced(Long branchId) {
    branchAccessGuard.assertAccess(branchId);
    Long tenantId = TenantContext.get();
    List<Object[]> velocityRows = repository.findVelocityDataForBranch(branchId, tenantId);
    if (velocityRows.isEmpty()) {
      return List.of();
    }

    Map<Long, List<AiQueryRow.DailySalesRow>> dailySalesByProduct =
        aiQueryService.getDailySales(tenantId, branchId, LOOKBACK_DAYS).stream()
            .collect(Collectors.groupingBy(AiQueryRow.DailySalesRow::productId));

    try {
      MlForecastRequest request = buildRequest(branchId, velocityRows, dailySalesByProduct);
      MlForecastResponse response =
          mlServiceRestClient
              .post()
              .uri("/forecast")
              .contentType(MediaType.APPLICATION_JSON)
              .body(request)
              .retrieve()
              .body(MlForecastResponse.class);
      return mapToSuggestions(velocityRows, response);
    } catch (RestClientException ex) {
      log.warn(
          "Dich vu du bao AI nang cao (ml-service) khong kha dung, dung ve goi y don gian: {}",
          ex.getMessage());
      return fallbackService.suggest(branchId);
    }
  }

  private MlForecastRequest buildRequest(
      Long branchId,
      List<Object[]> velocityRows,
      Map<Long, List<AiQueryRow.DailySalesRow>> dailySalesByProduct) {
    List<MlProductInput> products = new ArrayList<>();
    for (Object[] row : velocityRows) {
      Long productId = ((Number) row[0]).longValue();
      String productName = (String) row[1];
      String sku = (String) row[2];
      BigDecimal currentStock = (BigDecimal) row[3];
      BigDecimal minStock = (BigDecimal) row[4];
      List<MlDailySale> dailySales =
          dailySalesByProduct.getOrDefault(productId, List.of()).stream()
              .map(r -> new MlDailySale(r.saleDay().toString(), r.quantitySold()))
              .toList();
      products.add(
          new MlProductInput(productId, productName, sku, currentStock, minStock, dailySales));
    }
    return new MlForecastRequest(branchId, LOOKBACK_DAYS, products);
  }

  private List<PurchaseSuggestionResponse> mapToSuggestions(
      List<Object[]> velocityRows, MlForecastResponse response) {
    List<MlForecastResult> results =
        response != null && response.results() != null ? response.results() : List.of();
    Map<Long, MlForecastResult> resultsByProduct =
        results.stream()
            .collect(Collectors.toMap(MlForecastResult::productId, r -> r, (a, b) -> a));

    List<PurchaseSuggestionResponse> suggestions = new ArrayList<>();
    for (Object[] row : velocityRows) {
      Long productId = ((Number) row[0]).longValue();
      MlForecastResult result = resultsByProduct.get(productId);
      if (result == null
          || result.suggestedQty() == null
          || result.suggestedQty().compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      String productName = (String) row[1];
      String sku = (String) row[2];
      BigDecimal currentStock = (BigDecimal) row[3];
      BigDecimal minStock = (BigDecimal) row[4];
      suggestions.add(
          new PurchaseSuggestionResponse(
              productId,
              productName,
              sku,
              currentStock,
              minStock,
              result.suggestedQty().setScale(0, RoundingMode.CEILING),
              result.confidence()));
    }

    // Cung thu tu uu tien nhu AiPurchaseSuggestionService (Prompt #11) - thieu nhieu nhat truoc.
    suggestions.sort(
        Comparator.comparing(
            (PurchaseSuggestionResponse s) -> s.getCurrentStock().subtract(s.getMinStock())));
    return suggestions;
  }

  /**
   * Hop dong HTTP voi ml-service (xem ml-service/README hoac docs tuong duong) - CAN kiem chung lai
   * contract nay khop dung voi implementation Python thuc te truoc khi trien khai that (xem ghi chu
   * trien khai trong PROJECT_STATE.md muc "Prompt #12").
   */
  record MlForecastRequest(Long branchId, int lookbackDays, List<MlProductInput> products) {}

  record MlProductInput(
      Long productId,
      String productName,
      String sku,
      BigDecimal currentStock,
      BigDecimal minStock,
      List<MlDailySale> dailySales) {}

  record MlDailySale(String date, BigDecimal quantity) {}

  record MlForecastResponse(List<MlForecastResult> results) {}

  record MlForecastResult(
      Long productId,
      BigDecimal forecastDailyVelocity,
      BigDecimal daysOfStockRemaining,
      BigDecimal suggestedQty,
      BigDecimal confidence,
      Integer outliersRemoved) {}
}
