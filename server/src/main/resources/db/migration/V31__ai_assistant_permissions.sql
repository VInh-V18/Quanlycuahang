-- Prompt #11 (P3, module AI Assistant) - 2 quyen moi theo dung khuon mau resource:action co san
-- (giong V26__reconciliation_permission.sql):
--   ai:use - hoi dap bao cao + xem goi y nhap hang (owner/manager, cong cu ho tro quyet dinh)
--   ai:manage-settings - cau hinh khoa API nha cung cap AI (CHI owner - anh huong chi phi thuc te
--     tra cho nha cung cap AI ngoai, nhay cam hon 1 cai dat thong thuong)
INSERT INTO permissions (code, description) VALUES
    ('ai:use', 'Hỏi đáp báo cáo bằng AI & xem gợi ý nhập hàng'),
    ('ai:manage-settings', 'Cấu hình khoá API nhà cung cấp AI');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'ai:use'
WHERE r.code IN ('owner', 'manager')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'ai:manage-settings'
WHERE r.code = 'owner'
ON CONFLICT (role_id, permission_id) DO NOTHING;
