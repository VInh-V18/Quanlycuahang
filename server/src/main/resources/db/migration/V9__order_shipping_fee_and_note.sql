-- Them phi ship va ghi chu cho don hang — dung cho mau hoa don giao hang (FH-hoa-don-v2). Phi ship
-- cong them vao total_amount SAU khi OrderPricingService tinh xong (khong qua CK/VAT/lam tron cua
-- hang hoa) de khong dung vao thuat toan phan bo chiet khau da duoc kiem thu ky o Phase 8.
ALTER TABLE orders
    ADD COLUMN shipping_fee NUMERIC(15, 0) NOT NULL DEFAULT 0,
    ADD COLUMN note VARCHAR(500);
