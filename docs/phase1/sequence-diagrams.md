# Phase 1.4a — Sequence diagram (Mermaid)

## 1. Đăng nhập + Refresh token rotation

```mermaid
sequenceDiagram
    actor U as Người dùng
    participant FE as Frontend (React)
    participant AC as AuthController
    participant AS as AuthService
    participant JS as JwtService
    participant R as Redis
    participant DB as PostgreSQL (users)

    U->>FE: Nhập username/password
    FE->>AC: POST /api/v1/auth/login
    AC->>AS: authenticate(username, password)
    AS->>R: kiểm tra rate limit (Bucket4j, key=IP)
    R-->>AS: còn lượt
    AS->>DB: findByUsername(username)
    DB-->>AS: User (password hash)
    AS->>AS: BCrypt.matches(password, hash)
    AS->>JS: generateAccessToken(user) + generateRefreshToken(user)
    JS-->>AS: accessToken (15p), refreshToken (7 ngày)
    AS->>R: lưu refreshToken theo tokenFamily (TTL 7 ngày)
    AS-->>AC: accessToken, refreshToken
    AC-->>FE: 200 { accessToken } + Set-Cookie refreshToken (httpOnly, SameSite=Strict)
    FE->>FE: lưu accessToken trong memory (Redux)

    Note over FE,AC: --- Sau 15 phút, access token hết hạn ---
    FE->>AC: GET /api/v1/products (Authorization hết hạn)
    AC-->>FE: 401 Unauthorized
    FE->>AC: POST /api/v1/auth/refresh (kèm cookie refreshToken)
    AC->>AS: refresh(refreshToken)
    AS->>R: kiểm tra refreshToken tồn tại & chưa rotate
    alt refreshToken hợp lệ
        R-->>AS: hợp lệ
        AS->>JS: generateAccessToken + generateRefreshToken mới (rotation)
        AS->>R: vô hiệu hóa token cũ, lưu token mới (cùng tokenFamily)
        AS-->>AC: accessToken mới, refreshToken mới
        AC-->>FE: 200 { accessToken } + Set-Cookie refreshToken mới
        FE->>AC: retry GET /api/v1/products (Authorization mới)
    else refreshToken đã bị dùng lại (reuse)
        R-->>AS: phát hiện reuse
        AS->>R: thu hồi toàn bộ tokenFamily
        AS->>DB: ghi AuditLog(AUTH_TOKEN_REUSE_DETECTED)
        AS-->>AC: lỗi
        AC-->>FE: 401, buộc đăng nhập lại
    end
```

## 2. Bán hàng POS đầy đủ

```mermaid
sequenceDiagram
    actor CS as Thu ngân
    participant FE as Frontend (POS)
    participant OC as OrderController
    participant HI as HandlerInterceptor (Idempotency)
    participant OPS as OrderPricingService
    participant OS as OrderService
    participant DB as PostgreSQL
    participant R as Redis

    CS->>FE: Quét barcode, thêm sản phẩm vào giỏ
    FE->>FE: Áp CK dòng/CK đơn/voucher, tính tổng theo B4 (client-side)
    CS->>FE: Chọn phương thức thanh toán, nhấn "Thanh toán"
    FE->>OC: POST /api/v1/orders (Idempotency-Key: uuid)
    OC->>HI: kiểm tra Idempotency-Key
    HI->>R: GET idempotency:{key}
    alt key đã tồn tại (request lặp)
        R-->>HI: kết quả đã lưu
        HI-->>FE: trả lại kết quả lần đầu (không tạo đơn mới)
    else key mới
        HI->>R: đánh dấu đang xử lý
        OC->>OPS: calculate(cartItems, discounts, voucher, vatConfig)
        OPS-->>OC: kết quả tính toán (Backend)
        OC->>OC: so sánh kết quả FE gửi vs Backend tính
        alt lệch số tiền
            OC-->>FE: 422 ORDER_PRICE_MISMATCH
        else khớp
            OC->>OS: createOrder(...)
            activate OS
            OS->>DB: BEGIN TRANSACTION
            OS->>DB: UPDATE inventory SET stock = stock - qty WHERE ... AND version = ? (optimistic lock)
            alt OptimisticLockException (hết tồn/tranh chấp)
                DB-->>OS: lỗi version mismatch
                OS->>DB: ROLLBACK
                OS-->>OC: PRODUCT_OUT_OF_STOCK
                OC-->>FE: 409 PRODUCT_OUT_OF_STOCK
            else thành công
                OS->>DB: INSERT order, order_items (snapshot), inventory_transactions (sale)
                OS->>DB: INSERT invoice, order_payments (+ debts nếu bán nợ)
                OS->>DB: COMMIT
                deactivate OS
                OS-->>OC: Order + Invoice data
                OC->>R: lưu kết quả vào Redis theo Idempotency-Key (TTL 24h)
                OC-->>FE: 201 { order, invoice }
                FE->>FE: render hóa đơn K80, window.print()
            end
        end
    end
```

## 3. Nhập kho (kèm tính lại giá vốn)

```mermaid
sequenceDiagram
    actor W as Nhân viên kho
    participant FE as Frontend
    participant PC as PurchaseOrderController
    participant PS as PurchaseOrderService
    participant ACS as AverageCostService
    participant DB as PostgreSQL

    W->>FE: Chọn NCC, nhập các dòng (SP, SL, đơn giá nhập)
    FE->>PC: POST /api/v1/purchase-orders
    PC->>PS: createPurchaseOrder(supplierId, items)
    activate PS
    PS->>DB: BEGIN TRANSACTION
    loop mỗi dòng nhập
        PS->>DB: SELECT stock, cost_price FROM inventory WHERE product_id=? AND branch_id=? FOR UPDATE
        DB-->>PS: tồn_hiện_tại, giá_vốn_cũ
        PS->>ACS: calculateNewCost(tồn_hiện_tại, giá_vốn_cũ, SL_nhập, giá_nhập)
        ACS-->>PS: giá_vốn_mới (BigDecimal, HALF_UP)
        PS->>DB: UPDATE inventory SET stock = stock + SL_nhập, cost_price = giá_vốn_mới
        PS->>DB: INSERT inventory_transactions (type=purchase)
    end
    PS->>DB: INSERT purchase_order, purchase_order_items
    alt mua thiếu (chưa trả đủ)
        PS->>DB: INSERT/UPDATE debts (công nợ phải trả NCC)
    end
    PS->>DB: COMMIT
    deactivate PS
    PS-->>PC: PurchaseOrder đã lưu
    PC-->>FE: 201 { purchaseOrder }
```

## 4. Trả hàng (khách hàng)

```mermaid
sequenceDiagram
    actor CS as Thu ngân
    participant FE as Frontend
    participant RC as ReturnController
    participant RS as ReturnService
    participant DB as PostgreSQL

    CS->>FE: Tra cứu hóa đơn gốc theo mã HĐ
    FE->>RC: GET /api/v1/orders/{id}
    RC-->>FE: Order + OrderItems (đã mua, đã trả)
    CS->>FE: Chọn dòng + số lượng trả
    FE->>RC: POST /api/v1/returns
    RC->>RS: createReturn(orderId, items)
    activate RS
    RS->>DB: BEGIN TRANSACTION
    RS->>DB: kiểm tra SL trả ≤ SL đã mua - đã trả (theo OrderItem)
    alt vượt số lượng còn lại
        RS-->>RC: RETURN_QUANTITY_EXCEEDED
        RC-->>FE: 422
    else hợp lệ
        RS->>DB: tính tiền hoàn = đơn giá thực trả (snapshot, sau CK đã phân bổ)
        RS->>DB: UPDATE inventory SET stock = stock + SL_trả (dùng costPrice snapshot trên OrderItem)
        RS->>DB: INSERT inventory_transactions (type=customer_return)
        RS->>DB: INSERT returns, return_items
        RS->>DB: UPDATE orders SET status = partially_returned/fully_returned
        alt đơn gốc bán nợ
            RS->>DB: UPDATE debts (giảm công nợ tương ứng)
        else
            RS->>DB: INSERT order_payments (hoàn tiền mặt/chuyển khoản, số âm)
        end
        RS->>DB: COMMIT
        deactivate RS
        RS-->>RC: Return đã lưu
        RC-->>FE: 201 { return }
    end
```

## 5. Kết ca

```mermaid
sequenceDiagram
    actor CS as Thu ngân
    participant FE as Frontend
    participant SC as ShiftController
    participant SS as ShiftService
    participant DB as PostgreSQL

    CS->>FE: Nhấn "Kết ca"
    FE->>SC: GET /api/v1/shifts/{id}/summary
    SC->>SS: getShiftSummary(shiftId)
    SS->>DB: SELECT openingCash, SUM(order_payments tiền mặt), SUM(cash_transactions thu/chi)
    DB-->>SS: dữ liệu tổng hợp
    SS->>SS: tiềnMặtLýThuyết = openingCash + Σthu - Σchi
    SS-->>SC: tiềnMặtLýThuyết
    SC-->>FE: hiển thị tiền lý thuyết, yêu cầu nhập tiền đếm thực tế
    CS->>FE: Nhập tiền đếm thực tế (+ ghi chú nếu chênh lệch)
    FE->>SC: POST /api/v1/shifts/{id}/close { actualCash, note }
    SC->>SS: closeShift(shiftId, actualCash, note)
    activate SS
    SS->>DB: BEGIN TRANSACTION
    SS->>SS: chênh lệch = actualCash - tiềnMặtLýThuyết
    alt còn ParkedOrder chưa xử lý
        SS-->>SC: cảnh báo xác nhận trước khi đóng ca
        SC-->>FE: 409 SHIFT_HAS_PARKED_ORDERS (xác nhận lại)
    else
        SS->>DB: UPDATE shifts SET status=closed, actual_cash=?, discrepancy=?, note=?
        SS->>DB: COMMIT
        deactivate SS
        SS-->>SC: Shift đã đóng
        SC-->>FE: 200 { shift }
    end
```
