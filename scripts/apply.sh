#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

K="${K:-kubectl}"
NS="${NS:-semicolon}"
INGRESS_MODE="${INGRESS_MODE:-local}"
ENABLE_MONITORING="${ENABLE_MONITORING:-true}"
ENABLE_CERT_MANAGER="${ENABLE_CERT_MANAGER:-false}"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env}"
REJECT_APP_LATEST_IMAGE="${REJECT_APP_LATEST_IMAGE:-false}"
declare -a RENDERED_FILES=()
APP_DEPLOYMENTS=(
  "auth:k8s/semicolon/services/auth/deploy.yml"
  "user:k8s/semicolon/services/user/deploy.yml"
  "product:k8s/semicolon/services/product/deploy.yml"
  "order:k8s/semicolon/services/order/deploy.yml"
  "coupon:k8s/semicolon/services/coupon/deploy.yml"
  "payment:k8s/semicolon/services/payment/deploy.yml"
  "deposit:k8s/semicolon/services/deposit/deploy.yml"
  "settlement:k8s/semicolon/services/settlement/deploy.yml"
  "ai:k8s/semicolon/services/ai/deploy.yml"
  "log-consumer:k8s/semicolon/monitoring/log-consumer-deploy.yml"
)

cleanup_rendered_files() {
  local rendered_file

  for rendered_file in "${RENDERED_FILES[@]}"; do
    if [ -n "$rendered_file" ] && [ -f "$rendered_file" ]; then
      rm -f "$rendered_file"
    fi
  done

  return 0
}

read_env_value() {
  local target_key="$1"

  [ -f "$ENV_FILE" ] || return 0

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

load_render_var() {
  local key="$1"
  local fallback="${2:-}"
  local value="${!key:-}"

  if [ -z "$value" ]; then
    value="$(read_env_value "$key")"
  fi

  if [ -z "$value" ] && [ -n "$fallback" ]; then
    value="$fallback"
  fi

  if [ -z "$value" ]; then
    echo "[fail] $key 값이 없어 템플릿을 렌더링할 수 없습니다." >&2
    exit 1
  fi

  export "$key=$value"
}

derive_tls_secret_name() {
  local hostname="$1"
  echo "${hostname//./-}-tls"
}

render_template() {
  local source_file="$1"
  local rendered_file

  rendered_file="$(mktemp)"

  perl -pe 's/\$\{([A-Z0-9_]+)\}/exists $ENV{$1} ? $ENV{$1} : die("[fail] missing template variable $1\n")/ge' \
    "$source_file" > "$rendered_file"

  echo "$rendered_file"
}

apply_rendered_template() {
  local source_file="$1"
  local rendered_file

  rendered_file="$(render_template "$source_file")"
  RENDERED_FILES+=("$rendered_file")

  $K -n "$NS" apply -f "$rendered_file"
}

validate_app_image() {
  local module="$1"
  local image="$2"
  local source="$3"
  local image_name

  if [ "$REJECT_APP_LATEST_IMAGE" != "true" ]; then
    return 0
  fi

  case "$image" in
    *:latest)
      echo "[fail] ${module} image가 mutable latest로 해석됐습니다. image=${image}, source=${source}" >&2
      echo "[hint] remote 배포에서는 <MODULE>_IMAGE로 SHA 태그 이미지를 넘기거나 workflow_dispatch rebuild_all_images=true로 기준선을 다시 만드세요." >&2
      exit 1
      ;;
    *@sha256:*)
      return 0
      ;;
  esac

  image_name="${image##*/}"
  case "$image_name" in
    *:*)
      return 0
      ;;
    *)
      echo "[fail] ${module} image에 명시 tag/digest가 없습니다. image=${image}, source=${source}" >&2
      echo "[hint] Kubernetes는 tag 없는 image를 latest로 취급할 수 있으므로 remote 배포에서는 SHA 태그 또는 digest를 명시해야 합니다." >&2
      exit 1
      ;;
  esac
}

resolve_app_image() {
  local module="$1"
  local env_key
  local value
  local current_image
  local fallback_image

  env_key="$(printf '%s_IMAGE' "$(printf '%s' "$module" | tr '[:lower:]-' '[:upper:]_')")"
  value="${!env_key:-}"
  if [ -n "$value" ]; then
    validate_app_image "$module" "$value" "$env_key"
    printf '%s\n' "$value"
    return 0
  fi

  if $K -n "$NS" get deploy/"$module" >/dev/null 2>&1; then
    current_image="$($K -n "$NS" get deploy/"$module" -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || true)"
    if [ -n "$current_image" ]; then
      validate_app_image "$module" "$current_image" "current deployment"
      printf '%s\n' "$current_image"
      return 0
    fi
  fi

  fallback_image="dukku/semicolon-${module}:latest"
  validate_app_image "$module" "$fallback_image" "manifest fallback"
  printf '%s\n' "$fallback_image"
}

render_app_deployment() {
  local source_file="$1"
  local image="$2"
  local rendered_file

  rendered_file="$(mktemp)"
  APP_DEPLOY_IMAGE="$image" perl -0pe 'my $img = $ENV{"APP_DEPLOY_IMAGE"}; s{^(\s*image:\s*).*$}{$1.$img}me' \
    "$source_file" > "$rendered_file"

  echo "$rendered_file"
}

apply_app_deployments() {
  local entry
  local module
  local source_file
  local image
  local rendered_file

  for entry in "${APP_DEPLOYMENTS[@]}"; do
    module="${entry%%:*}"
    source_file="${entry#*:}"
    image="$(resolve_app_image "$module")"
    rendered_file="$(render_app_deployment "$source_file" "$image")"
    RENDERED_FILES+=("$rendered_file")

    echo "[info] ${module} deployment apply: image=${image}"
    $K -n "$NS" apply -f "$rendered_file"
  done
}

apply_static_manifests() {
  while IFS= read -r manifest; do
    $K -n "$NS" apply -f "$manifest"
  done < <(
    find k8s/semicolon/dependencies k8s/semicolon/services k8s/semicolon/monitoring \
      -type f \
      \( -name '*.yml' -o -name '*.yaml' \) \
      ! -path 'k8s/semicolon/services/*/deploy.yml' \
      ! -path 'k8s/semicolon/monitoring/log-consumer-deploy.yml' \
      | sort
  )
}

verify_ingress_exists() {
  local ingress_name="$1"
  local host_name="$2"

  if ! $K -n "$NS" get ingress "$ingress_name" >/dev/null 2>&1; then
    echo "[fail] ${ingress_name} 가 존재하지 않습니다. host=${host_name}, ingress_mode=${INGRESS_MODE}, enable_monitoring=${ENABLE_MONITORING}" >&2
    exit 1
  fi

  echo "[info] ${ingress_name} 확인 완료: host=${host_name}"
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

trap cleanup_rendered_files EXIT

$K apply -f k8s/semicolon/00-namespace.yml

if [ "$ENABLE_CERT_MANAGER" = "true" ]; then
  $K apply -f k8s/semicolon/ingress/clusterissuer-letsencrypt-prod.yml
fi

apply_static_manifests
apply_app_deployments

export SHOWCASE_RESET_NAMESPACE="$NS"
export SHOWCASE_RESET_CRON="$(read_env_or_default SHOWCASE_RESET_CRON '0 0 * * *')"
export SHOWCASE_RESET_TIMEZONE="$(read_env_or_default SHOWCASE_RESET_TIMEZONE 'Asia/Seoul')"
export SHOWCASE_RESET_TIMEOUT_SECONDS="$(read_env_or_default SHOWCASE_RESET_TIMEOUT_SECONDS '900')"

showcase_reset_enabled="$(read_env_or_default SHOWCASE_RESET_ENABLED 'true')"
if [ "$showcase_reset_enabled" = "true" ]; then
  export SHOWCASE_RESET_SUSPEND="false"
else
  export SHOWCASE_RESET_SUSPEND="true"
fi

apply_rendered_template k8s/semicolon/templates/showcase-reset.yml.tpl

case "$INGRESS_MODE" in
  local)
    echo "[info] local ingress 적용 시작: enable_monitoring=${ENABLE_MONITORING}"
    $K -n "$NS" apply -f k8s/semicolon/ingress/api-gateway-ingress.local.yml

    if [ "$ENABLE_MONITORING" = "true" ]; then
      local_grafana_host="${PUBLIC_GRAFANA_HOST:-}"
      if [ -z "$local_grafana_host" ]; then
        local_grafana_host="$(read_env_value PUBLIC_GRAFANA_HOST)"
      fi

      if [ -n "$local_grafana_host" ]; then
        export PUBLIC_GRAFANA_HOST="$local_grafana_host"
        echo "[info] grafana ingress 적용: host=${PUBLIC_GRAFANA_HOST}"
        apply_rendered_template k8s/semicolon/ingress/grafana-ingress.local.yml
        verify_ingress_exists "grafana-ingress" "$PUBLIC_GRAFANA_HOST"
      else
        echo "[warn] ENABLE_MONITORING=true 이지만 PUBLIC_GRAFANA_HOST가 비어 있어 grafana ingress를 건너뜁니다."
      fi
    fi
    ;;
  prod)
    echo "[info] prod ingress 적용 시작: enable_monitoring=${ENABLE_MONITORING}"
    load_render_var "PUBLIC_API_HOST"
    load_render_var "PUBLIC_GRAFANA_HOST"
    load_render_var "PUBLIC_API_TLS_SECRET" "$(derive_tls_secret_name "$PUBLIC_API_HOST")"
    load_render_var "PUBLIC_GRAFANA_TLS_SECRET" "$(derive_tls_secret_name "$PUBLIC_GRAFANA_HOST")"

    apply_rendered_template k8s/semicolon/ingress/api-gateway-ingress.yml

    if [ "$ENABLE_MONITORING" = "true" ]; then
      echo "[info] grafana ingress 적용: host=${PUBLIC_GRAFANA_HOST}"
      apply_rendered_template k8s/semicolon/ingress/grafana-ingress.yml
      verify_ingress_exists "grafana-ingress" "$PUBLIC_GRAFANA_HOST"
    fi
    ;;
  *)
    echo "[fail] 지원하지 않는 INGRESS_MODE=$INGRESS_MODE (local|prod)" >&2
    exit 1
    ;;
esac

$K -n "$NS" get deploy -o wide
$K -n "$NS" get svc -o wide
$K -n "$NS" get ingress -o wide
