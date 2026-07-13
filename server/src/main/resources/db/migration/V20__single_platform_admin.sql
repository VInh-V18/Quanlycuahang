-- He thong chi co DUY NHAT 1 tai khoan Super Admin (quyet dinh san pham): khong co UI/endpoint nao
-- tao them platform_admins, nhung truoc day khong co gi o tang DB ngan viec insert them dong thu 2
-- (vd thao tac SQL tay nham). Unique index tren bieu thuc hang ((true)) lam MOI dong co cung khoa
-- index -> dong thu 2 tro di vi pham unique -> bao dam bat bien "1 dong duy nhat" ngay tai DB.
-- Username/mat khau van doi duoc binh thuong (UPDATE khong dinh gi den index nay); chi viec THEM
-- tai khoan moi bi chan.
CREATE UNIQUE INDEX uq_platform_admins_single_row ON platform_admins ((true));
