-- =====================================================================
-- V3__number_sequences.sql
-- Sequence nguyen tu cho sinh so (order_number, invoice_number, sku).
-- Thay the pattern count()+existsBy() cu (khong atomic — 2 request dong thoi
-- co the doc cung count truoc khi ben nao commit, sinh trung so, vi pham
-- unique constraint duoi tai POS nhieu quay dong thoi).
-- =====================================================================

CREATE SEQUENCE IF NOT EXISTS order_number_seq;
CREATE SEQUENCE IF NOT EXISTS invoice_number_seq;
CREATE SEQUENCE IF NOT EXISTS sku_seq;

SELECT setval(
    'order_number_seq',
    COALESCE((SELECT max(substring(order_number from '[0-9]+$')::bigint) FROM orders), 0));

SELECT setval(
    'invoice_number_seq',
    COALESCE((SELECT max(substring(invoice_number from '[0-9]+$')::bigint) FROM invoices), 0));

SELECT setval(
    'sku_seq',
    COALESCE((SELECT max(substring(sku from '[0-9]+$')::bigint) FROM products), 0));
