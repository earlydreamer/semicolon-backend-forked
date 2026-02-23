-- V1__init.sql (payment)
-- 최소 스키마: payments, payment_order_items, refunds, refund_items

CREATE TABLE IF NOT EXISTS payments (
  id serial PRIMARY KEY,
  uuid uuid NOT NULL,
  order_uuid uuid NOT NULL,
  coupon_uuid uuid,
  user_uuid uuid NOT NULL,
  amount bigint NOT NULL,
  payment_deposit bigint NOT NULL,
  amount_pg bigint NOT NULL,
  payment_type varchar(50) NOT NULL,
  payment_status varchar(50) NOT NULL,
  pg_payment_key varchar(255),
  approved_at timestamptz,
  created_at timestamptz,
  updated_at timestamptz
);

CREATE TABLE IF NOT EXISTS payment_order_items (
  id serial PRIMARY KEY,
  payment_id integer NOT NULL,
  product_id integer,
  product_uuid uuid,
  quantity integer NOT NULL,
  price bigint NOT NULL,
  CONSTRAINT fk_payment_order_items_payment FOREIGN KEY(payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS refunds (
  id serial PRIMARY KEY,
  payment_id integer NOT NULL,
  refund_amount bigint NOT NULL,
  idempotency_key varchar(255),
  created_at timestamptz
);

CREATE TABLE IF NOT EXISTS refund_items (
  id serial PRIMARY KEY,
  refund_id integer NOT NULL,
  order_item_id integer,
  amount bigint NOT NULL,
  CONSTRAINT fk_refund_items_refund FOREIGN KEY(refund_id) REFERENCES refunds(id) ON DELETE CASCADE
);
