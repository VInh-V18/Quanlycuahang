package com.quanlycuahang.erp.ai.provider;

import java.util.List;
import java.util.Map;

/**
 * Hop dong tich hop nha cung cap AI (Prompt #11, streaming + hoi thoai nhieu luot o Prompt #12) -
 * cung khuon mau voi {@link com.quanlycuahang.erp.operation.invoice.EInvoiceProvider}: 1 interface
 * + 1 implementation that dau tien (o day la {@link ClaudeAiProvider}), de sau nay doi/them nha
 * cung cap khac (OpenAI, Gemini...) khong dung cham gi den {@link
 * com.quanlycuahang.erp.ai.service.AiAssistantService}.
 *
 * <p><b>An toan cot loi (Text→API, KHONG phai Text→SQL)</b>: {@link #askStreaming} KHONG bao gio de
 * AI tu sinh cau lenh (SQL hay bat ky gi) - AI CHI duoc chon 1 trong danh sach {@code tools} co san
 * (moi tool la 1 ham Java cu the, tham so co kieu ro rang), va {@code toolExecutor} (Backend, KHONG
 * phai AI) la noi THAT SU chay tool do. Ke ca prompt injection thanh cong khien AI "muon" lam gi
 * khac thuong, no van bi gioi han trong dung danh sach tool duoc cung cap.
 *
 * <p><b>Prompt #12 - vi sao co 2 phuong thuc rieng ({@code askStreaming}/{@code explain}) thay vi 1
 * phuong thuc chung</b>: 2 tinh nang co hinh dang khac han nhau - hoi dap co tool-calling + lich su
 * hoi thoai + stream tung phan (dung cho widget chat), con giai thich bao cao la 1 lan goi don,
 * KHONG tool, KHONG lich su, dua HOAN TOAN vao 1 khoi du lieu client tu gui len (dung cho nut "AI
 * giai thich" tren ReportsPage) - go chung vao 1 phuong thuc se bat ca 2 phia phai gia lap tham so
 * khong dung voi minh (vd tools=null, history=null) - 2 phuong thuc rieng phan anh dung 2 nhu cau
 * khac nhau.
 */
public interface AiProvider {

  /**
   * Hoi dap co tool-calling, streaming tung phan van ban cau tra loi cuoi cung qua {@code listener}
   * (luot 1 - AI chon tool - KHONG stream vi chi la 1 quyet dinh ngan; luot 2 - AI tong hop cau tra
   * loi tu ket qua tool that su chay - MOI stream, vi day la phan van ban dai nguoi dung thuc su
   * doc).
   *
   * @param userQuestion cau hoi tieng Viet cua nguoi dung (vd "doanh thu tuan nay so voi tuan
   *     truoc?")
   * @param history lich su hoi thoai gan day (toi da 10 luot, xem AiConversationMemoryService) -
   *     rong neu phien moi/chua co lich su
   * @param tools danh sach cong cu AI duoc phep chon (ten + mo ta + schema tham so)
   * @param toolExecutor Backend thuc thi tool AI chon (KHONG bao gio la AI tu thuc thi)
   * @param listener nhan tung phan van ban + su kien tool/hoan tat/loi
   */
  void askStreaming(
      String userQuestion,
      List<ConversationTurn> history,
      List<AiTool> tools,
      AiToolExecutor toolExecutor,
      AiStreamListener listener);

  /**
   * Giai thich 1 khoi du lieu bao cao (JSON, CHI so lieu tong hop client da tu tinh san - KHONG
   * phai toan bo dong du lieu tho) bang tieng Viet - 1 lan goi, KHONG tool-calling, KHONG lich su
   * hoi thoai, dung model tang "phan tich phuc tap" (xem {@code app.ai.provider.advanced-model}).
   * System prompt rang buoc CHI dung so trong {@code dataContextJson}, khong tu bia.
   *
   * @param dataContextJson du lieu tong hop dang JSON (vd {"doanhThuThang6": 12000000, ...})
   * @param question cau hoi/yeu cau giai thich (vd "Giai thich xu huong doanh thu nay")
   * @return cau tra loi tieng Viet, KHONG stream (phan hoi ngan, khong can hieu ung go tung chu)
   */
  String explain(String dataContextJson, String question);

  /** 1 luot hoi thoai da qua (nguoi dung hoac tro ly) - dung de gui lai lam ngu canh cho AI. */
  record ConversationTurn(String role, String content) {}

  /** 1 cong cu AI duoc phep chon - Backend tu dinh nghia san, AI khong the "sang tao" tool moi. */
  record AiTool(String name, String description, Map<String, Object> parametersJsonSchema) {}

  /** Chay 1 tool CU THE (Backend, khong phai AI) va tra ve ket qua dang JSON-serializable. */
  @FunctionalInterface
  interface AiToolExecutor {
    Object execute(String toolName, Map<String, Object> arguments);
  }

  /**
   * Nhan su kien tu {@link #askStreaming} - goi tren THREAD NEN (khong phai thread request goc, xem
   * AiAssistantController dung {@code SseEmitter} + virtual thread), implementation PHAI tu chiu
   * trach nhiem thread-safety neu tich luy trang thai (vd StringBuilder khong dung chung giua nhieu
   * request dong thoi).
   */
  interface AiStreamListener {

    /** Goi 1 lan duy nhat NGAY SAU luot 1 (AI da chon xong tool, TRUOC khi tool that su chay). */
    void onToolSelected(String toolName);

    /** Goi nhieu lan, moi lan 1 phan van ban cau tra loi cuoi cung (luot 2) vua nhan duoc. */
    void onChunk(String textChunk);

    /**
     * Goi 1 lan duy nhat khi hoan tat - {@code fullText} la toan bo van ban da ghep (dung de ghi
     * audit log + luu lich su hoi thoai, KHONG can FE tu ghep lai tu cac chunk).
     */
    void onComplete(String fullText, String toolUsed);

    /** Goi khi co loi (API loi, timeout...) - KHONG goi onComplete sau do. */
    void onError(String errorMessage);
  }
}
