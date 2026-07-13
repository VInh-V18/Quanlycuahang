-- Prompt #12 (P3, module AI Assistant - nang cap dot 2) - view whitelist THU 5 cho role
-- fruithouse_ai_readonly (V30) - phuc vu tinh nang "Du bao nhap hang" (AiForecastService goi sang
-- Python ML service): can du lieu ban theo TUNG NGAY (khong chi tong hop 30 ngay nhu
-- v_ai_top_products) de mo hinh IsolationForest phat hien ngay ban bat thuong (outlier) truoc khi
-- tinh toc do ban trung binh.
--
-- KHONG gioi han khoang thoi gian trong view (giong v_ai_revenue) - AiQueryService tu loc so ngay
-- can lay (mac dinh 90 ngay) qua tham so WHERE sale_day >= ?, tranh phai tao them 1 view khac neu
-- sau nay can khoang thoi gian khac.
CREATE VIEW v_ai_daily_sales AS
SELECT
    p.id AS product_id,
    o.branch_id,
    date_trunc('day', o.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh') AS sale_day,
    SUM(oi.quantity) AS quantity_sold
FROM order_items oi
JOIN orders o ON o.id = oi.order_id
JOIN products p ON p.id = oi.product_id
WHERE oi.tenant_id = current_setting('app.current_tenant_id')::bigint
  AND o.status IN ('completed', 'partially_returned', 'fully_returned')
  AND o.deleted_at IS NULL
  AND oi.deleted_at IS NULL
  AND p.deleted_at IS NULL
GROUP BY p.id, o.branch_id, sale_day;

GRANT SELECT ON v_ai_daily_sales TO fruithouse_ai_readonly;
