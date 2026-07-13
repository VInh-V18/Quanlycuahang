package com.quanlycuahang.erp.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quanlycuahang.erp.ai.service.AiSettingsService;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Tich hop that voi Claude API (Anthropic Messages API, {@code POST /v1/messages}) qua tool-calling
 * — Prompt #11, nang cap streaming + phan tang model o Prompt #12. <b>CHUA duoc kiem chung bang 1
 * lan goi API that</b>: sandbox lam viec nay khong co API key Anthropic that de thu (giong tinh
 * trang cua {@link com.quanlycuahang.erp.operation.invoice.EInvoiceProvider} truoc day). Code duoi
 * day viet dung theo dinh dang request/response cua Anthropic Messages API (tools/tool_use/
 * tool_result, streaming SSE voi content_block_delta) theo tai lieu chinh thuc tai thoi diem viet —
 * CAN kiem chung lai bang 1 lan goi that truoc khi bat tinh nang cho tenant that.
 *
 * <p><b>2 luot goi cho hoi dap co tool (askStreaming)</b>: luot 1 (khong stream - chi la 1 quyet
 * dinh ngan "chon tool nao") AI chon tool + tham so, Backend THAT SU chay tool do; luot 2 (STREAM -
 * day la van ban dai nguoi dung thuc su doc) gui ket qua that lai cho AI de tong hop cau tra loi
 * cuoi cung, tung phan van ban duoc chuyen ve {@link AiStreamListener#onChunk} ngay khi nhan duoc
 * (khong doi ca cau tra loi xong moi tra ve).
 *
 * <p><b>Vi sao tu doc SSE thu cong qua {@code RestClient.exchange()} thay vi WebClient</b>: du an
 * nay co chu dich KHONG dua them dependency reactive (RestClient da duoc chon truoc do vi ly do
 * nay, xem Javadoc cu) - {@code exchange()} cho phep doc {@code InputStream} cua response TRUC TIEP
 * (khong doi Spring buffer toan bo body truoc), du khong "non-blocking" thuan tuy nhu WebClient
 * nhung van dam bao dung ngu nghia streaming: tung dong SSE duoc xu ly ngay khi socket nhan duoc,
 * khong phai sau khi ca response tai xong. Danh doi nay chap nhan duoc voi quy mo cua tinh nang.
 *
 * <p><b>Phan tang model (Prompt #12)</b>: hoi dap thuong ngay (askStreaming, widget chat Dashboard)
 * dung model NHANH/RE ({@code app.ai.provider.default-model}, mac dinh Haiku); giai thich bao cao
 * (explain, phan tich phuc tap hon) dung model MANH HON ({@code app.ai.provider.advanced-model},
 * mac dinh Sonnet). CAN KIEM CHUNG LAI ten model chinh xac voi tai lieu Anthropic hien tai luc
 * trien khai that (docs.anthropic.com), giong luu y ve default model truoc day.
 *
 * <p><b>{@code @ConditionalOnProperty matchIfMissing = true}</b>: day la provider MAC DINH (giu
 * nguyen hanh vi truoc khi co lua chon Ollama tu-host o Prompt #12 tiep theo) - CHI bi thay the khi
 * {@code app.ai.provider.type=ollama} tuong minh. Bat buoc dung dieu kien nay (khong de ca 2
 * provider cung khong dieu kien) de tranh dung LAP LAI dung bug lop "2 bean cung kieu, Spring tu
 * chon nham" da tung gap voi DataSource/JdbcTemplate (xem Javadoc AiReadOnlyDataSourceConfig) - o
 * day neu ca ClaudeAiProvider va OllamaAiProvider deu la @Component khong dieu kien, se co 2 bean
 * AiProvider cung luc, AiAssistantService se KHONG BIET dung provider nao (loi khoi dong ro rang,
 * may man hon truong hop DataSource vi it nhat se FAIL FAST thay vi im lang dung nham).
 */
@Component
@ConditionalOnProperty(
    prefix = "app.ai.provider",
    name = "type",
    havingValue = "claude",
    matchIfMissing = true)
public class ClaudeAiProvider implements AiProvider {

  private static final Logger log = LoggerFactory.getLogger(ClaudeAiProvider.class);
  private static final String API_URL = "https://api.anthropic.com/v1/messages";
  private static final String ANTHROPIC_VERSION = "2023-06-01";
  private static final int MAX_TOKENS = 1024;

  private static final String QA_SYSTEM_PROMPT =
      "Ban la tro ly bao cao cho phan mem quan ly ban hang FruitHouse. Tra loi ngan gon, chinh xac,"
          + " bang tieng Viet, dua HOAN TOAN vao ket qua tool duoc cung cap - khong tu bia so lieu."
          + " Neu cau hoi khong lien quan bao cao/kinh doanh, tra loi lich su rang ban chi ho tro"
          + " cau hoi ve bao cao ban hang.";

  private static final String EXPLAIN_SYSTEM_PROMPT =
      "Ban la chuyen gia phan tich kinh doanh cho phan mem quan ly ban hang FruitHouse. Nguoi dung"
          + " la chu cua hang, khong ranh chuyen mon tai chinh/ky thuat. CHI su dung so lieu trong"
          + " the <data> duoc cung cap - TUYET DOI khong tu bia them so lieu nao khac; neu cau hoi"
          + " can 1 so khong co trong <data>, noi ro 'khong co du lieu nay' thay vi doan. Tra loi"
          + " ngan gon bang tieng Viet thong thuong (khong dung jargon tai chinh), uu tien 1 cau tom"
          + " tat truoc, sau do chi tiet/khuyen nghi hanh dong neu phu hop.";

  private final AiSettingsService aiSettingsService;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private final String defaultModel;
  private final String advancedModel;

  public ClaudeAiProvider(
      AiSettingsService aiSettingsService,
      ObjectMapper objectMapper,
      @Value("${app.ai.provider.default-model}") String defaultModel,
      @Value("${app.ai.provider.advanced-model}") String advancedModel,
      @Value("${app.ai.provider.timeout-seconds:30}") int timeoutSeconds) {
    this.aiSettingsService = aiSettingsService;
    this.objectMapper = objectMapper;
    this.defaultModel = defaultModel;
    this.advancedModel = advancedModel;
    this.restClient =
        RestClient.builder()
            .baseUrl(API_URL)
            .requestFactory(
                new org.springframework.http.client.SimpleClientHttpRequestFactory() {
                  {
                    setConnectTimeout((int) Duration.ofSeconds(timeoutSeconds).toMillis());
                    // Timeout nay ap dung cho MOI lan doc tu socket (khong phai tong thoi gian ca
                    // ket noi) - dung ngu nghia cho ket noi streaming: se KHONG bi timeout chi vi
                    // tong thoi gian stream vuot 30s, chi timeout neu 1 khoang lang > 30s giua 2
                    // lan nhan du lieu (dau hieu ket noi thuc su bi treo).
                    setReadTimeout((int) Duration.ofSeconds(timeoutSeconds).toMillis());
                  }
                })
            .build();
  }

  @Override
  public void askStreaming(
      String userQuestion,
      List<ConversationTurn> history,
      List<AiTool> tools,
      AiToolExecutor toolExecutor,
      AiStreamListener listener) {
    String apiKey;
    try {
      apiKey =
          aiSettingsService
              .getApiKey()
              .orElseThrow(
                  () ->
                      new BusinessRuleException(
                          "AI_NOT_CONFIGURED",
                          "Chưa cấu hình trợ lý AI cho cửa hàng — vào Cài đặt để thêm khoá API"));
    } catch (RuntimeException ex) {
      listener.onError(ex.getMessage());
      return;
    }

    ArrayNode messages = objectMapper.createArrayNode();
    for (ConversationTurn turn : history) {
      messages.add(historyMessage(turn));
    }
    messages.add(userMessage(userQuestion));

    JsonNode firstResponse;
    try {
      firstResponse = callMessagesApi(apiKey, messages, tools, defaultModel);
    } catch (RuntimeException ex) {
      listener.onError(ex.getMessage());
      return;
    }

    JsonNode toolUseBlock = findContentBlock(firstResponse, "tool_use");
    if (toolUseBlock == null) {
      String text = extractText(firstResponse);
      listener.onChunk(text);
      listener.onComplete(text, null);
      return;
    }

    String toolName = toolUseBlock.get("name").asText();
    listener.onToolSelected(toolName);
    Map<String, Object> arguments = objectMapper.convertValue(toolUseBlock.get("input"), Map.class);
    Object toolResult;
    try {
      toolResult = toolExecutor.execute(toolName, arguments);
    } catch (RuntimeException ex) {
      log.warn("AI tool execution failed: tool={}", toolName, ex);
      String text =
          "Xin lỗi, tôi không lấy được dữ liệu để trả lời câu hỏi này. Vui lòng thử lại hoặc kiểm"
              + " tra trực tiếp trên trang báo cáo.";
      listener.onChunk(text);
      listener.onComplete(text, toolName);
      return;
    }

    // Luot 2: gui lai assistant message (chua tool_use) + tool_result (ket qua THAT, Backend vua
    // chay) de AI tong hop cau tra loi cuoi cung, STREAM tung phan van ban ve listener.
    messages.add(assistantMessageWithToolUse(firstResponse));
    messages.add(toolResultMessage(toolUseBlock.get("id").asText(), toolResult));

    StringBuilder fullText = new StringBuilder();
    try {
      streamMessagesApi(apiKey, messages, tools, defaultModel, fullText::append, listener::onChunk);
    } catch (RuntimeException ex) {
      listener.onError(ex.getMessage());
      return;
    }
    listener.onComplete(fullText.toString(), toolName);
  }

  @Override
  public String explain(String dataContextJson, String question) {
    String apiKey =
        aiSettingsService
            .getApiKey()
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "AI_NOT_CONFIGURED",
                        "Chưa cấu hình trợ lý AI cho cửa hàng — vào Cài đặt để thêm khoá API"));

    ArrayNode messages = objectMapper.createArrayNode();
    messages.add(userMessage("<data>" + dataContextJson + "</data>\n\nCâu hỏi: " + question));

    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", advancedModel);
    body.put("max_tokens", MAX_TOKENS);
    body.put("system", EXPLAIN_SYSTEM_PROMPT);
    body.set("messages", messages);

    JsonNode response = executeMessagesApi(apiKey, body);
    return extractText(response);
  }

  private JsonNode callMessagesApi(
      String apiKey, ArrayNode messages, List<AiTool> tools, String model) {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", model);
    body.put("max_tokens", MAX_TOKENS);
    body.put("system", QA_SYSTEM_PROMPT);
    body.set("messages", messages);
    body.set("tools", toolsToJson(tools));
    return executeMessagesApi(apiKey, body);
  }

  private JsonNode executeMessagesApi(String apiKey, ObjectNode body) {
    try {
      return restClient
          .post()
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .header("x-api-key", apiKey)
          .header("anthropic-version", ANTHROPIC_VERSION)
          .body(body)
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientResponseException ex) {
      // Loi HTTP tu Claude (vd 401 sai khoa API, 429 vuot han muc, 5xx phia Anthropic).
      log.warn(
          "Claude API loi: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
      throw new BusinessRuleException(
          "AI_PROVIDER_ERROR", "Trợ lý AI đang gặp sự cố, vui lòng thử lại sau ít phút");
    } catch (ResourceAccessException ex) {
      // Timeout (30s, xem app.ai.provider.timeout-seconds) hoac mat ket noi mang toi Anthropic -
      // KHONG de loi nay vo trang (roadmap: "loi API → thong bao nhe nhang, khong vo trang").
      log.warn("Claude API timeout/mat ket noi: {}", ex.getMessage());
      throw new BusinessRuleException(
          "AI_PROVIDER_TIMEOUT", "Trợ lý AI phản hồi quá lâu, vui lòng thử lại sau");
    }
  }

  /**
   * Doc SSE tu Anthropic ({@code "stream": true}) - CHI quan tam su kien {@code
   * content_block_delta} kieu {@code text_delta} (van ban cau tra loi), bo qua moi su kien khac
   * ({@code message_start}, {@code content_block_stop}...). Truong hop hiem AI lai chon THEM 1 tool
   * o luot nay (khong co text_delta nao ca) - KHONG duoc xu ly (gioi han da biet, xem Javadoc lop)
   * - onAppend se don gian khong nhan duoc gi, listener.onComplete se nhan chuoi rong.
   */
  private void streamMessagesApi(
      String apiKey,
      ArrayNode messages,
      List<AiTool> tools,
      String model,
      java.util.function.Consumer<String> onAppend,
      java.util.function.Consumer<String> onChunk) {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", model);
    body.put("max_tokens", MAX_TOKENS);
    body.put("system", QA_SYSTEM_PROMPT);
    body.set("messages", messages);
    body.set("tools", toolsToJson(tools));
    body.put("stream", true);

    try {
      restClient
          .post()
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .header("x-api-key", apiKey)
          .header("anthropic-version", ANTHROPIC_VERSION)
          .body(body)
          .exchange(
              (request, response) -> {
                try (BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                  String line;
                  while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) {
                      continue;
                    }
                    String json = line.substring("data: ".length()).trim();
                    if (json.isEmpty()) {
                      continue;
                    }
                    JsonNode event = objectMapper.readTree(json);
                    if (!"content_block_delta".equals(event.path("type").asText())) {
                      continue;
                    }
                    JsonNode delta = event.path("delta");
                    if ("text_delta".equals(delta.path("type").asText())) {
                      String text = delta.path("text").asText();
                      onAppend.accept(text);
                      onChunk.accept(text);
                    }
                  }
                }
                return null;
              });
    } catch (RestClientException ex) {
      log.warn("Claude API streaming loi: {}", ex.getMessage());
      throw new BusinessRuleException(
          "AI_PROVIDER_ERROR", "Trợ lý AI đang gặp sự cố, vui lòng thử lại sau ít phút");
    } catch (RuntimeException ex) {
      // BufferedReader/ObjectMapper.readTree co the nem IOException duoc boc lai thanh
      // UncheckedIOException tu lambda exchange() - vao chung 1 nhanh loi voi RestClientException.
      log.warn("Loi doc stream tu Claude API: {}", ex.getMessage());
      throw new BusinessRuleException(
          "AI_PROVIDER_ERROR", "Trợ lý AI đang gặp sự cố, vui lòng thử lại sau ít phút");
    }
  }

  private ArrayNode toolsToJson(List<AiTool> tools) {
    ArrayNode array = objectMapper.createArrayNode();
    for (AiTool tool : tools) {
      ObjectNode node = objectMapper.createObjectNode();
      node.put("name", tool.name());
      node.put("description", tool.description());
      node.set("input_schema", objectMapper.valueToTree(tool.parametersJsonSchema()));
      array.add(node);
    }
    return array;
  }

  private ObjectNode userMessage(String text) {
    ObjectNode message = objectMapper.createObjectNode();
    message.put("role", "user");
    message.put("content", text);
    return message;
  }

  private ObjectNode historyMessage(ConversationTurn turn) {
    ObjectNode message = objectMapper.createObjectNode();
    message.put("role", turn.role());
    message.put("content", turn.content());
    return message;
  }

  private ObjectNode assistantMessageWithToolUse(JsonNode firstResponse) {
    ObjectNode message = objectMapper.createObjectNode();
    message.put("role", "assistant");
    message.set("content", firstResponse.get("content"));
    return message;
  }

  private ObjectNode toolResultMessage(String toolUseId, Object toolResult) {
    ObjectNode resultBlock = objectMapper.createObjectNode();
    resultBlock.put("type", "tool_result");
    resultBlock.put("tool_use_id", toolUseId);
    resultBlock.put("content", toJsonString(toolResult));

    ArrayNode content = objectMapper.createArrayNode();
    content.add(resultBlock);

    ObjectNode message = objectMapper.createObjectNode();
    message.put("role", "user");
    message.set("content", content);
    return message;
  }

  private String toJsonString(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (IOException ex) {
      return "{}";
    }
  }

  private JsonNode findContentBlock(JsonNode response, String type) {
    JsonNode content = response.get("content");
    if (content == null || !content.isArray()) {
      return null;
    }
    for (JsonNode block : content) {
      if (type.equals(block.path("type").asText())) {
        return block;
      }
    }
    return null;
  }

  private String extractText(JsonNode response) {
    JsonNode textBlock = findContentBlock(response, "text");
    if (textBlock != null) {
      return textBlock.get("text").asText();
    }
    return "Xin lỗi, tôi không có câu trả lời cho câu hỏi này.";
  }
}
