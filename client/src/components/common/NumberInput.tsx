import { useEffect, useRef, useState } from "react";
import { cn } from "@/lib/utils";

export interface NumberInputProps {
  value: number | string | null | undefined;
  onValueChange: (value: number | null) => void;
  onBlur?: () => void;
  allowDecimal?: boolean;
  min?: number;
  disabled?: boolean;
  placeholder?: string;
  suffix?: string;
  className?: string;
  id?: string;
  title?: string;
}

function sanitize(raw: string, allowDecimal: boolean): string {
  let cleaned = allowDecimal ? raw.replace(/[^\d.]/g, "") : raw.replace(/[^\d]/g, "");
  if (allowDecimal) {
    const firstDot = cleaned.indexOf(".");
    if (firstDot !== -1) {
      cleaned = cleaned.slice(0, firstDot + 1) + cleaned.slice(firstDot + 1).replace(/\./g, "");
    }
  }
  // Bo so 0 thua o dau (vd "007" -> "7") - KHONG dong den truong hop dang go do "0" (chua co chu so
  // sau) hoac "0." (dang go phan thap phan), tranh chan giua chung khi nguoi dung dang go dang do.
  cleaned = cleaned.replace(/^0+(?=\d)/, "");
  return cleaned;
}

/**
 * O nhap so dung chung, thay the <input type="number"> tran cua trinh duyet (mui ten tang/giam de
 * gay loi go nham, khong tu lam sach gia tri) - phat hien tu bao cao that: go so hay bi thua so 0 o
 * dau/so 1 o cuoi. Dung <input type="text" inputMode="decimal"> + tu loc ky tu, chon het noi dung
 * luc focus (go so dau tien se THAY THE gia tri cu thay vi noi them vao).
 *
 * State noi bo (text) tach khoi value ben ngoai de cho phep cac trang thai go do (rong, "0.",
 * chi co dau ".") ma khong ep ve so ngay lap tuc - chi dong bo lai tu value khi KHONG dang focus
 * (tranh ghi de luc nguoi dung dang go).
 */
export function NumberInput({
  value,
  onValueChange,
  onBlur,
  allowDecimal = true,
  min,
  disabled,
  placeholder,
  suffix,
  className,
  id,
  title,
}: NumberInputProps) {
  const [text, setText] = useState(() => (value == null ? "" : String(value)));
  const focused = useRef(false);

  useEffect(() => {
    if (focused.current) return;
    setText(value == null ? "" : String(value));
  }, [value]);

  return (
    <div className="relative">
      <input
        id={id}
        type="text"
        inputMode={allowDecimal ? "decimal" : "numeric"}
        disabled={disabled}
        placeholder={placeholder}
        title={title}
        value={text}
        className={cn(
          "flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
          suffix && "pr-8",
          className,
        )}
        onFocus={(e) => {
          focused.current = true;
          e.target.select();
        }}
        onChange={(e) => {
          const cleaned = sanitize(e.target.value, allowDecimal);
          setText(cleaned);
          if (cleaned === "" || cleaned === "." || /\.$/.test(cleaned)) {
            // Dang go do (rong, hoac vua go dau "." chua co chu so sau) - CHUA bao gia tri cuoi
            // cung ra ngoai (khong goi onValueChange, ke ca voi null) - tranh lap lai loi that da
            // gap: coi rong giua chung la "0" roi xoa luon dong hang dang sua (vd gio hang POS).
            return;
          }
          const parsed = Number(cleaned);
          if (!Number.isNaN(parsed)) {
            onValueChange(parsed);
          }
        }}
        onBlur={(e) => {
          focused.current = false;
          const cleaned = e.target.value.replace(/\.$/, "");
          if (cleaned === "") {
            // Roi o luc dang go do (vd xoa trang de nhap lai nhung lo bam ra ngoai) - khoi phuc
            // lai hien thi tu gia tri ben ngoai, KHONG bao gia tri moi (giu nguyen state cu).
            setText(value == null ? "" : String(value));
            onBlur?.();
            return;
          }
          let parsed = Number(cleaned);
          if (min != null && parsed < min) {
            parsed = min;
          }
          setText(String(parsed));
          onValueChange(parsed);
          onBlur?.();
        }}
      />
      {suffix && (
        <span className="pointer-events-none absolute inset-y-0 right-3 flex items-center text-sm text-muted-foreground">
          {suffix}
        </span>
      )}
    </div>
  );
}
