#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

K="${K:-kubectl}"
NS="${NS:-semicolon}"
INGRESS_MODE="${INGRESS_MODE:-local}"
ENABLE_MONITORING="${ENABLE_MONITORING:-true}"
ENABLE_EDGE="${ENABLE_EDGE:-false}"
ENABLE_CERT_MANAGER="${ENABLE_CERT_MANAGER:-false}"

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
    ;;
  prod)
    # shellcheck disable=SC2086
    $K -n "$NS" apply -f k8s/semicolon/ingress/api-gateway-ingress.yml
    ;;
  *)
    echo "[fail] 지원하지 않는 INGRESS_MODE=$INGRESS_MODE (local|prod)" >&2
    exit 1
    ;;
esac

if [ "$ENABLE_EDGE" = "true" ]; then
  apply_recursive k8s/semicolon/edge
fi

# shellcheck disable=SC2086
$K -n "$NS" get deploy -o wide
# shellcheck disable=SC2086
$K -n "$NS" get svc -o wide
# shellcheck disable=SC2086
$K -n "$NS" get ingress -o wide
