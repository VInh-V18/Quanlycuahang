-- Index tong hop cho cac truy van bao cao (ReportService/DashboardService) - moi cau deu loc
-- tenant_id + khoang created_at (+ branch_id/status), truoc day moi bang chi co index don le
-- tenant_id (V13) va branch_id (V1) rieng re, DB phai AND 2 bitmap index thay vi di 1 index tong
-- hop; se cham dan khi du lieu nhieu tenant tich luy (phat hien khi rieng soat hieu nang).
-- Thu tu cot: tenant_id (equality) truoc, created_at (range) sau - dung quy tac equality-first.
CREATE INDEX idx_orders_tenant_created_branch_status
    ON orders (tenant_id, created_at, branch_id, status);

-- Cong no con du theo chieu thu/tra (findAgingBuckets/summaryByDirection/aging theo doi tac) -
-- partial index dung dieu kien loc thuc te (amount > 0 AND chua xoa mem) nen rat nho va luon khop.
CREATE INDEX idx_debts_tenant_direction_outstanding
    ON debts (tenant_id, direction)
    WHERE amount > 0 AND deleted_at IS NULL;
