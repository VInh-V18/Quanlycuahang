# Quản lý cửa hàng — Frontend

React 18 + TypeScript 5 + Vite 5 + Tailwind 3 + shadcn/ui (Part C — tech stack đã chốt, xem
`docs/conventions.md`).

## Chạy dev

```bash
npm install
npm run dev
```

Mở **`http://localhost:5173`** (không dùng `127.0.0.1` — Backend chỉ whitelist CORS cho origin
`localhost:5173`, xem `docs/phase5/frontend-foundation.md`). Vite dev server tự proxy `/api/*`
sang `http://localhost:8080` (backend Spring Boot phải chạy trước, xem `server/README` hoặc
`docs/phase6/backend-foundation.md`).

## Các lệnh khác

```bash
npm run build       # tsc -b && vite build
npm test            # Vitest
npx eslint . --ext ts,tsx
```

## Cấu trúc

Xem `docs/phase5/frontend-foundation.md` để biết chi tiết kiến trúc (layout, routing, state
management, component nền) và các quyết định kỹ thuật đã ghi nhận.
