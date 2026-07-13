import { useEffect, useState, type ReactNode } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";

/**
 * Dialog xac nhan hanh dong KHONG THE HOAN TAC bang cach go dung 1 chuoi cho truoc (chuan "type to
 * confirm") - dung chung cho xoa vinh vien tenant (go ten cua hang) va xoa vinh vien tai khoan
 * (go username), 2 noi giong het co che nen tach rieng thay vi lap lai state input + so khop.
 */
export function TypeToConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmWord,
  confirmLabel,
  pending,
  onConfirm,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: ReactNode;
  confirmWord: string;
  confirmLabel: string;
  pending: boolean;
  onConfirm: () => void;
}) {
  const [typed, setTyped] = useState("");

  useEffect(() => {
    if (!open) setTyped("");
  }, [open]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="text-destructive">{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <div className="space-y-1.5">
          <label className="text-sm font-medium">
            Gõ chính xác <span className="font-mono font-semibold">{confirmWord}</span> để xác nhận
          </label>
          <Input
            value={typed}
            onChange={(e) => setTyped(e.target.value)}
            autoComplete="off"
            autoFocus
          />
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Huỷ
          </Button>
          <Button variant="destructive" disabled={typed !== confirmWord || pending} onClick={onConfirm}>
            {pending ? "Đang xóa..." : confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
