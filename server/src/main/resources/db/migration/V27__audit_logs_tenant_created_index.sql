-- Prompt #7 (P2, hieu nang) - audit_logs la bang tang truong khong gioi han nhanh nhat he thong
-- (AuditAspect ghi tu dong o rat nhieu hanh dong), nhung AuditLogRepository.search() (trang Nhat
-- ky audit) luon loc tenant_id + khoang created_at ma bang nay TRUOC GIO chi co index don le tren
-- created_at (V1) - dung y het loi da tim thay va sua cho bang orders o V24
-- (idx_orders_tenant_created_branch_status), nhung bi sot voi audit_logs. Thu tu cot: tenant_id
-- (equality) truoc, created_at (range) sau - dung quy tac equality-first nhu V24.
CREATE INDEX idx_audit_logs_tenant_created ON audit_logs (tenant_id, created_at);

-- idx_audit_logs_created_at (V1) tro thanh du thua sau khi co composite tren (MOI truy van deu di
-- kem tenant_id, khong co truy van nao loc rieng created_at) - bo de giam chi phi ghi tren bang
-- insert-heavy nay (moi hanh dong duoc @Audited deu INSERT 1 dong).
DROP INDEX idx_audit_logs_created_at;
