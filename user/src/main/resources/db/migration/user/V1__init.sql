-- V1__init.sql
-- 최소한의 스키마로 User 애플리케이션이 기동하도록 필요한 테이블을 생성합니다.

-- users 테이블
CREATE TABLE IF NOT EXISTS users (
  id serial PRIMARY KEY,
  uuid uuid NOT NULL,
  email varchar(255) NOT NULL UNIQUE,
  password varchar(255) NOT NULL,
  role varchar(50) NOT NULL,
  nickname varchar(100),
  status varchar(50) NOT NULL,
  withdrawal_email_backup varchar(255),
  withdrawal_nickname_backup varchar(100),
  deleted_at timestamptz,
  created_at timestamptz,
  updated_at timestamptz
);

-- addresses 테이블
CREATE TABLE IF NOT EXISTS addresses (
  id bigserial PRIMARY KEY,
  user_id integer NOT NULL,
  address text NOT NULL,
  zonecode varchar(10) NOT NULL,
  is_default boolean NOT NULL DEFAULT false,
  created_at timestamptz,
  updated_at timestamptz,
  CONSTRAINT fk_addresses_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- user_sanctions 테이블
CREATE TABLE IF NOT EXISTS user_sanctions (
  id serial PRIMARY KEY,
  user_id integer NOT NULL,
  sanction_type varchar(50) NOT NULL,
  reason_code varchar(80) NOT NULL,
  status varchar(50) NOT NULL,
  memo varchar(1000) NOT NULL,
  evidence_url varchar(500),
  start_at timestamptz NOT NULL,
  end_at timestamptz,
  created_by uuid NOT NULL,
  revoked_by uuid,
  revoked_at timestamptz,
  created_at timestamptz,
  updated_at timestamptz,
  CONSTRAINT fk_user_sanctions_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- user_sanction_audit_logs 테이블
CREATE TABLE IF NOT EXISTS user_sanction_audit_logs (
  id serial PRIMARY KEY,
  sanction_id integer NOT NULL,
  user_uuid uuid NOT NULL,
  action varchar(50) NOT NULL,
  sanction_type varchar(50) NOT NULL,
  reason_code varchar(80) NOT NULL,
  memo varchar(1000),
  performed_by uuid NOT NULL,
  created_at timestamptz,
  updated_at timestamptz
);

-- 간단한 인덱스(필요시 확장)
CREATE INDEX IF NOT EXISTS idx_users_uuid ON users(uuid);
CREATE INDEX IF NOT EXISTS idx_addresses_user_id ON addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sanctions_user_id ON user_sanctions(user_id);
