#!/usr/bin/env bash
# 루트 .env를 읽어 공통 Secret과 서비스별 DB Secret을 생성합니다.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

K="${K:-kubectl}"
NAMESPACE="${NAMESPACE:-semicolon}"
SECRET_NAME="${SECRET_NAME:-semicolon-env}"
ENV_FILE="${ENV_FILE:-$SCRIPT_DIR/../../../.env}"
NAMESPACE_MANIFEST="${NAMESPACE_MANIFEST:-$SCRIPT_DIR/../00-namespace.yml}"
DOCKER_REGISTRY_SERVER_DEFAULT="https://index.docker.io/v1/"
IMAGE_PULL_SECRET_NAME_DEFAULT="dockerhub-creds"

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

read_env_or_default() {
  local key="$1"
  local fallback="${2:-}"
  local value="${!key:-}"

  if [ -z "$value" ]; then
    value="$(read_env_value "$key")"
  fi

  if [ -z "$value" ]; then
    value="$fallback"
  fi

  printf '%s' "$value"
}

build_image_pull_secrets_patch() {
  local secret_name="$1"
  local existing_secret_names current_name
  local patch_payload='{"imagePullSecrets":['
  local first="true"

  existing_secret_names="$($K -n "$NAMESPACE" get serviceaccount default -o jsonpath='{range .imagePullSecrets[*]}{.name}{"\n"}{end}' 2>/dev/null || true)"

  if printf '%s\n' "$existing_secret_names" | grep -Fxq "$secret_name"; then
    printf ''
    return 0
  fi

  while IFS= read -r current_name; do
    [ -n "$current_name" ] || continue

    if [ "$first" = "true" ]; then
      first="false"
    else
      patch_payload+=","
    fi

    patch_payload+='{"name":"'"$current_name"'"}'
  done <<< "$existing_secret_names"

  if [ "$first" = "true" ]; then
    first="false"
  else
    patch_payload+=","
  fi

  patch_payload+='{"name":"'"$secret_name"'"}]}'
  printf '%s' "$patch_payload"
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
        key == "DOCKER_SHARED_NETWORK" ||
        key == "DOCKER_USERNAME" ||
        key == "DOCKER_PASSWORD" ||
        key == "DOCKER_REGISTRY_SERVER" ||
        key == "IMAGE_PULL_SECRET_NAME" ||
        key == "K8S_NAMESPACE" ||
        key == "REMOTE_WORKSPACE" ||
        key == "DEPLOY_INGRESS_MODE" ||
        key == "DEPLOY_ENABLE_MONITORING" ||
        key == "DEPLOY_ENABLE_CERT_MANAGER" ||
        key == "SHOWCASE_RESET_ENABLED" ||
        key == "SHOWCASE_RESET_CRON" ||
        key == "SHOWCASE_RESET_TIMEZONE" ||
        key == "SHOWCASE_RESET_TIMEOUT_SECONDS" ||
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

docker_username="$(read_env_or_default DOCKER_USERNAME)"
docker_password="$(read_env_or_default DOCKER_PASSWORD)"
docker_registry_server="$(read_env_or_default DOCKER_REGISTRY_SERVER "$DOCKER_REGISTRY_SERVER_DEFAULT")"
image_pull_secret_name="$(read_env_or_default IMAGE_PULL_SECRET_NAME "$IMAGE_PULL_SECRET_NAME_DEFAULT")"

if [ -n "$docker_username" ] && [ -n "$docker_password" ]; then
  # imagePullSecret은 앱 env secret과 별도로 docker-registry 타입으로 생성해야 한다.
  $K -n "$NAMESPACE" create secret docker-registry "$image_pull_secret_name" \
    --docker-server="$docker_registry_server" \
    --docker-username="$docker_username" \
    --docker-password="$docker_password" \
    --dry-run=client \
    -o yaml | \
    $K apply -f -

  image_pull_patch="$(build_image_pull_secrets_patch "$image_pull_secret_name")"
  if [ -n "$image_pull_patch" ]; then
    $K -n "$NAMESPACE" patch serviceaccount default --type merge -p "$image_pull_patch" >/dev/null
  fi

  echo "네임스페이스 '$NAMESPACE'에 imagePullSecret '$image_pull_secret_name'을(를) 적용했습니다."
elif [ -n "$docker_username" ] || [ -n "$docker_password" ]; then
  echo "DOCKER_USERNAME/DOCKER_PASSWORD 중 하나가 비어 있어 imagePullSecret 생성을 건너뜁니다." >&2
fi

echo "네임스페이스 '$NAMESPACE'에 시크릿 '$SECRET_NAME'을(를) 적용했습니다."
