package com.quanlycuahang.erp.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlycuahang.erp.ai.provider.AiProvider;
import com.quanlycuahang.erp.ai.provider.AiProvider.AiStreamListener;
import com.quanlycuahang.erp.ai.provider.AiProvider.AiToolExecutor;
import com.quanlycuahang.erp.ai.provider.AiProvider.ConversationTurn;
import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.auth.security.TenantSessionBinder;
import com.quanlycuahang.erp.system.entity.AuditLog;
import com.quanlycuahang.erp.system.entity.Tenant;
import com.quanlycuahang.erp.system.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Prompt #11/#12 ("Hoi dap bao cao" + streaming/lich su hoi thoai) - kiem chung {@link
 * AiAssistantService} chi dieu phoi (chon dung 1 trong 4 tool co san, chuyen tham so dung kieu, doc
 * ghi lich su hoi thoai, ghi audit log) - KHONG goi AiProvider that (mock), KHONG cham DB that
 * (mock AiQueryService/AuditLogRepository) - xem AiReadOnlyPermissionIT cho lop an toan connection
 * DB rieng.
 *
 * <p>Vi {@link AiProvider#askStreaming} la callback-based (khong tra ve gia tri), test o day tu
 * "dong vai" AiProvider: bat lai {@code AiToolExecutor}/{@code AiStreamListener} ma Service truyen
 * xuong qua {@link #stubProviderCapturing()}, roi TU GOI cac callback do (onToolSelected/onChunk/
 * onComplete/onError) de mo phong hanh vi cua provider that, xac nhan Service phan ung dung (ghi
 * lich su hoi thoai, ghi audit log, chuyen tiep dung cho listener cua nguoi goi).
 */
@ExtendWith(MockitoExtension.class)
class AiAssistantServiceTest {

  private static final Long TENANT_ID = 42L;
  private static final Long USER_ID = 7L;

  @Mock private AiProvider aiProvider;
  @Mock private AiQueryService aiQueryService;
  @Mock private AiConversationMemoryService conversationMemoryService;
  @Mock private AuditLogRepository auditLogRepository;
  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private EntityManager entityManager;
  @Mock private TenantSessionBinder tenantSessionBinder;

  private AiAssistantService service;
  private AiToolExecutor capturedExecutor;
  private AiStreamListener capturedProviderListener;

  @BeforeEach
  void setUp() {
    service =
        new AiAssistantService(
            aiProvider,
            aiQueryService,
            conversationMemoryService,
            auditLogRepository,
            currentUserProvider,
            entityManager,
            new ObjectMapper(),
            tenantSessionBinder);
    TenantContext.set(TENANT_ID);
    // lenient(): khong phai test nao cung di den logInteraction()/tenantSessionBinder.bind() (vd
    // cac test executeTool* chi goi truc tiep executor, khong tu kich hoat onComplete).
    lenient().when(entityManager.getReference(eq(Tenant.class), any())).thenReturn(new Tenant());
    lenient().when(tenantSessionBinder.bind(any())).thenReturn(entityManager);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  /**
   * Tuong minh mo phong AiProvider: khong tra loi ngay, chi luu lai executor/listener de test tu
   * goi callback sau, giong dung cach ClaudeAiProvider that su hoat dong (bat dong bo).
   */
  private void stubProviderCapturing() {
    doAnswer(
            invocation -> {
              capturedExecutor = invocation.getArgument(3);
              capturedProviderListener = invocation.getArgument(4);
              return null;
            })
        .when(aiProvider)
        .askStreaming(anyString(), any(), any(), any(), any());
  }

  private AiToolExecutor captureToolExecutor() {
    stubProviderCapturing();
    service.askStreaming(TENANT_ID, null, "Cau hoi bat ky", new CapturingListener());
    return capturedExecutor;
  }

  @Test
  void askStreamingBindsAndUnbindsTenantSession() {
    stubProviderCapturing();

    service.askStreaming(TENANT_ID, null, "Cau hoi", new CapturingListener());

    verify(tenantSessionBinder).bind(TENANT_ID);
    verify(tenantSessionBinder).unbind(entityManager);
  }

  @Test
  void askStreamingLoadsHistoryFromMemoryAndPassesToProvider() {
    List<ConversationTurn> history = List.of(new ConversationTurn("user", "cau hoi truoc do"));
    when(conversationMemoryService.getHistory(TENANT_ID, USER_ID)).thenReturn(history);
    stubProviderCapturing();

    service.askStreaming(TENANT_ID, USER_ID, "Cau hoi moi", new CapturingListener());

    verify(aiProvider).askStreaming(eq("Cau hoi moi"), eq(history), any(), any(), any());
  }

  @Test
  void askStreamingSkipsHistoryLookupWhenUserIdNull() {
    stubProviderCapturing();

    service.askStreaming(TENANT_ID, null, "Cau hoi", new CapturingListener());

    verify(aiProvider).askStreaming(anyString(), eq(List.of()), any(), any(), any());
    verify(conversationMemoryService, never()).getHistory(any(), any());
  }

  @Test
  void askStreamingForwardsToolSelectedAndChunksToCallerListener() {
    stubProviderCapturing();
    CapturingListener callerListener = new CapturingListener();

    service.askStreaming(TENANT_ID, USER_ID, "Doanh thu tuan nay?", callerListener);
    capturedProviderListener.onToolSelected("get_revenue_summary");
    capturedProviderListener.onChunk("Doanh thu ");
    capturedProviderListener.onChunk("la 10 trieu.");

    assertThat(callerListener.toolSelected).isEqualTo("get_revenue_summary");
    assertThat(callerListener.chunks).containsExactly("Doanh thu ", "la 10 trieu.");
  }

  @Test
  void askStreamingAppendsUserAndAssistantTurnsToMemoryOnComplete() {
    stubProviderCapturing();

    service.askStreaming(TENANT_ID, USER_ID, "Cau hoi", new CapturingListener());
    capturedProviderListener.onComplete("Tra loi day du", "get_revenue_summary");

    verify(conversationMemoryService).appendTurn(TENANT_ID, USER_ID, "user", "Cau hoi");
    verify(conversationMemoryService).appendTurn(TENANT_ID, USER_ID, "assistant", "Tra loi day du");
  }

  @Test
  void askStreamingSkipsMemoryAppendWhenUserIdNull() {
    stubProviderCapturing();

    service.askStreaming(TENANT_ID, null, "Cau hoi", new CapturingListener());
    capturedProviderListener.onComplete("Tra loi", null);

    verify(conversationMemoryService, never()).appendTurn(any(), any(), any(), any());
  }

  @Test
  void askStreamingForwardsCompletionToCallerListener() {
    stubProviderCapturing();
    CapturingListener callerListener = new CapturingListener();

    service.askStreaming(TENANT_ID, USER_ID, "Cau hoi", callerListener);
    capturedProviderListener.onComplete("Tra loi cuoi cung", "get_top_products");

    assertThat(callerListener.completed).isTrue();
    assertThat(callerListener.completedText).isEqualTo("Tra loi cuoi cung");
    assertThat(callerListener.completedTool).isEqualTo("get_top_products");
  }

  @Test
  void askStreamingForwardsErrorWithoutSavingMemoryOrAuditLog() {
    stubProviderCapturing();
    CapturingListener callerListener = new CapturingListener();

    service.askStreaming(TENANT_ID, USER_ID, "Cau hoi", callerListener);
    capturedProviderListener.onError("Loi ket noi Claude API");

    assertThat(callerListener.error).isEqualTo("Loi ket noi Claude API");
    verifyNoInteractions(auditLogRepository);
    verify(conversationMemoryService, never()).appendTurn(any(), any(), any(), any());
  }

  @Test
  void askStreamingSavesAuditLogWithQuestionAndToolUsed() {
    stubProviderCapturing();

    service.askStreaming(TENANT_ID, null, "San pham nao ban chay nhat?", new CapturingListener());
    capturedProviderListener.onComplete("Top 5 san pham la...", "get_top_products");

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captor.capture());
    AuditLog saved = captor.getValue();
    assertThat(saved.getAction()).isEqualTo("AI_ASK");
    assertThat(saved.getEntityName()).isEqualTo("get_top_products");
    assertThat(saved.getBefore()).isEqualTo("\"San pham nao ban chay nhat?\"");
    assertThat(saved.getAfter()).isEqualTo("\"Top 5 san pham la...\"");
  }

  @Test
  void askStreamingStillCompletesWhenAuditLoggingFails() {
    stubProviderCapturing();
    when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB tam thoi loi"));
    CapturingListener callerListener = new CapturingListener();

    service.askStreaming(TENANT_ID, null, "Cau hoi bat ky", callerListener);
    capturedProviderListener.onComplete("Cau tra loi van tra ve binh thuong.", null);

    assertThat(callerListener.completed).isTrue();
    assertThat(callerListener.completedText).isEqualTo("Cau tra loi van tra ve binh thuong.");
  }

  @Test
  void askStreamingSetsUserOnAuditLogViaEntityManagerReferenceWhenUserIdPresent() {
    User user = new User();
    when(entityManager.getReference(User.class, USER_ID)).thenReturn(user);
    stubProviderCapturing();

    service.askStreaming(TENANT_ID, USER_ID, "Cau hoi", new CapturingListener());
    capturedProviderListener.onComplete("Tra loi", null);

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captor.capture());
    assertThat(captor.getValue().getUser()).isSameAs(user);
  }

  @Test
  void askStreamingDoesNotSetUserOnAuditLogWhenUserIdNull() {
    stubProviderCapturing();

    service.askStreaming(TENANT_ID, null, "Cau hoi", new CapturingListener());
    capturedProviderListener.onComplete("Tra loi", null);

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captor.capture());
    assertThat(captor.getValue().getUser()).isNull();
  }

  @Test
  void executeToolDispatchesRevenueSummaryWithLookbackDaysArg() {
    AiToolExecutor executor = captureToolExecutor();
    when(aiQueryService.getRevenue(TENANT_ID, 7)).thenReturn(List.of());

    executor.execute("get_revenue_summary", Map.of("lookback_days", 7));

    verify(aiQueryService).getRevenue(TENANT_ID, 7);
  }

  @Test
  void executeToolDefaultsLookbackDaysWhenArgMissing() {
    AiToolExecutor executor = captureToolExecutor();
    when(aiQueryService.getRevenue(eq(TENANT_ID), anyInt())).thenReturn(List.of());

    executor.execute("get_revenue_summary", Map.of());

    verify(aiQueryService).getRevenue(TENANT_ID, 14);
  }

  @Test
  void executeToolDispatchesTopProductsWithLimitArg() {
    AiToolExecutor executor = captureToolExecutor();
    when(aiQueryService.getTopProducts(TENANT_ID, 3)).thenReturn(List.of());

    executor.execute("get_top_products", Map.of("limit", 3));

    verify(aiQueryService).getTopProducts(TENANT_ID, 3);
  }

  @Test
  void executeToolDispatchesLowStockWithBranchIdArg() {
    AiToolExecutor executor = captureToolExecutor();
    when(aiQueryService.getLowStockInventory(TENANT_ID, 5L)).thenReturn(List.of());

    executor.execute("get_low_stock_products", Map.of("branch_id", 5));

    verify(aiQueryService).getLowStockInventory(TENANT_ID, 5L);
  }

  @Test
  void executeToolDispatchesLowStockWithNullBranchIdWhenOmitted() {
    AiToolExecutor executor = captureToolExecutor();
    when(aiQueryService.getLowStockInventory(eq(TENANT_ID), isNull())).thenReturn(List.of());

    executor.execute("get_low_stock_products", Map.of());

    verify(aiQueryService).getLowStockInventory(eq(TENANT_ID), isNull());
  }

  @Test
  void executeToolDispatchesDebtAgingWithDirectionArg() {
    AiToolExecutor executor = captureToolExecutor();
    when(aiQueryService.getDebtAging(TENANT_ID, "payable")).thenReturn(List.of());

    executor.execute("get_debt_aging", Map.of("direction", "payable"));

    verify(aiQueryService).getDebtAging(TENANT_ID, "payable");
  }

  @Test
  void executeToolDefaultsDebtAgingDirectionToReceivableWhenMissing() {
    AiToolExecutor executor = captureToolExecutor();
    when(aiQueryService.getDebtAging(eq(TENANT_ID), anyString())).thenReturn(List.of());

    executor.execute("get_debt_aging", Map.of());

    verify(aiQueryService).getDebtAging(TENANT_ID, "receivable");
  }

  @Test
  void executeToolRejectsUnknownToolName() {
    AiToolExecutor executor = captureToolExecutor();

    assertThatThrownBy(() -> executor.execute("delete_all_orders", Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("delete_all_orders");
  }

  @Test
  void explainReturnsProviderTextAsIs() {
    when(aiProvider.explain(anyString(), eq("Giai thich doanh thu")))
        .thenReturn("Doanh thu tang vi san pham A ban chay.");

    String result = service.explain(Map.of("doanhThu", 1_000_000), "Giai thich doanh thu");

    assertThat(result).isEqualTo("Doanh thu tang vi san pham A ban chay.");
  }

  @Test
  void explainSerializesDataContextToJsonForProvider() {
    ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
    when(aiProvider.explain(jsonCaptor.capture(), anyString())).thenReturn("OK");

    service.explain(Map.of("doanhThu", 500_000), "Giai thich");

    assertThat(jsonCaptor.getValue()).contains("\"doanhThu\":500000");
  }

  @Test
  void explainLogsAuditEntryWithAiExplainAction() {
    when(aiProvider.explain(anyString(), anyString())).thenReturn("Giai thich");

    service.explain(Map.of("a", 1), "Cau hoi giai thich");

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captor.capture());
    assertThat(captor.getValue().getAction()).isEqualTo("AI_EXPLAIN");
  }

  /**
   * Test double don gian dong vai listener cua nguoi goi (Controller that su dung SseEmitter, o day
   * chi can ghi lai da nhan duoc gi de assert).
   */
  private static final class CapturingListener implements AiStreamListener {
    private final List<String> chunks = new ArrayList<>();
    private String toolSelected;
    private boolean completed;
    private String completedText;
    private String completedTool;
    private String error;

    @Override
    public void onToolSelected(String toolName) {
      this.toolSelected = toolName;
    }

    @Override
    public void onChunk(String textChunk) {
      chunks.add(textChunk);
    }

    @Override
    public void onComplete(String fullText, String toolUsed) {
      this.completed = true;
      this.completedText = fullText;
      this.completedTool = toolUsed;
    }

    @Override
    public void onError(String errorMessage) {
      this.error = errorMessage;
    }
  }
}
