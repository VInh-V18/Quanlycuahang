-- Sequence toan cuc (order_number_seq/invoice_number_seq/sku_seq tu V3) sinh so nguyen KHONG phan
-- biet tenant -> tenant A ban hang lam tenant B thay so don/hoa don/SKU cua minh nhay coc bat thuong
-- (vd tenant A tao don #1, tenant B tao don #2, tenant A tao don tiep theo lai la #5) - moi cua hang
-- ky vong so cua RIENG minh lien tuc tu 1, giong KiotViet moi cua hang co day so doc lap.
-- Thay bang 1 bang dem rieng tung tenant, dung INSERT ... ON CONFLICT DO UPDATE ... RETURNING de tang
-- nguyen tu (Postgres tu khoa dong xung dot, khong can SELECT FOR UPDATE thu cong).

CREATE TABLE tenant_sequences (
    tenant_id BIGINT NOT NULL REFERENCES tenants (id),
    sequence_name VARCHAR(50) NOT NULL,
    current_value BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant_id, sequence_name)
);

-- Tenant #1 (du lieu that dang chay) phai tiep tuc dung dung gia tri hien tai cua sequence toan cuc
-- cu - neu khong se sinh lai so da dung, vi pham UNIQUE (tenant_id, order_number/invoice_number/sku).
INSERT INTO tenant_sequences (tenant_id, sequence_name, current_value)
SELECT 1, 'order_number_seq', last_value FROM order_number_seq
UNION ALL
SELECT 1, 'invoice_number_seq', last_value FROM invoice_number_seq
UNION ALL
SELECT 1, 'sku_seq', last_value FROM sku_seq;

DROP SEQUENCE order_number_seq;
DROP SEQUENCE invoice_number_seq;
DROP SEQUENCE sku_seq;
