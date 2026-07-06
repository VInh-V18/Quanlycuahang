# Phase 2 — Kiến trúc Backend

## 1. Kiến trúc phân lớp

```
Controller (@RestController)
    ↓ gọi
Service (@Service, @Transactional)
    ↓ gọi
Repository (extends JpaRepository / JpaSpecificationExecutor)
    ↓ thao tác
Entity (@Entity)
```

Phụ thuộc **một chiều**: Controller → Service → Repository → Entity.
Repository/Entity không biết Service; Service không biết Controller.

- **Controller**: chỉ map request/response (nhận DTO qua `@Valid @RequestBody`,
  trả `ApiResponse<T>`), tuyệt đối không chứa logic nghiệp vụ (không tính
  toán tiền, không quyết định trạng thái đơn hàng...). Nếu Controller có
  hơn vài dòng xử lý ngoài việc gọi Service + map DTO, đó là dấu hiệu logic
  bị đặt sai lớp.
- **Service**: chứa toàn bộ nghiệp vụ, quản lý transaction
  (`@Transactional`), gọi 1 hoặc nhiều Repository. **Không biết**
  `HttpServletRequest`/`HttpServletResponse` — nếu Service cần thông tin
  request (vd correlation id, IP), thông tin đó phải được truyền vào qua
  tham số method từ Controller, không import trực tiếp servlet API.
- **Repository**: interface `extends JpaRepository<Entity, Long>` (+
  `JpaSpecificationExecutor<Entity>` khi cần filter động). Không chứa logic,
  chỉ định nghĩa query (derived method hoặc `@Query`).
- **Entity**: ánh xạ bảng DB, không có logic nghiệp vụ phức tạp (business
  logic thuần — như `OrderPricingService`, `AverageCostService` — luôn nằm
  ở Service, để unit test không cần Entity/Spring context).

### Ngoại lệ có chủ đích: Service nghiệp vụ thuần Java

`OrderPricingService` (tính tiền POS) và các phần tính toán thuần (state
machine `canTransition`, `AverageCostService`) được thiết kế là **Java
thuần túy**, không phụ thuộc Spring annotation, để unit test không cần khởi
động `ApplicationContext` (B4, D5). Các class này vẫn nằm trong package
`service` nhưng không có `@Service` — được các Service Spring khác
(`OrderService`) khởi tạo trực tiếp (`new OrderPricingService()`) hoặc định
nghĩa `@Bean` nếu cần inject config.

## 2. Dependency Injection — Constructor injection bắt buộc

**Quy ước**: mọi Service/Controller dùng **constructor injection**, cấm
`@Autowired` field injection.

```java
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }
}
```

**Lý do**:
- Field cho phép object ở trạng thái "nửa khởi tạo" (dependency null) nếu
  không dùng qua Spring container — dễ gây `NullPointerException` khó truy
  vết trong test viết tay (`new ProductService()`).
- Constructor injection cho phép khai báo `final` — bất biến sau khi khởi
  tạo, IDE/compiler cảnh báo ngay nếu thiếu dependency.
- Viết unit test dễ hơn: `new ProductService(mockRepo, mockMapper)` không
  cần reflection hay Spring test context.
- Với 1 constructor duy nhất, Spring 4.3+ không cần `@Autowired` trên
  constructor (tự động inject) — code gọn hơn.

Với Spring Boot 3.3 + 1 constructor, **không cần** annotation `@Autowired`
ở bất kỳ đâu trong dự án.

## 3. Chuẩn DTO + MapStruct mapper

**Lý do không trả thẳng Entity ra API**:
1. Lộ trường nhạy cảm (vd `passwordHash` trên `User`, `costPrice` trên
   `Product` nếu người gọi không có quyền `report:gross-profit`).
2. Vòng lặp quan hệ JPA khi serialize JSON (`Order` → `OrderItem` →
   `Order`...) gây `StackOverflowError` hoặc lazy-loading exception ngoài
   transaction.
3. DTO là hợp đồng API ổn định, độc lập với thay đổi cấu trúc Entity nội bộ.

**Quy ước mỗi module**:
```
product/
├── dto/
│   ├── ProductRequest.java     # input tạo/sửa — @Valid + Jakarta Bean Validation
│   ├── ProductResponse.java    # output trả cho FE
│   └── ProductSummaryResponse.java  # output rút gọn cho danh sách/dropdown
├── mapper/
│   └── ProductMapper.java      # interface MapStruct
```

```java
@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductResponse toResponse(Product entity);
    ProductSummaryResponse toSummaryResponse(Product entity);
    Product toEntity(ProductRequest request);
    void updateEntityFromRequest(ProductRequest request, @MappingTarget Product entity);
}
```

MapStruct sinh code lúc compile (annotation processor đã khai báo ở
`pom.xml` Phase 0) — không dùng reflection runtime, hiệu năng tốt, lỗi map
sai field phát hiện ngay lúc build thay vì runtime.

## 4. Exception hierarchy

```
RuntimeException
└── AppException (abstract; code, httpStatus, message, details)
    ├── ValidationException        → 400
    ├── ResourceNotFoundException  → 404
    ├── PermissionDeniedException  → 403
    ├── ConflictException          → 409
    └── BusinessRuleException      → 422
```

Đã sinh tại `common/exception/`. `GlobalExceptionHandler`
(`@RestControllerAdvice`) xử lý tập trung, map mọi exception (kể cả
`MethodArgumentNotValidException`, `OptimisticLockingFailureException` —
oversell, `DataIntegrityViolationException` — trùng khóa unique,
`AccessDeniedException`, `AuthenticationException`, và `Exception` bắt mọi
lỗi không lường trước) thành response D2 (`ApiResponse.error(...)`).
Controller/Service **không** viết `try/catch` để trả response lỗi thủ
công — chỉ ném đúng exception nghiệp vụ, tầng Advice lo phần còn lại.

## 5. Logging

- SLF4J + Logback (`logback-spring.xml`), cấu hình 2 profile:
  - `local`: console pattern dễ đọc, có `[correlationId]`.
  - `docker`/`prod`: JSON qua `logstash-logback-encoder`, dễ ingest vào hệ
    thống log tập trung (ELK/Loki...) sau này.
- `CorrelationIdFilter` (`common/web/`) gán correlation id vào MDC cho mọi
  request (ưu tiên header `X-Correlation-Id` từ client/gateway, không có
  thì tự sinh UUID) — mọi dòng log trong cùng 1 request có thể truy vết
  xuyên suốt qua giá trị này.
- **Cấm log mật khẩu/token** (D3) — code review/tự kiểm tra thủ công vì
  Logback không tự động che các trường này.

## 6. Cấu trúc package chi tiết

```
com.quanlycuahang.erp/
├── ErpApplication.java
├── config/                      # cấu hình cross-cutting (JpaAuditingConfig, SecurityConfig...)
├── common/
│   ├── entity/                  # BaseEntity (id, createdAt, updatedAt, deletedAt)
│   ├── dto/                     # ApiResponse, ApiError, PageMeta, ReportFilter (Phase 10)
│   ├── exception/                # AppException + 5 exception con + GlobalExceptionHandler
│   └── web/                     # CorrelationIdFilter, các filter/interceptor dùng chung
└── <feature>/                   # 1 package cho mỗi module nghiệp vụ, vd:
    ├── auth/
    ├── product/
    ├── inventory/
    ├── sales/
    ├── partner/
    ├── promotion/
    ├── operation/
    └── report/
        ├── controller/
        ├── service/
        ├── repository/
        ├── entity/
        ├── dto/
        ├── mapper/
        └── exception/            # (tùy chọn) exception rất riêng của module, hiếm dùng
```

**Gate tự đánh giá**: developer mới thêm 1 module nghiệp vụ (vd
`shipping`) chỉ cần tạo `com.quanlycuahang.erp.shipping` với đúng 6
sub-package (`controller/service/repository/entity/dto/mapper`), tham
chiếu `common/` cho `ApiResponse`/`AppException`/`BaseEntity`, và biết đặt
test tương ứng tại `src/test/java/com/quanlycuahang/erp/shipping/` cùng
cấu trúc — không cần đọc thêm tài liệu nào khác ngoài file này.

## Danh sách file đã sinh ở Phase 2

- `common/entity/BaseEntity.java`
- `common/exception/AppException.java`, `ValidationException.java`,
  `BusinessRuleException.java`, `ResourceNotFoundException.java`,
  `PermissionDeniedException.java`, `ConflictException.java`,
  `GlobalExceptionHandler.java`
- `common/dto/ApiResponse.java`, `ApiError.java`, `PageMeta.java`
- `common/web/CorrelationIdFilter.java`
- `config/JpaAuditingConfig.java`
- `resources/logback-spring.xml`

## Lệnh kiểm tra

```bash
cd server
mvn clean install   # compile + Spotless format check
```
