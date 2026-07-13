package com.quanlycuahang.erp.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlycuahang.erp.ai.provider.AiProvider.ConversationTurn;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Prompt #12 - bo nho hoi thoai AI theo phien (Redis, TTL 30 phut truot, toi da 10 luot). Mock
 * {@link StringRedisTemplate} nhung backing bang 1 Map trong bo nho de kiem chung round-trip THAT
 * (ghi roi doc lai dung), khong chi kiem tung loi goi rieng le.
 */
@ExtendWith(MockitoExtension.class)
class AiConversationMemoryServiceTest {

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  private final Map<String, String> fakeStore = new HashMap<>();
  private AiConversationMemoryService service;

  @BeforeEach
  void setUp() {
    // lenient(): day la hanh vi CHUNG cua "backing store" gia lap, khong phai ky vong rieng cho
    // tung test - khong phai test nao cung goi ca 3 thao tac (get/set/delete), Mockito strict-stubs
    // (mac dinh cua MockitoExtension) se bao "unnecessary stubbing" cho test nao khong dung het.
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    lenient()
        .when(valueOperations.get(anyString()))
        .thenAnswer(inv -> fakeStore.get((String) inv.getArgument(0)));
    lenient()
        .doAnswer(
            inv -> {
              fakeStore.put(inv.getArgument(0), inv.getArgument(1));
              return null;
            })
        .when(valueOperations)
        .set(anyString(), anyString(), any(Duration.class));
    lenient()
        .when(redisTemplate.delete(anyString()))
        .thenAnswer(inv -> fakeStore.remove((String) inv.getArgument(0)) != null);

    service = new AiConversationMemoryService(redisTemplate, new ObjectMapper());
  }

  @Test
  void getHistoryReturnsEmptyWhenNoSessionExists() {
    assertThat(service.getHistory(1L, 2L)).isEmpty();
  }

  @Test
  void appendTurnThenGetHistoryReturnsAppendedTurn() {
    service.appendTurn(1L, 2L, "user", "Doanh thu thang nay bao nhieu?");

    List<ConversationTurn> history = service.getHistory(1L, 2L);

    assertThat(history).hasSize(1);
    assertThat(history.get(0).role()).isEqualTo("user");
    assertThat(history.get(0).content()).isEqualTo("Doanh thu thang nay bao nhieu?");
  }

  @Test
  void appendTurnAccumulatesMultipleTurnsInOrder() {
    service.appendTurn(1L, 2L, "user", "Cau hoi 1");
    service.appendTurn(1L, 2L, "assistant", "Tra loi 1");
    service.appendTurn(1L, 2L, "user", "Cau hoi 2");

    List<ConversationTurn> history = service.getHistory(1L, 2L);

    assertThat(history)
        .extracting(ConversationTurn::content)
        .containsExactly("Cau hoi 1", "Tra loi 1", "Cau hoi 2");
  }

  @Test
  void appendTurnEvictsOldestWhenExceedingMaxTenTurns() {
    for (int i = 0; i < 12; i++) {
      service.appendTurn(1L, 2L, "user", "cau hoi " + i);
    }

    List<ConversationTurn> history = service.getHistory(1L, 2L);

    assertThat(history).hasSize(10);
    assertThat(history.get(0).content()).isEqualTo("cau hoi 2");
    assertThat(history.get(9).content()).isEqualTo("cau hoi 11");
  }

  @Test
  void appendTurnTruncatesOverlyLongContent() {
    String longContent = "a".repeat(3000);

    service.appendTurn(1L, 2L, "assistant", longContent);

    String stored = service.getHistory(1L, 2L).get(0).content();
    assertThat(stored.length()).isLessThan(3000);
    assertThat(stored).endsWith("...");
  }

  @Test
  void clearSessionRemovesHistory() {
    service.appendTurn(1L, 2L, "user", "cau hoi");

    service.clearSession(1L, 2L);

    assertThat(service.getHistory(1L, 2L)).isEmpty();
  }

  @Test
  void sessionsAreIsolatedPerTenantAndUser() {
    service.appendTurn(1L, 2L, "user", "tenant 1 user 2");
    service.appendTurn(1L, 3L, "user", "tenant 1 user 3");
    service.appendTurn(9L, 2L, "user", "tenant 9 user 2");

    assertThat(service.getHistory(1L, 2L))
        .extracting(ConversationTurn::content)
        .containsExactly("tenant 1 user 2");
    assertThat(service.getHistory(1L, 3L))
        .extracting(ConversationTurn::content)
        .containsExactly("tenant 1 user 3");
    assertThat(service.getHistory(9L, 2L))
        .extracting(ConversationTurn::content)
        .containsExactly("tenant 9 user 2");
  }

  @Test
  void getHistoryReturnsEmptyWhenStoredValueIsCorrupted() {
    fakeStore.put("ai:session:1:2", "not valid json { ");

    assertThat(service.getHistory(1L, 2L)).isEmpty();
  }
}
