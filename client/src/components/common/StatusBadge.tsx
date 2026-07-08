import { cn } from "@/lib/utils";
import { orderStatusMeta } from "@/lib/orderStatus";

const TONE_CLASSES = {
  success: "bg-success/10 text-success",
  warning: "bg-warning/10 text-warning",
  destructive: "bg-destructive/10 text-destructive",
  info: "bg-info/10 text-info",
  muted: "bg-muted-foreground/10 text-muted-foreground",
} as const;

const DOT_CLASSES = {
  success: "bg-success",
  warning: "bg-warning",
  destructive: "bg-destructive",
  info: "bg-info",
  muted: "bg-muted-foreground",
} as const;

export function StatusBadge({ status }: { status: string }) {
  const meta = orderStatusMeta(status);
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold",
        TONE_CLASSES[meta.tone],
      )}
    >
      <span className={cn("h-1.5 w-1.5 rounded-full", DOT_CLASSES[meta.tone])} />
      {meta.label}
    </span>
  );
}
