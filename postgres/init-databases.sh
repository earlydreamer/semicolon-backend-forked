#!/bin/bash
set -e

# 도메인별 데이터베이스 목록
DATABASES="ai_service auth_service user_service product_service order_service payment_service deposit_service coupon_service"

for DB in $DATABASES; do
    echo "Creating database: $DB"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE $DB;
EOSQL

    echo "Installing extensions on: $DB"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$DB" <<-EOSQL
        CREATE EXTENSION IF NOT EXISTS vector;
        CREATE EXTENSION IF NOT EXISTS pgroonga;
EOSQL
done

echo "All databases and extensions created successfully."
