-- warehouse_staff la nguoi dung chinh cua nut "Xuat Excel" o trang Ton kho nhung V2 seed chi gan
-- report:inventory-value, thieu report:export di kem (InventoryController.export moi them yeu
-- cau ca 2 quyen, giong OrderController/ReportController) — khong co dong nay se bi 403 khi bam.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'report:export'
WHERE r.code = 'warehouse_staff'
ON CONFLICT (role_id, permission_id) DO NOTHING;
