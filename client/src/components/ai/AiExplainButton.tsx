import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Bot, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { PermissionGate } from "@/components/common/PermissionGate";
import { explainReport } from "@/lib/api/ai";
import { getApiErrorMessage } from "@/lib/http/errors";

const DEFAULT_QUESTION = "Hãy giải thích số liệu này và đưa ra khuyến nghị ngắn gọn.";

interface AiExplainButtonProps {
  /** So lieu TONG HOP (khong phai toan bo dong du lieu tho) - vd {"doanhThuThang6": 12000000,
   * "soDon": 320} - xem Javadoc AiExplainRequest phia Backend ve ly do gioi han nay. */
  dataContext: Record<string, unknown>;
  question?: string;
}

/** Nut "AI giải thích" tren ReportsPage (Prompt #12) - goi rieng 1 lan khi mo popover (khong tu goi
 * lai khi so lieu doi trong luc popover dang dong, tranh ton chi phi AI khong can thiet) - nguoi
 * dung tu bam "Phân tích lại" trong popover neu muon cap nhat theo so lieu moi nhat. */
export function AiExplainButton({ dataContext, question = DEFAULT_QUESTION }: AiExplainButtonProps) {
  const [open, setOpen] = useState(false);
  const explainMutation = useMutation({
    mutationFn: () => explainReport(dataContext, question),
  });

  return (
    <PermissionGate perm="ai:use">
      <Popover
        open={open}
        onOpenChange={(next) => {
          setOpen(next);
          if (next && !explainMutation.data && !explainMutation.isPending) {
            explainMutation.mutate();
          }
        }}
      >
        <PopoverTrigger asChild>
          <Button variant="outline" size="sm">
            <Bot className="h-4 w-4" />
            AI giải thích
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-80">
          <div className="space-y-2">
            {explainMutation.isPending && (
              <p className="text-sm text-muted-foreground">Đang phân tích...</p>
            )}
            {explainMutation.isError && (
              <p className="text-sm text-destructive">
                {getApiErrorMessage(explainMutation.error, "Không thể lấy giải thích từ AI")}
              </p>
            )}
            {explainMutation.data && (
              <p className="whitespace-pre-wrap text-sm">{explainMutation.data}</p>
            )}
            <Button
              variant="ghost"
              size="sm"
              className="w-full"
              disabled={explainMutation.isPending}
              onClick={() => explainMutation.mutate()}
            >
              <RefreshCw className="h-3.5 w-3.5" />
              Phân tích lại
            </Button>
            <p className="text-xs text-muted-foreground">
              Thông tin do AI tổng hợp, kiểm tra lại trước khi quyết định.
            </p>
          </div>
        </PopoverContent>
      </Popover>
    </PermissionGate>
  );
}
