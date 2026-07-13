import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import { Bot, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { PermissionGate } from "@/components/common/PermissionGate";
import { useAiChat } from "@/lib/hooks/useAiChat";

const MAX_QUESTION_LENGTH = 500;

/** Cau hoi goi y theo trang dang xem (Prompt #12) - chi 1 vai trang chinh, KHONG lam day du moi
 * route de tranh danh sach roi rac; "/pos" khong co trong danh sach vi widget nay AN HOAN TOAN o
 * do (thu ngan dang ban hang khong can AI can thiep vao luong POS, xem check ben duoi). */
const ROUTE_SUGGESTIONS: Record<string, string> = {
  "/": "Doanh thu hôm nay thế nào?",
  "/reports": "Doanh thu tháng này so với tháng trước thế nào?",
  "/inventory": "Sản phẩm nào đang tồn thấp?",
  "/debts": "Công nợ quá hạn hiện tại là bao nhiêu?",
};

function FloatingAiButtonInner() {
  const [open, setOpen] = useState(false);
  const [question, setQuestion] = useState("");
  const location = useLocation();
  const { answer, toolInProgress, isStreaming, error, ask, clearHistory } = useAiChat();

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.ctrlKey && event.shiftKey && event.key.toLowerCase() === "a") {
        event.preventDefault();
        setOpen((prev) => !prev);
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  // Thu ngan dang ban hang (POS) khong can AI can thiep vao luong checkout - an HOAN TOAN (khong
  // render gi ca), khac voi cac trang khac chi thu gon thanh nut tron.
  if (location.pathname === "/pos") {
    return null;
  }

  const suggestion = ROUTE_SUGGESTIONS[location.pathname];

  function handleAsk(value: string) {
    const trimmed = value.trim();
    if (!trimmed || isStreaming) return;
    ask(trimmed);
    setQuestion("");
  }

  return (
    <div className="fixed bottom-6 right-6 z-40 print:hidden">
      {open ? (
        <Card className="w-96 shadow-lg">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="flex items-center gap-2 text-base">
              <Bot className="h-4 w-4" />
              Trợ lý AI
            </CardTitle>
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              onClick={() => setOpen(false)}
              title="Đóng (Ctrl+Shift+A)"
            >
              <X className="h-4 w-4" />
            </Button>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="max-h-64 overflow-y-auto rounded-md border bg-muted/30 p-2 text-sm">
              {toolInProgress && <p className="text-muted-foreground">Đang tra cứu dữ liệu...</p>}
              {error && <p className="text-destructive">{error}</p>}
              {answer && <p className="whitespace-pre-wrap">{answer}</p>}
              {!answer && !toolInProgress && !error && (
                <p className="text-muted-foreground">Hỏi tôi về doanh thu, tồn kho, công nợ...</p>
              )}
            </div>

            {suggestion && !answer && !isStreaming && (
              <button
                type="button"
                className="w-full rounded-md border border-dashed px-2 py-1.5 text-left text-xs text-muted-foreground hover:bg-accent"
                onClick={() => handleAsk(suggestion)}
              >
                {suggestion}
              </button>
            )}

            <div className="flex gap-2">
              <Input
                value={question}
                onChange={(e) => setQuestion(e.target.value.slice(0, MAX_QUESTION_LENGTH))}
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleAsk(question);
                }}
                placeholder="Nhập câu hỏi..."
                disabled={isStreaming}
                className="h-8 text-sm"
              />
              <Button
                size="sm"
                disabled={!question.trim() || isStreaming}
                onClick={() => handleAsk(question)}
              >
                Hỏi
              </Button>
            </div>

            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Thông tin do AI tổng hợp, kiểm tra lại trước khi quyết định.</span>
              <button
                type="button"
                className="shrink-0 underline hover:text-foreground"
                onClick={() => void clearHistory()}
              >
                Xoá lịch sử
              </button>
            </div>
          </CardContent>
        </Card>
      ) : (
        <Button
          size="icon"
          className="h-12 w-12 rounded-full shadow-lg"
          title="Trợ lý AI (Ctrl+Shift+A)"
          onClick={() => setOpen(true)}
        >
          <Bot className="h-5 w-5" />
        </Button>
      )}
    </div>
  );
}

/** Nut AI noi tren toan bo layout (Prompt #12) - PermissionGate an hoan toan voi nguoi khong co
 * quyen {@code ai:use}, giong quy uoc chung AiChatWidget/AiExplainButton. */
export function FloatingAiButton() {
  return (
    <PermissionGate perm="ai:use">
      <FloatingAiButtonInner />
    </PermissionGate>
  );
}
