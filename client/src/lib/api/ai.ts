import { apiClient } from "@/lib/http/apiClient";
import { store } from "@/store";
import type { ApiSuccess } from "@/types/api";

export interface PurchaseSuggestion {
  productId: number;
  productName: string;
  sku: string;
  currentStock: number;
  minStock: number;
  suggestedQty: number;
  /** Chi co gia tri o "Gợi ý AI nâng cao" (Prompt #12, qua ml-service) - null o "Gợi ý từ AI" don
   * gian (Prompt #11, cong thuc xac dinh khong co khai niem do tin cay). */
  confidence?: number | null;
}

export interface AiSettingsStatus {
  configured: boolean;
}

export interface AiStreamHandlers {
  /** Goi 1 lan duy nhat NGAY SAU khi AI chon xong tool, TRUOC khi thay chunk van ban dau tien -
   * FE dung de hien "Dang tra cuu du lieu..." trong luc cho luot 2 (tong hop cau tra loi) stream ve. */
  onToolSelected?: (toolName: string) => void;
  /** Goi nhieu lan, moi lan 1 phan van ban cau tra loi cuoi cung vua nhan duoc - noi len chuoi
   * hien tai de co hieu ung go tung chu (khong can doi ca cau tra loi xong). */
  onChunk: (text: string) => void;
  /** Goi 1 lan duy nhat khi stream ket thuc thanh cong. */
  onDone: () => void;
  /** Goi khi co loi (mat ket noi, Backend tra ve loi, AI chua cau hinh...) - KHONG goi onDone sau do. */
  onError: (message: string) => void;
}

/** Hoi dap bao cao bang tieng Viet, STREAMING tung phan (Prompt #12 - truoc day Prompt #11 doi ca
 * cau tra loi xong moi hien) - dung {@code fetch} + {@code ReadableStream} THAY VI axios/EventSource:
 * EventSource goc cua trinh duyet chi ho tro GET va KHONG cho gan header tuy chinh (khong gui duoc
 * Authorization: Bearer ...), trong khi endpoint nay can POST (than yeu cau la cau hoi) + JWT - day
 * la mau chuan cho "streaming qua POST co xac thuc" ma nhieu ung dung chat hien nay dung thay
 * EventSource. Backend gui khung SSE thuc (event: tool/chunk/done/error, data: JSON) qua
 * {@code SseEmitter} - xem AiAssistantController.
 *
 * @param signal AbortSignal tuy chon de nguoi dung tu huy stream giua chung (vd doi trang) */
export async function askAiStreaming(
  question: string,
  handlers: AiStreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  const token = store.getState().auth.accessToken;
  let response: Response;
  try {
    response = await fetch("/api/v1/ai/ask", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ question }),
      signal,
    });
  } catch (err) {
    if (err instanceof DOMException && err.name === "AbortError") {
      return;
    }
    handlers.onError("Không thể kết nối tới trợ lý AI, vui lòng kiểm tra mạng và thử lại");
    return;
  }

  if (!response.ok || !response.body) {
    handlers.onError("Trợ lý AI đang gặp sự cố, vui lòng thử lại sau ít phút");
    return;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  // Neu ket noi dut giua chung (mat mang) MA server chua kip gui khung "done"/"error", vong lap
  // duoi day se ket thuc tu nhien (reader bao done=true) ma KHONG goi onDone/onError - phai co co
  // nay de goi onError nhu 1 phuong an du phong, tranh FE ket lai mai o trang thai "dang stream".
  let finished = false;
  const wrappedHandlers: AiStreamHandlers = {
    onToolSelected: handlers.onToolSelected,
    onChunk: handlers.onChunk,
    onDone: () => {
      finished = true;
      handlers.onDone();
    },
    onError: (message) => {
      finished = true;
      handlers.onError(message);
    },
  };

  for (;;) {
    const { done: streamEnded, value } = await reader.read();
    if (streamEnded) break;
    buffer += decoder.decode(value, { stream: true });

    let frameEnd: number;
    while ((frameEnd = buffer.indexOf("\n\n")) !== -1) {
      const frame = buffer.slice(0, frameEnd);
      buffer = buffer.slice(frameEnd + 2);
      handleSseFrame(frame, wrappedHandlers);
    }
  }

  if (!finished) {
    handlers.onError("Mất kết nối với trợ lý AI, vui lòng thử lại");
  }
}

function handleSseFrame(frame: string, handlers: AiStreamHandlers): void {
  let eventName = "message";
  let dataText = "";
  for (const line of frame.split("\n")) {
    if (line.startsWith("event:")) {
      eventName = line.slice("event:".length).trim();
    } else if (line.startsWith("data:")) {
      dataText += line.slice("data:".length).trim();
    }
  }
  if (!dataText) return;

  let data: Record<string, unknown>;
  try {
    data = JSON.parse(dataText);
  } catch {
    return;
  }

  switch (eventName) {
    case "tool":
      handlers.onToolSelected?.(String(data.toolUsed));
      break;
    case "chunk":
      handlers.onChunk(String(data.text));
      break;
    case "done":
      handlers.onDone();
      break;
    case "error":
      handlers.onError(typeof data.message === "string" ? data.message : "Đã xảy ra lỗi");
      break;
  }
}

/** "Xoa lich su" (Prompt #12) - xoa phien hoi thoai AI hien tai (Redis, xem
 * AiConversationMemoryService) de cau hoi tiep theo bat dau ngu canh moi hoan toan. */
export async function clearAiSession(): Promise<void> {
  await apiClient.delete("/ai/session");
}

/** "AI giai thich" tren ReportsPage (Prompt #12) - {@code dataContext} CHI duoc chua so lieu TONG
 * HOP FE da tu tinh san (vd {"doanhThuThang6": 12000000}), KHONG duoc gui toan bo dong du lieu tho. */
export async function explainReport(
  dataContext: Record<string, unknown>,
  question: string,
): Promise<string> {
  const response = await apiClient.post<ApiSuccess<{ explanation: string }>>("/ai/explain", {
    dataContext,
    question,
  });
  return response.data.data.explanation;
}

export async function getPurchaseSuggestions(branchId?: number): Promise<PurchaseSuggestion[]> {
  const response = await apiClient.get<ApiSuccess<PurchaseSuggestion[]>>(
    "/ai/purchase-suggestions",
    { params: { branchId } },
  );
  return response.data.data;
}

/** "Gợi ý AI nâng cao" (Prompt #12) - qua ml-service (IsolationForest lọc ngày bán bất thường).
 * Backend tự động rơi về thuật toán đơn giản (cùng shape response) nếu ml-service không khả dụng
 * - FE không cần tự xử lý fallback, chỉ khác ở việc `confidence` có thể null trong trường hợp đó. */
export async function getAdvancedPurchaseSuggestions(
  branchId?: number,
): Promise<PurchaseSuggestion[]> {
  const response = await apiClient.get<ApiSuccess<PurchaseSuggestion[]>>(
    "/ai/purchase-suggestions/advanced",
    { params: { branchId } },
  );
  return response.data.data;
}

export async function getAiSettingsStatus(): Promise<AiSettingsStatus> {
  const response = await apiClient.get<ApiSuccess<AiSettingsStatus>>("/ai/settings");
  return response.data.data;
}

export async function updateAiSettings(apiKey: string): Promise<void> {
  await apiClient.put("/ai/settings", { apiKey });
}

export async function disableAiSettings(): Promise<void> {
  await apiClient.delete("/ai/settings");
}
