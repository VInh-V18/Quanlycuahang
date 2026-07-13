package com.quanlycuahang.erp.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlycuahang.erp.ai.provider.AiProvider.ConversationTurn;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Bo nho hoi thoai AI theo phien (Prompt #12) - Redis, key {@code ai:session:{tenantId}:{userId}},
 * TTL 30 phut TRUOT (reset moi lan co tin nhan moi, giong quy uoc {@link
 * com.quanlycuahang.erp.common.web.IdempotencyService}), toi da 10 luot gan nhat (cu hon bi loai
 * dan) - cho phep AI hieu cau hoi noi tiep ("Vay tuan nay thi sao?" sau cau hoi ve doanh thu thang
 * truoc) ma khong can nguoi dung nhac lai ngu canh.
 *
 * <p><b>Uoc luong do dai THO qua so ky tu, KHONG phai dem token chinh xac</b>: dem token chinh xac
 * (jtokkit) de lai cho Prompt #16 (kiem soat chi phi) - o day chi can 1 nguong an toan don gian de
 * tranh 1 luot hoi thoai qua dai lam context gui len AI phinh to bat thuong, khong can chinh xac
 * tuyet doi cho muc dich "nho ngu canh gan day".
 */
@Service
public class AiConversationMemoryService {

  private static final Logger log = LoggerFactory.getLogger(AiConversationMemoryService.class);
  private static final String KEY_PREFIX = "ai:session:";
  private static final Duration TTL = Duration.ofMinutes(30);
  private static final int MAX_TURNS = 10;
  private static final int MAX_CHARS_PER_TURN = 2000;

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public AiConversationMemoryService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  /**
   * Rong neu phien moi/chua co lich su/da het han (30 phut idle) - KHONG nem loi, chi tra danh sach
   * rong (mat lich su hoi thoai khong nghiem trong bang lam gian doan tinh nang chinh).
   */
  public List<ConversationTurn> getHistory(Long tenantId, Long userId) {
    String json = redisTemplate.opsForValue().get(key(tenantId, userId));
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<ConversationTurn>>() {});
    } catch (Exception ex) {
      log.warn(
          "Khong doc duoc lich su hoi thoai AI (session={}): {}",
          key(tenantId, userId),
          ex.getMessage());
      return List.of();
    }
  }

  public void appendTurn(Long tenantId, Long userId, String role, String content) {
    List<ConversationTurn> history = new ArrayList<>(getHistory(tenantId, userId));
    history.add(new ConversationTurn(role, truncate(content)));
    while (history.size() > MAX_TURNS) {
      history.remove(0);
    }
    try {
      String json = objectMapper.writeValueAsString(history);
      redisTemplate.opsForValue().set(key(tenantId, userId), json, TTL);
    } catch (Exception ex) {
      log.warn(
          "Khong luu duoc lich su hoi thoai AI (session={}): {}",
          key(tenantId, userId),
          ex.getMessage());
    }
  }

  public void clearSession(Long tenantId, Long userId) {
    redisTemplate.delete(key(tenantId, userId));
  }

  private String key(Long tenantId, Long userId) {
    return KEY_PREFIX + tenantId + ":" + userId;
  }

  private String truncate(String content) {
    return content.length() > MAX_CHARS_PER_TURN
        ? content.substring(0, MAX_CHARS_PER_TURN) + "..."
        : content;
  }
}
