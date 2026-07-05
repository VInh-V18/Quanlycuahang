## PROJECT_STATE — sau Phase 0 — 2026-07-05

### Đã chốt
- Không dùng Lombok (getter/setter/constructor viết tay, tường minh, không phụ thuộc annotation processor)
- Build tool: Maven
- Package gốc Java: `com.quanlycuahang.erp`
- Cấu trúc repo: `client/` + `server/` đặt thẳng ở root repo (không có thư mục wrapper `kiotclone/`)
- Format code Java: Spotless (google-java-format), chạy ở phase `verify`
- Spring Boot 3.3.5, Java 21, PostgreSQL 16, Redis 7, Flyway 10 (qua BOM Spring Boot)

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
│   └── PROJECT_STATE.md
├── scripts/                         # rỗng
├── .env.example
├── .gitignore
└── README.md
```

### Database
- Bảng đã có: chưa có (schema thiết kế ở Phase 3)
- Migration Flyway mới nhất: chưa có

### API đã sinh
- Chưa có (chỉ có Actuator mặc định của Spring Boot: `GET /actuator/health`, `GET /actuator/info`)

### FE đã sinh
- Chưa có (thư mục `client/` để trống, khởi tạo ở Phase 5)

### Nợ kỹ thuật / dang dở
- Chưa có Dockerfile/docker-compose.yml — xử lý ở Phase 12
- Chưa có CI (GitHub Actions) — xử lý ở Phase 12
- Chưa có test nào (unit/integration) — Phase 0 chỉ có khung ứng dụng, chưa có nghiệp vụ để test

### Kế tiếp
- Phase 1: Phân tích nghiệp vụ — ma trận phân quyền, đặc tả nghiệp vụ MUST+SHOULD, use case/sequence/activity/class/component diagram (Mermaid)
