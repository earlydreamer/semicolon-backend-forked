-- V1__init.sql (deposit)
-- 최소 스키마: deposits, deposit_histories, processed_refund_events

CREATE TABLE IF NOT EXISTS deposits (
  id serial PRIMARY KEY,
  uuid uuid NOT NULL,
  user_uuid uuid NOT NULL,
  balance bigint NOT NULL,
  created_at timestamptz,
  updated_at timestamptz
);

CREATE TABLE IF NOT EXISTS deposit_histories (
  id serial PRIMARY KEY,
  deposit_id integer NOT NULL,
  change_amount bigint NOT NULL,
  reason varchar(255),
  created_at timestamptz,
  CONSTRAINT fk_deposit_histories_deposit FOREIGN KEY(deposit_id) REFERENCES deposits(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS processed_refund_events (
  id serial PRIMARY KEY,
  refund_uuid uuid,
  payment_uuid uuid,
  processed_at timestamptz,
  created_at timestamptz
);
