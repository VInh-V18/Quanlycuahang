-- =====================================================================
-- V7__debt_original_amount.sql
-- FH-12: Cong no chi tiet can hien thi lich su doi chieu (moi dot phat sinh no "ban no"/"mua no"
-- + moi lan thu/tra no), nhung debts.amount bi tru dan khi thanh toan (khong con giu gia tri goc).
-- Them original_amount de tach rieng "so tien no phat sinh luc dau" (khong doi) voi "amount"
-- (con lai, giam dan) - moi debt hien co coi nhu chua tung duoc tra (amount = original_amount).
-- =====================================================================

ALTER TABLE debts
    ADD COLUMN original_amount NUMERIC(15, 0) NOT NULL DEFAULT 0;

UPDATE debts SET original_amount = amount;
