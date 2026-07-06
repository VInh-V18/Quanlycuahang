# Phase 4.1 — Design tokens

## Nguyên tắc chọn màu

Ứng dụng ERP vận hành nhiều giờ liên tục (thu ngân đứng POS cả ca) → ưu
tiên **độ tương phản cao, ít mỏi mắt**, màu ngữ nghĩa nhất quán (xanh lá =
tốt/hoàn tất, đỏ = lỗi/hết hàng, vàng = cảnh báo) hơn là màu trang trí.
Dùng cơ chế biến CSS của shadcn/ui (`--background`, `--foreground`...) để
đổi light/dark mà không sửa component.

## Bảng màu (hex)

| Token | Light | Dark | Dùng cho |
|---|---|---|---|
| `background` | `#FFFFFF` | `#0B0F19` | Nền trang |
| `foreground` | `#0F172A` | `#E2E8F0` | Chữ chính |
| `primary` | `#2563EB` | `#3B82F6` | Nút chính, link, focus |
| `primary-foreground` | `#FFFFFF` | `#0B0F19` | Chữ trên nút primary |
| `secondary` | `#F1F5F9` | `#1E293B` | Nút phụ, nền card |
| `muted` | `#F8FAFC` | `#111827` | Nền vùng phụ, placeholder |
| `muted-foreground` | `#64748B` | `#94A3B8` | Chữ phụ, mô tả |
| `accent` | `#EFF6FF` | `#1E3A8A` | Hover, vùng nhấn nhẹ |
| `success` | `#16A34A` | `#22C55E` | Hoàn tất, còn hàng, đã thanh toán |
| `warning` | `#F59E0B` | `#FBBF24` | Sắp hết hàng, chờ duyệt |
| `destructive` | `#DC2626` | `#EF4444` | Hết hàng, lỗi, hủy đơn, xóa |
| `border` | `#E2E8F0` | `#1E293B` | Viền input, card, divider |
| `ring` | `#2563EB` | `#3B82F6` | Focus ring (accessibility) |

Tỷ lệ tương phản `foreground`/`background` và `primary-foreground`/`primary`
đều ≥ 4.5:1 (WCAG AA) ở cả 2 theme.

## Typography

- Font: **Inter** (hệ thống fallback: `ui-sans-serif, system-ui`) — hỗ trợ
  tiếng Việt có dấu đầy đủ, số liệu (tabular figures) rõ ràng cho bảng giá.
- Thang cỡ chữ (Tailwind mặc định, không tùy biến thêm):

| Cấp | Kích thước | Dùng cho |
|---|---|---|
| `text-xs` (12px) | Ghi chú, badge, timestamp |
| `text-sm` (14px) | Nội dung bảng, form label |
| `text-base` (16px) | Nội dung chính |
| `text-lg` (18px) | Tiêu đề card |
| `text-xl`–`text-2xl` (20–24px) | Tiêu đề trang |
| `text-3xl` (30px) | Số liệu nổi bật (dashboard KPI, tổng tiền POS) |

Số tiền luôn dùng `font-variant-numeric: tabular-nums` (căn cột đều nhau
trong bảng/hóa đơn).

## Spacing & Radius & Shadow

- Spacing: thang mặc định Tailwind (bội số 4px: `1`=4px, `2`=8px,
  `4`=16px, `6`=24px, `8`=32px...) — đủ linh hoạt, không cần token riêng.
- Radius: `--radius: 0.5rem` (8px) mặc định cho card/button/input;
  `rounded-full` cho avatar/badge tròn.
- Shadow: `shadow-sm` cho card thường; `shadow-md` cho dropdown/popover;
  `shadow-lg` cho modal/dialog — tăng dần theo độ "nổi" (elevation) đúng
  quy ước Radix/shadcn.

## `tailwind.config.ts` (áp dụng khi khởi tạo Frontend ở Phase 5.1)

```ts
import type { Config } from "tailwindcss";

export default {
  darkMode: ["class"],
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        success: {
          DEFAULT: "hsl(var(--success))",
          foreground: "hsl(var(--success-foreground))",
        },
        warning: {
          DEFAULT: "hsl(var(--warning))",
          foreground: "hsl(var(--warning-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui"],
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
} satisfies Config;
```

CSS variables tương ứng (`src/index.css`, khai báo ở Phase 5.1):

```css
:root {
  --background: 0 0% 100%;
  --foreground: 222 47% 11%;
  --primary: 217 91% 60%;
  --primary-foreground: 0 0% 100%;
  --secondary: 210 40% 96%;
  --secondary-foreground: 222 47% 11%;
  --muted: 210 40% 98%;
  --muted-foreground: 215 16% 47%;
  --accent: 214 100% 97%;
  --accent-foreground: 222 47% 11%;
  --success: 142 71% 35%;
  --success-foreground: 0 0% 100%;
  --warning: 38 92% 50%;
  --warning-foreground: 0 0% 100%;
  --destructive: 0 72% 51%;
  --destructive-foreground: 0 0% 100%;
  --border: 214 32% 91%;
  --input: 214 32% 91%;
  --ring: 217 91% 60%;
  --radius: 0.5rem;
}

.dark {
  --background: 222 39% 8%;
  --foreground: 213 27% 88%;
  --primary: 217 91% 60%;
  --primary-foreground: 222 39% 8%;
  --secondary: 217 33% 17%;
  --secondary-foreground: 213 27% 88%;
  --muted: 220 26% 10%;
  --muted-foreground: 215 20% 65%;
  --accent: 217 60% 20%;
  --accent-foreground: 213 27% 88%;
  --success: 142 69% 48%;
  --success-foreground: 222 39% 8%;
  --warning: 43 96% 56%;
  --warning-foreground: 222 39% 8%;
  --destructive: 0 84% 60%;
  --destructive-foreground: 222 39% 8%;
  --border: 217 33% 17%;
  --input: 217 33% 17%;
  --ring: 217 91% 60%;
}
```
