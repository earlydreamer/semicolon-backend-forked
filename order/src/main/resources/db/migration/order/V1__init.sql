-- V1__init.sql (order)
-- 최소 스키마: orders, order_items (간단 버전)

CREATE TABLE IF NOT EXISTS orders (
  id serial PRIMARY KEY,
  uuid uuid NOT NULL,
  user_id integer NOT NULL,
  status varchar(50) NOT NULL,
  total_amount bigint NOT NULL,
  created_at timestamptz,
  updated_at timestamptz
);

CREATE TABLE IF NOT EXISTS order_items (
  id serial PRIMARY KEY,
  order_id integer NOT NULL,
  product_id integer NOT NULL,
  product_uuid uuid,
  quantity integer NOT NULL,
  price bigint NOT NULL,
  created_at timestamptz,
  updated_at timestamptz,
  CONSTRAINT fk_order_items_order FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_orders_uuid ON orders(uuid);
