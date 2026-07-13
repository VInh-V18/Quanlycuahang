-- Cho phep xoa VINH VIEN 1 tenant (TenantAdminService.deleteTenant) ma khong lam mat lich su
-- nhat ky Super Admin da tung ghi ve tenant do (TENANT_CREATE, TENANT_USER_CREATE...) - truoc day
-- FK tenant_id la RESTRICT (mac dinh), xoa dong tenants se bi chan boi chinh cac dong nhat ky cua
-- no. Doi sang ON DELETE SET NULL: dong nhat ky VAN GIU LAI (target_description da co san ten
-- tenant dang doc duoc), chi mat lien ket FK toi 1 tenant khong con ton tai - dung dung ban chat
-- 1 audit trail (ghi lai NHUNG GI DA XAY RA, phai song lau hon chinh doi tuong duoc ghi lai).
ALTER TABLE platform_audit_logs DROP CONSTRAINT platform_audit_logs_tenant_id_fkey;
ALTER TABLE platform_audit_logs
    ADD CONSTRAINT platform_audit_logs_tenant_id_fkey
    FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE SET NULL;
