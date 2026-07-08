-- Tai khoan Super Admin DAU TIEN (khong co UI dang ky - phai seed truoc, giong cach owner01 duoc
-- seed san o V2). Mat khau (BCrypt strength 12, dung cung BCryptPasswordEncoder(12) o SecurityConfig):
-- username: superadmin
-- password: GvCpcK55Kv2KJ%CpqT4g
-- Doi mat khau ngay sau lan dang nhap dau tien qua POST /api/v1/platform-admin/auth/change-password.
INSERT INTO platform_admins (username, password_hash, full_name)
VALUES (
    'superadmin',
    '$2b$12$WV5ZEda3QWDLxKejonBGF.eSJXI9h15.MdsUqizVoo9C3heCke9y.',
    'Quan tri he thong'
);
