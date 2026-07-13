-- Nhat ky audit RIENG cho hanh dong Super Admin (tao/khoa tenant, CRUD tai khoan bat ky tenant nao,
-- dat lai mat khau...) - bang audit_logs hien co bat buoc tenant_id NOT NULL (TenantScopedEntity)
-- nen KHONG the dung chung: moi hanh dong Super Admin (khong co TenantContext) se bi AuditAspect
-- am tham bo qua, hoan toan khong de lai dau vet - phat hien khi rieng soat bao mat toan codebase.
CREATE TABLE platform_audit_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    platform_admin_username VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    tenant_id BIGINT REFERENCES tenants (id),
    target_description VARCHAR(255),
    detail JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_platform_audit_logs_tenant_id ON platform_audit_logs (tenant_id);
CREATE INDEX idx_platform_audit_logs_created_at ON platform_audit_logs (created_at);
