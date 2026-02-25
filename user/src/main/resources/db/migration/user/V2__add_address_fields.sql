-- V2__add_address_fields.sql
-- V1에서 누락된 addresses 테이블 컬럼 추가

ALTER TABLE addresses ADD COLUMN IF NOT EXISTS name VARCHAR(50);
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS recipient VARCHAR(50);
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS phone VARCHAR(50);
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS detail_address TEXT;
