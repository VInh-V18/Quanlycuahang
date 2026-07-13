package com.quanlycuahang.erp.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Tich hop voi model AI TU HOST qua Ollama (https://ollama.com, container rieng {@code
 * ollama/ollama}, xem docker-compose.yml) — lua chon thay the {@link ClaudeAiProvider} khi KHONG
 * muon phu thuoc API dam may tra phi/can khoa API (bat qua {@code app.ai.provider.type=ollama}).
 * Hoat dong TREN CHINH may chu cua FruitHouse, khong gui du lieu ra ngoai.
 *
 * <p><b>DA kiem chung hop dong HTTP bang Ollama that (khac ClaudeAiProvider - Claude van CHUA kiem
 * chung duoc vi khong co khoa API that)</b>: chay 1 container Ollama tam thoi doc lap, pull that
 * {@code qwen2.5:0.5b}/{@code qwen2.5:1.5b}, goi truc tiep {@code POST /api/chat} voi dung cau truc
 * message/tool nhu code duoi day tao ra - xac nhan CA 2 luot: luot 1 (khong stream) tra ve dung
 * {@code message.tool_calls[0].function.name}/{@code .arguments} nhu ky vong, va luot 2 (stream)
 * tra ve dung NDJSON voi {@code message.content} tang dan + {@code done:true} o dong cuoi, model
 * dung DUNG so lieu tool-result gia lap trong cau tra loi (khong bia). <b>Phat hien quan trong khi
 * kiem chung</b>: model NHO (0.5B) hau nhu KHONG bao gio tu chon goi tool du co system prompt yeu
 * cau ro; model 1.5B goi tool THANH CONG khi prompt bang TIENG ANH ro rang, nhung KHONG goi duoc
 * tool khi dung nguyen system prompt tieng Viet cua {@link #QA_SYSTEM_PROMPT} (van tra loi hoi
 * chung chung, xin lam ro thay vi goi tool ngay) - nghia la voi model nho, DO TIN CAY tool-calling
 * tieng Viet co the thap hon dang ke so voi tieng Anh hoac so voi Claude. Model {@code qwen2.5:3b}
 * (mac dinh trien khai) CHUA duoc thu truc tiep trong lan kiem chung nay (chi 0.5B/1.5B, nho hon,
 * de tai/thu nhanh hon) - CAN tu thu lai voi dung model se trien khai that + dung system prompt
 * tieng Viet that truoc khi thong bao tinh nang cho nguoi dung cuoi, xem huong dan o
 * README_DEPLOY.md muc 12c.
 *
 * <p><b>Vi sao KHONG dung {@link com.quanlycuahang.erp.ai.service.AiSettingsService} (khong can
 * khoa API rieng tung tenant)</b>: Ollama la 1 tai nguyen CHUNG cua toan nen tang (chay tren may
 * chu cua nguoi van hanh FruitHouse, khong phai dich vu tra phi theo token ma tung chu cua hang tu
 * tra), khac han mo hinh "moi tenant tu mang khoa API rieng" cua Claude - vi vay KHONG can co che
 * bat/tat + luu khoa theo tung tenant, chi can quyen {@code ai:use} (da co san, V31) la du dieu
 * kien su dung. Neu Ollama khong ket noi duoc (chua pull model, container chua khoi dong xong...),
 * loi tra ve giong het truong hop Claude loi API - thong bao nhe nhang, khong vo trang.
 *
 * <p><b>CHI 1 model duy nhat cho ca hoi dap thuong VA "giai thich"</b> (khac Claude co phan tang
 * Haiku/Sonnet, Prompt #12) - danh doi CHU DICH: may chu tu host thuong co RAM han che (vd 8GB dung
 * chung voi Postgres/Redis/server/web/ml-service), giu 2 model khac nhau cung luc trong bo nho se
 * de OOM hon la mot may chu API dam may co tai nguyen rieng cho tung request. Neu can chat luong
 * cao hon cho "giai thich", nang cap model qua {@code app.ai.ollama.model} (doi 1 model lon hon,
 * chap nhan cham hon) thay vi chay song song 2 model.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai.provider", name = "type", havingValue = "ollama")
public class OllamaAiProvider implements AiProvider {

  private static final Logger log = LoggerFactory.getLogger(OllamaAiProvider.class);

  // "30m" (mac dinh Ollama la 5 phut) - DA DO THAT: nap model qwen2.5:3b vao RAM lan dau ton them
  // ~16-17 GIAY tren phan cung khong GPU (xem log kiem chung trien khai that trong
  // PROJECT_STATE.md) - giu model "am" lau hon giua cac lan hoi giam tan suat nguoi dung gap do
  // tre nay, du van con 1 lan cold-start dau tien sau khi container Ollama moi khoi dong/restart.
  private static final String KEEP_ALIVE = "30m";

  // Manh me hon ban cua ClaudeAiProvider (them 2 cau BAT BUOC/VND/dinh dang so) - DA KIEM CHUNG
  // that can thiet: qwen2.5:3b that (khong phai model nho hon dung thu truoc do) tra loi bang
  // TIENG ANH + don vi RMB (nham lan) khi dung nguyen ban prompt "nhe" nhu Claude, du la cung 1 cau
  // hoi + cung ket qua tool. Sau khi doi sang ban duoi day, model tra loi dung 100% tieng Viet +
  // dong (VND) + dinh dang so kieu Viet Nam, tinh dung ca phep cong tong. Model nho can chi dan
  // TUONG MINH hon Claude (model manh hon, khong can nhan manh nhieu van hieu y).
  private static final String QA_SYSTEM_PROMPT =
      "Ban la tro ly bao cao cho phan mem quan ly ban hang FruitHouse tai Viet Nam. BAT BUOC tra loi"
          + " 100% bang TIENG VIET (khong duoc dung tieng Anh du chi 1 tu), don vi tien te la VND"
          + " (ghi la 'đồng' hoac 'đ'), dinh dang so kieu Viet Nam (vi du 1.500.000 đ, KHONG dung"
          + " dau phay ngan cach hang nghin kieu tieng Anh). Tra loi ngan gon, chinh xac, dua HOAN"
          + " TOAN vao ket qua tool duoc cung cap - khong tu bia so lieu. Neu cau hoi khong lien"
          + " quan bao cao/kinh doanh, tra loi lich su rang ban chi ho tro cau hoi ve bao cao ban"
          + " hang.";

  private static final String EXPLAIN_SYSTEM_PROMPT =
      "Ban la chuyen gia phan tich kinh doanh cho phan mem quan ly ban hang FruitHouse tai Viet Nam."
          + " Nguoi dung la chu cua hang, khong ranh chuyen mon tai chinh/ky thuat. BAT BUOC tra loi"
          + " 100% bang TIENG VIET (khong duoc dung tieng Anh du chi 1 tu), don vi tien te la VND"
          + " (ghi la 'đồng' hoac 'đ'), dinh dang so kieu Viet Nam (vi du 1.500.000 đ). CHI su dung"
          + " so lieu trong the <data> duoc cung cap - TUYET DOI khong tu bia them so lieu nao"
          + " khac; neu cau hoi can 1 so khong co trong <data>, noi ro 'khong co du lieu nay' thay"
          + " vi doan. Tra loi ngan gon (khong dung jargon tai chinh), uu tien 1 cau tom tat truoc,"
          + " sau do chi tiet/khuyen nghi hanh dong neu phu hop.";

  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private final String model;

  public OllamaAiProvider(
      ObjectMapper objectMapper,
      @Value("${app.ai.ollama.base-url}") String baseUrl,
      @Value("${app.ai.ollama.model}") String model,
      @Value("${app.ai.ollama.timeout-seconds:60}") int timeoutSeconds) {
    this.objectMapper = objectMapper;
    this.model = model;
    this.restClient =
        RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(
                new SimpleClientHttpRequestFactory() {
                  {
                    // Timeout DAI hon Claude (60s mac dinh, khac 30s) - model tu host tren phan
                    // cung khiem ton (khong GPU) co the cham hon dang ke so voi API dam may chuyen
                    // dung, dac biet luot dau (nap model vao bo nho neu chua "am" tu lan goi
                    // truoc).
                    setConnectTimeout((int) Duration.ofSeconds(timeoutSeconds).toMillis());
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
    ArrayNode messages = objectMapper.createArrayNode();
    messages.add(roleMessage("system", QA_SYSTEM_PROMPT));
    for (ConversationTurn turn : history) {
      messages.add(roleMessage(turn.role(), turn.content()));
    }
    messages.add(roleMessage("user", userQuestion));

    JsonNode firstResponse;
    try {
      firstResponse = callChatApi(messages, tools);
    } catch (RuntimeException ex) {
      listener.onError(ex.getMessage());
      return;
    }

    JsonNode message = firstResponse.path("message");
    JsonNode toolCalls = message.path("tool_calls");
    if (!toolCalls.isArray() || toolCalls.isEmpty()) {
      String text = extractContent(message);
      listener.onChunk(text);
      listener.onComplete(text, null);
      return;
    }

    JsonNode firstCall = toolCalls.get(0);
    String toolName = firstCall.path("function").path("name").asText();
    listener.onToolSelected(toolName);
    Map<String, Object> arguments =
        objectMapper.convertValue(firstCall.path("function").path("arguments"), Map.class);

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

    // Luot 2: gui lai assistant message (chua tool_calls) + ket qua tool THAT (role "tool", dinh
    // dang tuong thich OpenAI ma Ollama dung), STREAM tung phan van ban tong hop cuoi cung.
    messages.add(assistantMessageWithToolCall(firstCall));
    messages.add(roleMessage("tool", toJsonString(toolResult)));

    StringBuilder fullText = new StringBuilder();
    try {
      streamChatApi(messages, tools, fullText::append, listener::onChunk);
    } catch (RuntimeException ex) {
      listener.onError(ex.getMessage());
      return;
    }
    listener.onComplete(fullText.toString(), toolName);
  }

  @Override
  public String explain(String dataContextJson, String question) {
    ArrayNode messages = objectMapper.createArrayNode();
    messages.add(roleMessage("system", EXPLAIN_SYSTEM_PROMPT));
    messages.add(
        roleMessage("user", "<data>" + dataContextJson + "</data>\n\nCâu hỏi: " + question));
    JsonNode response = callChatApi(messages, List.of());
    return extractContent(response.path("message"));
  }

  private JsonNode callChatApi(ArrayNode messages, List<AiTool> tools) {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", model);
    body.set("messages", messages);
    if (!tools.isEmpty()) {
      body.set("tools", toolsToJson(tools));
    }
    body.put("stream", false);
    body.put("keep_alive", KEEP_ALIVE);

    try {
      return restClient
          .post()
          .uri("/api/chat")
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientResponseException ex) {
      log.warn(
          "Ollama API loi: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
      throw new BusinessRuleException(
          "AI_PROVIDER_ERROR", "Trợ lý AI đang gặp sự cố, vui lòng thử lại sau ít phút");
    } catch (ResourceAccessException ex) {
      log.warn("Ollama API timeout/mat ket noi: {}", ex.getMessage());
      throw new BusinessRuleException(
          "AI_PROVIDER_TIMEOUT", "Trợ lý AI phản hồi quá lâu, vui lòng thử lại sau");
    }
  }

  /**
   * Doc NDJSON (moi dong 1 doi tuong JSON hoan chinh, KHONG phai khung SSE "data: " nhu Anthropic)
   * tu Ollama ({@code "stream": true}) - moi dong co {@code message.content} (1 phan van ban) va co
   * {@code done: true} o dong cuoi cung.
   */
  private void streamChatApi(
      ArrayNode messages, List<AiTool> tools, Consumer<String> onAppend, Consumer<String> onChunk) {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", model);
    body.set("messages", messages);
    if (!tools.isEmpty()) {
      body.set("tools", toolsToJson(tools));
    }
    body.put("stream", true);
    body.put("keep_alive", KEEP_ALIVE);

    try {
      restClient
          .post()
          .uri("/api/chat")
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .exchange(
              (request, response) -> {
                try (BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                  String line;
                  while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                      continue;
                    }
                    JsonNode chunk = objectMapper.readTree(line);
                    String text = chunk.path("message").path("content").asText("");
                    if (!text.isEmpty()) {
                      onAppend.accept(text);
                      onChunk.accept(text);
                    }
                    if (chunk.path("done").asBoolean(false)) {
                      break;
                    }
                  }
                }
                return null;
              });
    } catch (RestClientException ex) {
      log.warn("Ollama API streaming loi: {}", ex.getMessage());
      throw new BusinessRuleException(
          "AI_PROVIDER_ERROR", "Trợ lý AI đang gặp sự cố, vui lòng thử lại sau ít phút");
    } catch (RuntimeException ex) {
      // BufferedReader/ObjectMapper.readTree co the nem IOException duoc boc lai thanh
      // UncheckedIOException tu lambda exchange() - vao chung 1 nhanh loi voi RestClientException.
      log.warn("Loi doc stream tu Ollama API: {}", ex.getMessage());
      throw new BusinessRuleException(
          "AI_PROVIDER_ERROR", "Trợ lý AI đang gặp sự cố, vui lòng thử lại sau ít phút");
    }
  }

  /**
   * Dinh dang tool tuong thich OpenAI function-calling ma Ollama dung ({@code type: "function"} +
   * {@code function.parameters} la JSON schema) - KHAC dinh dang {@code input_schema} rieng cua
   * Anthropic (xem ClaudeAiProvider.toolsToJson).
   */
  private ArrayNode toolsToJson(List<AiTool> tools) {
    ArrayNode array = objectMapper.createArrayNode();
    for (AiTool tool : tools) {
      ObjectNode function = objectMapper.createObjectNode();
      function.put("name", tool.name());
      function.put("description", tool.description());
      function.set("parameters", objectMapper.valueToTree(tool.parametersJsonSchema()));

      ObjectNode wrapper = objectMapper.createObjectNode();
      wrapper.put("type", "function");
      wrapper.set("function", function);
      array.add(wrapper);
    }
    return array;
  }

  private ObjectNode roleMessage(String role, String content) {
    ObjectNode message = objectMapper.createObjectNode();
    message.put("role", role);
    message.put("content", content);
    return message;
  }

  private ObjectNode assistantMessageWithToolCall(JsonNode toolCall) {
    ObjectNode message = objectMapper.createObjectNode();
    message.put("role", "assistant");
    message.put("content", "");
    ArrayNode calls = objectMapper.createArrayNode();
    calls.add(toolCall);
    message.set("tool_calls", calls);
    return message;
  }

  private String extractContent(JsonNode message) {
    String content = message.path("content").asText("");
    return content.isBlank() ? "Xin lỗi, tôi không có câu trả lời cho câu hỏi này." : content;
  }

  private String toJsonString(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (IOException ex) {
      return "{}";
    }
  }
}
