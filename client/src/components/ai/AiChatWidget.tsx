import { useState } from "react";
import { Bot, Send, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { PermissionGate } from "@/components/common/PermissionGate";
import { useAiChat } from "@/lib/hooks/useAiChat";

const MAX_QUESTION_LENGTH = 500;

const TOOL_LABELS: Record<string, string> = {
  get_revenue_summary: "Đang tra cứu doanh thu...",
  get_top_products: "Đang tra cứu sản phẩm bán chạy...",
  get_low_stock_products: "Đang tra cứu tồn kho...",
  get_debt_aging: "Đang tra cứu công nợ...",
};

function AiChatWidgetInner() {
  const [question, setQuestion] = useState("");
  const { answer, toolInProgress, isStreaming, error, ask, clearHistory } = useAiChat();

  function handleAsk() {
    const trimmed = question.trim();
    if (!trimmed || isStreaming) return;
    ask(trimmed);
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between gap-2">
          <div>
            <CardTitle className="flex items-center gap-2">
              <Bot className="h-5 w-5" />
              Trợ lý AI
            </CardTitle>
            <CardDescription>
              Hỏi nhanh về doanh thu, sản phẩm bán chạy, tồn kho thấp, công nợ... bằng tiếng Việt.
            </CardDescription>
          </div>
          <Button
            variant="ghost"
            size="sm"
            title="Xoá lịch sử hội thoại"
            onClick={() => {
              setQuestion("");
              void clearHistory();
            }}
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex gap-2">
          <Input
            value={question}
            onChange={(e) => setQuestion(e.target.value.slice(0, MAX_QUESTION_LENGTH))}
            onKeyDown={(e) => {
              if (e.key === "Enter") handleAsk();
            }}
            placeholder="VD: Doanh thu tuần này so với tuần trước thế nào?"
            disabled={isStreaming}
          />
          <Button onClick={handleAsk} disabled={!question.trim() || isStreaming}>
            <Send className="h-4 w-4" />
            {isStreaming ? "Đang hỏi..." : "Hỏi"}
          </Button>
        </div>

        {toolInProgress && (
          <p className="text-sm text-muted-foreground">
            {TOOL_LABELS[toolInProgress] ?? "Đang tra cứu dữ liệu..."}
          </p>
        )}

        {error && <p className="text-sm text-destructive">{error}</p>}

        {answer && (
          <div className="whitespace-pre-wrap rounded-md border bg-muted/30 p-3 text-sm">
            {answer}
          </div>
        )}

        <p className="text-xs text-muted-foreground">
          Thông tin do AI tổng hợp, kiểm tra lại trước khi quyết định.
        </p>
      </CardContent>
    </Card>
  );
}

/** Widget hoi dap AI (Prompt #11, streaming + lich su hoi thoai o Prompt #12) tren Dashboard - chi
 * hien voi quyen {@code ai:use} (owner/manager, xem V31), an hoan toan (khong render gi, khong goi
 * API) voi nhan vien thuong - dung PermissionGate giong quy uoc chung cua toan he thong. */
export function AiChatWidget() {
  return (
    <PermissionGate perm="ai:use">
      <AiChatWidgetInner />
    </PermissionGate>
  );
}
