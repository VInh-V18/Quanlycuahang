-- Quyen moi "Sua don hang da hoan tat" (order:edit) - tinh nang moi (yeu cau nguoi dung), khac voi
-- order:void da co san tu V2. Chi cap cho owner + manager (giong pham vi order:void) - sua gia/so
-- luong 1 don da ban la hanh dong nhay cam, gioi han o 2 vai tro cao nhat, khong dua vao cross-join
-- cua V2 (migration do da chay xong tu lau, khong tu chay lai cho quyen moi nay).
INSERT INTO permissions (code, description) VALUES
    ('order:edit', 'Sửa đơn hàng đã hoàn tất');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('owner', 'manager') AND p.code = 'order:edit';
