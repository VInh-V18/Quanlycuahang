import { useCallback, useEffect, useRef, useState } from "react";
import { askAiStreaming, clearAiSession } from "@/lib/api/ai";

export interface UseAiChatResult {
  answer: string;
  /** Ten tool AI dang tra cuu (vd "get_revenue_summary"), null khi khong o giai doan cho tool. */
  toolInProgress: string | null;
  isStreaming: boolean;
  error: string | null;
  ask: (question: string) => void;
  clearHistory: () => Promise<void>;
}

/** Hook dung chung cho widget "Tro ly AI" (Prompt #12) - dung o 2 noi (Card tren Dashboard +
 * FloatingAiButton), tach logic streaming/state ra khoi UI de khong lap lai. Tu huy AbortController
 * cua lan hoi TRUOC neu nguoi dung hoi cau moi truoc khi cau cu tra loi xong, va huy khi component
 * unmount (tranh cap nhat state tren component da bi go). */
export function useAiChat(): UseAiChatResult {
  const [answer, setAnswer] = useState("");
  const [toolInProgress, setToolInProgress] = useState<string | null>(null);
  const [isStreaming, setIsStreaming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    return () => abortControllerRef.current?.abort();
  }, []);

  const ask = useCallback((question: string) => {
    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;

    setAnswer("");
    setError(null);
    setToolInProgress(null);
    setIsStreaming(true);

    void askAiStreaming(
      question,
      {
        onToolSelected: (toolName) => setToolInProgress(toolName),
        onChunk: (text) => {
          setToolInProgress(null);
          setAnswer((prev) => prev + text);
        },
        onDone: () => setIsStreaming(false),
        onError: (message) => {
          setError(message);
          setIsStreaming(false);
        },
      },
      controller.signal,
    );
  }, []);

  const clearHistory = useCallback(async () => {
    await clearAiSession();
    setAnswer("");
    setError(null);
    setToolInProgress(null);
  }, []);

  return { answer, toolInProgress, isStreaming, error, ask, clearHistory };
}
