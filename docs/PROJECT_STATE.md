## PROJECT_STATE — sau Phase 1 — 2026-07-05

### Đã chốt
- Không dùng Lombok (getter/setter/constructor viết tay, tường minh, không phụ thuộc annotation processor)
- Build tool: Maven
- Package gốc Java: `com.quanlycuahang.erp`
- Cấu trúc repo: `client/` + `server/` đặt thẳng ở root repo (không có thư mục wrapper `kiotclone/`)
- Format code Java: Spotless (google-java-format), chạy ở phase `verify`
- Spring Boot 3.3.5, Java 21, PostgreSQL 16, Redis 7, Flyway 10 (qua BOM Spring Boot)
- 6 vai trò RBAC: `owner`, `manager`, `cashier`, `sales_staff`, `warehouse_staff`, `accountant` — ma trận resource:action đầy đủ tại `docs/phase1/permission-matrix.md`
- 19 use case (10 MUST + 9 SHOULD) đặc tả đầy đủ luồng chính/phụ/ngoại lệ/quy tắc, đối chiếu khớp mọi quy tắc B4 (giá vốn, 7 bước tính tiền POS, state machine, chống oversell, edge case 1–7)
- Mermaid không có cú pháp `usecaseDiagram`/`deploymentDiagram` chuẩn UML → dùng `flowchart` thay thế (ghi chú "cần kiểm chứng" nếu nâng cấp Mermaid sau này)

### Cấu trúc project hiện tại
```
Quanlycuahang/
├── client/                          # rỗng — khởi tạo ở Phase 5
├── server/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/quanlycuahang/erp/
│       │   │   └── ErpApplication.java
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-local.yml
│       │       ├── application-docker.yml
│       │       ├── application-prod.yml
│       │       └── db/migration/    # rỗng — migration đầu tiên ở Phase 3
│       └── test/java/com/quanlycuahang/erp/   # rỗng
├── docker/                          # rỗng — cấu hình ở Phase 12
├── docs/
│   ├── conventions.md
│   ├── PROJECT_STATE.md
│   └── phase1/
│       ├── permission-matrix.md
│       ├── business-specs-must.md
│       ├── business-specs-should.md
│       ├── use-case-diagram.md
│       ├── sequence-diagrams.md
│       ├── activity-diagrams.md
│       ├── class-diagram.md
│       └── component-deployment-diagram.md
├── scripts/                         # rỗng
├── .env.example
├── .gitignore
└── README.md
```

### Database
- Bảng đã có: chưa có — nhưng đã xác định đủ 7 nhóm / ~30 bảng (class diagram domain, `docs/phase1/class-diagram.md`), thiết kế schema chi tiết + Flyway migration ở Phase 3
- Migration Flyway mới nhất: chưa có

### API đã sinh
- Chưa có (chỉ có Actuator mặc định của Spring Boot: `GET /actuator/health`, `GET /actuator/info`)

### FE đã sinh
- Chưa có (thư mục `client/` để trống, khởi tạo ở Phase 5)

### Nợ kỹ thuật / dang dở
- Chưa có Dockerfile/docker-compose.yml — xử lý ở Phase 12
- Chưa có CI (GitHub Actions) — xử lý ở Phase 12
- Chưa có test nào (unit/integration) — chưa có nghiệp vụ code để test, Phase 11 sẽ viết đầy đủ
- Class diagram domain chưa gồm `stock_transfers` (đa chi nhánh + chuyển kho là COULD, chưa thiết kế chi tiết) — bổ sung nếu triển khai COULD

### Tự đánh giá Phase 1
- **Mạnh**: mọi quy tắc B4 (giá vốn bình quân, 7 bước tính tiền, state machine, chống oversell 2 lớp, 7 edge case) đều xuất hiện nhất quán trong đặc tả UC-04/05/12/13; ma trận phân quyền là nguồn seed duy nhất, không tạo nhánh code đặc biệt cho `owner`.
- **Thiếu**: chưa có đặc tả chi tiết cho `stock_transfers` (chuyển kho đa chi nhánh — COULD, không bắt buộc MVP).
- **Rủi ro**: Mermaid dùng `flowchart` thay cho `usecaseDiagram`/`deploymentDiagram` chuẩn UML do bản ổn định hiện tại không hỗ trợ cú pháp UML gốc — đã ghi rõ "cần kiểm chứng" trong 2 file liên quan, không ảnh hưởng nội dung nghiệp vụ.

### Kế tiếp
- Phase 2: Kiến trúc Backend (Controller → Service → Repository → Entity), DI constructor injection, chuẩn DTO + MapStruct, Exception hierarchy, Logging, cấu trúc package chi tiết
