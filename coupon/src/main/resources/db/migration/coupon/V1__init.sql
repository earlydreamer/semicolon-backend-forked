-- V1__init.sql (coupon)
-- 최소 스키마: coupons, coupon_users, coupon_issue_logs

CREATE TABLE IF NOT EXISTS coupons (
  id serial PRIMARY KEY,
  code varchar(100) NOT NULL,
  description varchar(1000),
  discount_amount bigint,
  discount_percent integer,
  valid_from timestamptz,
  valid_to timestamptz,
  created_at timestamptz,
  updated_at timestamptz
);

CREATE TABLE IF NOT EXISTS coupon_users (
  id serial PRIMARY KEY,
  coupon_id integer NOT NULL,
  user_uuid uuid NOT NULL,
  used boolean DEFAULT false,
  created_at timestamptz,
  updated_at timestamptz,
  CONSTRAINT fk_coupon_users_coupon FOREIGN KEY(coupon_id) REFERENCES coupons(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS coupon_issue_logs (
  id serial PRIMARY KEY,
  coupon_id integer,
  user_uuid uuid,
  action varchar(50),
  created_at timestamptz
);
