-- Quyen chay/xem doi soat toan ven du lieu - chi owner/manager (roadmap Prompt #6: "quyen owner/
-- manager"), dung dung khuon mau resource:action da co san thay vi kiem tra role truc tiep (nhat
-- quan voi toan bo he thong con lai).
INSERT INTO permissions (code, description) VALUES
    ('reconciliation:run', 'Chạy & xem kết quả đối soát toàn vẹn dữ liệu');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'reconciliation:run'
WHERE r.code IN ('owner', 'manager')
ON CONFLICT (role_id, permission_id) DO NOTHING;
