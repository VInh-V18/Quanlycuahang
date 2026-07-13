-- Prompt #6 (P1): job doi soat toan ven du lieu - 2 bang moi, TenantScopedEntity binh thuong
-- (giong AuditLog), khong dac biet nhu platform_audit_logs (day la du lieu nghiep vu CUA TUNG
-- tenant, khong phai audit toan he thong cua Super Admin).
CREATE TABLE reconciliation_runs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    trigger_type VARCHAR(20) NOT NULL CHECK (trigger_type IN ('manual', 'scheduled')),
    triggered_by BIGINT REFERENCES users(id),
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'running' CHECK (status IN ('running', 'completed', 'failed')),
    findings_count INTEGER NOT NULL DEFAULT 0,
    duration_ms BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_reconciliation_runs_tenant_id ON reconciliation_runs (tenant_id);
CREATE INDEX idx_reconciliation_runs_tenant_started ON reconciliation_runs (tenant_id, started_at DESC);

CREATE TABLE reconciliation_findings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES reconciliation_runs(id),
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    check_type VARCHAR(40) NOT NULL CHECK (check_type IN (
        'INVENTORY_MISMATCH', 'DEBT_MISMATCH', 'ORDER_TOTAL_MISMATCH',
        'INVOICE_MISSING', 'INVOICE_DUPLICATE', 'RETURN_OVER_QUANTITY',
        'ORPHANED_REFERENCE', 'SHIFT_DISCREPANCY_MISMATCH')),
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('critical', 'high', 'medium', 'low')),
    entity_type VARCHAR(30),
    entity_id BIGINT,
    details JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'acknowledged', 'resolved')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_reconciliation_findings_run_id ON reconciliation_findings (run_id);
CREATE INDEX idx_reconciliation_findings_tenant_id ON reconciliation_findings (tenant_id);
-- Dung cho canh bao tren DashboardPage: dem nhanh finding OPEN cua tenant hien tai, khong quet
-- toan bang.
CREATE INDEX idx_reconciliation_findings_tenant_open
    ON reconciliation_findings (tenant_id, status)
    WHERE status = 'open' AND deleted_at IS NULL;
