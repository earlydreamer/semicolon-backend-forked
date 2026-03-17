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
declare -a RENDERED_FILES=()

cleanup_rendered_files() {
  local rendered_file

  for rendered_file in "${RENDERED_FILES[@]:-}"; do
    [ -f "$rendered_file" ] && rm -f "$rendered_file"
  done
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
  RENDERED_FILES+=("$rendered_file")

  perl -pe 's/\$\{([A-Z0-9_]+)\}/exists $ENV{$1} ? $ENV{$1} : die("[fail] missing template variable $1\n")/ge' \
    "$source_file" > "$rendered_file"

  echo "$rendered_file"
}

trap cleanup_rendered_files EXIT

apply_recursive() {
  local target="$1"
  if [ -d "$target" ]; then
    # shellcheck disable=SC2086
    $K -n "$NS" apply --recursive -f "$target"
  fi
}

# shellcheck disable=SC2086
$K apply -f k8s/semicolon/00-namespace.yml

if [ "$ENABLE_CERT_MANAGER" = "true" ]; then
  # shellcheck disable=SC2086
  $K apply -f k8s/semicolon/ingress/clusterissuer-letsencrypt-prod.yml
fi

apply_recursive k8s/semicolon/dependencies
apply_recursive k8s/semicolon/services

if [ "$ENABLE_MONITORING" = "true" ]; then
  apply_recursive k8s/semicolon/monitoring
fi

case "$INGRESS_MODE" in
  local)
    # shellcheck disable=SC2086
    $K -n "$NS" apply -f k8s/semicolon/ingress/api-gateway-ingress.local.yml

    if [ "$ENABLE_MONITORING" = "true" ]; then
      local_grafana_host="${PUBLIC_GRAFANA_HOST:-}"
      if [ -z "$local_grafana_host" ]; then
        local_grafana_host="$(read_env_value PUBLIC_GRAFANA_HOST)"
      fi

      if [ -n "$local_grafana_host" ]; then
        export PUBLIC_GRAFANA_HOST="$local_grafana_host"
        # shellcheck disable=SC2086
        $K -n "$NS" apply -f "$(render_template k8s/semicolon/ingress/grafana-ingress.local.yml)"
      fi
    fi
    ;;
  prod)
    load_render_var "PUBLIC_API_HOST"
    load_render_var "PUBLIC_GRAFANA_HOST"
    load_render_var "PUBLIC_API_TLS_SECRET" "$(derive_tls_secret_name "$PUBLIC_API_HOST")"
    load_render_var "PUBLIC_GRAFANA_TLS_SECRET" "$(derive_tls_secret_name "$PUBLIC_GRAFANA_HOST")"

    # shellcheck disable=SC2086
    $K -n "$NS" apply -f "$(render_template k8s/semicolon/ingress/api-gateway-ingress.yml)"

    if [ "$ENABLE_MONITORING" = "true" ]; then
      # shellcheck disable=SC2086
      $K -n "$NS" apply -f "$(render_template k8s/semicolon/ingress/grafana-ingress.yml)"
    fi
    ;;
  *)
    echo "[fail] 지원하지 않는 INGRESS_MODE=$INGRESS_MODE (local|prod)" >&2
    exit 1
    ;;
esac

# shellcheck disable=SC2086
$K -n "$NS" get deploy -o wide
# shellcheck disable=SC2086
$K -n "$NS" get svc -o wide
# shellcheck disable=SC2086
$K -n "$NS" get ingress -o wide
