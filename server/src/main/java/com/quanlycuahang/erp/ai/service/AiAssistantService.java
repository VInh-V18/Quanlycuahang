package com.quanlycuahang.erp.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlycuahang.erp.ai.provider.AiProvider;
import com.quanlycuahang.erp.ai.provider.AiProvider.AiStreamListener;
import com.quanlycuahang.erp.ai.provider.AiProvider.AiTool;
import com.quanlycuahang.erp.ai.provider.AiProvider.ConversationTurn;
import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.auth.security.TenantSessionBinder;
import com.quanlycuahang.erp.system.entity.AuditLog;
import com.quanlycuahang.erp.system.entity.Tenant;
import com.quanlycuahang.erp.system.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Dieu phoi tinh nang "Hoi dap bao cao bang tieng Viet" (Prompt #11, streaming + lich su hoi thoai
 * o Prompt #12) - AI CHI duoc chon 1 trong {@link #TOOLS} (4 ham co san, tham so co kieu ro rang),
 * KHONG bao gio tu sinh SQL. Tool duoc chon se THAT SU chay qua {@link AiQueryService} (pool rieng,
 * chi SELECT tren view whitelist - xem V30/AiReadOnlyDataSourceConfig).
 *
 * <p>Ghi lai MOI cau hoi + tool da goi vao {@code audit_logs} - ghi TRUC TIEP qua repository (khong
 * qua @Audited/AuditAspect, vi day khong phai 1 method CRUD binh thuong ma la 1 su kien tuy chinh).
 *
 * <p><b>Prompt #12 - vi sao {@link #askStreaming} nhan tenantId/userId TUONG MINH thay vi doc
 * TenantContext.get()/CurrentUserProvider trong than ham</b>: phuong thuc nay chay tren THREAD NEN
 * (xem AiAssistantController dung {@code SseEmitter} + virtual thread executor de KHONG chan thread
 * xu ly HTTP request goc trong suot thoi gian stream, co the toi hang chuc giay) - dung CHINH XAC
 * quy uoc da co san cua {@code InvoiceEmailService.sendInvoiceEmailAsync}: ThreadLocal cua thread
 * request goc (TenantContext, SecurityContextHolder) KHONG tu ke thua sang thread moi, nen tenantId
 * va userId phai duoc doc TRUOC (tren thread request goc, o Controller) roi truyen vao day, va
 * Session Hibernate/tenant filter phai tu bind lai qua {@link TenantSessionBinder} - KHONG duoc goi
 * lai {@code currentUserProvider.getCurrentUser()} ben trong ham nay (se luon tra ve rong vi
 * SecurityContextHolder chua thread nen).
 */
@Service
public class AiAssistantService {

  private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

  private static final List<AiTool> TOOLS =
      List.of(
          new AiTool(
              "get_revenue_summary",
              "Lay doanh thu va so don theo tung ngay trong N ngay gan day (dung de so sanh doanh"
                  + " thu tuan nay/thang nay voi truoc do)",
              Map.of(
                  "type",
                  "object",
                  "properties",
                  Map.of(
                      "lookback_days",
                      Map.of(
                          "type",
                          "integer",
                          "description",
                          "So ngay gan day can lay du lieu, vi du 14 de so sanh 2 tuan")),
                  "required",
                  List.of("lookback_days"))),
          new AiTool(
              "get_top_products",
              "Lay danh sach san pham ban chay nhat trong 30 ngay gan day, sap xep theo so luong"
                  + " ban giam dan",
              Map.of(
                  "type",
                  "object",
                  "properties",
                  Map.of(
                      "limit",
                      Map.of(
                          "type", "integer",
                          "description", "So san pham muon lay, vi du 5 hoac 10")),
                  "required",
                  List.of("limit"))),
          new AiTool(
              "get_low_stock_products",
              "Lay danh sach san pham dang co ton kho duoi hoac bang dinh muc toi thieu (sap het"
                  + " hang)",
              Map.of(
                  "type",
                  "object",
                  "properties",
                  Map.of(
                      "branch_id",
                      Map.of(
                          "type",
                          "integer",
                          "description",
                          "Chi loc theo 1 chi nhanh cu the (bo qua neu hoi chung toan bo cua hang)")),
                  "required",
                  List.of())),
          new AiTool(
              "get_debt_aging",
              "Lay tong hop cong no theo nhom tuoi no (0-30, 31-60, 61-90, tren 90 ngay)",
              Map.of(
                  "type",
                  "object",
                  "properties",
                  Map.of(
                      "direction",
                      Map.of(
                          "type",
                          "string",
                          "enum",
                          List.of("receivable", "payable"),
                          "description",
                          "receivable = cong no phai thu tu khach hang, payable = cong no phai tra"
                              + " nha cung cap")),
                  "required",
                  List.of("direction"))));

  private final AiProvider aiProvider;
  private final AiQueryService aiQueryService;
  private final AiConversationMemoryService conversationMemoryService;
  private final AuditLogRepository auditLogRepository;
  private final CurrentUserProvider currentUserProvider;
  private final EntityManager entityManager;
  private final ObjectMapper objectMapper;
  private final TenantSessionBinder tenantSessionBinder;

  public AiAssistantService(
      AiProvider aiProvider,
      AiQueryService aiQueryService,
      AiConversationMemoryService conversationMemoryService,
      AuditLogRepository auditLogRepository,
      CurrentUserProvider currentUserProvider,
      EntityManager entityManager,
      ObjectMapper objectMapper,
      TenantSessionBinder tenantSessionBinder) {
    this.aiProvider = aiProvider;
    this.aiQueryService = aiQueryService;
    this.conversationMemoryService = conversationMemoryService;
    this.auditLogRepository = auditLogRepository;
    this.currentUserProvider = currentUserProvider;
    this.entityManager = entityManager;
    this.objectMapper = objectMapper;
    this.tenantSessionBinder = tenantSessionBinder;
  }

  /**
   * Goi tu SSE endpoint, chay tren thread nen - xem Javadoc lop ve vi sao tenantId/userId la tham
   * so tuong minh. {@code userId} co the null (vd tai khoan he thong khong gan User cu the) - khi
   * do bo qua lich su hoi thoai + khong gan nguoi thuc hien vao audit log, van hoi dap binh thuong.
   */
  public void askStreaming(
      Long tenantId, Long userId, String question, AiStreamListener callerListener) {
    EntityManager boundEntityManager = tenantSessionBinder.bind(tenantId);
    try {
      List<ConversationTurn> history =
          userId != null ? conversationMemoryService.getHistory(tenantId, userId) : List.of();

      aiProvider.askStreaming(
          question,
          history,
          TOOLS,
          (toolName, args) -> executeTool(tenantId, toolName, args),
          new AiStreamListener() {
            @Override
            public void onToolSelected(String toolName) {
              callerListener.onToolSelected(toolName);
            }

            @Override
            public void onChunk(String textChunk) {
              callerListener.onChunk(textChunk);
            }

            @Override
            public void onComplete(String fullText, String toolUsed) {
              if (userId != null) {
                conversationMemoryService.appendTurn(tenantId, userId, "user", question);
                conversationMemoryService.appendTurn(tenantId, userId, "assistant", fullText);
              }
              logInteraction(tenantId, userId, "AI_ASK", toolUsed, question, fullText);
              callerListener.onComplete(fullText, toolUsed);
            }

            @Override
            public void onError(String errorMessage) {
              callerListener.onError(errorMessage);
            }
          });
    } finally {
      tenantSessionBinder.unbind(boundEntityManager);
    }
  }

  /**
   * Giai thich 1 khoi du lieu bao cao (Prompt #12, nut "AI giai thich" tren ReportsPage) - CHAY
   * TREN THREAD REQUEST GOC (khong stream, phan hoi ngan nen khong can chay nen) - khac {@link
   * #askStreaming}, o day doc thang TenantContext.get()/CurrentUserProvider vi dang chay dung tren
   * thread ma 2 ThreadLocal do da duoc JwtAuthenticationFilter/TenantFilter thiet lap.
   */
  public String explain(Map<String, Object> dataContext, String question) {
    Long tenantId = TenantContext.get();
    Long userId = currentUserProvider.getCurrentUser().map(User::getId).orElse(null);
    String dataContextJson;
    try {
      dataContextJson = objectMapper.writeValueAsString(dataContext);
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          "Du lieu bao cao khong hop le de gui cho AI giai thich", ex);
    }
    String explanation = aiProvider.explain(dataContextJson, question);
    logInteraction(tenantId, userId, "AI_EXPLAIN", null, question, explanation);
    return explanation;
  }

  private Object executeTool(Long tenantId, String toolName, Map<String, Object> args) {
    log.info("AI tool call: tenantId={} tool={} args={}", tenantId, toolName, args);
    return switch (toolName) {
      case "get_revenue_summary" ->
          aiQueryService.getRevenue(tenantId, intArg(args, "lookback_days", 14));
      case "get_top_products" -> aiQueryService.getTopProducts(tenantId, intArg(args, "limit", 5));
      case "get_low_stock_products" ->
          aiQueryService.getLowStockInventory(tenantId, longArgOrNull(args, "branch_id"));
      case "get_debt_aging" ->
          aiQueryService.getDebtAging(
              tenantId, (String) args.getOrDefault("direction", "receivable"));
      default -> throw new IllegalArgumentException("AI chon tool khong ton tai: " + toolName);
    };
  }

  private static int intArg(Map<String, Object> args, String key, int defaultValue) {
    Object value = args.get(key);
    return value instanceof Number number ? number.intValue() : defaultValue;
  }

  private static Long longArgOrNull(Map<String, Object> args, String key) {
    Object value = args.get(key);
    return value instanceof Number number ? number.longValue() : null;
  }

  private void logInteraction(
      Long tenantId, Long userId, String action, String toolUsed, String question, String answer) {
    try {
      AuditLog entry = new AuditLog();
      entry.setTenant(entityManager.getReference(Tenant.class, tenantId));
      entry.setAction(action);
      entry.setEntityName(toolUsed);
      entry.setBefore(objectMapper.writeValueAsString(question));
      entry.setAfter(objectMapper.writeValueAsString(answer));
      if (userId != null) {
        // Dung getReference (khong query lai) thay vi currentUserProvider.getCurrentUser(): userId
        // da duoc xac dinh tu truoc (tren thread request goc) va truyen vao day, goi lai
        // CurrentUserProvider o day se luon tra ve rong tren thread nen (xem Javadoc lop).
        entry.setUser(entityManager.getReference(User.class, userId));
      }
      auditLogRepository.save(entry);
    } catch (Exception ex) {
      // Ghi audit that bai khong duoc lam hong cau tra loi da co - chi log canh bao (giong quy uoc
      // cua AuditAspect).
      log.warn("Khong the ghi audit log cho {}: {}", action, ex.getMessage());
    }
  }
}
