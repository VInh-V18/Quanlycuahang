-- Prompt #9 (P2, CI/CD - backup/restore that): phat hien khi chay THAT scripts/restore-test.sh lan
-- dau (dung dung nguyen tac "backup chua tung restore thu = chua co backup") - ham
-- immutable_unaccent() o V1__init_schema.sql goi unaccent(...) KHONG ghi ro schema
-- (public.unaccent). Postgres "inline" ham SQL IMMUTABLE nay khi tao index bieu thuc
-- (idx_customers_name_trgm/idx_products_name_trgm) va kiem tra lai bang 1 search_path RIENG (an
-- toan hon search_path phien lam viec thuong) - search_path do KHONG mac dinh co 'public', nen ban
-- than luc TAO MOI qua Flyway (session thuong, search_path co 'public') thi qua, nhung luc
-- pg_restore tao lai INDEX (sau khi restore du lieu) lai that bai voi loi "function unaccent(unknown,
-- text) does not exist" - du liệu van restore dung, chi 2 index tim kiem khong dau nay bi thieu.
-- Ghi ro schema public.unaccent() de KHONG con phu thuoc search_path o bat ky ngu canh nao nua.
CREATE OR REPLACE FUNCTION immutable_unaccent(text) RETURNS text AS $$
    SELECT public.unaccent('unaccent', $1)
$$ LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT;
