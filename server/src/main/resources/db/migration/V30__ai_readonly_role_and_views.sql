-- Prompt #11 (P3, module AI Assistant) — TANG AN TOAN, lam TRUOC moi tinh nang (dung yeu cau
-- roadmap: "AI CHI DOC, khong bao gio ghi"). Tao 1 role Postgres RIENG chi duoc SELECT tren 4 view
-- whitelist da loc san (khong bao gio thay bang goc/password hash/token/settings), va Backend dung
-- 1 connection pool HOAN TOAN TACH BIET (xem AiReadOnlyDataSourceConfig) cho moi truy van AI khoi
-- xuong - ke ca prompt injection thanh cong khien AI "muon" chay 1 cau INSERT/UPDATE/DELETE, tang
-- quyen Postgres se tu choi truoc khi cham duoc du lieu that (xem AiReadOnlyPermissionIT).
--
-- ${aiReadonlyPassword}: Flyway placeholder (spring.flyway.placeholders.aiReadonlyPassword trong
-- application.yml, doc tu bien moi truong AI_DB_READONLY_PASSWORD) - KHONG hardcode mat khau that
-- vao file migration (se bi commit vao repo), giong cach DB_PASSWORD/JWT_SECRET dang duoc quan ly
-- qua .env cho toan he thong.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'fruithouse_ai_readonly') THEN
        CREATE ROLE fruithouse_ai_readonly LOGIN PASSWORD '${aiReadonlyPassword}';
    ELSE
        ALTER ROLE fruithouse_ai_readonly PASSWORD '${aiReadonlyPassword}';
    END IF;
END
$$;

-- Gioi han tai nguyen phong ho - 1 cau hoi AI vo tinh/co y quet toan bang lon cung khong duoc
-- chiem qua lau 1 connection cua pool rieng (chi 3 connection, xem application.yml). Khong can
-- GRANT CONNECT tuong minh - Postgres mac dinh cap CONNECT cho PUBLIC tren moi database (chua
-- ai REVOKE quyen nay), role moi tao da ket noi duoc ngay.
ALTER ROLE fruithouse_ai_readonly SET statement_timeout = '10s';

-- Tuong minh REVOKE truoc (phong ho, khong dua vao mac dinh "role moi khong co quyen gi" cua
-- Postgres - ghi ro y dinh cho nguoi doc sau nay), roi GRANT dung 4 view whitelist.
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM fruithouse_ai_readonly;

-- v_ai_revenue: doanh thu theo ngay + chi nhanh, CHI don da hoan tat/co gia tri (khong tinh don
-- huy). Loc tenant qua GUC "app.current_tenant_id" (Backend SET truoc moi truy van, xem
-- AiQueryService) - view KHONG nhan tham so truc tiep duoc (Postgres khong ho tro view co tham
-- so), day la ky thuat chuan de "tham so hoa" 1 view theo tung phien ket noi.
CREATE VIEW v_ai_revenue AS
SELECT
    date_trunc('day', o.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh') AS day,
    o.branch_id,
    SUM(o.total_amount) AS revenue,
    COUNT(*) AS order_count
FROM orders o
WHERE o.tenant_id = current_setting('app.current_tenant_id')::bigint
  AND o.status IN ('completed', 'partially_returned', 'fully_returned')
  AND o.deleted_at IS NULL
GROUP BY 1, 2;

-- v_ai_top_products: san pham ban chay 30 ngay gan nhat. order_items CUNG la bang tenant-scoped
-- rieng (co cot tenant_id cua chinh no, giong tenant_id cua don hang cha) - loc qua tenant_id cua
-- CHINH order_items (khong phai orders) de tranh 2 cot tenant_id trung ten gay "ambiguous" khi
-- join (da phat hien khi chay thu migration nay).
CREATE VIEW v_ai_top_products AS
SELECT
    p.id AS product_id,
    p.name AS product_name,
    p.sku,
    o.branch_id,
    SUM(oi.quantity) AS quantity_sold,
    SUM(oi.line_total) AS revenue
FROM order_items oi
JOIN orders o ON o.id = oi.order_id
JOIN products p ON p.id = oi.product_id
WHERE oi.tenant_id = current_setting('app.current_tenant_id')::bigint
  AND o.created_at >= now() - interval '30 days'
  AND o.status IN ('completed', 'partially_returned', 'fully_returned')
  AND o.deleted_at IS NULL
  AND oi.deleted_at IS NULL
  AND p.deleted_at IS NULL
GROUP BY p.id, p.name, p.sku, o.branch_id;

-- v_ai_inventory_summary: ton kho hien tai + dinh muc toi thieu (dung tinh "sap het hang" va
-- toc do ban 30 ngay o v_ai_top_products de goi y nhap hang).
CREATE VIEW v_ai_inventory_summary AS
SELECT
    p.id AS product_id,
    p.name AS product_name,
    p.sku,
    i.branch_id,
    i.stock,
    p.min_stock
FROM inventory i
JOIN products p ON p.id = i.product_id
WHERE i.tenant_id = current_setting('app.current_tenant_id')::bigint
  AND p.deleted_at IS NULL;

-- v_ai_debt_aging: cong no theo nhom tuoi no - CHI tong hop theo bucket, KHONG tra ve ten khach
-- hang/NCC cu the de giam be mat lo thong tin ca nhan qua AI (du la cung tenant, cau hoi dang
-- tho "cong no ra sao" khong can biet cu the ai no bao nhieu).
CREATE VIEW v_ai_debt_aging AS
SELECT
    d.direction,
    CASE
        WHEN now() - d.created_at <= interval '30 days' THEN '0-30'
        WHEN now() - d.created_at <= interval '60 days' THEN '31-60'
        WHEN now() - d.created_at <= interval '90 days' THEN '61-90'
        ELSE '90+'
    END AS aging_bucket,
    SUM(d.amount) AS total_amount,
    COUNT(*) AS debt_count
FROM debts d
WHERE d.tenant_id = current_setting('app.current_tenant_id')::bigint
  AND d.amount > 0
  AND d.deleted_at IS NULL
GROUP BY d.direction, aging_bucket;

GRANT USAGE ON SCHEMA public TO fruithouse_ai_readonly;
GRANT SELECT ON v_ai_revenue, v_ai_top_products, v_ai_inventory_summary, v_ai_debt_aging
    TO fruithouse_ai_readonly;
