package com.quanlycuahang.erp.ai.controller;

import com.quanlycuahang.erp.ai.dto.AiAskRequest;
import com.quanlycuahang.erp.ai.dto.AiExplainRequest;
import com.quanlycuahang.erp.ai.dto.AiExplainResponse;
import com.quanlycuahang.erp.ai.dto.AiSettingsStatusResponse;
import com.quanlycuahang.erp.ai.dto.AiSettingsUpdateRequest;
import com.quanlycuahang.erp.ai.dto.PurchaseSuggestionResponse;
import com.quanlycuahang.erp.ai.provider.AiProvider.AiStreamListener;
import com.quanlycuahang.erp.ai.service.AiAssistantService;
import com.quanlycuahang.erp.ai.service.AiConversationMemoryService;
import com.quanlycuahang.erp.ai.service.AiForecastService;
import com.quanlycuahang.erp.ai.service.AiPurchaseSuggestionService;
import com.quanlycuahang.erp.ai.service.AiSettingsService;
import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI Assistant (Prompt #11, streaming + phien hoi thoai o Prompt #12) — hoi dap bao cao (Text→API,
 * xem AiAssistantService), giai thich bao cao, va goi y nhap hang (thuat toan xac dinh, xem
 * AiPurchaseSuggestionService). Quyen {@code ai:use} (owner/manager) cho da so endpoint; {@code
 * ai:manage-settings} (chi owner) rieng cho cau hinh khoa API.
 *
 * <p><b>Vi sao {@code /ask} tra ve {@link SseEmitter} chay tren executor rieng (khong phai thread
 * xu ly HTTP request cua Tomcat)</b>: cau tra loi AI co the mat toi hang chuc giay (Prompt #12 -
 * streaming tung phan van ban thay vi doi ca cau xong moi tra ve) - giu thread Tomcat cho ca khoang
 * thoi gian do se can kiet pool thread khi nhieu nguoi dung hoi cung luc. Virtual thread (Java 21)
 * la lua chon tu nhien: re, khong can cau hinh pool size nhu thread thuong. tenantId/userId PHAI
 * doc TU THREAD REQUEST GOC (o day, truoc khi submit vao executor) va truyen tuong minh xuong
 * AiAssistantService.askStreaming - xem Javadoc chi tiet o do ve ly do ThreadLocal khong tu ke thua
 * sang thread moi.
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiAssistantController {

  private static final Logger log = LoggerFactory.getLogger(AiAssistantController.class);
  private static final long SSE_TIMEOUT_MILLIS = 40_000L;

  private final AiAssistantService aiAssistantService;
  private final AiConversationMemoryService conversationMemoryService;
  private final AiPurchaseSuggestionService aiPurchaseSuggestionService;
  private final AiForecastService aiForecastService;
  private final AiSettingsService aiSettingsService;
  private final CurrentUserProvider currentUserProvider;
  private final ExecutorService streamingExecutor = Executors.newVirtualThreadPerTaskExecutor();

  public AiAssistantController(
      AiAssistantService aiAssistantService,
      AiConversationMemoryService conversationMemoryService,
      AiPurchaseSuggestionService aiPurchaseSuggestionService,
      AiForecastService aiForecastService,
      AiSettingsService aiSettingsService,
      CurrentUserProvider currentUserProvider) {
    this.aiAssistantService = aiAssistantService;
    this.conversationMemoryService = conversationMemoryService;
    this.aiPurchaseSuggestionService = aiPurchaseSuggestionService;
    this.aiForecastService = aiForecastService;
    this.aiSettingsService = aiSettingsService;
    this.currentUserProvider = currentUserProvider;
  }

  @PreDestroy
  void shutdownExecutor() {
    streamingExecutor.shutdown();
  }

  @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize("hasAuthority('ai:use')")
  public SseEmitter ask(@Valid @RequestBody AiAskRequest request) {
    Long tenantId = TenantContext.get();
    Long userId = currentUserProvider.getCurrentUser().map(User::getId).orElse(null);
    SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

    streamingExecutor.execute(
        () ->
            aiAssistantService.askStreaming(
                tenantId, userId, request.getQuestion(), new SseStreamListener(emitter)));

    return emitter;
  }

  @DeleteMapping("/session")
  @PreAuthorize("hasAuthority('ai:use')")
  public ResponseEntity<ApiResponse<Void>> clearSession() {
    Long tenantId = TenantContext.get();
    currentUserProvider
        .getCurrentUser()
        .ifPresent(user -> conversationMemoryService.clearSession(tenantId, user.getId()));
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @PostMapping("/explain")
  @PreAuthorize("hasAuthority('ai:use')")
  public ResponseEntity<ApiResponse<AiExplainResponse>> explain(
      @Valid @RequestBody AiExplainRequest request) {
    String explanation =
        aiAssistantService.explain(request.getDataContext(), request.getQuestion());
    return ResponseEntity.ok(ApiResponse.success(new AiExplainResponse(explanation)));
  }

  @GetMapping("/purchase-suggestions")
  @PreAuthorize("hasAuthority('ai:use')")
  public ResponseEntity<ApiResponse<List<PurchaseSuggestionResponse>>> purchaseSuggestions(
      @RequestParam Long branchId) {
    return ResponseEntity.ok(ApiResponse.success(aiPurchaseSuggestionService.suggest(branchId)));
  }

  /**
   * "Gợi ý AI nâng cao" (Prompt #12) - qua ml-service (IsolationForest), tu dong fallback ve {@link
   * #purchaseSuggestions} (thuat toan don gian) neu ml-service khong kha dung - xem Javadoc
   * AiForecastService, KHONG BAO GIO tra loi/500 chi vi dich vu phu nay gap su co.
   */
  @GetMapping("/purchase-suggestions/advanced")
  @PreAuthorize("hasAuthority('ai:use')")
  public ResponseEntity<ApiResponse<List<PurchaseSuggestionResponse>>> purchaseSuggestionsAdvanced(
      @RequestParam Long branchId) {
    return ResponseEntity.ok(ApiResponse.success(aiForecastService.suggestAdvanced(branchId)));
  }

  @GetMapping("/settings")
  @PreAuthorize("hasAuthority('ai:manage-settings')")
  public ResponseEntity<ApiResponse<AiSettingsStatusResponse>> getSettings() {
    return ResponseEntity.ok(
        ApiResponse.success(new AiSettingsStatusResponse(aiSettingsService.isConfigured())));
  }

  @PutMapping("/settings")
  @PreAuthorize("hasAuthority('ai:manage-settings')")
  public ResponseEntity<ApiResponse<Void>> updateSettings(
      @Valid @RequestBody AiSettingsUpdateRequest request) {
    aiSettingsService.updateApiKey(request.getApiKey());
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @DeleteMapping("/settings")
  @PreAuthorize("hasAuthority('ai:manage-settings')")
  public ResponseEntity<ApiResponse<Void>> disableSettings() {
    aiSettingsService.disable();
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  /**
   * Dich su kien tu {@link AiStreamListener} thanh khung SSE gui ve trinh duyet - {@code event:
   * tool/chunk/done/error} (hop dong rieng cho FE, xem client/src/lib/api/ai.ts). Nuot IOException
   * tu {@code emitter.send()} (khach da ngat ket noi giua chung - khong con ai nhan nua, khong phai
   * loi can bao) chi log muc DEBUG-tuong-duong (warn nhe, khong lam on CI).
   */
  private static final class SseStreamListener implements AiStreamListener {

    private final SseEmitter emitter;

    private SseStreamListener(SseEmitter emitter) {
      this.emitter = emitter;
    }

    @Override
    public void onToolSelected(String toolName) {
      sendQuietly("tool", Map.of("toolUsed", toolName));
    }

    @Override
    public void onChunk(String textChunk) {
      sendQuietly("chunk", Map.of("text", textChunk));
    }

    @Override
    public void onComplete(String fullText, String toolUsed) {
      sendQuietly("done", Map.of());
      emitter.complete();
    }

    @Override
    public void onError(String errorMessage) {
      sendQuietly("error", Map.of("message", errorMessage));
      emitter.complete();
    }

    private void sendQuietly(String eventName, Object data) {
      try {
        emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
      } catch (IOException | IllegalStateException ex) {
        log.warn(
            "Khong gui duoc su kien SSE '{}' (khach co the da ngat ket noi): {}",
            eventName,
            ex.getMessage());
      }
    }
  }
}
