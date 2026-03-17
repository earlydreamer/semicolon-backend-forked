#!/usr/bin/env bash
# 루트 .env를 읽어 공통 Secret과 서비스별 DB Secret을 생성합니다.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

K="${K:-kubectl}"
NAMESPACE="${NAMESPACE:-semicolon}"
SECRET_NAME="${SECRET_NAME:-semicolon-env}"
ENV_FILE="${ENV_FILE:-$SCRIPT_DIR/../../../.env}"
NAMESPACE_MANIFEST="${NAMESPACE_MANIFEST:-$SCRIPT_DIR/../00-namespace.yml}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Env 파일 $ENV_FILE 을(를) 찾을 수 없습니다." >&2
  exit 1
fi

TMP_ENV="$(mktemp)"
TMP_DB_ENV="$(mktemp)"
trap 'rm -f "$TMP_ENV" "$TMP_DB_ENV"' EXIT

ensure_namespace() {
  # shellcheck disable=SC2086
  if $K get namespace "$NAMESPACE" >/dev/null 2>&1; then
    return 0
  fi

  echo "네임스페이스 '$NAMESPACE'가 없어 먼저 생성합니다."

  if [ -f "$NAMESPACE_MANIFEST" ]; then
    # shellcheck disable=SC2086
    $K apply -f "$NAMESPACE_MANIFEST"
    return 0
  fi

  # shellcheck disable=SC2086
  $K create namespace "$NAMESPACE"
}

awk '
  function trim(value) {
    sub(/^[[:space:]]+/, "", value)
    sub(/[[:space:]]+$/, "", value)
    return value
  }

  /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }

  {
    line = $0
    sub(/\r$/, "", line)

    split(line, parts, "=")
    key = trim(parts[1])
    value = substr(line, index(line, "=") + 1)
    value = trim(value)

    if (key == "" ||
        index(key, "LOCAL_") == 1 ||
        key == "DB_NAME" ||
        key == "SPRING_PROFILES_ACTIVE" ||
        key == "INTERNAL_BACK_URL" ||
        key == "SERVICE_USER_URL" ||
        key == "CUSTOM_CLIENT_USER_URL" ||
        key == "CUSTOM_CLIENT_PRODUCT_URL" ||
        key == "CUSTOM_CLIENT_CART_URL" ||
        key == "CUSTOM_CLIENT_SETTLEMENT_URL" ||
        key == "CUSTOM_CLIENT_ORDER_URL" ||
        key == "CUSTOM_CLIENT_PAYMENT_URL" ||
        key == "CUSTOM_CLIENT_COUPON_URL" ||
        key == "CUSTOM_CLIENT_DEPOSIT_URL" ||
        key == "CUSTOM_CLIENT_AI_URL" ||
        value == "") {
      next
    }

    print key "=" value
  }
' "$ENV_FILE" > "$TMP_ENV"

cat >> "$TMP_ENV" <<'EOF'
SPRING_PROFILES_ACTIVE=release
SERVICE_USER_URL=http://user-service
CUSTOM_CLIENT_USER_URL=http://user-service
CUSTOM_CLIENT_PRODUCT_URL=http://product-service
CUSTOM_CLIENT_CART_URL=http://product-service
CUSTOM_CLIENT_SETTLEMENT_URL=http://settlement-service
CUSTOM_CLIENT_ORDER_URL=http://order-service
CUSTOM_CLIENT_PAYMENT_URL=http://payment-service
CUSTOM_CLIENT_COUPON_URL=http://coupon-service
CUSTOM_CLIENT_DEPOSIT_URL=http://deposit-service
CUSTOM_CLIENT_AI_URL=http://ai-service
EOF

read_env_value() {
  local target_key="$1"

  awk -v target_key="$target_key" '
    function trim(value) {
      sub(/^[[:space:]]+/, "", value)
      sub(/[[:space:]]+$/, "", value)
      return value
    }

    /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }

    {
      line = $0
      sub(/\r$/, "", line)

      split(line, parts, "=")
      key = trim(parts[1])

      if (key == target_key) {
        value = substr(line, index(line, "=") + 1)
        print trim(value)
        exit
      }
    }
  ' "$ENV_FILE"
}

ensure_namespace

# shellcheck disable=SC2086
$K -n "$NAMESPACE" create secret generic "$SECRET_NAME" \
  --from-env-file="$TMP_ENV" \
  --dry-run=client \
  -o yaml | \
  # shellcheck disable=SC2086
  $K apply -f -

while IFS=: read -r secret_name env_key default_db_name; do
  db_name="$(read_env_value "$env_key")"
  db_name="${db_name:-$default_db_name}"

  printf 'DB_NAME=%s\n' "$db_name" > "$TMP_DB_ENV"

  # shellcheck disable=SC2086
  $K -n "$NAMESPACE" create secret generic "$secret_name" \
    --from-env-file="$TMP_DB_ENV" \
    --dry-run=client \
    -o yaml | \
    # shellcheck disable=SC2086
    $K apply -f -
done <<'EOF'
ai-db:AI_DB_NAME:ai_service
auth-db:AUTH_DB_NAME:auth_service
user-db:USER_DB_NAME:user_service
product-db:PRODUCT_DB_NAME:product_service
order-db:ORDER_DB_NAME:order_service
payment-db:PAYMENT_DB_NAME:payment_service
deposit-db:DEPOSIT_DB_NAME:deposit_service
coupon-db:COUPON_DB_NAME:coupon_service
settlement-db:SETTLEMENT_DB_NAME:settlement_service
EOF

echo "네임스페이스 '$NAMESPACE'에 시크릿 '$SECRET_NAME'을(를) 적용했습니다."
