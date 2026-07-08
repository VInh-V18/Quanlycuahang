-- =====================================================================
-- V6__purchase_order_discount.sql
-- FH-6: Chiet khau NCC tren phieu nhap kho (mockup 06-nhap-kho.png hien "Chiết khấu NCC").
-- =====================================================================

ALTER TABLE purchase_orders
    ADD COLUMN discount_amount NUMERIC(15, 0) NOT NULL DEFAULT 0;
