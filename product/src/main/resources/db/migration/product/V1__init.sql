-- V1__init.sql (product)
-- 최소한의 스키마 (products, categories, product_images, product_tags, tags)

CREATE TABLE IF NOT EXISTS categories (
  id serial PRIMARY KEY,
  name varchar(255) NOT NULL,
  parent_id integer,
  created_at timestamptz,
  updated_at timestamptz
);

CREATE TABLE IF NOT EXISTS products (
  id serial PRIMARY KEY,
  uuid uuid NOT NULL,
  seller_uuid uuid NOT NULL,
  category_id integer NOT NULL,
  title varchar(200) NOT NULL,
  description text,
  price bigint NOT NULL,
  shipping_fee bigint NOT NULL,
  condition_status varchar(50) NOT NULL,
  sale_status varchar(50) NOT NULL,
  visibility_status varchar(50) NOT NULL,
  view_count integer NOT NULL DEFAULT 0,
  like_count integer NOT NULL DEFAULT 0,
  comment_count integer NOT NULL DEFAULT 0,
  deleted_at timestamptz,
  reserved_order_uuid uuid,
  created_at timestamptz,
  updated_at timestamptz,
  CONSTRAINT fk_products_category FOREIGN KEY(category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS tags (
  id serial PRIMARY KEY,
  name varchar(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS product_tags (
  id serial PRIMARY KEY,
  product_id integer NOT NULL,
  tag_id integer NOT NULL,
  CONSTRAINT fk_product_tags_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_product_tags_tag FOREIGN KEY(tag_id) REFERENCES tags(id)
);

CREATE TABLE IF NOT EXISTS product_images (
  id serial PRIMARY KEY,
  product_id integer NOT NULL,
  image_url varchar(1000),
  sort_order integer,
  CONSTRAINT fk_product_images_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_products_uuid ON products(uuid);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
