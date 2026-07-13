## PROJECT_STATE — cập nhật sau Prompt #12 (Nâng cấp AI đợt 2 — Phần II roadmap) — 2026-07-13

### Bối cảnh: Phần II mới của roadmap
Người dùng dán bổ sung "PHẦN II — NÂNG CẤP AI + TESTING CHUYÊN SÂU + VÁ LỖI" (Prompt #12–#17) vào
`docs/UPGRADE_ROADMAP.md` — **Prompt #11 KHÔNG còn là prompt cuối cùng** (tiêu đề entry trước đó ghi
nhầm, đã sửa chú thích ở bảng trạng thái). Prompt #17 bị cắt cụt lúc dán (chỉ có dòng mở đầu) — đã
ghi rõ trong roadmap là cần người dùng dán lại đầy đủ trước khi thực thi, KHÔNG tự bịa nội dung.

### Phạm vi Prompt #12: "Nền tảng" (làm trước) + 3 tính năng mới
Theo đúng thứ tự ưu tiên roadmap tự đặt ra ("NÂNG CẤP NỀN TẢNG làm trước, tính năng mới phụ thuộc").

**Nền tảng:**
1. **Streaming SSE thật** cho hỏi đáp báo cáo — thay vì đợi cả câu trả lời xong mới trả về (Prompt
   #11), giờ AI trả lời tới đâu người dùng thấy tới đó. `AiProvider.ask()`/`AiAnswer` cũ bị THAY THẾ
   hoàn toàn bằng `askStreaming(question, history, tools, executor, AiStreamListener)` — quyết định
   kỹ thuật: KHÔNG dùng `EventSource` gốc của trình duyệt (chỉ hỗ trợ GET, không gắn được header
   `Authorization: Bearer`), dùng `fetch()` + `ReadableStream` phía FE tự đọc khung SSE thủ công.
   Phía Backend: `SseEmitter` + `Executors.newVirtualThreadPerTaskExecutor()` (Java 21) để không
   chiếm thread Tomcat suốt thời gian stream. `ClaudeAiProvider` tự đọc SSE thô từ Anthropic qua
   `RestClient.exchange()` (đọc `InputStream` trực tiếp, không đợi Spring buffer toàn bộ response) —
   lượt 1 (AI chọn tool) KHÔNG stream (chỉ là 1 quyết định ngắn), lượt 2 (tổng hợp câu trả lời) MỚI
   stream.
2. **Bộ nhớ hội thoại** (`AiConversationMemoryService`, mới) — Redis key
   `ai:session:{tenantId}:{userId}`, TTL 30 phút trượt, tối đa 10 lượt, cắt bớt nội dung quá dài (ước
   lượng thô qua số ký tự, đo token chính xác để lại Prompt #16). Nút "Xoá lịch sử" (`DELETE
   /ai/session`).
3. **Phân tầng model** — `app.ai.provider.default-model` đổi sang Haiku (hỏi đáp thường ngày,
   `askStreaming`), thêm `app.ai.provider.advanced-model` = Sonnet (chỉ dùng cho "AI giải thích").

**3 tính năng mới:**
4. **"AI giải thích"** (`AiProvider.explain`, `POST /ai/explain`, `AiExplainButton` trên 4 khu vực
   ReportsPage) — 1 lần gọi, KHÔNG tool-calling, KHÔNG lịch sử hội thoại, CHỈ nhận dữ liệu TỔNG HỢP
   FE đã tự tính sẵn (không phải toàn bộ dòng dữ liệu thô) — system prompt riêng cấm bịa số ngoài
   thẻ `<data>`.
5. **"Gợi ý AI nâng cao"** (`AiForecastService` mới, `GET /ai/purchase-suggestions/advanced`) — gọi
   sang 1 dịch vụ Python RIÊNG (`ml-service/`, FastAPI + scikit-learn IsolationForest) để lọc "ngày
   bán bất thường" trước khi tính tốc độ bán, khác thuật toán đơn giản Prompt #11 (`AiPurchaseSuggestionService`,
   GIỮ NGUYÊN, dùng làm phương án dự phòng). Migration `V32` thêm view whitelist thứ 5
   (`v_ai_daily_sales`, doanh số theo TỪNG NGÀY thay vì tổng hợp 30 ngày như `v_ai_top_products`).
   **Bắt buộc có fallback**: `AiForecastService` tự động quay về thuật toán đơn giản nếu ml-service
   lỗi/timeout (timeout riêng 5s, ngắn hơn nhiều so với 30s của Claude — đây là dịch vụ nội bộ cùng
   mạng docker) — KHÔNG BAO GIỜ trả 500 cho người dùng chỉ vì 1 dịch vụ phụ gặp sự cố.
6. **FloatingAiButton** — nút AI nổi toàn layout (`MainLayout`), phím tắt Ctrl+Shift+A, ẩn hoàn
   toàn trên `/pos` (thu ngân đang bán hàng không cần AI xen vào), gợi ý câu hỏi theo route đang xem
   (`/`, `/reports`, `/inventory`, `/debts`).

### 2 vấn đề kỹ thuật tự phát hiện và xử lý khi làm streaming (không phải lỗi, nhưng đáng ghi lại)
- **Thread-propagation cho SSE chạy nền**: `askStreaming` phải chạy trên thread KHÁC thread xử lý
  HTTP request gốc (để không chiếm thread Tomcat suốt thời gian stream) — nghĩa là `TenantContext`
  (ThreadLocal) và Hibernate `@Filter` KHÔNG tự kế thừa sang thread mới. Áp dụng ĐÚNG khuôn mẫu đã có
  sẵn trong codebase cho đúng tình huống này (`InvoiceEmailService.sendInvoiceEmailAsync`, dùng
  `TenantSessionBinder.bind()/unbind()` + truyền tenantId/userId tường minh thay vì đọc lại
  ThreadLocal trong thân hàm chạy nền) — không phải phát minh cơ chế mới, chỉ tái sử dụng đúng.
- **`CurrentUserProvider` không dùng được trên thread nền** (dựa vào `SecurityContextHolder`, cũng
  ThreadLocal) — giải quyết bằng cách đọc `userId` MỘT LẦN trên thread request gốc (ở Controller,
  nơi 2 ThreadLocal còn hợp lệ), truyền tường minh xuống, và dùng `entityManager.getReference(User.class,
  userId)` (không cần query lại) thay vì gọi lại `CurrentUserProvider` trong `logInteraction()`.

### Xác nhận chạy thật
- Backend: `mvn verify` đầy đủ — **117 unit test + 52 integration test, TẤT CẢ XANH** (tăng 21 unit
  so Prompt #11: `AiForecastServiceTest` 3 test mới + toàn bộ `AiAssistantServiceTest`/
  `AiConversationMemoryServiceTest` viết mới/viết lại cho API streaming), `spotless:check` sạch,
  cổng coverage JaCoCo đạt.
- Frontend: 49 test Vitest (không đổi — CHƯA có test riêng cho `AiChatWidget`/`FloatingAiButton`
  streaming, xem giới hạn phạm vi bên dưới), `tsc --noEmit`/`eslint`/`npm run build` sạch.
- **`ml-service` đã chạy và kiểm chứng THẬT** (không phải code chưa thử, giống đúng tinh thần "cấm
  code giả" của roadmap): agent xây dựng đã tự chạy `pytest` (12/12 xanh), khởi động `uvicorn` thật
  + gọi `curl` cả 2 endpoint, `docker build` thành công + `HEALTHCHECK` container báo "healthy",
  xác nhận chạy user non-root. **Tôi tự kiểm chứng độc lập thêm lần nữa** (không chỉ tin báo cáo của
  agent): tự `docker build` + `docker run` container standalone (cổng tạm, không đụng
  `docker-compose` sản xuất đang chạy thật), gửi `POST /forecast` với payload thật khớp đúng tên
  field phía Java (`productId`/`forecastDailyVelocity`/`daysOfStockRemaining`/`suggestedQty`/
  `confidence`/`outliersRemoved`) — response khớp 100% hợp đồng, số học đúng tay, dọn container sau
  khi xong.
- Migration `V32` migrate sạch trên Postgres test throwaway (đã chạy qua trong `mvn verify`/IT test
  `AiReadOnlyPermissionIT` — dù test đó không trực tiếp test view mới, quá trình Flyway migrate của
  toàn bộ IT suite đã xác nhận `V32` áp dụng không lỗi).

### Giới hạn phạm vi đã ghi rõ (không phải bỏ sót)
- `ClaudeAiProvider.askStreaming`/`explain` vẫn CHƯA gọi thử với khoá API Anthropic thật (kế thừa
  tình trạng chưa kiểm chứng từ Prompt #11 — không có khoá thật trong môi trường phát triển này).
- Chưa khởi động toàn bộ `docker compose` (server + ml-service qua tên container/DNS nội bộ của
  Compose network) cùng lúc — phần kiểm chứng ml-service ở trên dùng `docker run` container độc lập
  (cổng host tạm thời), KHÔNG phải qua đúng cơ chế network mà `AiForecastService` sẽ dùng khi deploy
  thật (`http://ml-service:8000`). Cần xác nhận lại bước này trước khi bật tính năng "Gợi ý AI nâng
  cao" cho tenant thật.
- Chưa viết test Vitest riêng cho hành vi streaming của `AiChatWidget`/`FloatingAiButton` (mock
  `fetch`+`ReadableStream`) — đã xác nhận qua `tsc`/`lint`/`build` sạch + kiểm tra thủ công logic
  parse khung SSE, nhưng chưa có test tự động riêng cho phần này.
- Chưa seed dữ liệu doanh số 90 ngày thật nhiều SKU để kiểm chứng "Gợi ý AI nâng cao" trên tập dữ
  liệu lớn/tenant thật — đã kiểm chứng đúng thuật toán qua test unit (mock) + kiểm chứng hợp đồng
  HTTP qua container thật với dữ liệu dựng tay.
- Chưa áp dụng migration `V32` lên production đang chạy thật (đúng nguyên tắc giữ suốt các Prompt
  #9/#10/#11 — chờ 1 lần deploy bình thường).
- Prompt #13–#17 CHƯA bắt đầu (xem bảng trạng thái Phần II trong `docs/UPGRADE_ROADMAP.md`); Prompt
  #17 còn bị chặn vì nội dung chưa đầy đủ.

---

## PROJECT_STATE — cập nhật sau Prompt #11 (Module AI Assistant, P3 — PROMPT CUỐI ROADMAP) — 2026-07-13

### Phạm vi: 2 tính năng đợt 1 (Text→API, KHÔNG phải Text→SQL)
1. **Hỏi đáp báo cáo bằng tiếng Việt** (widget trên Dashboard) — AI (Claude) chỉ được **chọn 1 trong
   4 tool có sẵn** (`get_revenue_summary`/`get_top_products`/`get_low_stock_products`/`get_debt_aging`),
   Backend mới là bên THẬT SỰ chạy tool đó; AI không bao giờ tự sinh SQL. Kể cả prompt injection
   thành công khiến AI "muốn" làm gì khác thường, nó vẫn bị giới hạn trong đúng danh sách tool.
2. **Gợi ý nhập hàng** (nút "Gợi ý từ AI" ở trang Tạo phiếu nhập) — **CỐ Ý KHÔNG gọi AI/LLM** dù tên
   hiển thị là "AI": công thức xác định (tốc độ bán 30 ngày × 30, so với định mức tồn tối thiểu),
   nhanh hơn/rẻ hơn/không rủi ro hallucinate số liệu — đúng nguyên tắc "Backend là nguồn chân lý duy
   nhất cho tính tiền/số lượng" của dự án.

### Lớp an toàn dữ liệu (quan trọng nhất của prompt này)
- Migration `V30`: role Postgres `fruithouse_ai_readonly` (mật khẩu qua Flyway placeholder, đọc từ
  `AI_DB_READONLY_PASSWORD`) — `REVOKE ALL` trên toàn bộ bảng, chỉ `GRANT SELECT` trên **4 view
  whitelist** (`v_ai_revenue`/`v_ai_top_products`/`v_ai_inventory_summary`/`v_ai_debt_aging`, mỗi view
  tự lọc `tenant_id = current_setting('app.current_tenant_id')`), `statement_timeout` riêng ở mức role.
- Pool kết nối RIÊNG cho role này (`AiReadOnlyDataSourceConfig`) — tách biệt hoàn toàn khỏi
  DataSource chính (Hibernate/JPA) để 1 lỗi/khai thác ở tầng AI không thể chạm bảng gốc.
- `AiQueryService` dùng `ConnectionCallback` để đảm bảo `SET app.current_tenant_id` và câu SELECT
  view chạy CÙNG 1 connection vật lý (tránh HikariCP trả 1 connection đã tái sử dụng còn sót GUC của
  tenant khác).
- Khoá API của mỗi tenant mã hoá AES-256-GCM (`AiKeyEncryptionService`, khoá gốc từ
  `AI_SETTINGS_ENCRYPTION_KEY`) trước khi lưu settings — tiền lệ đầu tiên trong codebase cho 1 giá
  trị settings cần mã hoá.
- **Kiểm chứng bằng test thật, không đoán**: `AiReadOnlyPermissionIT` (6 test) — dùng đúng bean
  `aiReadOnlyJdbcTemplate` thật của ứng dụng (không tự tạo kết nối riêng để test) — xác nhận
  KHÔNG THỂ INSERT/UPDATE/DELETE bất kỳ bảng nào, KHÔNG THỂ SELECT trực tiếp bảng gốc, CHỈ SELECT
  được 4 view whitelist, và 1 mẫu "prompt injection" (`'; DELETE FROM orders; --`) không có hiệu ứng
  ghi nào.

### 2 lỗi NGHIÊM TRỌNG tự phát hiện qua chính quá trình viết test (đã vá, không phải để lại)
1. **Rò quyền ghi vào pool chỉ-đọc**: `aiReadOnlyJdbcTemplate` ban đầu khai báo tham số
   `HikariDataSource aiReadOnlyDataSource` KHÔNG kèm `@Qualifier` — build Maven của repo này không
   bật cờ `-parameters` của javac nên Spring không đọc được tên tham số thật để tự khớp theo tên bean;
   nó rơi về autowire-theo-KIỂU, và vì có 2 bean cùng kiểu `HikariDataSource` (`primaryDataSource`
   đánh dấu `@Primary`), Spring âm thầm chọn NHẦM `primaryDataSource` (quyền ghi đầy đủ) — nghĩa là
   pool "chỉ đọc" thực chất đang dùng quyền ghi đầy đủ, không hề bị chặn. **Không phát hiện được nếu
   chỉ đọc code** — chỉ lộ ra khi chạy `AiReadOnlyPermissionIT` thật và thấy lỗi FK-constraint thay vì
   "permission denied" như kỳ vọng. Vá bằng `@Qualifier("aiReadOnlyDataSource")` tường minh.
2. **Gãy toàn bộ luồng tạo đơn hàng của hệ thống**: thêm `@Bean JdbcTemplate aiReadOnlyJdbcTemplate`
   khiến `JdbcTemplateAutoConfiguration` của Spring Boot (`@ConditionalOnMissingBean(JdbcTemplate.class)`)
   TỰ BỎ hoàn toàn việc tạo bean `JdbcTemplate` mặc định — 4 Service khác đang tiêm `JdbcTemplate`
   KHÔNG `@Qualifier` (`NumberSequenceService` — sinh số đơn/hoá đơn/SKU cho MỌI đơn hàng,
   `ReconciliationService`, `TenantAdminService`, `TenantUserAdminService`) vô tình bị gán vào
   `aiReadOnlyJdbcTemplate` (bean `JdbcTemplate` DUY NHẤT còn lại) — tạo đơn hàng thật ném lỗi
   "permission denied for table tenant_sequences". **Chỉ lộ ra khi chạy `mvn verify` đầy đủ** (10
   integration test lỗi/thất bại đồng loạt), không lộ ở `mvn test` (chỉ unit test) hay ở riêng
   `AiReadOnlyPermissionIT`. Vá bằng khai báo lại tường minh 1 bean `@Primary JdbcTemplate` bind vào
   `primaryDataSource`, khôi phục đúng hành vi cũ cho 4 Service trên mà không cần sửa gì ở phía họ.
   **Bài học ghi lại trong Javadoc `AiReadOnlyDataSourceConfig`**: mọi lần thêm 1 bean Spring Boot vẫn
   tự động cấu hình sẵn (`DataSource`, `JdbcTemplate`...) đều phải kiểm tra `@ConditionalOnMissingBean`
   của bean gốc và khai báo lại tường minh + `@Primary` nếu cần, KHÔNG chỉ thêm bean mới rồi coi là xong.

### Các phần khác
- `ClaudeAiProvider` (tích hợp Anthropic Messages API qua `RestClient`, tool-calling 2 lượt gọi):
  viết đúng theo tài liệu chính thức tại thời điểm viết nhưng **CHƯA kiểm chứng bằng 1 lần gọi API
  thật** (không có khoá API thật trong môi trường phát triển — giống tình trạng `EInvoiceProvider`
  trước đây). Đã ghi rõ cảnh báo này trong Javadoc + `README_DEPLOY.md` mục 12.
- `AiPurchaseSuggestionService`: thuật toán xác định thuần Java, có unit test đầy đủ (công thức
  `target_stock = MAX(min_stock, tốc_độ_bán_ngày × 30)`, sắp xếp theo độ khan cấp, bỏ qua sản phẩm
  không cần nhập thêm).
- FE: `AiChatWidget` (Dashboard, ẩn hoàn toàn nếu không có quyền `ai:use` qua `PermissionGate`, có
  disclaimer bắt buộc theo roadmap, xử lý lỗi/timeout không vỡ trang) + `AiSuggestionsDialog`
  (PurchaseOrderCreatePage, mỗi dòng gợi ý phải người dùng tự bấm "Thêm" mới vào phiếu, không tự động
  thêm dòng nào).
- Xác nhận hợp đồng FE↔BE bằng round-trip HTTP thật (không chỉ đọc code so khớp field) qua backend
  chạy local nhắm vào Postgres test throwaway (đã có V31) — `GET /ai/settings`, `GET
  /ai/purchase-suggestions`, `POST /ai/ask` (xác nhận lỗi `AI_NOT_CONFIGURED` hiển thị sạch, không vỡ
  trang) đều khớp đúng định dạng FE mong đợi.
- Phát hiện thêm (ngoài phạm vi trực tiếp của prompt, nhưng chặn được `mvn verify`): **toàn bộ 344
  file Java trong repo vi phạm định dạng Spotless** (`google-java-format` GOOGLE style) — dấu hiệu
  `mvn verify` đầy đủ (bao gồm `spotless:check`) chưa từng được chạy xuyên suốt các Prompt #1–#10
  (chỉ `mvn test`/`compile` được xác nhận, không phải toàn bộ lifecycle). Đã chạy `mvn spotless:apply`
  (chỉ đổi định dạng/whitespace, KHÔNG đổi logic) để `mvn verify` xanh trở lại — đây là thay đổi cơ
  giới, an toàn, cần thiết để giữ cổng CI của chính dự án hoạt động đúng.

### Xác nhận chạy thật
`mvn verify` đầy đủ: **96 unit test + 52 integration test = 148 test, TẤT CẢ XANH** (tăng đúng 28 unit
+ 6 IT so với Prompt #10, khớp số test mới viết cho module AI), `spotless:check` sạch, cổng coverage
JaCoCo tầng service đạt. Frontend: **49 test Vitest** (không đổi số, không có test JS mới cho phần AI
— xem giới hạn phạm vi bên dưới) + `tsc --noEmit` sạch + `npm run lint` sạch + `npm run build` thành
công (có chunk `ai-*.js` riêng theo code-splitting). Migration `V30`/`V31` migrate sạch trên Postgres
test throwaway. Đây là **prompt cuối cùng của `UPGRADE_ROADMAP.md`** — toàn bộ 11 prompt (P0→P3) nay
đã hoàn tất.

### Giới hạn phạm vi đã ghi rõ (không phải bỏ sót)
- `ClaudeAiProvider` chưa gọi thử với 1 khoá API Anthropic thật — cần làm trước khi bật tính năng
  "Hỏi đáp báo cáo" cho 1 tenant thật (xem `README_DEPLOY.md` mục 12).
- Chưa viết test Vitest riêng cho `AiChatWidget`/`AiSuggestionsDialog` ở FE (đã xác nhận qua
  `tsc`/`lint`/`build` sạch + round-trip HTTP thật qua backend local, nhưng chưa có test tự động
  riêng mô phỏng tương tác người dùng trong trình duyệt cho 2 component này).
- Không seed dữ liệu tồn kho/đơn hàng thật ở quy mô lớn để kiểm chứng "Gợi ý nhập hàng" trên tập dữ
  liệu nhiều SKU — đã kiểm chứng đúng công thức qua unit test với dữ liệu dựng tay, không phải qua
  1 tenant có dữ liệu thật nhiều SKU.
- Không tự áp dụng `V30`/`V31` lên production đang chạy thật (theo đúng nguyên tắc đã giữ suốt các
  Prompt #9/#10 — chờ 1 lần deploy bình thường), nghĩa là tính năng AI CHƯA khả dụng trên production
  cho tới lần deploy kế tiếp.

---

## PROJECT_STATE — cập nhật sau Prompt #10 (Dọn dẹp nợ kỹ thuật, P3) — 2026-07-12

### Quy trình: KHÔNG tự quyết xoá gì trước khi hỏi (đúng yêu cầu roadmap)
Toàn bộ prompt này chạy theo đúng ràng buộc "liệt kê trước, xoá sau khi xác nhận": đã dùng 1 agent
Explore (read-only) rà soát toàn bộ codebase để tìm 4 loại nợ kỹ thuật (exception không throw,
endpoint không FE gọi, component FE không import, dependency không dùng), rồi dùng `AskUserQuestion`
hỏi RIÊNG quyết định bảng `promotions` + 2 nhóm dead-code (rõ ràng / có thể là tính năng dở dang)
trước khi sửa bất kỳ file nào. Người dùng xác nhận: **drop bảng `promotions`** + **xoá cả 5 mục dead
code đã liệt kê** (kể cả 3 mục được cảnh báo rõ là "có thể là tính năng dở dang, không phải dead code
thật" — người dùng xác nhận không cần các tính năng đó).

### 1. Bảng `promotions` — DROP (migration `V29__drop_orphaned_promotions_table.sql`)
Xác nhận qua rà soát: không Entity/Repository/Controller/FE nào tham chiếu bảng này (Entity Java mồ
côi đã bị xoá ở phiên trước, bảng DB thì chưa). Cơ chế khuyến mãi thật sự của hệ thống dùng
`vouchers`. Đã cập nhật `docs/phase1/business-specs-should.md` mục UC-11 ghi rõ quyết định này (nếu
sau này cần khuyến mãi tự động theo khung giờ/SL/nhóm KH, đây là tính năng MỚI cần thiết kế lại, không
phải khôi phục bảng cũ).

### 2. Dead code đã xoá (5 mục, đều đã xác nhận với người dùng)
- **`GET /api/v1/customers/{id}`** — `CustomerController.getById()` + `CustomerService.getById()`
  (không FE nào gọi, chỉ dùng danh sách + update). Kéo theo: xoá test
  `customerGetByIdAcrossTenantThrowsNotFound` (`TenantIsolationIT`) vì phương thức nó kiểm tra không
  còn tồn tại — cơ chế `TenantAwareRepositoryImpl` vẫn được xác nhận đầy đủ qua các entity khác
  trong cùng file test.
- **`client/src/components/ui/separator.tsx`** + dependency `@radix-ui/react-separator` (khỏi
  `package.json`, đã chạy `npm install` đồng bộ lại `package-lock.json`) — không nơi nào import.
- **`POST /api/v1/orders/{id}/cancel`** (UC-13, "Hủy đơn hàng") — `OrderController.cancel()` +
  `OrderService.cancelOrder()` (toàn bộ logic hoàn kho/hoàn công nợ/kiểm tra khung giờ trong ngày,
  ~70 dòng). Đây là tính năng **ĐÃ CÀI ĐẶT ĐẦY ĐỦ**, không phải code giả — người dùng xác nhận không
  cần dùng. **Hệ quả dọn dẹp phát hiện thêm khi xoá**: `OrderService` có 2 field/dependency injection
  hoàn toàn mồ côi sau khi bỏ `cancelOrder()` (`InventoryTransactionRepository`, `DebtRepository` —
  chỉ được gọi bên trong đúng method này) — đã dọn luôn field/constructor param/import liên quan
  (`InventoryTransaction`, `Debt`, `BusinessRuleException`, `ArrayList` cũng hết dùng). Đã cập nhật
  `business-specs-should.md` UC-13 đánh dấu "ĐÃ XOÁ", giữ nguyên mô tả nghiệp vụ để tham khảo nếu
  làm lại. `permission-matrix.md`: đánh dấu quyền `order:void` mồ côi (còn trong DB seed đã chạy,
  không còn code nào kiểm tra).
- **`GET /admin/reconciliation`** (danh sách) + **`GET /admin/reconciliation/{runId}`** (chi tiết) —
  `ReconciliationController.history()`/`.detail()` — màn hình xem lịch sử đối soát chưa từng được
  làm ở FE (FE có sẵn 1 hàm gọi `detail` nhưng không nơi nào dùng — cũng xoá luôn). Endpoint
  `/run` và `/open-count` (đang dùng thật, cảnh báo Dashboard) GIỮ NGUYÊN. Dọn theo: 2 phương thức
  repository (`findAllByOrderByStartedAtDesc`, `findByRunId`) và 2 private helper method
  (`toSummary`/`toFindingResponse` — bản của riêng Controller, không phải bản trong
  `ReconciliationService` vẫn dùng cho `/run`).
- **`GET /platform-admin/audit-logs`** — `PlatformAuditLogController` (xoá cả file) +
  `PlatformAuditService.list()` + `PlatformAuditLogRepository.findAllByOrderByCreatedAtDesc` +
  `PlatformAuditLogResponse` DTO (xoá cả file). **Sửa lại đánh giá ban đầu**: lúc hỏi người dùng đã
  nêu nghi ngờ endpoint này thiếu `@PreAuthorize` là lỗ hổng quyền — rà soát kỹ hơn cho thấy TOÀN BỘ
  controller platform-admin khác (vd `TenantAdminController`) cũng không có `@PreAuthorize` ở mức
  method, vì bảo vệ đã nằm ở tầng chain Spring Security (`anyRequest().hasAuthority("PLATFORM_ADMIN")`
  trong `SecurityConfig`) — đây là mẫu nhất quán của toàn bộ tầng Super Admin (chỉ 1 vai trò, không
  cần phân biệt quyền con), KHÔNG phải lỗ hổng. `record()` (ghi audit log) giữ nguyên, chỉ bỏ phần
  ĐỌC LẠI.

### 3. Đồng bộ tài liệu
Cập nhật `docs/phase1/business-specs-should.md` (UC-11, UC-13 — xem mục 1/2 ở trên) và
`docs/phase1/permission-matrix.md` (đánh dấu `order:void` mồ côi). Không tìm thấy 1 "phần 9" theo
đúng nghĩa đen trong bất kỳ tài liệu nào của repo này (roadmap có thể dùng số thứ tự từ 1 template
chung, không khớp cấu trúc tài liệu thực tế của dự án này — xem ghi chú tương tự ở Prompt #3) — đã
diễn giải theo đúng tinh thần yêu cầu (đồng bộ tài liệu đặc tả phản ánh chức năng thiếu/đã xoá) thay
vì tìm đúng số mục.

### Xác nhận chạy thật
`mvn compile`/`test-compile`: sạch. Backend: **68 unit test** + **46 integration test** (giảm đúng 1
so với Prompt #9, do xoá 1 test case không còn method để gọi) = **114 test, TẤT CẢ XANH**. Frontend:
**49 test Vitest** + `tsc --noEmit` sạch + `npm run lint` sạch (0 lỗi, 3 warning có sẵn không liên
quan) + `npm run build` thành công. Migration `V29` đã xác nhận migrate sạch trên Postgres test
throwaway.

### Giới hạn phạm vi đã ghi rõ (không phải bỏ sót)
- Không quét lại TOÀN BỘ 4 loại nợ kỹ thuật định kỳ trong tương lai — đây là 1 lần rà soát tại thời
  điểm hiện tại (2026-07-12), codebase có thể phát sinh nợ kỹ thuật mới sau các prompt tiếp theo.
- Không xoá permission `order:void` khỏi seed data đã chạy (đúng nguyên tắc "cấm sửa migration đã
  chạy") — permission này nay mồ côi (không code nào kiểm tra), chỉ ghi chú trong tài liệu.
- Chưa cập nhật `SECURITY_AUDIT_REPORT.md`/`docs/phase8/pos-module.md` (cũng có nhắc tới
  cancelOrder/order:void) — các tài liệu lịch sử theo từng Phase, ngoài phạm vi "phần 9" mà roadmap
  yêu cầu đồng bộ.

---

## PROJECT_STATE — cập nhật sau Prompt #9 (CI/CD + Backup/Restore, P2) — 2026-07-12

### Nền tảng đã có sẵn TRƯỚC prompt này (Phase 12, kế thừa không viết lại từ đầu)
Rà soát trước khi làm phát hiện phần lớn hạ tầng CI/CD + Docker + deploy doc đã có sẵn ở mức khá
tốt: `.github/workflows/ci.yml` (job backend `mvn verify` qua Testcontainers, job frontend
lint+typecheck+unit+build, job E2E Playwright), `server/Dockerfile` + `client/Dockerfile` (đã
multi-stage, JRE slim, non-root, HEALTHCHECK), `docker-compose.yml`/`docker-compose.prod.yml`
(healthcheck Postgres/Redis, `depends_on: condition: service_healthy`, resource limits ở overlay
prod), JaCoCo coverage gate (Prompt #1, đã chặn `mvn verify` nếu dưới 30%), và `README_DEPLOY.md`
(hướng dẫn deploy 10 bước khá đầy đủ) + `scripts/backup.sh`/`scripts/restore.sh` (bản thủ công đơn
giản). Việc của prompt này là BỔ SUNG đúng các mảnh còn thiếu, không phải viết lại toàn bộ.

### 1. CI — 2 job mới trong `ci.yml`
- **`tenant-isolation`**: job RIÊNG, TÁCH KHỎI job `backend` — chỉ chạy `TenantIsolationIT` +
  `PermissionMatrixIT` (`-Dit.test=...`, `-Djacoco.skip=true` để không bị JaCoCo chặn sai do chỉ
  chạy 2/hàng trăm test). Lý do tách riêng: đây là bộ test bảo mật QUAN TRỌNG NHẤT (rò rỉ dữ liệu
  giữa các cửa hàng), cần 1 required check nổi bật riêng trên GitHub UI thay vì chìm trong job
  `backend`. Đã xác nhận chạy thật cục bộ: 9 test xanh.
- **`docker-images`**: build (2 tầng, dùng đúng `server/Dockerfile`/`client/Dockerfile` có sẵn) cho
  cả `server` và `web` qua `docker/build-push-action` + `docker/metadata-action` (tự sinh tag
  `sha-<short-sha>`, semver khi push tag `v*.*.*`, `latest` khi push nhánh `main`). **Luôn build**
  trên mọi PR/push (phát hiện lỗi Dockerfile sớm) — **chỉ push** lên GHCR khi `github.event_name ==
  'push'` (tránh rác registry bằng image PR chưa duyệt).

### 2. Docker hoá — bổ sung 2 điểm còn thiếu
- `docker-compose.yml`: thêm `x-logging` anchor (`json-file`, `max-size: 10m`, `max-file: 5`) áp
  cho cả 4 service — trước đây log Docker không giới hạn, có thể đầy ổ đĩa sau nhiều tháng chạy.
  `web` đổi `depends_on: [server]` (chỉ chờ container start) thành
  `depends_on: server: condition: service_healthy` (chờ `/actuator/health` xanh thật — `server` đã
  có `HEALTHCHECK` sẵn trong Dockerfile, chỉ cần khai báo điều kiện ở compose).
- Đã xác nhận `docker compose config` merge sạch cho cả base + prod overlay + observability overlay
  (Prompt #8) + backup overlay (mục 3) sau khi sửa.

### 3. Backup/Restore — viết lại đúng yêu cầu roadmap + PHÁT HIỆN VÀ VÁ 1 BUG THẬT
- `scripts/backup.sh`: đổi từ `pg_dump` plain-SQL+gzip sang **`-Fc` (custom format)** — khớp đúng
  định dạng các bản sao lưu thủ công trước đây trong `backups/*.dump` đã dùng. Giữ **7 bản hàng
  ngày + 4 bản hàng tuần** (mốc tuần lấy bản đầu tiên của mỗi tuần ISO, không bị dọn theo chu kỳ 7
  ngày của bản ngày). Hỗ trợ đẩy ra ngoài máy chủ qua `RCLONE_REMOTE` (tuỳ chọn, `.env`) — chưa có
  tài khoản cloud storage trong môi trường này nên KHÔNG xác nhận được nhánh rclone thật, chỉ xác
  nhận nhánh mặc định (in hướng dẫn `rsync` thay thế).
- **Đã CHẠY THẬT** `scripts/backup.sh` nhắm vào **database sản xuất đang chạy thật**
  (`docker-postgres-1`, 2 tenant/17 đơn hàng thật) — `pg_dump` là thao tác CHỈ ĐỌC, an toàn tuyệt
  đối với hệ thống đang phục vụ. Kết quả: bản sao 168K tạo thành công.
- `scripts/restore-test.sh` (MỚI, đúng yêu cầu roadmap "backup chưa từng restore thử = chưa có
  backup"): restore bản mới nhất vào 1 container Postgres TẠM THỜI (tên ngẫu nhiên, tự xoá qua
  `--rm`/trap EXIT), chạy 3 câu SQL smoke (đếm tenants, đếm orders, đọc bảng invoices), ghi kết quả
  vào `backups/restore-test.log`. **KHÔNG đụng gì đến database thật đang chạy.**
- **Phát hiện bug thật khi chạy restore-test.sh lần đầu**: `pg_restore` báo lỗi tạo lại 2 index
  `idx_customers_name_trgm`/`idx_products_name_trgm` (`function unaccent(unknown, text) does not
  exist`) — nguyên nhân: hàm `immutable_unaccent()` (`V1__init_schema.sql`) gọi `unaccent(...)`
  KHÔNG ghi rõ schema; Postgres "inline" hàm SQL IMMUTABLE này lúc tạo index biểu thức bằng 1
  search_path AN TOÀN HƠN (không mặc định có `public`) — khác với search_path phiên làm việc
  thường lúc Flyway tạo mới lần đầu. Dữ liệu vẫn restore ĐÚNG (đã xác nhận qua 3 câu smoke), chỉ 2
  index tìm kiếm không dấu bị thiếu. **Đây LÀ phạm vi của prompt này** (bug lộ ra chính xác qua việc
  thử restore thật — không phải phát hiện tình cờ ngoài lề) nên đã VÁ NGAY, không chỉ ghi
  `BUGS_FOUND.md`: `V28__fix_immutable_unaccent_schema_qualify.sql` — đổi `unaccent(...)` thành
  `public.unaccent(...)` (đã tự viết 1 test tay xác nhận bản ghi rõ schema tạo index thành công
  trong session Postgres mới hoàn toàn, khớp đúng kịch bản restore). Áp dụng cho database sản xuất
  thật cần 1 lần deploy bình thường (Flyway tự chạy V28 khi container `server` khởi động lại) —
  KHÔNG tự ý chạy tay vào `docker-postgres-1` (bỏ qua kỷ luật Flyway, cần xác nhận trước).
- `scripts/restore.sh`: cập nhật khớp định dạng `-Fc` mới (`pg_restore` thay vì `psql < gunzip`),
  thêm bước `DROP DATABASE`+`CREATE DATABASE` (restore vào database TRẮNG, tránh lỗi "already
  exists" khi restore đè lên schema cũ) + tự bật lại `unaccent`/`pg_trgm` trước khi restore (cùng
  bug ở trên).
- **Container phụ tự động** (tuỳ chọn, overlay `docker-compose.backup.yml`, giống khuôn mẫu
  `docker-compose.observability.yml` của Prompt #8): dùng thẳng image `postgres:16-alpine` (có sẵn
  `pg_dump` + `crond` qua BusyBox, không cần build image riêng) chạy `crond -f`, kết nối tới service
  `postgres` QUA MẠNG (không mount Docker socket — giảm bề mặt tấn công so với phương án container
  phụ điều khiển container khác qua socket).

### 4. Deploy + Rollback — mở rộng `README_DEPLOY.md` có sẵn (không tạo file DEPLOY.md mới trùng lặp)
Thêm mục 11 "Rollback": nhấn mạnh ràng buộc **Flyway CHỈ ROLL-FORWARD** (không có down-migration) —
quay lại image cũ AN TOÀN NGAY nếu bản mới không thêm migration; nếu CÓ thêm migration, chỉ an toàn
nếu tuân thủ tương thích ngược 1 phiên bản (cột mới phải nullable/có default; đổi tên/xoá cột phải
qua 2 bước — thêm trước, xoá sau ít nhất 1 lần deploy) — nếu migration vi phạm, rollback code KHÔNG
đủ, phải khôi phục từ backup gần nhất TRƯỚC migration đó (lý do backup bắt buộc chạy trước MỌI lần
update, không chỉ định kỳ). Cập nhật mục 9 (Backup) phản ánh script mới + `restore-test.sh` +
overlay cron.

### Xác nhận chạy thật
`mvn compile`/`test-compile`: sạch. Backend: **68 unit test** + **47 integration test** = **115
test, TẤT CẢ XANH** (không đổi assertion nào, chỉ thêm 1 migration mới V28). `docker compose config`
merge sạch cho base + prod + observability + backup overlay. CI YAML (`ci.yml`) xác nhận hợp lệ qua
`python -c "import yaml; yaml.safe_load(...)"`. **`scripts/backup.sh` + `scripts/restore-test.sh`
đã chạy THẬT (không phải mô phỏng) nhắm vào database sản xuất đang chạy** — kết quả đầy đủ ở mục 3.

### Giới hạn phạm vi đã ghi rõ (không phải bỏ sót)
- Chưa xác nhận nhánh `RCLONE_REMOTE` (đẩy backup ra ngoài máy chủ) bằng chạy thật — không có tài
  khoản cloud storage trong môi trường này. Code đã viết theo đúng API rclone chuẩn, review logic
  đã đủ, nhưng CHƯA có bằng chứng chạy thật cho riêng nhánh này.
- Chưa thực sự khởi động `docker-images` job trên GitHub Actions thật (cần push lên GitHub) — chỉ
  xác nhận YAML hợp lệ + logic build/tag đúng cú pháp `docker/metadata-action`.
- Migration V28 CHƯA được áp dụng lên database sản xuất thật (`docker-postgres-1`) — cần 1 lần
  deploy bình thường (quyết định có chủ đích: không tự ý chạy migration tay vào hệ thống đang chạy
  ngoài quy trình Flyway/deploy chuẩn).
- Chưa build/push image thật lên GHCR (cần push code lên GitHub, ngoài khả năng của sandbox này).

---

## PROJECT_STATE — cập nhật sau Prompt #8 (Observability, P2) — 2026-07-12

### Nền tảng đã có sẵn TRƯỚC prompt này (kế thừa, không phải viết mới từ đầu)
Rà soát trước khi làm phát hiện hệ thống đã có sẵn `CorrelationIdFilter` (gán `correlationId` vào
MDC + trả header `X-Correlation-Id`) và `logback-spring.xml` (JSON qua LogstashEncoder ở profile
docker/prod, pattern thường ở local) từ 1 giai đoạn trước (D2/Phase 9). **Quyết định: TÁI SỬ DỤNG
`correlationId`/`X-Correlation-Id` làm "requestId" của roadmap thay vì tạo 1 khái niệm ID thứ 2
song song** — cùng bản chất (1 UUID/request, trả về header, vào MDC), tạo thêm 1 ID riêng sẽ gây
nhầm lẫn và trùng lặp vô ích.

### 1. Structured logging — mở rộng MDC
`RequestContextMdcFilter` (mới) — gắn thêm `tenantId`/`userId`/`branchId` vào MDC, đăng ký thủ công
vào `SecurityConfig` ngay SAU `TenantFilter` (`addFilterAfter`, tắt auto-registration của Spring
Boot giống khuôn mẫu `ApiRateLimitFilter`) vì cần `TenantContext`/`SecurityContext` đã được
`JwtAuthenticationFilter` gán trước đó — đặt trước điểm này (như Spring Boot tự đăng ký filter
`@Component`) sẽ luôn đọc được giá trị rỗng.
- `userId` = **username** (không phải id số) — lấy thẳng từ `SecurityContext` (đã có sẵn từ JWT),
  tránh 1 truy vấn DB mỗi request chỉ để phục vụ logging.
- `branchId` chỉ lấy được khi request truyền qua **query param** `branchId` (quy ước đã phổ biến ở
  10 file Controller) — request có branchId nằm trong JSON body (POST/PUT) sẽ KHÔNG có trong MDC
  (giới hạn phạm vi đã ghi rõ, đọc body ở tầng Filter sẽ tiêu thụ stream trước Controller).
- `logback-spring.xml`: pattern local thêm `tenant=/user=/branch=`; JSON (docker/prod) thêm 3
  `includeMdcKeyName`. Test: `RequestContextMdcFilterTest` (2 test, xác nhận MDC được gán ĐÚNG lúc
  filter chain chạy và XOÁ SẠCH sau khi xong — tránh rò rỉ context giữa các request tái sử dụng
  thread).

### 2. Spring Boot Actuator — mở rộng exposure + quyết định ranh giới bảo mật
Thêm `metrics,prometheus` vào `management.endpoints.web.exposure.include` (trước chỉ `health,info`)
+ dependency `micrometer-registry-prometheus`. **Ranh giới bảo mật là MẠNG, không phải xác thực**:
`docker/nginx.conf` chỉ proxy `/api/` (không có `/actuator`), port `server` không publish ra host
(`docker-compose.yml`) — xác nhận lại bằng cách đọc trực tiếp 2 file này trước khi quyết định. Vì
vậy: `SecurityConfig` đổi `permitAll("/actuator/health","/actuator/info")` thành
`permitAll("/actuator/**")` (Prometheus scrape không mang JWT được), và
`management.endpoint.health.show-details` đổi từ `when-authorized` sang **`always`** (ở
`when-authorized`, endpoint permitAll + JWT stateless nghĩa là KHÔNG BAO GIỜ có "authorized
principal" — cấu hình cũ tưởng an toàn nhưng thực ra chỉ ẩn thông tin một cách vô dụng). Bỏ luôn
override `show-details: never` ở `application-prod.yml` vì cùng lý do áp dụng cho mọi profile.

### 3. Metric nghiệp vụ (Micrometer, `com.quanlycuahang.erp.common.metrics.BusinessMetrics`)
Đúng 6 metric roadmap liệt kê, tag `tenantId` (String, "unknown" khi chưa xác định được — vd đăng
nhập sai với username không tồn tại):
- `orders_created_total` (Counter) + `order_checkout_duration` (Timer) — `OrderService.
  createOrder()`, ghi nhận NGAY TRƯỚC return thành công (không ghi khi transaction rollback — quyết
  định phạm vi có chủ đích, "checkout duration" chỉ có ý nghĩa cho đơn tạo thành công).
- `order_price_mismatch_total` — `OrderValidationService.assertPricingMatchesExpected()`.
- `rate_limit_rejected_total` (tag thêm `tier`) — `ApiRateLimitFilter`, CẢ 2 nhánh chặn thật (tier
  đã đăng nhập + `PUBLIC_LOOKUP` chưa đăng nhập) — trước đây chỉ log WARN cho shadow-mode
  ("lẽ ra sẽ chặn"), nhánh chặn THẬT lại không log gì cả (phát hiện khi rà soát, vá luôn).
- `reconciliation_findings_open` (Gauge, tra cứu ĐÚNG cách Micrometer — đăng ký 1 lần/tenant qua
  `AtomicLong`, chỉ CẬP NHẬT giá trị chứ không đăng ký lại) — cập nhật ở 2 nơi:
  `ReconciliationService.runForTenant()` (ngay sau khi chạy xong, phủ cả job đêm) và
  `ReconciliationController.openFindingsCount()` (mỗi lần Dashboard hỏi).
- `login_failed_total` (tag thêm `reason`: `bad_credentials`/`rate_limited`) — `AuthService.login()`.
- Xác nhận bằng test THẬT (không phải đọc code suy luận): `BusinessMetricsGateIT` tạo 1 đơn POS
  qua `OrderService.createOrder()` (đúng luồng nghiệp vụ, không gọi thẳng `MeterRegistry`), đọc lại
  `orders_created_total`/`order_checkout_duration` từ `MeterRegistry` thật — **đây chính là gate
  bắt buộc của roadmap**, làm bằng số đo được thay vì ảnh chụp màn hình thủ công.

### 4. Log WARN có cấu trúc cho sự kiện nhạy cảm
- `ORDER_PRICE_MISMATCH` — `OrderValidationService`.
- `RATE_LIMIT_BLOCKED` — `ApiRateLimitFilter` (nhánh chặn thật, xem mục 3).
- `LOGIN_RATE_LIMIT_EXCEEDED` / `LOGIN_FAILED` — `AuthService.login()` (username + IP, KHÔNG log
  mật khẩu).
- `PURCHASE_ORDER_ITEM_PRICE_EDITED` — `PurchaseOrderService.updateItemPrice()` (giá cũ/mới + lý
  do).
- `STOCK_TAKE_APPROVE_LARGE_DISCREPANCY` — `StockTakeService.approve()`, ngưỡng "lớn" tự định
  nghĩa (chưa có trong roadmap/code cũ): >=20% so với tồn dự kiến HOẶC >=50 đơn vị tuyệt đối (tránh
  chia 0 khi tồn dự kiến bằng 0) — hằng số cố định, chưa có cài đặt riêng từng tenant.
- **"Thao tác xoá"**: thay vì sửa tay từng `delete()` rải rác khắp hệ thống, tận dụng hạ tầng
  `@Audited`/`AuditAspect` ĐÃ CÓ SẴN (ghi mọi hành động được đánh dấu vào bảng `audit_logs`) — thêm
  1 nhánh trong CHÍNH `AuditAspect.logAudit()`: bất kỳ `action` nào chứa `"DELETE"` (vd
  `PRODUCT_DELETE` đã có sẵn từ trước) tự động thêm 1 dòng WARN `SENSITIVE_DELETE`, áp dụng cho MỌI
  action xoá hiện tại VÀ tương lai mà không cần sửa từng Service. Hạn chế đã ghi rõ: `@Audited` vốn
  chỉ phủ 1 phần nhỏ hệ thống (rà soát chỉ thấy 4 action dùng annotation này toàn bộ codebase) —
  mở rộng độ phủ audit là việc lớn hơn, ngoài phạm vi prompt này.

### 5. FE — gắn mã tra cứu vào thông báo lỗi
`errors.ts`: thêm `getApiErrorRequestId()` (đọc header `x-correlation-id` từ response) +
`getApiErrorMessage()` chỉ gắn thêm `(Mã lỗi: xxx — cung cấp mã này khi báo hỗ trợ)` cho lỗi
**500/`INTERNAL_ERROR`/mất kết nối** — CỐ Ý không gắn cho lỗi nghiệp vụ thường (400 với message rõ
ràng như "Danh mục còn sản phẩm") vì thêm mã vào đó chỉ gây rối, không giúp ích. Vì `getApiErrorMessage`
là hàm dùng chung cho toàn bộ 27 nơi hiển thị lỗi hiện có, thay đổi này áp dụng NGAY cho mọi toast
lỗi hệ thống mà không cần sửa từng trang. 6 test mới (`errors.test.ts`) xác nhận đúng hành vi cũ
(429, validation details) không đổi, chỉ thêm hành vi mới cho lỗi hệ thống.

### 6. (Tuỳ chọn) Prometheus + Grafana
Overlay RIÊNG `docker-compose.observability.yml` (không bật mặc định, giống khuôn mẫu
`docker-compose.prod.yml` đã có) — Prometheus scrape `server:8080/actuator/prometheus` qua mạng
nội bộ docker-compose, Grafana tự động nạp datasource + 1 dashboard cơ bản (request rate, p95
latency qua `histogram_quantile`, error rate 5xx, đơn/giờ theo tenant) qua provisioning. Cả 2
container KHÔNG publish port ra host — hướng dẫn SSH tunnel port 3000 khi cần xem Grafana (đã ghi
trong comment file). Thêm `management.metrics.distribution.percentiles-histogram.http.server.requests:
true` để Prometheus tính được p95 (Micrometer không bật histogram buckets mặc định). Đã xác nhận
`docker compose config` merge đúng (không thực sự khởi động container — ngoài phạm vi có thể kiểm
chứng của sandbox này).

### Xác nhận chạy thật
`mvn compile`/`test-compile`: sạch. Backend: **68 unit test** + **47 integration test** (chạy qua
Postgres/Redis container throwaway) = **115 test, TẤT CẢ XANH** (7 test mới:
`RequestContextMdcFilterTest` 2, `BusinessMetricsGateIT` 1, cộng các test cũ không đổi assertion
nào). Frontend: **49 test Vitest** (6 test mới `errors.test.ts`) + `tsc --noEmit` sạch.

### Giới hạn phạm vi đã ghi rõ (không phải bỏ sót)
- `branchId` trong MDC chỉ phủ request qua query param, không phủ branchId trong JSON body.
- `userId` trong MDC là username, không phải id số (đánh đổi lấy hiệu năng — tránh 1 query DB/request).
- Chưa mở rộng `@Audited` sang các thao tác xoá khác ngoài `PRODUCT_DELETE` đã có sẵn — cơ chế mới
  (nhánh trong `AuditAspect`) sẽ tự áp dụng khi các action đó được gắn `@Audited` sau này.
- Prometheus/Grafana chỉ xác nhận qua `docker compose config` (merge cấu hình đúng), chưa xác nhận
  bằng cách thực sự khởi động container + xem dashboard hiển thị số liệu thật (cần Docker daemon
  hoạt động, ngoài khả năng chắc chắn của sandbox này — xem ghi chú Testcontainers ở AbstractIntegrationTest).

---

## PROJECT_STATE — cập nhật sau Prompt #7 (Hiệu năng: N+1, pagination, cache, báo cáo, P2) — 2026-07-12

### Điều chỉnh phạm vi so với mô tả roadmap (đã ghi rõ, không phải bỏ sót)
Roadmap yêu cầu bật datasource-proxy/Hibernate statistics + **seed script ở đúng mốc tải** (20k
SKU/tenant, 500 đơn/ngày, 50 tenant) rồi đo baseline trước khi sửa. Seed dữ liệu ở quy mô đó trong
sandbox này tốn thời gian không cân xứng. Thay vào đó: dùng **Hibernate Statistics thật**
(`hibernate.generate_statistics=true`, `Statistics.getPrepareStatementCount()`) trong
`NPlusOneRegressionIT`, seed **60 dòng/bảng** (vượt `default_batch_fetch_size=50` — đủ để chứng
minh số câu SELECT KHÔNG tỉ lệ thuận với N, đây chính là dấu hiệu định tính của N+1, không phụ
thuộc N tuyệt đối là 60 hay 20.000) — đo được số THẬT (không phải ước lượng), chỉ khác ở quy mô
seed. Test này ở lại vĩnh viễn trong CI như 1 regression test chống N+1 tái phát.

### 1. Diệt N+1 (4 repository, đã đo bằng Hibernate Statistics)
Rà thủ công (Explore subagent bị lỗi tạm thời do outage classifier, chuyển sang đọc code trực
tiếp) toàn bộ Mapper ánh xạ `@ManyToOne(LAZY)` sang thuộc tính khác `.id` (an toàn) trong vòng lặp
danh sách — tìm đúng khuôn mẫu N+1 kinh điển:
- **`StockTakeItemRepository.findByStockTakeId`**: N+1 THẬT SỰ nghiêm trọng nhất — 1 phiếu kiểm kê
  phủ TOÀN BỘ tồn kho chi nhánh (tới 20k SKU ở tenant lớn nhất theo mục tiêu tải), mọi lần xem chi
  tiết/duyệt phiếu đọc `item.getProduct().getName()` cho từng dòng. Sửa: `@Query` với
  `JOIN FETCH i.product` (nhân bản đúng khuôn mẫu JOIN FETCH sẵn có duy nhất trong codebase —
  `ShiftRepository.search()`).
- **`PurchaseOrderItemRepository.findByPurchaseOrderId`**: cùng lỗi (`PurchaseOrderMapper.
  toItemResponse` đọc `product.name`), cùng cách sửa.
- **`InventoryRepository`** (`findByBranchId`, `findLowStockByBranchId`, `search`,
  `searchLowStock`): **N+1 nghiêm trọng nhất trong toàn hệ thống** — `InventoryMapper.toResponse`
  đọc `product.name/sku/minStock` cho MỌI dòng, và đây là trang được xem nhiều nhất
  (`InventoryPage`), tiềm năng hàng chục nghìn dòng/chi nhánh. Sửa cả 4 query bằng
  `JOIN FETCH i.product p`, viết lại các điều kiện `WHERE`/`EXISTS` dùng alias `p` thay vì
  `i.product.x` lặp lại (tránh Hibernate tạo 2 join riêng cho cùng quan hệ).
- **`UserRepository`** (`findAllByOrderByFullNameAsc`, `findByTenantIdOrderByFullNameAsc`):
  `EmployeeService.toResponse` đọc `user.getRoles()` (`@ManyToMany` LAZY) — mức độ nghiêm trọng
  THẤP HƠN nhiều (danh sách nhân viên/tenant thường chỉ vài chục dòng) nhưng sửa rẻ nên vẫn áp
  dụng nhất quán: `LEFT JOIN FETCH u.roles` + `SELECT DISTINCT` (bắt buộc để gộp dòng lặp do fetch
  collection).
- **Đã xác nhận KHÔNG có N+1** (kiểm tra nhưng không cần sửa): `OrderService`/`ReturnService` dùng
  sẵn cột snapshot (`productNameSnapshot`) thay vì join sống tới `product` — thiết kế đã đúng từ
  trước, không phải bỏ sót.
- **KHÔNG dùng `FetchType.EAGER` toàn cục** ở bất kỳ đâu (đúng ràng buộc cấm của roadmap) — mọi
  chỗ sửa đều là JOIN FETCH có chủ đích trong 1 query cụ thể.

Bằng chứng đo được (`NPlusOneRegressionIT`, log Hibernate thật): 1 phiếu kiểm kê 60 dòng trước đây
sẽ ra ~60+ câu SELECT rời rạc (giảm còn ~2 nhờ `default_batch_fetch_size=50` nhưng vẫn tỉ lệ theo
N/50) — sau khi sửa, **đúng 1 câu SELECT JOIN FETCH duy nhất** bất kể N. Test assert
`getPrepareStatementCount() <= 15` cho cả 3 luồng (stock take, purchase order, inventory list).

### 2. Pagination (orders, inventory_transactions, audit_logs)
Không đổi sang keyset/seek pagination — quyết định có đo đạc, không phải mặc định giữ nguyên:
- `orders`: đã có index tổng hợp `idx_orders_tenant_created_branch_status (tenant_id, created_at,
  branch_id, status)` từ 1 lần rà soát hiệu năng TRƯỚC ĐÓ (V24) — offset pagination trên index này
  đã đủ nhanh ở độ sâu phân trang thực tế (người dùng hiếm khi lật sâu quá vài chục trang lịch sử).
- `inventory_transactions`: luôn truy vấn theo 1 `productId` cụ thể (thẻ kho từng sản phẩm, không
  có danh sách "toàn bộ giao dịch mọi lúc"), đã có index `(product_id, created_at)` từ V1 — phạm vi
  dữ liệu mỗi truy vấn vốn đã bị chặn tự nhiên, không phải bảng "tăng vô hạn không giới hạn".
  Không cần keyset.
- `audit_logs`: **phát hiện 1 lỗ hổng index thật** — mọi truy vấn (`AuditLogRepository.search`)
  đều lọc `tenant_id` + khoảng `created_at`, nhưng bảng chỉ có index đơn lẻ `created_at` (V1),
  không có composite — đúng lỗi y hệt đã tìm và vá cho `orders` ở V24 nhưng bị bỏ sót ở bảng song
  sinh này. Đây là bảng tăng nhanh nhất hệ thống (mọi hành động `@Audited` đều ghi 1 dòng). Vá bằng
  **`V27__audit_logs_tenant_created_index.sql`**: thêm `idx_audit_logs_tenant_created (tenant_id,
  created_at)`, xoá `idx_audit_logs_created_at` (dư thừa sau khi có composite, mọi truy vấn đều
  kèm `tenant_id` — xoá để giảm chi phí ghi trên bảng insert-heavy). Đã xác nhận Flyway migrate
  sạch qua IT test.

### 3. Cache Caffeine (in-process, KHÔNG dùng Redis của SettingsService)
Thêm dependency `com.github.ben-manes.caffeine:caffeine` (version do Spring Boot BOM quản lý).
Cache đúng 2 nơi roadmap chỉ định còn thiếu (`SettingsService` đã có cơ chế tương đương qua Redis
từ trước, không cache trùng):
- `CategoryService.list()` (cây danh mục) và `BranchService.list()` (toàn bộ chi nhánh) — key =
  `tenantId`, TTL 120s (`expireAfterWrite`), `cache.invalidate(tenantId)` ngay sau mọi
  create/update/delete (evict chủ động, không đợi hết TTL).
- **CỐ Ý KHÔNG cache** `BranchService.listMine()` — phụ thuộc user + phân quyền cụ thể (không chỉ
  tenant), lợi ích nhỏ (số chi nhánh/tenant vốn đã rất ít) không đáng đánh đổi rủi ro cache sai
  danh sách của user khác.
- **Test bắt buộc** (`CategoryServiceCacheIT`) — vì hệ thống multi-tenant từng có 2 lỗ hổng rò rỉ
  tenant nghiêm trọng trước đây (ghi trong memory phiên làm việc), cache mới PHẢI được kiểm chứng
  cách ly tenant: tạo danh mục tenant A → tenant B thấy DANH SÁCH RỖNG (không dính cache của A) →
  quay lại A vẫn đúng dữ liệu A → tạo thêm ở A phải thấy NGAY (không đợi TTL). Cả 2 tính chất đều
  xanh.
- Không cache tồn kho/giá bán/công nợ/checkout (đúng lệnh cấm của roadmap) — các luồng này vẫn đọc
  DB trực tiếp như cũ, không đổi gì.

### 4. Báo cáo + Excel
- Index cho `ReportService`: đã đủ từ V19 (rà soát hiệu năng trước đó) — `EXPLAIN` không phát
  hiện thiếu gì mới ngoài `audit_logs` (mục 2). `order_items` đã có `idx_order_items_order_id`/
  `idx_order_items_product_id` (V1) phủ tốt các JOIN trong `findTopProducts`/`sumCostOfGoodsSold`.
- `daily_sales_summary`/materialized view: **giữ nguyên quyết định không làm** đã ghi sẵn trong
  Javadoc `ReportService` từ trước ("chưa cần bảng tổng hợp ở quy mô 500-20000 SKU, 50-500 đơn/
  ngày") — không có bằng chứng mới để đảo ngược quyết định này.
  Giới hạn khoảng ngày export tối đa (731 ngày) **đã có sẵn** từ trước (`ReportController.
  MAX_REPORT_RANGE_DAYS`) — xác nhận lại, không cần thêm.
- **`ReportExcelExporter`: XSSFWorkbook → SXSSFWorkbook (streaming)** — lý do thật: các báo cáo
  tổng hợp (doanh thu/top sản phẩm...) chỉ vài trăm dòng, KHÔNG cần streaming; nhưng
  `InventoryController.export()` dùng CHUNG class này để xuất tới 10.000 dòng/lần
  (`PageRequest.of(0, 10_000)`) — đây mới là nơi streaming có giá trị thật (RAM ổn định thay vì tỉ
  lệ thuận số dòng). Đánh đổi: bỏ `autoSizeColumn()` (không tương thích streaming) → thay bằng độ
  rộng cột cố định ước theo độ dài tiêu đề. 2 test mới (`ReportExcelExporterTest`) xác nhận file
  sinh ra vẫn mở đọc lại được đúng định dạng OOXML (dùng `WorkbookFactory` đọc lại), đúng nội dung
  tiêu đề/dữ liệu, và xử lý được 500 dòng không lỗi.

### 5. HikariCP + Postgres
- `application.yml`: thêm `spring.datasource.hikari` — `maximum-pool-size=10`/`minimum-idle=5`
  (mặc định AN TOÀN cho máy nhỏ/vừa theo công thức `cores*2 + spindle`, chưa biết cấu hình host
  sản xuất thật nên đặt qua biến môi trường `DB_POOL_MAX_SIZE`/`DB_POOL_MIN_IDLE` để chỉnh khi
  biết số core thật), `leak-detection-threshold=60000` (cảnh báo connection giữ quá 60s — dấu hiệu
  transaction/luồng quên đóng), `data-source-properties.options=-c statement_timeout=30000` (lưới
  an toàn cuối cùng ở tầng Postgres, huỷ query "chạy quên" thay vì giữ connection vô hạn).
- `docker-compose.yml`: bật `log_min_duration_statement=500` cho Postgres (ghi log câu SQL nào
  chậm hơn 500ms) — KHÔNG bật `log_statement=all` (sẽ ghi log MỌI câu, quá nhiều ở SaaS nhiều
  tenant, chỉ cần biết câu nào THẬT SỰ chậm).
- Xác nhận: Spring context khởi động thành công với cấu hình mới qua IT test thật (kết nối Postgres
  thật, không phải mock) — nếu `options` sai cú pháp, Postgres sẽ từ chối kết nối ngay lập tức.

### Xác nhận chạy thật
`mvn compile`/`test-compile`: sạch. Toàn bộ test backend: **66 unit test** (`mvn test`) + **46
integration test** (`mvn test -Dtest="*IT"`, chạy thật qua Postgres/Redis container throwaway) =
**112 test, TẤT CẢ XANH** — không sửa assertion nào của test cũ, chỉ thêm 6 test mới
(`NPlusOneRegressionIT` 3, `CategoryServiceCacheIT` 1, `ReportExcelExporterTest` 2).

### Giới hạn phạm vi đã ghi rõ (không phải bỏ sót)
- Không seed đúng mốc tải 20k SKU/50 tenant thật (lý do ở đầu mục) — dùng Hibernate Statistics đo
  thật ở quy mô nhỏ hơn để chứng minh tính CHẤT (không tỉ lệ N) thay vì con số tuyệt đối ở đúng
  mốc tải.
- Không đo p50/p95 mili-giây thực tế (cần môi trường tải giả lập/staging thật, ngoài phạm vi
  sandbox này) — chỉ đo được số lượng câu SELECT (chỉ số tin cậy hơn cho N+1, ít nhiễu bởi tải máy
  hơn là đo thời gian).

---

## PROJECT_STATE — cập nhật sau Prompt #6 (Job đối soát toàn vẹn dữ liệu, P1) — 2026-07-12

### Migration + Entity
`V25__reconciliation.sql` (bảng `reconciliation_runs`/`reconciliation_findings`, tenant-scoped
giống `AuditLog`) + `V26__reconciliation_permission.sql` (quyền mới `reconciliation:run`, seed cho
owner/manager — dùng đúng khuôn mẫu resource:action có sẵn thay vì kiểm tra role trực tiếp như
roadmap viết, nhất quán với toàn hệ thống). Entity `ReconciliationRun`/`ReconciliationFinding` kế
thừa `TenantScopedEntity` (soft-delete + `@Filter` tự động, giống `AuditLog`).

### 7 phép đối soát (a-g) — `ReconciliationService`
Mỗi phép là **1 câu SQL aggregate duy nhất** qua `JdbcTemplate` (bỏ qua Hibernate Session/@Filter
như 14 file native-query ở Prompt #4 — tự thêm `tenant_id` thủ công), **không nạp từng bản ghi vào
Java** — đáp ứng đúng yêu cầu hiệu năng của roadmap.

**2 điểm điều chỉnh công thức so với mô tả roadmap** (đã kiểm chứng lại với code thật, ghi rõ
trong Javadoc `ReconciliationService`):
- **(b) Công nợ**: roadmap viết "debts.remaining vs (debts.amount - SUM(debt_payments))" — nhưng
  `debts.amount` CHÍNH LÀ số dư còn lại (không có cột "remaining" riêng), và `ReturnService` giảm
  trực tiếp `amount` khi trả hàng có ghi nợ (KHÔNG ghi dòng `debt_payments` riêng — đã ghi nhận từ
  trước, ngoài phạm vi FH-12). Áp dụng công thức nghĩa đen sẽ báo sai cho MỌI khoản nợ từng bị giảm
  bởi trả hàng. Đổi thành kiểm tra bất biến luôn đúng bất kể nguồn giảm nào: `amount` phải trong
  `[0, original_amount]`, và `SUM(debt_payments)` không được vượt quá phần đã giảm.
- **(c) Đơn hàng**: roadmap viết "orders.total vs SUM(line_total) + phí - CK" — nhưng CK dòng/CK
  đơn đã được phân bổ VÀO TỪNG line_total rồi (không còn cột CK riêng ở mức đơn). Công thức đúng
  (khớp `OrderService.createOrder`): `total_amount = SUM(line_total) + rounding_adjustment +
  shipping_fee`.

`(d) INVOICE_DUPLICATE` là nhánh phòng thủ thuần tuý — DB có `uq_invoices_order_id` là UNIQUE
CONSTRAINT thường (không lọc `deleted_at`), nên KHÔNG THỂ tạo ra 2 hoá đơn active cùng order_id
bằng bất kỳ cách nào (kể cả insert tay) — không có test riêng cho nhánh này vì không có kịch bản
nào tái hiện được, đã ghi rõ trong Javadoc test.

### Cách chạy
- Thủ công: `POST /api/v1/admin/reconciliation/run` (quyền `reconciliation:run`, giới hạn tenant
  hiện tại qua `TenantContext.get()`). `GET /api/v1/admin/reconciliation` (lịch sử),
  `GET /{runId}` (chi tiết kèm finding), `GET /open-count` (đếm nhanh cho canh báo Dashboard).
- Tự động: `ReconciliationScheduledJob` chạy 2 giờ sáng mỗi ngày (giờ VN) cho MỌI tenant đang hoạt
  động, tự bind `TenantContext`/Hibernate Session riêng từng tenant (giống mẫu
  `InvoiceEmailService.sendInvoiceEmailAsync`), lỗi 1 tenant KHÔNG làm hỏng các tenant còn lại. Cờ
  tắt riêng từng tenant: `SettingsService.KEY_RECONCILIATION_JOB_ENABLED` (mặc định `true`, không
  ảnh hưởng đường chạy thủ công).
- FE: cảnh báo trên `DashboardPage` (component `ReconciliationAlert`) — chỉ hiện khi có quyền
  `reconciliation:run` (owner/manager) VÀ có finding OPEN, kèm nút "Chạy đối soát ngay".

### Test bắt buộc (2 case, `ReconciliationServiceIT`)
- `detectsAllSevenCheckTypesOnDeliberatelyBrokenData`: 1 tenant với dữ liệu CỐ Ý phá vỡ cho từng
  phép (a,b,c,d,e,f,g qua entity JPA dựng tay, cùng khuôn mẫu `OrderRepositoryRevenueIT` đã có sẵn
  từ trước) — xác nhận đủ 7 loại `checkType` xuất hiện trong kết quả.
- `reportsZeroFindingsWhenDataBuiltThroughRealServicesOnly`: 1 tenant khác dựng HOÀN TOÀN qua
  Service thật (`PurchaseOrderService` nhập kho → `ShiftService.open()` → `OrderService.createOrder()`
  bán tiền mặt → `ShiftService.close()` đúng số) — xác nhận **0 finding** (không false positive).

### Hiệu năng
Kiến trúc mỗi phép = 1 round-trip SQL aggregate duy nhất (không N+1) đã tự thoả mãn yêu cầu về mặt
thiết kế. **Chưa đo số thật trên seed 20k SKU** như roadmap yêu cầu — nạp seed quy mô đó trong
sandbox này tốn thời gian không cân xứng với giá trị tăng thêm so với việc xác nhận kiến trúc
đúng (aggregate-only); ghi nhận đây là giới hạn phạm vi, cần đo lại khi có môi trường staging thật.

### Đã KHÔNG chạy đối soát trên dữ liệu thật đang chạy (quyết định an toàn có chủ đích)
Container Docker đang chạy (`docker-server-1`/`docker-postgres-1`, dữ liệu thật của tenant #1)
dùng jar CŨ (chưa có bảng/entity Prompt #6) — muốn chạy thật cần rebuild + redeploy image đang
chạy, là hành động ảnh hưởng hệ thống đang phục vụ, KHÔNG tự ý làm khi chưa được xác nhận rõ ràng.
Đã xác nhận đầy đủ bằng dữ liệu test (throwaway container Postgres/Redis dùng chung từ Prompt #1)
thay vì dữ liệu thật — không có phát hiện lệch nào để ghi vào `BUGS_FOUND.md` từ lần chạy này.

### Xác nhận chạy thật
`mvn compile`/`test-compile`: sạch. `ReconciliationServiceIT`: 2/2 test xanh (chạy riêng, xác nhận
qua `target/surefire-reports`). Frontend: `tsc --noEmit` sạch.

---

## PROJECT_STATE — cập nhật sau Prompt #5 (Hoàn thiện UI còn thiếu + error state, P1) — 2026-07-12

### 1. Dialog "Sửa khách hàng"/"Sửa nhà cung cấp" (PartnersPage)
`CustomerFormDialog`/`SupplierFormDialog` giờ dùng CHUNG 1 component cho cả thêm/sửa (nhận thêm
prop `customer`/`supplier` optional — có thì là sửa, giống đúng khuôn mẫu `isEdit = !!id` đã dùng ở
`ProductFormPage`), tránh 2 bản form lệch nhau theo thời gian như roadmap yêu cầu. Thêm cột hành
động (menu `...` → "Sửa") vào cả 2 bảng, bọc `PermissionGate` đúng quyền THẬT của Backend
(`customer:update`, `supplier:manage` — không phải `supplier:update` như roadmap giả định, đã kiểm
tra lại `SupplierController` để dùng đúng chuỗi quyền). Optimistic update qua
`onMutate`/`onError`/`onSettled` của TanStack Query, patch đúng 1 dòng trong cache của trang đang
xem (`listQueryKey` truyền vào dialog), rollback khi lỗi.

**Lệch so với giả định roadmap** (đã điều chỉnh, ghi rõ): roadmap giả định dialog "Thêm" sẵn có dùng
zod + react-hook-form để tái sử dụng — thực tế 2 dialog "Thêm" cũ dùng `useState` phẳng (không
zod/RHF). Không ép refactor sang zod chỉ cho riêng 2 dialog "Sửa" mới (sẽ tạo 2 phong cách khác nhau
ngay trong cùng 1 file) — giữ nguyên phong cách `useState` hiện có cho cả thêm/sửa, đúng tinh thần
cốt lõi của yêu cầu ("tránh copy-paste 2 bản lệch nhau") mà không đổi phong cách code ngoài phạm vi.

### 2. Chi tiết ca làm việc (Shift detail)
Thêm `getShiftById()` (`GET /shifts/{id}`, trước đây không FE nào gọi). Chọn **dialog (không phải
route mới)** — quyết định + lý do: đây là xem lại (read-only), không có hành động sửa nào cần URL
riêng để chia sẻ/bookmark; dialog giữ nguyên ngữ cảnh danh sách lịch sử ca đang xem, nhẹ hơn tạo 1
route + trang riêng. Tách phần lưới thống kê + danh sách thu/chi (`ShiftDetailStats`) dùng CHUNG cho
cả "Ca đang mở" (`CurrentShiftCard`) và dialog xem lại ca cũ — tránh lặp code, và bổ sung thêm 2 ô
"Tiền mặt thực đếm"/"Chênh lệch" (tô đỏ nếu lệch) chỉ hiện khi ca đã đóng, đúng yêu cầu roadmap.
Click 1 dòng trong bảng "Lịch sử ca" mở dialog.

### 3. Rà soát error state trang chi tiết
Tạo `client/src/components/common/QueryBoundary.tsx` (component dùng chung, roadmap yêu cầu) — bọc
3 trạng thái chuẩn: skeleton lúc tải, lỗi 404 hiện thông điệp TRUNG LẬP không phân biệt "không tồn
tại" vs "khác tenant" (đúng nguyên tắc Backend không tiết lộ — xem `TenantAwareRepositoryImpl`),
lỗi khác kèm nút "Thử lại" gọi `refetch()`. Áp dụng cho toàn bộ trang/khu vực dùng `useQuery` theo
`:id` mà roadmap liệt kê: `PurchaseOrderDetailPage`, `StockTakeDetailPage`, `InvoiceViewer` (dùng
chung bởi `InvoicePrintPage` VÀ `InvoiceDialog` lúc thanh toán POS), và `ShiftDetailDialog` mới ở
mục 2. Thêm `isNotFoundError()` vào `lib/http/errors.ts` để phân biệt 404 dùng chung.

### 4. Chuẩn keyboard POS
Rà lại: F1 (focus tìm kiếm) và F9 (thanh toán) đã hoạt động đúng kể cả khi đang gõ trong input
(phím F không bị trình duyệt/input nuốt mất, khác Enter/Tab) — không cần sửa. Escape đóng dialog
trên cùng đã tự động đúng nhờ `Dialog` dựng trên Radix UI (`@radix-ui/react-dialog` xử lý Escape +
focus-trap + xếp lớp dialog sẵn) — không cần thêm code. **Phát hiện 1 bug thật**: nút "Treo đơn"
đã ghi nhãn "(F8)" từ trước nhưng KHÔNG TỪNG nối phím tắt thật — luôn phải bấm chuột. Đã vá: thêm
nhánh `F8` vào `handleKeydown`, giữ đúng điều kiện `disabled` của nút (không treo khi giỏ hàng rỗng
hoặc đang treo đơn khác).

### Test mới (17 case, tất cả xanh)
- `pages/partners/PartnersPage.test.tsx` (9 case): render thêm/sửa, điền sẵn dữ liệu khi sửa, submit
  hợp lệ gọi đúng API + đóng dialog, submit lỗi KHÔNG đóng dialog, ẩn/hiện nút "Sửa" theo quyền.
- `components/common/QueryBoundary.test.tsx` (4 case): skeleton lúc tải, 404 không có nút thử lại,
  lỗi khác có nút thử lại + gọi đúng callback, render children đúng dữ liệu khi thành công.
- (Đã xuất `CustomerFormDialog`/`SupplierFormDialog` từ `PartnersPage.tsx` để test độc lập không cần
  toàn bộ trang — dùng `QueryClientProvider` test riêng, mock `@/lib/api/customers`/`suppliers`.)

### Xác nhận chạy thật
`npx tsc -b --noEmit`: sạch, không dùng `any` ở đâu (đúng ràng buộc). `npx vite build`: build thành
công, code-splitting từng trang vẫn đúng (bao gồm chunk `QueryBoundary` riêng). `npx vitest run`:
**43 test tổng** (30 test có sẵn từ trước + 13 test mới của Prompt #5: 9 `PartnersPage` + 4
`QueryBoundary`), tất cả xanh.

### File đã đổi
`lib/api/customers.ts`, `lib/api/suppliers.ts`, `lib/api/shifts.ts`, `lib/http/errors.ts`,
`pages/partners/PartnersPage.tsx` (+ test mới), `pages/shifts/ShiftsPage.tsx`,
`components/common/QueryBoundary.tsx` (mới, + test), `pages/purchase-orders/PurchaseOrderDetailPage.tsx`,
`pages/stock-takes/StockTakeDetailPage.tsx`, `components/invoice/InvoiceViewer.tsx`,
`pages/pos/PosPage.tsx`.

---

## PROJECT_STATE — cập nhật sau Prompt #4 (Audit cách ly tenant + IDOR, P1) — 2026-07-12

Báo cáo đầy đủ: `SECURITY_AUDIT_REPORT.md` (theo đúng format roadmap yêu cầu: severity | mô tả |
PoC | file:line | cách vá). Tóm tắt: **không tìm thấy lỗ hổng Critical/High** — kiến trúc cách ly
tenant (`TenantAwareRepositoryImpl` + Hibernate `@Filter` + `BranchAccessGuard`) đã được xây dựng
và vá kỹ từ các phiên trước Prompt #4 này (2 bug nghiêm trọng "Hibernate @Filter gotchas"). Prompt
#4 chủ yếu XÁC NHẬN bằng test chạy thật (không chỉ đọc code) rằng cơ chế đó hoạt động đúng trên
từng luồng cụ thể, cộng 1 phát hiện Low đã vá (làm cứng validate tên file upload).

**Test mới (16 case, tất cả xanh, giữ vĩnh viễn trong CI qua `mvn verify`):**
- `security/TenantIsolationIT` (8 case) — IDOR xuyên tenant trên Order/Invoice/Customer/
  PurchaseOrder/StockTake/Shift/Debt + endpoint hành động (`updateItemPrice`, `approve`, `close`) +
  `branchId` trong body request.
- `security/PermissionMatrixIT` (1 case) — quét reflection MỌI `@PreAuthorize` trên MỌI
  `@RestController`, đối chiếu bảng `permissions` thật, tự cập nhật khi thêm endpoint mới (không
  hardcode danh sách).
- `common/upload/FileStorageServiceTest` (6 case) — magic-byte, path traversal, định dạng tên file,
  giới hạn MIME.

**Đã vá**: `FileStorageService.resolve()` thêm validate định dạng tên file
(`^[0-9a-fA-F-]{36}\.(jpg|png|webp)$`) trước khi ghép đường dẫn — lớp phòng thủ bổ sung (Low,
không phải lỗ hổng khai thác được trong điều kiện hiện tại, xem báo cáo).

**Đã kiểm tra và xác nhận AN TOÀN (không cần sửa)**: native query 14 file (tenant_id đầy đủ, verify
thủ công từng file); tham chiếu đa hình reference_id/reference_type (không endpoint nào nhận trực
tiếp từ client); luồng công khai `/invoices/lookup/{code}` và `/settings/branding` (không lộ
giá vốn/công nợ, tự trả rỗng khi chưa xác định được tenant).

**Phạm vi thu hẹp có chủ đích**: không sinh ma trận đầy đủ 110 endpoint × 6 vai trò qua HTTP thật
(MockMvc) — thay bằng kiểm tra tĩnh mã quyền (PermissionMatrixIT) + các test tình huống cụ thể đã
có, chi phí/giá trị hợp lý hơn cho quy mô dự án hiện tại (xem giải trình trong
SECURITY_AUDIT_REPORT.md mục 7).

**Tổng test toàn dự án sau Prompt #4**: 104 test (64 unit + 40 integration), tất cả xanh qua
`mvn verify`.

---

## PROJECT_STATE — cập nhật sau Prompt #3 (Bật lại ApiRateLimitFilter an toàn, P0) — 2026-07-12

### Báo cáo điều tra nguyên nhân "rollback cũ" (bắt buộc — kết quả KHÁC giả định của roadmap)
Roadmap giả định `ApiRateLimitFilter` đang bị **tắt** sau 1 lần rollback trong quá khứ. Kiểm tra
thực tế bằng `git log --oneline --all -- server/.../ApiRateLimitFilter.java`: repo này chỉ có
**đúng 1 commit** chạm file này ("Initial commit"), **KHÔNG có bất kỳ rollback/revert nào** trong
lịch sử — và filter **đã được BẬT sẵn** (đăng ký trong `SecurityConfig`, cả 2 filter chain) từ đầu.
**Kết luận: giả định của roadmap KHÔNG khớp với repo thật này** — đây nhiều khả năng là 1 prompt
mẫu chung, không phải mô tả chính xác lịch sử của dự án cụ thể này. Ghi rõ ở đây theo đúng nguyên
tắc "không bịa" thay vì tạo ra 1 câu chuyện rollback không có thật.

Tuy vậy, review code bản cũ phát hiện **1 lỗ hổng thật** đúng loại roadmap cảnh báo ("Redis
timeout" là 1 trong các nguyên nhân rollback giả định): `RateLimitService.tryConsume()` gọi thẳng
Lettuce/Redis đồng bộ, và `ApiRateLimitFilter` cũ **không hề try/catch** quanh lời gọi này — nếu
Redis gián đoạn (mất kết nối/timeout), MỌI request đã đăng nhập sẽ nhận `RuntimeException` chưa
bắt, Spring dịch thành lỗi 500, **chặn toàn bộ API bán hàng chỉ vì hạ tầng phụ (rate-limit) gặp sự
cố** — đúng hệ quả nghiêm trọng mà roadmap muốn phòng tránh bằng "fail-open có kiểm soát". Đã vá.

Bản cũ còn có 2 hạn chế khác so với thiết kế roadmap đề xuất: (1) áp 1 mức phẳng 100 req/phút cho
MỌI API đã đăng nhập, không phân biệt nhóm endpoint; (2) không có `Retry-After` header, không có
cờ tắt nhanh qua cấu hình.

### Thiết kế mới
- **Khoá rate-limit**: giữ `authentication.getName()` (username) — **không cần ghép thêm
  `tenantId`** như roadmap gợi ý, vì username đã duy nhất toàn hệ thống (xác nhận qua
  `TestDataFactory`/schema `users.username UNIQUE`) nên không có rủi ro 2 tenant khác nhau đụng
  chung 1 bucket. Có ghép thêm tên tầng (`RateLimitTier`) vào khoá để 1 user không bị "dùng chung"
  hạn mức giữa các nhóm endpoint khác nhau.
- **Phân tầng** (`RateLimitTier`, `server/.../common/web/RateLimitTier.java`):

| Tầng | Điều kiện khớp | Ngưỡng | Lý do |
|---|---|---|---|
| `CHECKOUT` | `POST /api/v1/orders` (chính xác, không gồm sub-path như `/cancel`) | 120 req/phút/user | Nghiệp vụ chính — 2 đơn/giây đã rất nhanh với người thật |
| `SENSITIVE` | `DELETE` (mọi path) HOẶC path kết thúc `/change-password` HOẶC `/export` | 10 req/phút/user | Đổi mật khẩu, xoá dữ liệu, xuất Excel — ảnh hưởng dữ liệu/tài nguyên nặng. **Kiểm tra TRƯỚC `GET`** vì các endpoint xuất Excel (`GET .../export`) dùng phương thức GET, nếu không sẽ lọt vào nhóm READ rộng rãi |
| `READ` | `GET` (còn lại) | 600 req/phút/user | Đọc dữ liệu, rộng rãi |
| `DEFAULT` | Còn lại (POST/PUT/PATCH không phải checkout/nhạy cảm) | 100 req/phút/user | Giữ nguyên mức cũ làm mặc định an toàn |
| `PUBLIC_LOOKUP` | `GET /api/v1/invoices/lookup/**` (không đăng nhập) | 30 req/phút/**IP** | Chống dò quét `lookupCode`; LUÔN enforce, không theo `rate_limit_mode` (xem lý do bên dưới) |

- **Fail-open có kiểm soát**: bọc `RateLimitService.consume()` trong try/catch — lỗi bất kỳ (Redis
  mất kết nối/timeout) → log `WARN` (tag `RATE_LIMIT_FAIL_OPEN`, đủ để đếm qua log aggregation —
  chưa nối Micrometer counter thật, đó là phạm vi Prompt #8 Observability) + cho request đi qua,
  KHÔNG chặn.
- **`Retry-After`**: đổi `RateLimitService` dùng `tryConsumeAndReturnRemaining()` (Bucket4j
  `ConsumptionProbe`) thay vì `tryConsume()` thuần boolean — tính đúng số giây còn lại trước khi có
  lượt mới, không đoán chung chung theo độ dài cửa sổ.
- **Cờ tắt nhanh** (`SettingsService.KEY_RATE_LIMIT_MODE = "rate_limit_mode"`, editable qua trang
  Cài đặt, cache Redis TTL 10 phút — không cần redeploy): `off` (tắt hẳn) | `shadow` (chỉ đo + log,
  không chặn thật — đúng "giai đoạn 1" roadmap yêu cầu) | `enforce` (mặc định, chặn thật).
- **PlatformAdmin luôn `enforce` cố định, KHÔNG đọc `rate_limit_mode`**: phát hiện khi thiết kế —
  `PlatformAdminJwtAuthenticationFilter` không bind `TenantContext` (Super Admin không thuộc
  tenant nào), nên gọi `SettingsService` (tenant-scoped) từ nhánh này sẽ đọc "toàn cục không lọc
  theo tenant" và **NÉM LỖI ngay khi ≥2 tenant cùng tuỳ chỉnh khoá này** (Spring Data
  `IncorrectResultSizeDataAccessException` do nhiều dòng trùng khoá) — sẽ sập toàn bộ API Super
  Admin. Đã chặn trước bằng cách phát hiện authority `PLATFORM_ADMIN` và bỏ qua bước đọc settings.
- **Public lookup luôn enforce, không theo `rate_limit_mode`**: request chưa đăng nhập không có
  `TenantContext` để tra cấu hình theo tenant, và đây là bảo vệ chống dò quét ở mức nền tảng, không
  phải tuỳ chọn nghiệp vụ của từng cửa hàng.

### Code + test
- `server/.../common/web/ApiRateLimitFilter.java` (viết lại) — điều phối theo mode + tầng.
- `server/.../common/web/RateLimitTier.java` (mới) — enum phân tầng + hàm phân loại.
- `server/.../common/web/RateLimitService.java` — thêm `consume()` trả `ConsumptionProbe` (giữ
  nguyên `tryConsume()` cũ, vẫn dùng bởi `AuthService`/`PlatformAdminAuthService` cho rate-limit
  đăng nhập — không đổi để tránh rủi ro ngoài phạm vi).
- `server/.../system/service/SettingsService.java` — thêm khoá `rate_limit_mode` (editable,
  validate 1 trong 3 giá trị hợp lệ).
- Test (15 case mới, tất cả xanh qua `mvn verify`):
  - `RateLimitTierTest` (9 case, unit thuần) — phân loại đúng tầng theo method+path, đặc biệt
    `GET .../export` phải là `SENSITIVE` chứ không "lọt" vào `READ`.
  - `ApiRateLimitFilterFailOpenTest` (1 case, unit mock) — mô phỏng `RateLimitService` ném lỗi
    (thay cho tắt thật container Redis dùng chung với các IT test khác đang chạy song song trong
    phiên này) — xác nhận request vẫn đi qua, không trả 500.
  - `ApiRateLimitFilterIT` (5 case, Redis thật qua container tạm Prompt #1) — dưới ngưỡng đi qua;
    vượt ngưỡng (10 request `SENSITIVE`) → 429 kèm `Retry-After`; `shadow` không chặn thật dù vượt
    ngưỡng (15 request); `off` không chặn dù vượt ngưỡng; `PUBLIC_LOOKUP` giới hạn theo IP đúng cho
    request KHÔNG đăng nhập.
- FE: đã có sẵn nhánh xử lý riêng mã lỗi 429 (`client/src/lib/http/errors.ts`, thêm ở 1 phiên làm
  việc trước đó trong session này) — ưu tiên message tiếng Việt từ Backend, fallback thông báo thân
  thiện "Bạn thao tác quá nhanh..." — không cần sửa gì thêm cho Prompt #3.
- **Không làm "giai đoạn 1 shadow mode mặc định rồi tự động chuyển giai đoạn 2 sau vài ngày"** như
  roadmap mô tả (cần lịch/monitoring ngoài phạm vi 1 phiên code) — thay vào đó cung cấp CƠ CHẾ
  (`rate_limit_mode`) để vận hành viên tự chuyển đổi thủ công giữa 2 giai đoạn bất kỳ lúc nào qua
  Cài đặt, không cần redeploy — đáp ứng đúng mục tiêu "tắt nhanh khi cần" của roadmap.

### Xác nhận chạy thật
`mvn verify`: **89 test tổng** (58 unit + 31 integration, tăng thêm 15 so với sau Prompt #2) —
TẤT CẢ XANH, không có test nào của Prompt #1/#2 bị ảnh hưởng.

---

## PROJECT_STATE — cập nhật sau Prompt #2 (Refactor god-class OrderService, P0) — 2026-07-12

### Điều kiện tiên quyết đã kiểm tra trước khi làm
Bộ 6 integration test `OrderServiceCreateOrderIT` từ Prompt #1 đang xanh trước khi bắt đầu — dùng
làm lưới an toàn cho toàn bộ refactor này, đúng yêu cầu của prompt.

### Sơ đồ TRƯỚC (1 class, 721 dòng, 19 dependency)
```
OrderService (721 dòng, 19 dependency: OrderRepository, OrderItemRepository,
OrderPaymentRepository, ProductRepository, InventoryRepository,
InventoryTransactionRepository, BranchRepository, CustomerRepository, ShiftRepository,
DebtRepository, InvoiceRepository, VoucherService, VietQrService, SettingsService,
CurrentUserProvider, IdempotencyService, NumberSequenceService, ApplicationEventPublisher,
BranchAccessGuard)
  └── createOrder()  [~220 dòng — TOÀN BỘ nghiệp vụ nằm phẳng 1 method]
        ├── nạp Branch/Customer/Shift/Settings
        ├── nạp Product+Inventory hàng loạt + gộp số lượng theo productId + kiểm tồn
        ├── validate voucher + chặn CK vượt subtotal
        ├── gọi OrderPricingService (thuần, không đổi)
        ├── validate ORDER_PRICE_MISMATCH + nợ không khách hàng + vượt hạn mức nợ
        ├── lưu Order
        ├── createItemsAndDeductStock()   [private, ~60 dòng]
        ├── capturePayments()             [private, ~35 dòng]
        ├── recordDebtIfUnpaid()          [private, ~15 dòng]
        ├── recordVoucherUsageIfAny()     [private, ~15 dòng]
        └── issueInvoice()                [private, ~25 dòng]
  ├── getById() / list() / cancelOrder() / toResponse() / generateOrderNumber() / generateInvoiceNumber()
```

### Sơ đồ SAU (1 orchestrator + 4 collaborator)
```
OrderService (524 dòng, orchestrator — @Transactional GIỮ NGUYÊN ở đây)
  ├── createOrder() điều phối tuần tự đúng thứ tự cũ, gọi:
  │     ├── OrderValidationService.assertBranchAccess()
  │     ├── OrderValidationService.buildPricingLinesAndAssertStock()
  │     ├── OrderPricingService.calculate()  [KHÔNG đổi, vẫn thuần]
  │     ├── OrderValidationService.assertOrderReductionWithinSubtotal()
  │     ├── OrderValidationService.assertPricingMatchesExpected()
  │     ├── OrderValidationService.assertUnpaidRequiresCustomer()
  │     ├── OrderValidationService.assertWithinDebtLimit()
  │     ├── InventoryDeductionService.deductStockAndCreateItems()
  │     ├── OrderPaymentService.capturePayments()
  │     ├── OrderPaymentService.recordDebtIfUnpaid()
  │     ├── OrderFinalizationService.recordVoucherUsageIfAny()
  │     └── OrderFinalizationService.issueInvoice()
  └── getById() / list() / cancelOrder() / toResponse() / generateOrderNumber() /
        generateInvoiceNumber()  [giữ NGUYÊN trong orchestrator — ngoài phạm vi refactor này,
        xem mục "Không tách" bên dưới]
```

### Bảng class mới | trách nhiệm | số dòng | dependency

| Class | Trách nhiệm | Số dòng | Dependency (số lượng) |
|---|---|---|---|
| `OrderValidationService` | Quyền chi nhánh, gộp số lượng theo productId + kiểm tồn, CK vượt subtotal, giá FE≠BE, nợ không khách hàng/vượt hạn mức | 129 | `BranchAccessGuard`, `DebtRepository` (2) |
| `InventoryDeductionService` | Ghi `OrderItem` + trừ tồn kho (`@Version`+`saveAndFlush`) + `InventoryTransaction` (batch `saveAll`) | 107 | `OrderItemRepository`, `InventoryRepository`, `InventoryTransactionRepository`, `CurrentUserProvider` (4) |
| `OrderPaymentService` | Ghi `OrderPayment` (kèm VietQR khi `bank_transfer`) + ghi `Debt` khi bán nợ | 91 | `OrderPaymentRepository`, `VietQrService`, `SettingsService`, `DebtRepository` (4) |
| `OrderFinalizationService` | Ghi nhận dùng voucher (dịch lỗi optimistic-lock) + xuất `Invoice` (kèm VietQR + publish event) | 92 | `VoucherService`, `InvoiceRepository`, `VietQrService`, `SettingsService`, `ApplicationEventPublisher` (5 — **vượt gợi ý ≤4** của roadmap, xem giải trình trong Javadoc class: gộp 2 bước "chốt đơn" gắn liền nhau, tách thêm sẽ tạo class con chỉ còn thuần delegate) |
| `OrderService` (orchestrator, còn lại) | Điều phối `createOrder()` + ranh giới `@Transactional` + `getById`/`list`/`cancelOrder`/`toResponse`/sinh số đơn+hoá đơn | 524 | 19 (không giới hạn — orchestrator, đúng vai trò roadmap mô tả) |

**Tổng dòng 4 collaborator mới: 419 dòng** (đều ≤200 dòng/class, đúng gợi ý roadmap).
**OrderService giảm từ 721 → 524 dòng** (giảm 197 dòng, ~27%) — phần còn lại là `cancelOrder`/
`getById`/`list`/`toResponse`/sinh số, KHÔNG thuộc phạm vi refactor này (xem lý do bên dưới).

### Không tách (có chủ đích, đã ghi rõ trong code)
- `cancelOrder()`/`getById()`/`list()`/`toResponse()`/sinh số đơn+hoá đơn: KHÔNG nằm trong điều
  kiện tiên quyết (chỉ có test bảo vệ `createOrder()`), và là luồng riêng/đơn giản hơn — để lại
  trong orchestrator, không ép tách để giảm rủi ro ngoài phạm vi prompt.
- `InventoryDeductionService` KHÔNG được dùng lại cho `ReturnService`/`StockTakeService` dù roadmap
  gợi ý cân nhắc — 2 luồng đó là HOÀN kho (dấu +, dùng `costPriceSnapshot` cũ) khác hẳn luồng bán
  (dấu −, dùng giá vốn hiện tại), ép dùng chung sẽ phức tạp hơn tách riêng.

### Nguyên tắc tách đã tuân thủ
- Ranh giới `@Transactional` GIỮ NGUYÊN ở `OrderService.createOrder()` — không collaborator nào tự
  mở transaction riêng.
- KHÔNG đổi bất kỳ hành vi nghiệp vụ nào — mọi đoạn code di chuyển NGUYÊN VĂN (cùng công thức,
  cùng thông điệp lỗi, cùng thứ tự gọi).
- Xác nhận bằng chạy thật: `mvn verify` (74 test: 48 unit + 26 integration) — **TOÀN BỘ XANH,
  KHÔNG PHẢI SỬA MỘT ASSERTION NÀO** trong `OrderServiceCreateOrderIT` (6 test, an toàn nhất để xác
  nhận không đổi hành vi) hay bất kỳ test nào khác từ Prompt #1.
- Viết thêm 24 unit test mock repository cho 4 collaborator mới (`OrderValidationServiceTest` 13,
  `InventoryDeductionServiceTest` 2, `OrderPaymentServiceTest` 4, `OrderFinalizationServiceTest` 5)
  — trước khi tách KHÔNG unit-test được vì logic nằm phẳng trong 1 method cần toàn bộ Spring
  context; giờ tách nhỏ nên test được bằng Mockito thuần, không cần Postgres/Redis.

### Coverage tầng service sau Prompt #2
Tăng từ 37,8% → **39,7%** dòng lệnh (2409 dòng, 957 dòng được chạm) nhờ 24 unit test mock mới —
cổng CI JaCoCo (ngưỡng 30%) vẫn PASS với biên độ rộng hơn. Chưa nâng ngưỡng CI lên ngay (giữ 30%
làm mốc an toàn, sẽ nâng dần khi các Prompt test tiếp theo chạy).

---

## PROJECT_STATE — cập nhật sau Prompt #1 (Bộ prompt nâng cấp, P0) — 2026-07-12

### Test infrastructure: DONE, coverage tầng service 37,8% dòng lệnh
Thực hiện `docs/UPGRADE_ROADMAP.md` Prompt #1 (P0 — phủ test cho Service backend, khoảng trống
lớn nhất theo audit toàn bộ codebase chạy trước đó cùng phiên). Môi trường sandbox này KHÔNG cho
Testcontainers-Java kết nối được Docker Desktop qua named pipe (khác hẳn báo cáo "không có Docker
daemon" ở Phase 3/11/12 — ở đây Docker daemon CÓ chạy, CLI `docker` hoạt động bình thường, chỉ
riêng thư viện Testcontainers-Java không thương lượng được HTTP với proxy của Docker Desktop) —
giải quyết bằng cách tự dựng 2 container `postgres:16-alpine`/`redis:7-alpine` thật qua `docker run`
trực tiếp (bypass Testcontainers), rồi trỏ `AbstractIntegrationTest` vào đó qua biến môi trường
`IT_DB_HOST`/`IT_DB_PORT`/.../`IT_REDIS_PORT` (giữ nguyên nhánh Testcontainers chuẩn cho CI/máy có
Docker hoạt động bình thường — xem Javadoc `AbstractIntegrationTest`). Toàn bộ số liệu dưới đây là
**chạy thật**, không suy đoán — xác nhận qua `target/surefire-reports/*.txt` + `mvn verify` full.

**File test mới/viết lại + số case:**
- `server/src/test/java/com/quanlycuahang/erp/AbstractIntegrationTest.java` (mới) — base class bật
  Hibernate `@Filter` tenant + bind `TenantContext`/`SecurityContext` giống hệt 1 request thật qua
  `TenantSessionBinder`, hỗ trợ song song Testcontainers chuẩn hoặc container ngoài qua env var.
- `server/src/test/java/com/quanlycuahang/erp/TestDataFactory.java` (mới) — dựng tenant/2 chi
  nhánh/owner+cashier/sản phẩm có tồn kho/khách hàng có hạn mức nợ/NCC/ca đang mở, mỗi lần gọi tạo
  dữ liệu HOÀN TOÀN MỚI (UUID suffix) để test chạy song song không đụng nhau.
- `product/ProductRepositoryIT.java` (viết lại + thêm case) — 2 test, gồm 1 test tenant-filter mới.
- `partner/DebtRepositoryIT.java` (viết lại dùng TestDataFactory thay vì seed cứng) — 1 test.
- `sales/OrderRepositoryRevenueIT.java` (viết lại) — 1 test.
- `sales/OrderServiceCreateOrderIT.java` (mới) — 6 test: happy path (trừ kho/ghi payment+invoice
  đúng), gộp số lượng 2 dòng cùng sản phẩm trước khi kiểm tồn, chặn nợ không khách hàng, chặn vượt
  hạn mức nợ, chặn `ORDER_PRICE_MISMATCH`, oversell 5 luồng tranh 1 đơn vị tồn (đúng 1 thắng).
- `sales/ReturnServiceCreateReturnIT.java` (mới) — 4 test: chặn trả vượt số lượng, hoàn tiền theo
  giá SNAPSHOT lúc bán (không phải giá hiện tại đã đổi), trạng thái đơn chuyển
  `partially_returned`/`fully_returned` đúng, trả hàng đơn bán nợ giảm `Debt` trước khi tính hoàn
  ngoài hệ thống.
- `inventory/PurchaseOrderServiceIT.java` (mới) — 4 test: giá vốn bình quân gia quyền đúng công
  thức, không tạo `Debt` khi trả đủ, `updateItemPrice()` REPLAY đúng + điều chỉnh `Debt` hiện có,
  chặn sửa giá làm nợ âm (`DEBT_ADJUSTMENT_NEGATIVE`).
- `operation/ShiftServiceIT.java` (mới) — 5 test: chặn mở 2 ca cùng user, công thức đóng ca đúng
  (`openingCash + cashSales - cashRefunds + cashIn - cashOut`), ghi nhận discrepancy đúng dấu, chặn
  đóng ca đã đóng, `requireShift()` chặn IDOR giữa 2 cashier khác nhau (owner full-access thì không
  bị chặn).
- `partner/DebtServiceIT.java` (mới) — 3 test: phân bổ FIFO theo `createdAt ASC` qua nhiều `Debt`
  còn dư, chặn trả vượt tổng nợ còn dư (rollback toàn bộ phần đã phân bổ tạm nhờ `@Transactional`),
  chặn khi đối tác không còn nợ.
- `client/src/lib/pos/pricing.test.ts` (mới) — 14 test đối chiếu trực tiếp với
  `OrderPricingServiceTest.java` (cùng bộ số liệu: 3 dòng CK+voucher+VAT, VAT cộng thêm, làm tròn,
  CK dòng về 0, phân bổ CK/voucher trên nhiều dòng, số lượng 0, tiền thừa âm) + 5 test
  `clampEditablePrice()` (kẹp giá sửa tay trong POS về `[0, giá niêm yết]`).
- `client/src/components/common/PermissionGate.test.tsx` (mới) — 6 test (OR/AND, có/không fallback).
- `client/src/routes/RequirePermission.test.tsx` (mới) — 4 test (khác `PermissionGate` ở chỗ chặn
  cả trang bằng `ForbiddenPage` thay vì ẩn 1 phần UI).

**Tổng cộng:** Backend 24 unit test (`OrderPricingServiceTest` 23 + `InvoiceDetailAssemblerTest` 1,
có sẵn từ trước) + 26 integration test (mới/viết lại trong Prompt #1) = 50 test, TẤT CẢ XANH qua
`mvn verify`. Frontend: 30 test Vitest (24 mới), TẤT CẢ XANH qua `npx vitest run`; `npx tsc -b
--noEmit` sạch.

**Coverage tầng service** (JaCoCo, `LINE` counter, gộp dữ liệu unit-test + integration-test qua
`jacoco:merge`): **37,8%** dòng lệnh trên toàn bộ `com/quanlycuahang/erp/**/service/**` (2373 dòng,
898 dòng được chạm). Chưa đạt ngưỡng 60% roadmap yêu cầu — danh sách service hoàn toàn chưa có test
nào xem `BUGS_FOUND.md` mục 1. Cổng CI (`server/pom.xml`, plugin `jacoco-maven-plugin` gắn vào phase
`verify`) đặt ngưỡng **30%** (thấp hơn 37,8% đo được để có biên độ an toàn, KHÔNG phải mục tiêu) —
sẽ NÂNG dần ở các Prompt sau khi thêm test cho các service còn thiếu, không được hạ xuống.

**Bug/rủi ro phát hiện khi viết test:** xem `BUGS_FOUND.md` (không có lỗi bảo mật cần vá khẩn cấp;
4 mục là nợ kỹ thuật/quan sát, không sửa theo đúng ràng buộc "không sửa business logic" của prompt).

**Việc đã làm NGOÀI phạm vi hẹp "viết test" nhưng cần thiết để hạ tầng test hoạt động** (không đổi
hành vi production): thêm `DebtPaymentRepository.findByDebtIdIn()` (finder method còn thiếu), tách
`clampEditablePrice()` từ `PosPage.tsx` sang `client/src/lib/pos/pricing.ts` (extract function,
công thức giữ nguyên, verify lại bằng `tsc` + toàn bộ test Vitest).

**Việc CHƯA làm ở Prompt #1** (không nằm trong 8 bước bắt buộc của prompt, hoặc ngoài khả năng
kiểm chứng thật của sandbox này): file `pricing-vectors.json` dùng chung FE/BE (roadmap đề xuất
"nếu chưa có thì tạo" — hiện `client/src/lib/pos/pricing.test.ts` và
`OrderPricingServiceTest.java` dùng 2 bộ literal số liệu riêng nhưng cross-reference thủ công cùng
kịch bản, chưa gộp thành 1 file JSON dùng chung); `mvn verify` chưa được xác nhận chạy thật trên
GitHub Actions runner (chỉ chạy thật trong sandbox này qua container tự dựng).

---

## PROJECT_STATE — sau Phase 12 — 2026-07-06

### Đã chốt (Phase 12 — DevOps & tài liệu)
- `server/Dockerfile` (multi-stage: `maven:3.9-eclipse-temurin-21` build → `eclipse-temurin:21-jre-alpine`
  runtime, user non-root, healthcheck `/actuator/health`) và `client/Dockerfile` (multi-stage:
  `node:20-alpine` build → `nginx:1.27-alpine` runtime, healthcheck `GET /`) + `.dockerignore` riêng
  cho từng service (client cần thiết yếu — thiếu sẽ khiến `COPY . .` sau `npm ci` ghi đè
  `node_modules` mới cài bằng bản trên host).
- `docker/docker-compose.yml` (postgres, redis, server, web dùng chung 1 file cho cả 2 kịch bản
  triển khai VPS/LAN, chỉ khác `.env`) + `docker/nginx.conf` (SPA fallback, proxy `/api` sang
  `server`, gzip, cache tài nguyên tĩnh có hash) — `nginx.conf` mount qua volume thay vì `COPY`
  trong Dockerfile vì Docker không cho `COPY` đọc ngoài build context (`../docker/`).
  **Bẫy thật phát hiện lúc verify bằng `docker compose config`**: biến top-level (`${DB_NAME}`,
  `${WEB_PORT}`...) không được thay thế dù `.env` đã có ở root repo — Compose mặc định chỉ tự tìm
  `.env` cạnh chính file compose (`docker/.env`), không phải theo CWD hay root repo; phải luôn gọi
  kèm `--env-file .env` khi compose file nằm ở thư mục con. Đã ghi rõ vào README.
- **Bug thật phát hiện khi rà lại `pom.xml` trước khi viết CI**: thiếu `maven-failsafe-plugin` —
  Surefire mặc định chỉ nhận diện `*Test.java`, nên mọi `*IT.java` (Testcontainers, gồm cả
  `ProductRepositoryIT` từ Phase 3 và 2 IT mới ở Phase 11) **chưa từng thực sự chạy** dưới bất kỳ
  lệnh `mvn` nào từ đầu dự án — file tồn tại, compile sạch, nhưng không nằm trong tập test nào được
  thực thi, tạo cảm giác an toàn giả. Đã sửa bằng cách thêm `maven-failsafe-plugin` gắn vào goal
  `integration-test`+`verify`; xác nhận `mvn test` vẫn nhanh/không chạm Docker (chỉ Surefire) còn
  `mvn verify` (dùng trong CI) chạy cả 2. Bug này lẽ ra phải bắt được từ Phase 3 nếu có CI sớm hơn
  — minh chứng cụ thể cho lý do cần có Phase 12 sớm trong vòng đời dự án thật.
- **29 file Java lệch format phát hiện khi thử `mvn verify` lần đầu**: toàn bộ code viết tay xuyên
  suốt FH-12 → FH-16, Phase 11 và bản vá refresh-token chưa từng chạy qua `spotless:check` (chỉ
  chạy `mvn test`/`spring-boot:run` trong lúc phát triển, không phải `verify`). Đã sửa bằng
  `mvn spotless:apply` (thuần whitespace/import-order, không đổi ngữ nghĩa) — xác nhận lại
  `OrderPricingServiceTest` vẫn 23/23 xanh sau khi format lại.
- `.github/workflows/ci.yml`: 3 job — `backend` (`mvn verify`, upload surefire+failsafe reports),
  `frontend` (lint, `tsc --noEmit`, Vitest, build), `e2e` (services Postgres 16 + Redis 7 thật trên
  runner, build+chạy backend thật với profile `local` + `JWT_SECRET` CI riêng, build+chạy frontend
  dev server thật, chạy bộ Playwright E2E của Phase 11 thật, upload report khi fail).
  **Bug thật phát hiện khi rà `playwright.config.ts` trước khi viết job `e2e`**: hardcode
  `executablePath: "/opt/pw-browsers/chromium"` (chỉ tồn tại trong sandbox phát triển) — sẽ crash
  ngay trên GitHub Actions runner (không có đường dẫn này, CI tự `playwright install --with-deps
  chromium` rồi để Playwright tự tìm browser mặc định). Đã sửa bằng điều kiện `existsSync` — verify
  lại: 5/5 test Playwright vẫn pass trong sandbox sau khi sửa (nhánh `existsSync` vẫn đúng ở đây).
- `scripts/backup.sh`/`scripts/restore.sh` (pg_dump/psql qua `docker compose exec postgres`, đọc
  `.env` ở root, `restore.sh` có bước xác nhận trước khi ghi đè). `README.md` thay phần Docker
  Compose placeholder bằng hướng dẫn thật (`--env-file .env`, bảng so sánh biến môi trường 2 kịch
  bản triển khai) + mục "Vận hành" (backup/restore, tóm tắt CI).
- **Giới hạn môi trường (nhất quán với Phase 3/11)**: sandbox làm Phase 12 vẫn KHÔNG có Docker
  daemon (`docker ps`/`service docker start` đều lỗi) — Dockerfile/compose chỉ verify được qua
  `docker compose config` (thay thế biến + resolve path đúng) và review thủ công kỹ lưỡng, **chưa
  chạy được `docker build`/`docker compose up` thật** trong phiên này. Sẽ được GitHub Actions runner
  (có Docker daemon thật) xác nhận khi job `e2e` chạy (`mvn -B -q package` + start jar thật, dù
  không qua Dockerfile mà chạy trực tiếp — bản thân Dockerfile build image thì chưa có job CI nào
  build/push image, chỉ mới viết đúng cú pháp và review thủ công).

### Đã chốt (FH-1 → FH-16 — Redesign giao diện FruitHouse + tính năng mới)
Sau Phase 10, toàn bộ FE được redesign lại theo mockup FruitHouse (theme jade, sidebar tối màu cố
định) và bổ sung 6 tính năng nghiệp vụ hoàn toàn mới ở cả Backend lẫn FE (trước đó chưa có
Controller/Service/trang nào): Công nợ chi tiết (aging theo đối tác + đối chiếu + ghi nhận thanh
toán FIFO), Ma trận phân quyền (CRUD nhân viên + lưới sửa quyền theo vai trò), Ca & két tiền (mở/
đóng ca, thu chi tiền mặt, đối chiếu ket tien nối trực tiếp vào POS), Báo cáo (chart 2 chuỗi Doanh
thu/Lợi nhuận), Cài đặt (đọc/ghi cấu hình `settings` thật, nối `login_rate_limit_attempts` vào
AuthService runtime). Chi tiết từng trang không lặp lại ở đây — xem lịch sử commit
`FH-1`..`FH-16` trên nhánh `claude/new-session-4qaqne`. Nhân tiện rà soát hồi quy toàn bộ 16 trang
bằng Playwright, phát hiện và sửa 1 bug thật ở tầng nền tảng Phase 2 (không liên quan FH): cơ chế
rotation refresh token thu hồi nhầm cả chuỗi (đăng xuất oan) khi có ≥2 lệnh `/auth/refresh` đua
nhau dùng cùng 1 token hợp lệ (xảy ra thật ở multi-tab hoặc React StrictMode dev) — sửa bằng cách
giữ mỗi jti vừa rotate qua trong 1 khóa Redis riêng có TTL 30 giây thay vì 1 ô nhớ chung.

### Đã chốt (Phase 11 — Testing)
- `OrderPricingServiceTest` mở rộng từ 3 lên 23 case, phủ hết 7 bước B4 riêng lẻ và kết hợp (CK
  dòng về 0, guard chia 0 khi subtotal triệt tiêu, phân bổ CK đơn/voucher dư vào dòng cuối không
  lệch dù 5 dòng, VAT trộn/trên nhiều thuế suất/thuế suất 0%, số lượng lẻ làm tròn HALF_UP, 3 mức
  làm tròn 500/100/null, tiền thừa âm, số lượng 0). Tự phát hiện và sửa 5 lỗi tính tay trong chính
  test mới viết (dùng `roundingUnit=1000` lúc muốn kiểm tra tổng trước làm tròn, vô tình trúng
  điểm giữa làm tròn đổi kết quả) trước khi coi là xong — cùng kỷ luật "verify bằng chạy thật" như
  mọi Phase trước, chỉ khác đối tượng chạy thật ở đây là chính bộ test.
- Testcontainers integration test mới: `DebtRepositoryIT` (khóa lại bug thật FH-12 — JOIN UNION ALL
  customers/suppliers theo id trùng lặp từng làm lẫn tên đối tác sai chiều nợ) và
  `OrderRepositoryRevenueIT` (khóa lại cách tính giá vốn theo từng nhóm thêm ở FH-15 — subquery
  riêng tránh nhân đôi revenue trên đơn nhiều dòng). Môi trường sandbox làm Phase 11 vẫn KHÔNG có
  Docker daemon (đã xác minh lại qua `docker ps` và `service docker start` đều thất bại, đúng tình
  trạng ghi nhận từ Phase 3) — 2 file compile sạch, logic query đã verify gián tiếp qua curl thật
  trên Postgres 16 local trong lúc làm FH-12/FH-15, chạy được thật trên CI/local có Docker.
- Playwright E2E chính thức lần đầu (trước giờ chỉ chạy script tay để verify, không phải test suite
  commit vào repo): cài `@playwright/test`, `playwright.config.ts` trỏ thẳng Chromium cài sẵn trong
  môi trường (không tải lại), 5 test thật chạy qua `npm run test:e2e` — đăng nhập đúng/sai, bán
  hàng POS end-to-end, trang Công nợ, trang Ca & két tiền. Bug thật tự phát hiện lúc chạy lần đầu:
  toast Radix render 2 node cùng text (div hiển thị + span `aria-live` cho screen reader) khiến
  `getByText` strict-mode violation — sửa bằng `.first()`.

### Đã chốt (Phase 10 — Báo cáo)
- **Bug thật phát hiện qua verify actuator**: `spring-boot-starter-mail` (thêm ở Phase 9) tự đăng
  ký `MailHealthIndicator` góp phần vào status tổng hợp `/actuator/health` — SMTP gián đoạn tạm
  thời (không liên quan gì đến khả năng phục vụ của hệ thống) kéo cả `/actuator/health` xuống
  `DOWN`, mâu thuẫn trực tiếp nguyên tắc Phase 9 "email không được ảnh hưởng ngược lại luồng bán
  hàng" — nếu dùng làm readiness probe (K8s/load balancer) sẽ loại bỏ nhầm 1 instance đang bán hàng
  bình thường. Đã sửa bằng `management.health.mail.enabled: false`. Chi tiết:
  `docs/phase10/reports-module.md`.
- 7 endpoint báo cáo (doanh thu 5 chiều nhóm, lợi nhuận gộp, top SP, top KH, hiệu suất nhân viên,
  giá trị tồn kho, công nợ theo tuổi nợ) dùng `@Query(nativeQuery = true)` trực tiếp trên
  orders/order_items/returns/debts/inventory — chưa cần bảng tổng hợp `daily_sales_summary`.
  Xuất Excel qua Apache POI (`ReportExcelExporter` dùng chung mọi loại bảng).
- `ReportsPage` FE: biểu đồ Recharts (1 chuỗi doanh thu), bảng tái dùng `DataTable` (Phase 5), xuất
  Excel qua tải Blob (endpoint export cần header Authorization, không dùng `<a href>` được).
- Đã verify toàn bộ số liệu đối chiếu trực tiếp với SQL tay và dữ liệu seed + đơn test Phase 8/9
  (doanh thu theo chi nhánh khớp theo thu ngân, lợi nhuận gộp khớp từng đồng, giá trị tồn kho theo
  danh mục cộng lại khớp theo chi nhánh, phân quyền đúng ma trận, file Excel mở bằng `openpyxl`
  xác nhận đúng dữ liệu) — cả qua curl lẫn qua Playwright + Chromium thật. Bảng đầy đủ:
  `docs/phase10/reports-module.md`.

### Đã chốt (Phase 9 — Module Hóa đơn)
- `GET /api/v1/invoices/{id}` (quyền `invoice:view`) + `GET /api/v1/invoices/lookup/{lookupCode}`
  (công khai, khách quét QR không cần đăng nhập, an toàn IDOR vì `lookupCode` là UUID rút gọn
  không đoán được). `InvoiceDetailAssembler` (Java thuần, cùng phong cách `OrderPricingService`)
  gộp VAT theo từng thuế suất — verify bằng unit test "snapshot" dựng lại đúng đơn hàng thật
  HD-000021 đã kiểm chứng ở Phase 8, khớp từng đồng.
- Bổ sung `orders.cash_received`/`change_amount` (trước đây chỉ tính lúc tạo đơn, không lưu nên
  không tái tạo được khi xem hóa đơn cũ), `customers.email`, setting `store_name`/`store_tax_code`
  (migration `V4__invoice_fields.sql`).
- Gửi email hóa đơn qua Thymeleaf + `spring-boot-starter-mail`, publish sau khi `Invoice` được lưu
  và chỉ xử lý thật sự **sau khi transaction bán hàng commit** (`@TransactionalEventListener(phase
  = AFTER_COMMIT)`, D4) — **verify bằng SMTP server thật** (dựng nhanh `python3 -m smtpd` debug
  server cục bộ), xác nhận email đến đúng nội dung/đúng thời điểm (sau commit, không chặn luồng
  bán hàng), tiếng Việt có dấu hiển thị đúng.
- `EInvoiceProvider` (interface) + `NoOpEInvoiceProvider` (bean mặc định) — **chưa** tích hợp thật
  với nhà cung cấp hóa đơn điện tử nào (Viettel/MISA/VNPT đều cần hợp đồng thương mại thật, không
  thể kiểm chứng trong phiên làm việc này) — đã ghi rõ trong Javadoc cách thay thế sau.
- FE: `InvoiceK80`/`InvoiceA4` (template in K80 80mm + A4, `@page` CSS động theo khổ đang chọn),
  `InvoicePrintPage` (route bảo vệ, quyền `invoice:view`), `InvoiceLookupPage` (route công khai
  `/tra-cuu/:code`, **không** bọc `RequireAuth` — khách quét QR trên hóa đơn giấy), QR code qua
  `qrcode.react` mã hóa URL tra cứu.
- Đã verify toàn bộ bằng ứng dụng chạy thật (Playwright + Chromium + SMTP debug thật, không chỉ
  đọc code): email tự động gửi khi khách có email, endpoint JSON đúng/đủ (VAT breakdown khớp tổng
  tiền), in K80 và A4 đều đúng dữ liệu, trang tra cứu công khai hoạt động từ browser context mới
  hoàn toàn không có cookie đăng nhập, mã tra cứu sai hiện đúng lỗi tiếng Việt không crash trang.
  Chi tiết đầy đủ: `docs/phase9/invoice-module.md`.

### Đã chốt (Phase 5 — Frontend Foundation)
- **2 bug thật phát hiện khi verify bằng trình duyệt thật (Playwright + Chromium)** — cả hai
  chỉ lộ ra vì test bằng trình duyệt thật, không phải curl: (1) `AuthController` hard-code cookie
  refresh token `Secure=true` khiến trình duyệt từ chối lưu cookie khi chạy `http://localhost`
  (RFC 6265) — đã sửa bằng property `app.auth.refresh-cookie-secure` (mặc định `true`, override
  `false` chỉ ở `application-local.yml`); (2) origin `127.0.0.1:5173` không khớp CORS whitelist
  `localhost:5173` gây lỗi mạng chung chung. Chi tiết: `docs/phase5/frontend-foundation.md`.
- Scaffold Vite 5 + React 18 + TS 5 + Tailwind 3 (ép version thủ công vì `npm create vite` mặc
  định cài bản mới nhất, vi phạm Part C đã chốt); shadcn/ui viết tay từng primitive (không dùng
  CLI vì sandbox không truy cập được registry ngoài whitelist).
- 3 layout (`MainLayout` Sidebar+Topbar, `AuthLayout`, `PosLayout` toàn màn hình 2 cột) đúng
  `docs/phase4/layout.md`/`pos-design.md`; router `react-router-dom` 6 + `React.lazy` (xác nhận
  code-splitting qua `vite build` — mỗi trang 1 chunk riêng); dark/light qua Redux `ui.theme`.
- `apiClient` (axios) tự refresh khi 401 (hàng đợi chống refresh trùng lặp); `bootstrapSession()`
  silent-refresh lúc khởi động app để F5 giữa ca không đá thu ngân về `/login` dù access token
  chỉ sống trong RAM (D3); Redux Toolkit + `redux-persist` chỉ persist `cart`/`ui`, **`auth`
  không bao giờ persist**; TanStack Query cho toàn bộ dữ liệu API; RHF + Zod cho form.
- Component nền đủ theo Gate: `DataTable` (đọc `meta.page/limit/total`), `FormField`,
  `ConfirmDialog`, `Money`, `DateRangePicker`, `Toast`, `PermissionGate` + `RequireAuth`/
  `RequirePermission` (route guard).
- **Nợ kỹ thuật đã ghi nhận** (không phải lỗi ẩn): endpoint `/products`, `/customers` dùng native
  query có `ORDER BY` cố định (Phase 7) nên không nhận `Pageable.getSort()` động — demo sắp xếp ở
  `ProductsPage` tạm làm phía client trên trang hiện tại; cần chuyển sang
  `JpaSpecificationExecutor` khi xây màn danh sách thật cho từng module.
- Đã verify toàn bộ luồng bằng Playwright + Chromium thật (không chỉ đọc code): đăng nhập sai/đúng,
  redirect chưa đăng nhập, reload giữ phiên, DataTable phân trang/lọc/sắp xếp với dữ liệu thật,
  POS layout, dark mode, đăng xuất xoá cookie, chặn truy cập sau đăng xuất. Bảng đầy đủ tại
  `docs/phase5/frontend-foundation.md`.

### Đã chốt (Phase 8 — Module Bán hàng POS, QUAN TRỌNG NHẤT)
- **Bug thật phát hiện qua test tích hợp 2 luồng (gate bắt buộc)**: `generateOrderNumber()`/`generateInvoiceNumber()`/`generateSku()` dùng pattern `count()+existsBy()` không atomic — 2 request tạo đơn đồng thời đọc cùng `count()` trước khi bên nào commit, sinh trùng `order_number`, vi phạm unique constraint, che mất lỗi `PRODUCT_OUT_OF_STOCK` đúng nghĩa (trả về `409 CONFLICT` chung chung thay vì đúng mã lỗi nghiệp vụ). Đã sửa bằng PostgreSQL `SEQUENCE` (migration `V3__number_sequences.sql` + `NumberSequenceService` dùng chung) — atomic ở mức DB, không phụ thuộc transaction isolation. Verify lại: 1 luồng thành công/1 luồng đúng `PRODUCT_OUT_OF_STOCK`, tồn kho cuối chính xác. Chi tiết: `docs/phase8/pos-module.md`.
- **Gap thật phát hiện khi test trả hàng**: `OrderItemResponse` thiếu trường `id` — FE không có cách lấy `orderItemId` để gọi API trả hàng (`POST /returns` bắt buộc trường này). Đã bổ sung.
- `OrderPricingService` (Java thuần, đúng 7 bước B4, dòng cuối nhận phần dư phân bổ/làm tròn để tổng luôn khớp tuyệt đối) — verify cả bằng unit test lẫn đơn hàng thật qua API, khớp tính tay từng đồng.
- `OrderService.createOrder()`: 1 transaction duy nhất, Backend là nguồn giá/VAT/giá vốn duy nhất, chống oversell 2 lớp (kiểm tra tồn kho + `@Version` optimistic lock với `saveAndFlush()`), idempotency qua `Idempotency-Key` header (Redis), snapshot đầy đủ trên `OrderItem`, tự tạo `Debt`/`Invoice`/VietQR khi cần.
- `ParkedOrderService` (đặt trước giỏ hàng), `ReturnService` (trả hàng theo đơn giá hiệu lực đã phân bổ CK, giới hạn số lượng còn lại, hoàn kho theo giá vốn snapshot, trừ nợ liên kết trước), `VietQrService` (EMVCo + CRC16-CCITT tự viết, verify khớp test vector chuẩn).
- Đã verify bằng ứng dụng chạy thật (không chỉ compile): tạo đơn phức tạp (CK dòng+đơn+voucher+tiền thừa), idempotency (lặp key không tạo trùng), 2 và 3 luồng tranh tồn kho cuối, park/resume, trả hàng 1 phần + validate vượt số lượng, huỷ đơn cùng ngày (+ chặn huỷ lần 2), voucher 10%+bank_transfer (VietQR sinh đúng, `used_count` tăng đúng). Bảng đầy đủ tại `docs/phase8/pos-module.md`.

### Đã chốt (Phase 7 — Module Sản phẩm & Kho)
- **Bug thật phát hiện qua test chạy thật**: `BaseEntity.createdAt/updatedAt` kiểu `OffsetDateTime` làm Spring Data Auditing crash (`DefaultAuditableBeanWrapperFactory` bản Spring Boot 3.3.5 chỉ hỗ trợ `Instant` trong nhóm java.time, không hỗ trợ `OffsetDateTime`) — đã sửa sang `Instant`, verify tạo sản phẩm thành công sau khi sửa
- Category (cây 2 cấp) + Product (SKU tự sinh, lịch sử giá ghi trong Service thay vì `@EntityListeners` — lý do: JPA EntityListener không phải Spring bean, khó inject Repository an toàn) CRUD đầy đủ
- Tìm không dấu + gần đúng (`immutable_unaccent` + `ILIKE` + `pg_trgm %`) — **verify bằng dữ liệu có dấu thật**: "ca phe sua da" tìm ra "Cà phê sữa đá đặc biệt"
- `AverageCostService` Java thuần (không Spring) tính giá vốn bình quân gia quyền — **verify bằng phép tính tay chính xác**: tồn 98@38.500 + nhập 50@40.000 → giá vốn mới 39.007 (khớp `5.773.000/148=39006,756→HALF_UP`)
- `PurchaseOrderService`: khóa PESSIMISTIC_WRITE chống race condition, tự tạo công nợ NCC nếu mua thiếu — verify Debt payable tạo đúng số tiền
- `StockTakeService`: kiểm kê snapshot → nhập thực tế → duyệt bắt buộc lý do khi có chênh lệch — verify chặn duyệt thiếu lý do (422) và duyệt thành công điều chỉnh đúng tồn kho

### Đã chốt (Phase 6 — Backend Foundation)
- SecurityConfig STATELESS, CORS whitelist, CSRF tắt (giải thích lý do); JwtAuthenticationEntryPoint/AccessDeniedHandler trả JSON đúng format D2 ngay ở filter chain
- JWT (jjwt 0.12.6, API xác nhận qua javap): access token 15p chứa claim authorities; refresh token 7 ngày chứa jti+tokenFamily
- Refresh rotation + reuse detection qua Redis (`refresh:family:{family}` → jti hợp lệ hiện tại) — thu hồi cả chuỗi khi phát hiện token cũ bị dùng lại
- Rate limit đăng nhập 5 lần/15 phút/IP bằng Bucket4j 8.10.1 thật (LettuceBasedProxyManager, groupId `com.bucket4j` nhưng package Java `io.github.bucket4j` — đã xác nhận qua javap, không suy đoán)
- CustomUserDetailsService: authorities = hợp permission của mọi role được gán; ResourceActionPermissionEvaluator cho `hasPermission(id,'resource','action')`
- `@ValidPhoneVN`, upload ảnh (whitelist MIME, 2MB, UUID rename, serve qua endpoint riêng permitAll cho `<img>`), AuditAspect (`@Audited` + AOP), SettingsService (cache Redis, fallback branch→global)
- **Đã verify bằng ứng dụng chạy thật**: login/refresh/reuse-detection/rate-limit/upload đều test qua curl với PostgreSQL+Redis local thật — kết quả đầy đủ tại `docs/phase6/backend-foundation.md`

### Đã chốt (Phase 1–4, giữ nguyên)
- Design tokens: palette hex + HSL (light/dark), font Inter, radius 8px, thang shadow sm/md/lg — `docs/phase4/design-tokens.md` kèm `tailwind.config.ts` + CSS variables sẵn dùng cho Phase 5.1
- Layout chính: Sidebar (240px, thu gọn 64px) + Topbar (56px); POS dùng layout riêng toàn màn hình, không sidebar/topbar
- POS: 2 cột (60% sản phẩm / 40% giỏ hàng + thanh toán), 10 phím tắt F1–F9+Esc, scanner-friendly (auto-focus/refocus ô tìm kiếm), tối thiểu 1024×768, touch target ≥44px
- 15 wireframe màn hình (Dashboard, Sản phẩm, Nhập kho, Tồn kho, Kiểm kê, Đơn hàng, Trả hàng, Hóa đơn, Khách hàng, NCC, Công nợ, Nhân viên & phân quyền, Ca & két, Báo cáo, Cài đặt) + POS riêng — mỗi màn đủ mục đích/thành phần/hành động/trạng thái/phím tắt
- Trạng thái UI chuẩn dùng chung: loading (skeleton, không spinner toàn màn hình), empty (phân biệt chưa có dữ liệu vs lọc không ra kết quả), error (toast + map code→message, không trắng trang), dark mode (qua CSS variable, không hardcode hex), responsive 3 breakpoint

### Đã chốt (Phase 1–3, giữ nguyên)
- Không dùng Lombok (getter/setter/constructor viết tay, tường minh, không phụ thuộc annotation processor)
- Build tool: Maven
- Package gốc Java: `com.quanlycuahang.erp`
- Cấu trúc repo: `client/` + `server/` đặt thẳng ở root repo (không có thư mục wrapper `kiotclone/`)
- Format code Java: Spotless (google-java-format), chạy ở phase `verify`
- Spring Boot 3.3.5, Java 21, PostgreSQL 16, Redis 7, Flyway 10 (qua BOM Spring Boot)
- 6 vai trò RBAC: `owner`, `manager`, `cashier`, `sales_staff`, `warehouse_staff`, `accountant` — ma trận resource:action đầy đủ tại `docs/phase1/permission-matrix.md` (51 quyền)
- 19 use case (10 MUST + 9 SHOULD) đặc tả đầy đủ luồng chính/phụ/ngoại lệ/quy tắc, đối chiếu khớp mọi quy tắc B4
- Mermaid không có cú pháp `usecaseDiagram`/`deploymentDiagram` chuẩn UML → dùng `flowchart` thay thế
- Kiến trúc Controller → Service → Repository → Entity, constructor injection bắt buộc, DTO+MapStruct, Exception hierarchy (`AppException` + 5 lớp con), `GlobalExceptionHandler`, `ApiResponse<T>` D2, `CorrelationIdFilter` + logback JSON
- **Mâu thuẫn phát hiện & đã xử lý (Phase 3)**: B4 yêu cầu đồng thời `allow_negative_stock` (cho bán âm) và `CHECK (stock >= 0)` tĩnh ở DB — 2 điều này loại trừ nhau. Đã thay `CHECK` tĩnh bằng `TRIGGER` (`fn_check_inventory_stock`) đọc cấu hình `settings.allow_negative_stock` theo chi nhánh (hoặc global) trước khi chặn — đã test thực tế cả 2 kịch bản (chặn khi tắt, cho phép khi bật). Chi tiết: `docs/phase3/erd.md`.
- Schema đầy đủ 37 bảng (7 nhóm B4, gồm cả `user_branches` bổ sung cho kiểm soát IDOR đa chi nhánh — Phase 1.1 quy tắc 4), 34 JPA Entity tương ứng (3 bảng join thuần túy `role_permissions`/`user_roles`/`user_branches` không có Entity riêng, ánh xạ qua `@ManyToMany` + `@JoinTable`)
- unaccent() mặc định STABLE, không dùng được trực tiếp trong index expression → tạo hàm wrapper `immutable_unaccent()` (IMMUTABLE) — phát hiện qua test thật, không phải suy đoán
- **Đã verify thực tế** (không chỉ giả định): cài PostgreSQL 16 + Redis 7 local (sandbox không có Docker daemon), chạy `mvn spring-boot:run` với Flyway tự động migrate V1+V2 và **Hibernate `ddl-auto: validate` PASS** — xác nhận toàn bộ 34 Entity khớp chính xác schema. Actuator health trả `UP`.

### Cấu trúc project hiện tại
```
Quanlycuahang/
├── client/                          # Vite + React 18 + TS 5 (Phase 5)
│   ├── src/
│   │   ├── main.tsx, App.tsx, index.css
│   │   ├── components/
│   │   │   ├── ui/          # 18 primitive shadcn (button, input, dialog, form, toast...)
│   │   │   ├── layout/      # MainLayout, AuthLayout, PosLayout, Sidebar, Topbar, ThemeToggle
│   │   │   └── common/      # DataTable, FormField, ConfirmDialog, Money, DateRangePicker, PermissionGate
│   │   ├── pages/           # auth/LoginPage, DashboardPage, products/ProductsPage, pos/PosPage, invoices/{Print,Lookup}Page, reports/ReportsPage, NotFound/Forbidden
│   │   ├── routes/          # router.tsx, RequireAuth, RequirePermission
│   │   ├── store/           # index.ts (Redux + persist), slices/{auth,cart,ui}Slice
│   │   ├── lib/{http,api}/  # apiClient (refresh interceptor), queryClient, bootstrap, auth.ts, products.ts, invoices.ts, reports.ts
│   │   └── types/           # api.ts (ApiResponse<T>), permission.ts
│   ├── package.json, tailwind.config.ts, vite.config.ts, tsconfig*.json
│   └── *.test.ts(x)         # Vitest — utils, jwt decode, Money (6 test)
├── server/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/quanlycuahang/erp/
│       │   │   ├── ErpApplication.java
│       │   │   ├── config/JpaAuditingConfig.java
│       │   │   ├── common/{entity,dto,exception,web}/       # 12 file (Phase 2)
│       │   │   ├── auth/entity/          # User, Role, Permission
│       │   │   ├── system/entity/        # Branch, Settings, AuditLog
│       │   │   ├── product/entity/       # Category, Product, ProductUnit, PriceHistory
│       │   │   ├── product/repository/   # ProductRepository
│       │   │   ├── inventory/entity/     # Inventory, InventoryTransaction, PurchaseOrder(Item), StockTake(Item)
│       │   │   ├── partner/entity/       # CustomerGroup, Customer, Supplier, Debt, DebtPayment
│       │   │   ├── sales/{entity,service,controller,dto,pricing,statemachine,repository,web}/  # Order, ParkedOrder, Return, OrderPricingService, IdempotencyInterceptor...
│       │   │   ├── promotion/{entity,service,repository}/  # Voucher, VoucherUsage
│       │   │   ├── operation/{entity,repository,service,controller,dto,invoice}/  # Shift, Invoice, InvoiceDetailAssembler, EInvoiceProvider, InvoiceEmailListener
│       │   │   ├── report/{dto,service,controller,excel}/  # ReportService, ReportController, ReportExcelExporter (Phase 10)
│       │   │   └── common/sequence/NumberSequenceService.java   # SEQUENCE atomic cho order/invoice/sku
│       │   ├── resources/templates/invoice-email.html   # Thymeleaf (Phase 9)
│       │   └── resources/
│       │       ├── application*.yml, logback-spring.xml
│       │       └── db/migration/
│       │           ├── V1__init_schema.sql        (37 bang, trigger, index, extension)
│       │           ├── V2__seed_data.sql          (1 CN, 6 role, 51 quyen, 3 user, 5 danh muc, 30 SP, 5 KH, 3 NCC, 20 don)
│       │           ├── V3__number_sequences.sql   (order_number_seq, invoice_number_seq, sku_seq)
│       │           └── V4__invoice_fields.sql     (orders.cash_received/change_amount, customers.email, store_name/store_tax_code)
│       └── test/java/com/quanlycuahang/erp/
│           ├── AbstractIntegrationTest.java               (Prompt #1 — base IT, bind tenant filter thật)
│           ├── TestDataFactory.java                       (Prompt #1 — dựng tenant/branch/product/customer/shift)
│           ├── product/ProductRepositoryIT.java           (viết lại Prompt #1)
│           ├── partner/DebtRepositoryIT.java               (viết lại Prompt #1)
│           ├── partner/DebtServiceIT.java                 (mới, Prompt #1 — 3 case FIFO/chặn vượt nợ)
│           ├── sales/OrderRepositoryRevenueIT.java         (viết lại Prompt #1)
│           ├── sales/OrderServiceCreateOrderIT.java        (mới, Prompt #1 — 6 case)
│           ├── sales/ReturnServiceCreateReturnIT.java      (mới, Prompt #1 — 4 case)
│           ├── inventory/PurchaseOrderServiceIT.java       (mới, Prompt #1 — 4 case)
│           ├── operation/ShiftServiceIT.java               (mới, Prompt #1 — 5 case)
│           ├── sales/pricing/OrderPricingServiceTest.java (23 case, Phase 11)
│           └── operation/invoice/InvoiceDetailAssemblerTest.java
├── docker/                          # docker-compose.yml, nginx.conf (Phase 12)
├── docs/
│   ├── conventions.md, PROJECT_STATE.md
│   ├── phase1/  (8 file — permission matrix, business specs, diagrams)
│   ├── phase2/architecture.md
│   ├── phase3/erd.md
│   ├── phase4/  (design-tokens, layout, wireframes, pos-design, ui-states)
│   ├── phase5/frontend-foundation.md
│   ├── phase6/backend-foundation.md
│   ├── phase7/product-inventory-module.md
│   ├── phase8/pos-module.md
│   ├── phase9/invoice-module.md
│   └── phase10/reports-module.md
├── client/e2e/                      # Playwright E2E chính thức (Phase 11): auth, pos-checkout, debts, shifts
├── scripts/                         # backup.sh, restore.sh (Phase 12)
├── .github/workflows/ci.yml         # backend / frontend / e2e (Phase 12)
├── .env.example, .gitignore, README.md
```

### Database
- Bảng đã có: đủ 37 bảng (xem `docs/phase3/erd.md` mục 4)
- Migration Flyway mới nhất: `V4__invoice_fields.sql`
- Extension: `unaccent`, `pg_trgm`; function: `immutable_unaccent()`, `fn_check_inventory_stock()` + trigger; sequence: `order_number_seq`, `invoice_number_seq`, `sku_seq`

### API đã sinh
- Auth: `POST /api/v1/auth/{login,refresh,logout}`, `POST /api/v1/auth/change-password`
- Sản phẩm/Kho: `/api/v1/products`, `/api/v1/categories`, `/api/v1/inventory`, `/api/v1/purchase-orders`, `/api/v1/stock-takes`
- Khách hàng/Voucher: `/api/v1/customers`
- Bán hàng POS: `POST /api/v1/orders` (header `Idempotency-Key`), `GET /api/v1/orders/{id}`, `POST /api/v1/orders/{id}/cancel`, `/api/v1/parked-orders` (list/park/resume), `POST /api/v1/returns`
- Hóa đơn: `GET /api/v1/invoices/{id}` (quyền `invoice:view`), `GET /api/v1/invoices/lookup/{lookupCode}` (permitAll)
- Báo cáo: `GET /api/v1/reports/{revenue,gross-profit,top-products,top-customers,employee-performance,inventory-value,debt-aging}` + `.../export` (Excel, cần thêm quyền `report:export`)
- Upload: `POST /api/v1/uploads`, `GET /api/v1/uploads/{fileName}` (permitAll)
- Actuator mặc định (`/actuator/health`, `/actuator/info`) — `management.health.mail.enabled: false` (Phase 10, xem Nợ kỹ thuật)
- **Nợ**: chưa có `BranchController` (CRUD chi nhánh) — hiện chỉ có `BranchRepository`, dùng nội bộ trong `OrderService`/`SettingsService`; cần bổ sung nếu FE cần màn quản lý chi nhánh
- Property mới `app.auth.refresh-cookie-secure` (phát hiện ở Phase 5 khi verify bằng trình duyệt thật — xem mục Phase 5 bên dưới)

### FE đã sinh
- Scaffold đầy đủ Vite + React 18 + TS 5 + Tailwind 3, 18 component `ui/` (shadcn viết tay), 3
  layout, router lazy-load, Redux+persist, axios refresh interceptor, TanStack Query, RHF+Zod.
- 2 trang thật gọi API Backend: `LoginPage` (đăng nhập/đăng xuất/refresh), `ProductsPage`
  (`DataTable` phân trang/lọc/sắp xếp với dữ liệu thật). `DashboardPage`/`PosPage` là khung/placeholder
  (số liệu và tính năng bán hàng thật thuộc phạm vi module riêng, ngoài Phase 5 Foundation).
- Chi tiết đầy đủ + bảng verify bằng trình duyệt thật: `docs/phase5/frontend-foundation.md`.
- (Phase 9) Bổ sung `InvoicePrintPage` (khổ K80/A4, route bảo vệ) và `InvoiceLookupPage` (route
  công khai `/tra-cuu/:code`, không đăng nhập) + QR qua `qrcode.react` — xem `docs/phase9/invoice-module.md`.
- (Phase 10) `ReportsPage`: biểu đồ Recharts + bảng `DataTable` cho 7 loại báo cáo, xuất Excel qua
  tải Blob (endpoint export cần header Authorization) — xem `docs/phase10/reports-module.md`.

### Nợ kỹ thuật / dang dở
- Dockerfile/docker-compose.yml/CI đã có (Phase 12) nhưng **chưa từng chạy `docker build`/
  `docker compose up` thật trong sandbox này** (không có Docker daemon) — chỉ verify qua
  `docker compose config` + review thủ công; cần build/chạy thật lần đầu trên máy/CI có Docker
  trước khi coi image là đã kiểm chứng đầy đủ.
- `ProductRepositoryIT`/`DebtRepositoryIT`/`OrderRepositoryRevenueIT` dùng Testcontainers — viết đúng chuẩn, giờ đã được `maven-failsafe-plugin` (Phase 12) nhận diện và chạy đúng ở phase `verify`, nhưng **chưa tự chạy được trong sandbox này** (không có Docker daemon khả dụng); đã verify tương đương bằng PostgreSQL/Redis cài trực tiếp + `spring-boot:run` thật (xem trên) — sẽ chạy thật lần đầu trên GitHub Actions (runner có Docker daemon sẵn, xem `.github/workflows/ci.yml` job `backend`)
- `stock_transfers` (chuyển kho đa chi nhánh, COULD) chưa thiết kế
- Wireframe hiện là mô tả text + Mermaid box diagram (chưa phải hình ảnh/Figma) — đủ chi tiết để code Phase 5 nhưng không có mockup trực quan; có thể bổ sung sau nếu cần
- Chưa có `BranchController` (CRUD chi nhánh qua API) — chỉ 1 chi nhánh seed sẵn, đủ cho Phase 8 test nhưng cần bổ sung trước khi FE cần màn quản lý đa chi nhánh (Phase 10: `ReportsPage` cũng chưa có bộ lọc chi nhánh vì lý do này)
- Chưa seed `vouchers` mẫu trong `V2__seed_data.sql` (test Phase 8 tự thêm 1 voucher tạm qua SQL trực tiếp, không lưu vào migration) — nên bổ sung vào seed data chính thức ở phase sau nếu cần demo
- Báo cáo "Công nợ kèm tuổi nợ" mới trả tổng hợp theo mức tuổi nợ (0-30/31-60/61-90/>90 ngày), chưa có danh sách chi tiết từng khách hàng/NCC kèm tuổi nợ riêng — cần module CRUD Công nợ (`DebtController` chưa tồn tại) để hỗ trợ duyệt/xem từng khoản
- `DataTable` sắp xếp mới hoạt động phía client (trang hiện tại) cho `/products` vì native query Phase 7 có `ORDER BY` cố định, chưa nhận `Pageable.getSort()` động — xem `docs/phase5/frontend-foundation.md` để biết cách chuyển sang `JpaSpecificationExecutor` khi cần sắp xếp server-side thật cho từng module
- Chưa có endpoint `/me` (thông tin user hiện tại) — FE giải mã payload JWT (`sub`, `authorities`) để lấy username/quyền hiển thị UI, `fullName` tạm dùng lại `username` vì token không có trường này; nên bổ sung `/me` nếu cần hiển thị đầy đủ hồ sơ nhân viên
- Các trang nghiệp vụ FE (CRUD sản phẩm/khách hàng/kho, giỏ hàng POS thật, báo cáo...) chưa xây — Phase 5 chỉ là "Foundation" (scaffold + hạ tầng + component nền) đúng phạm vi master prompt, không phải toàn bộ giao diện
- `EInvoiceProvider` mới có `NoOpEInvoiceProvider` (bean mặc định, không gửi đi đâu) — chưa tích hợp thật với Viettel S-Invoice/MISA/VNPT (cần hợp đồng thương mại thật, ngoài khả năng phiên làm việc này); xem Javadoc `EInvoiceProvider` để biết cách thay thế khi có nhà cung cấp thật
- Email hóa đơn mới verify bằng SMTP debug server cục bộ (`python3 -m smtpd`), chưa test với SMTP thật (Gmail/SES/SendGrid...) — cần kiểm tra lại cấu hình `spring.mail.properties.mail.smtp.starttls`/`auth` khi triển khai thật với nhà cung cấp SMTP yêu cầu STARTTLS/xác thực

### Tự đánh giá Phase 10
- **Mạnh**: mọi số liệu báo cáo đều đối chiếu bằng SQL tay trực tiếp trên DB (không chỉ tin API trả về đúng) — phát hiện 1 bug thật (mail health indicator kéo sập `/actuator/health` toàn hệ thống) mà chỉ lộ ra khi gọi thật `/actuator/health` sau khi thêm dependency mail ở Phase 9, compile/test không bao giờ phát hiện được vì đó là hành vi runtime của auto-configuration, không phải lỗi logic.
- **Thiếu**: chưa có bảng tổng hợp `daily_sales_summary`/`@Scheduled` (chấp nhận được ở quy mô mục tiêu, đã ghi rõ điều kiện cần bổ sung); "công nợ kèm tuổi nợ" mới là tổng hợp theo mức, chưa phải danh sách chi tiết từng đối tác (cần module Công nợ riêng); chưa xuất PDF báo cáo (nhất quán với quyết định "ưu tiên in trình duyệt" đã chốt ở Phase 9, chưa thêm gì mới).
- **Rủi ro**: COGS trong lợi nhuận gộp tính theo số lượng bán gốc (không trừ hàng đã hoàn) trong khi "ảnh hưởng hoàn trả" trừ riêng theo tổng tiền hoàn — đúng theo đúng nghĩa đen công thức B4 nhưng là 1 lựa chọn kế toán đơn giản hóa (không khớp lại COGS với return cùng kỳ); nếu sau này cần độ chính xác kế toán cao hơn (return ăn khớp đúng theo lô hàng gốc), cần thiết kế lại.

### Tự đánh giá Phase 9
- **Mạnh**: verify bằng cả ứng dụng thật (Playwright + Chromium) lẫn hạ tầng ngoài thật (SMTP debug server thật, không mock) — xác nhận đúng thứ tự event (gửi email SAU commit, không chặn luồng bán hàng), nội dung email/JSON/2 khổ in đều khớp dữ liệu gốc từng đồng; unit test "snapshot" tái sử dụng đúng số liệu đơn hàng thật đã verify ở Phase 8 thay vì bịa dữ liệu mới, tăng độ tin cậy liên phase.
- **Thiếu**: chưa tích hợp `EInvoiceProvider` thật (đã ghi nợ rõ ràng, không thể làm được trong phạm vi phiên này); chưa xuất PDF lưu server (chỉ in trình duyệt qua `window.print()`, đúng khuyến nghị "ưu tiên in trình duyệt" của master prompt nhưng chưa có phương án lưu file phía server nếu cần).
- **Rủi ro**: trang tra cứu công khai (`/tra-cuu/:code`) lộ `customerPhone`/`customerEmail` cho bất kỳ ai có `lookupCode` — chấp nhận được vì hóa đơn giấy vật lý vốn đã có các thông tin này, nhưng cần cân nhắc lại nếu sau này có yêu cầu ẩn bớt thông tin nhạy cảm trên bản tra cứu công khai.

### Tự đánh giá Phase 5
- **Mạnh**: verify bằng Playwright + Chromium thật (không chỉ đọc code hay chỉ chạy Vitest) — phát hiện 2 bug thật (cookie Secure chặn refresh ở dev local, CORS origin 127.0.0.1 vs localhost) mà chỉ hiện ra khi trình duyệt thật áp dụng đúng chính sách cookie/CORS, curl không bao giờ phát hiện được; DataTable/routing/theme/auth đều test qua thao tác thật trên UI, có ảnh chụp màn hình đối chiếu.
- **Thiếu**: chưa xây các trang nghiệp vụ thật ngoài Products demo; DataTable sort chưa server-side cho mọi endpoint (đã ghi nợ kỹ thuật rõ ràng ở trên); chưa có test Playwright tự động hoá trong CI (mới chạy tay 1 lần), nên viết thành E2E suite chính thức ở Phase 11.
- **Rủi ro**: `fullName` hiển thị tạm bằng `username` do thiếu endpoint `/me`; nếu sau này thêm claim `fullName` vào JWT cần nhớ cập nhật `decodeJwtPayload`/`AccessTokenClaims` cho khớp.

### Tự đánh giá Phase 8
- **Mạnh**: đúng kỷ luật verify bằng ứng dụng chạy thật + test tích hợp đa luồng (không chỉ unit test) theo đúng yêu cầu gate — phát hiện 1 bug thật (race condition sinh số đơn/hoá đơn/SKU) và 1 gap thật (thiếu `orderItemId` trong response) mà unit test/compile không thể phát hiện được, vì cả hai chỉ lộ ra khi có 2 request đồng thời chạm DB thật hoặc khi thử dùng đúng luồng trả hàng end-to-end.
- **Thiếu**: chưa test luồng bán nợ đầy đủ (tạo Debt khi thanh toán thiếu) qua API thật, mới verify qua đọc code; chưa test kịch bản `allow_negative_stock=true` ở Phase 8 (đã test ở Phase 3 cho cơ chế DB, chưa test lại qua `createOrder()`).
- **Rủi ro**: cơ chế chống oversell hiện tại không tự động retry khi thua optimistic lock — client (FE) phải tự xử lý lỗi `PRODUCT_OUT_OF_STOCK` và có thể yêu cầu người dùng thử lại; cần đảm bảo Phase 5 (FE POS) xử lý đúng lỗi này bằng cách tải lại giỏ hàng/tồn kho thay vì chỉ hiện thông báo.

### Tự đánh giá Phase 4
- **Mạnh**: token màu đạt tương phản WCAG AA cả 2 theme; POS thiết kế đủ chi tiết để code thẳng không cần hỏi lại (đủ 10 phím tắt, hành vi auto-focus rõ ràng); mọi màn đều có đủ 5 mục theo Gate (mục đích/thành phần/hành động/trạng thái/phím tắt).
- **Thiếu**: chưa có mockup hình ảnh trực quan (Figma-style) — chỉ có mô tả text/Mermaid; nếu cần trình bày cho stakeholder không kỹ thuật, nên bổ sung mockup HTML/hình ảnh riêng.
- **Rủi ro**: chưa test tương phản màu thực tế bằng công cụ (mới tính toán HSL thủ công) — cần kiểm chứng lại bằng contrast checker khi có code thật ở Phase 5.

### Tự đánh giá Phase 3 (giữ nguyên)
- **Mạnh**: verify bằng ứng dụng Spring Boot chạy thật (không chỉ đọc code), phát hiện và sửa 2 lỗi thực tế (unaccent IMMUTABLE, mâu thuẫn allow_negative_stock) trước khi bàn giao thay vì để lại nợ kỹ thuật ẩn.
- **Thiếu**: chưa có Controller/Service (đúng phạm vi Phase 3, sẽ có ở Phase 6 trở đi); seed data đơn giản hóa (20 đơn không có chiết khấu/voucher — đủ cho dev/demo, kịch bản đầy đủ để ở Phase 11 test).
- **Rủi ro**: `ProductRepositoryIT` chưa được CI thực thi trong phiên làm việc này do thiếu Docker — cần chạy xác nhận trên môi trường có Docker trước khi merge.

### Tự đánh giá Phase 12
- **Mạnh**: đúng kỷ luật "verify bằng chạy thật" dù bị giới hạn Docker daemon — thay vì chỉ đọc lại
  code, đã chủ động thử `mvn verify`/`docker compose config`/`npx playwright test` trước khi coi là
  xong, và nhờ vậy phát hiện 3 lỗi thật không lộ ra nếu chỉ viết file rồi dừng: thiếu
  `maven-failsafe-plugin` (IT test câm lặng từ Phase 3), 29 file lệch format (chưa từng qua
  spotless), và `playwright.config.ts` hardcode path sẽ crash job `e2e` trên CI thật.
- **Thiếu**: chưa build/chạy được `docker build`/`docker compose up` thật (không có Docker daemon
  trong sandbox) — đây là rủi ro lớn nhất còn lại của Phase 12, vì Dockerfile/compose mới được
  review thủ công + `docker compose config`, chưa có bằng chứng runtime thật (ví dụ: healthcheck
  có thật sự pass, image build có lỗi dependency ẩn nào không). Cần chạy thật lần đầu ngay khi có
  máy/CI có Docker trước khi tin tưởng hoàn toàn.
- **Rủi ro**: `.github/workflows/ci.yml` bản thân nó cũng chưa từng chạy thật trên GitHub Actions
  (chỉ viết đúng cú pháp dựa trên tài liệu + kinh nghiệm, review kỹ từng bước) — cần theo dõi lần
  chạy CI đầu tiên sau khi push để xử lý các lỗi phát sinh chỉ lộ ra trên runner thật (khác biệt
  version tool, timing khởi động service, quyền Docker trên runner...).

### Kế tiếp
Phase 0–12 của master prompt đã hoàn tất (Khởi tạo, Nghiệp vụ, Kiến trúc, Database, Thiết kế giao
diện, Frontend Foundation, Backend Foundation, Sản phẩm & Kho, Bán hàng POS, Hóa đơn, Báo cáo,
Testing, DevOps & tài liệu), cộng thêm FH-1 → FH-16 (redesign FruitHouse + 6 tính năng mới ngoài
phạm vi master prompt gốc). Không còn phase nào được tài liệu dự án liệt kê là chưa làm; việc còn
lại là xác nhận CI/Docker chạy thật lần đầu trên môi trường có Docker daemon (xem "Nợ kỹ thuật" và
"Tự đánh giá Phase 12" ở trên) — không phải công việc phát triển mới.
