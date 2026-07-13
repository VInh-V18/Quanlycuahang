-- Bo sung IP/user-agent cho nhat ky Super Admin - phat hien khi audit: khong the truy vet duoc
-- ai thuc su thuc hien mot hanh dong (vd xoa tenant) neu chi co username, vi tai khoan superadmin
-- co the bi lo/chia se.
ALTER TABLE platform_audit_logs
    ADD COLUMN client_ip VARCHAR(45),
    ADD COLUMN user_agent VARCHAR(255);
