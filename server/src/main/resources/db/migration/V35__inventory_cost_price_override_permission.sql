-- Quyen moi "Ghi de gia von" (inventory:cost-price-override) - tinh nang moi (yeu cau nguoi dung),
-- cho phep owner/manager sua truc tiep Inventory.cost_price bat ky luc nao (khac voi bien dong binh
-- quan gia quyen tu dong khi nhap hang qua AverageCostService). Chi cap cho owner + manager (mirror
-- pham vi cua order:edit/stock-take:approve), khong dua vao cross-join cua V2 (da chay xong tu lau).
INSERT INTO permissions (code, description) VALUES
    ('inventory:cost-price-override', 'Ghi đè giá vốn tồn kho');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('owner', 'manager') AND p.code = 'inventory:cost-price-override';
