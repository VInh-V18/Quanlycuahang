# Phase 1.5a — Class diagram domain

Đây là mô hình **nghiệp vụ** (domain), chưa phải Entity JPA (sẽ ánh xạ chi
tiết ở Phase 3 với `@Entity`, `@Column`, `@ManyToOne`...). Chia theo 7 nhóm
đúng danh sách bảng đã chốt ở B4/Phase 3 (~30 bảng). Do số lượng lớn, mỗi
nhóm vẽ riêng 1 diagram cho dễ đọc; quan hệ liên nhóm liệt kê ở bảng cuối.

## Nhóm 1 — Hệ thống

```mermaid
classDiagram
    class Branch {
        +String name
        +String address
        +String phone
    }
    class User {
        +String username
        +String passwordHash
        +String fullName
        +boolean active
    }
    class Role {
        +String code
        +String displayName
    }
    class Permission {
        +String code
        +String description
    }
    class Settings {
        +String key
        +String value
    }
    class AuditLog {
        +String action
        +JSONB before
        +JSONB after
        +Instant createdAt
    }

    User "many" --> "many" Role : user_roles
    Role "many" --> "many" Permission : role_permissions
    User "many" --> "many" Branch : user_branches
    AuditLog --> User : performedBy
    Settings --> Branch : thuộc chi nhánh (hoặc global)
```

## Nhóm 2 — Sản phẩm

```mermaid
classDiagram
    class Category {
        +String name
        +Category parent
    }
    class Product {
        +String sku
        +String barcode
        +String name
        +BigDecimal sellPrice
        +BigDecimal vatRate
        +BigDecimal minStock
        +boolean priceIncludesVat
    }
    class ProductUnit {
        +String unitName
        +BigDecimal conversionRate
    }
    class PriceHistory {
        +BigDecimal oldPrice
        +BigDecimal newPrice
        +Instant changedAt
    }

    Category "1" --> "many" Category : parent (2 cấp)
    Category "1" --> "many" Product
    Product "1" --> "many" ProductUnit
    Product "1" --> "many" PriceHistory
```

## Nhóm 3 — Kho

```mermaid
classDiagram
    class Inventory {
        +BigDecimal stock
        +BigDecimal costPrice
        +Long version
    }
    class InventoryTransaction {
        +String type
        +BigDecimal quantity
        +BigDecimal unitCost
        +Instant createdAt
    }
    class PurchaseOrder {
        +Instant createdAt
        +String status
    }
    class PurchaseOrderItem {
        +BigDecimal quantity
        +BigDecimal unitPrice
    }
    class StockTake {
        +Instant createdAt
        +String status
    }
    class StockTakeItem {
        +BigDecimal expectedQty
        +BigDecimal actualQty
        +String reason
    }

    Inventory "1" --> "many" InventoryTransaction
    PurchaseOrder "1" --> "many" PurchaseOrderItem
    PurchaseOrderItem --> Inventory : ảnh hưởng
    StockTake "1" --> "many" StockTakeItem
    StockTakeItem --> Inventory : đối chiếu
```

## Nhóm 4 — Bán hàng

```mermaid
classDiagram
    class Order {
        +String orderNumber
        +String status
        +BigDecimal totalAmount
        +BigDecimal discountAmount
        +BigDecimal vatAmount
        +Instant createdAt
    }
    class OrderItem {
        +String productNameSnapshot
        +BigDecimal unitPriceSnapshot
        +BigDecimal costPriceSnapshot
        +BigDecimal quantity
        +BigDecimal returnedQuantity
    }
    class OrderPayment {
        +String method
        +BigDecimal amount
    }
    class Return {
        +Instant createdAt
        +BigDecimal totalRefund
    }
    class ReturnItem {
        +BigDecimal quantity
        +BigDecimal refundAmount
    }
    class ParkedOrder {
        +JSONB cartSnapshot
        +Instant parkedAt
    }

    Order "1" --> "many" OrderItem
    Order "1" --> "many" OrderPayment
    Order "1" --> "many" Return
    Return "1" --> "many" ReturnItem
    ReturnItem --> OrderItem : trả theo dòng gốc
```

## Nhóm 5 — Đối tác

```mermaid
classDiagram
    class CustomerGroup {
        +String name
    }
    class Customer {
        +String name
        +String phone
        +BigDecimal debtLimit
    }
    class Supplier {
        +String name
        +String phone
    }
    class Debt {
        +BigDecimal amount
        +String direction
        +Instant createdAt
    }
    class DebtPayment {
        +BigDecimal amount
        +Instant paidAt
    }

    CustomerGroup "1" --> "many" Customer
    Customer "1" --> "many" Debt : công nợ phải thu
    Supplier "1" --> "many" Debt : công nợ phải trả
    Debt "1" --> "many" DebtPayment
```

## Nhóm 6 — Khuyến mãi

```mermaid
classDiagram
    class Promotion {
        +String name
        +String discountType
        +BigDecimal discountValue
        +Instant startDate
        +Instant endDate
    }
    class Voucher {
        +String code
        +BigDecimal discountValue
        +BigDecimal minOrderAmount
        +int maxUsage
        +Instant expiresAt
    }
    class VoucherUsage {
        +Instant usedAt
    }

    Voucher "1" --> "many" VoucherUsage
    VoucherUsage --> Order : dùng trong đơn
```

## Nhóm 7 — Vận hành

```mermaid
classDiagram
    class Shift {
        +BigDecimal openingCash
        +BigDecimal actualCash
        +BigDecimal discrepancy
        +String status
        +Instant openedAt
        +Instant closedAt
    }
    class CashTransaction {
        +String type
        +BigDecimal amount
        +String note
    }
    class Invoice {
        +String invoiceNumber
        +String qrPayload
        +Instant issuedAt
    }
    class InvoiceTemplate {
        +String paperSize
        +String templateConfig
    }

    Shift "1" --> "many" CashTransaction
    Order "1" --> "1" Invoice
    Invoice --> InvoiceTemplate : dùng mẫu in
```

## Quan hệ liên nhóm (cross-group)

| Từ | Đến | Bản chất |
|---|---|---|
| `Order` | `Branch`, `User` (thu ngân), `Customer`, `Shift` | Đơn thuộc 1 chi nhánh, do 1 thu ngân tạo, có thể gắn khách hàng và ca làm việc |
| `OrderItem` | `Product` | Tham chiếu sản phẩm gốc, nhưng **snapshot** tên/giá/giá vốn — không tính lại từ Product khi hiển thị hóa đơn cũ (D5) |
| `Inventory` | `Product`, `Branch` | Khóa composite `productId + branchId` |
| `InventoryTransaction` | `Order`/`PurchaseOrder`/`StockTake`/`Return` | Nguồn gốc mọi biến động kho, tùy loại giao dịch (B4) |
| `PurchaseOrder` | `Supplier`, `Branch`, `User` | Phiếu nhập gắn NCC, chi nhánh, người tạo |
| `Debt` | `Customer` hoặc `Supplier` | Chiều nợ khác nhau tùy `direction` (phải thu / phải trả) |
| `Promotion`/`Voucher` | `Order` (qua `VoucherUsage`) | Áp dụng khi tạo đơn |
| `AuditLog` | Bất kỳ Entity nhạy cảm | Ghi qua AOP `@Aspect`, không gọi tay từng chỗ |

Tổng cộng đủ 7 nhóm / ~30 bảng theo danh sách đã chốt B4/Phase 3. Entity JPA
chi tiết (kiểu dữ liệu chính xác, `@Column(name=...)`, `@ManyToOne(fetch =
LAZY)`, `@Version`...) sẽ sinh ở Phase 3.
