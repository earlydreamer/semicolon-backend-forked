-- V1__init.sql (settlement)
-- 최소 스키마: settlements

CREATE TABLE IF NOT EXISTS settlements (
  id serial PRIMARY KEY,
  uuid uuid NOT NULL,
  amount bigint NOT NULL,
  status varchar(50) NOT NULL,
  created_at timestamptz,
  updated_at timestamptz
);
