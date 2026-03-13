#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

K="${K:-kubectl}"
NS="${NS:-semicolon}"
APP_SET="${APP_SET:-core}"
INGRESS_MODE="${INGRESS_MODE:-local}"
ENABLE_MONITORING="${ENABLE_MONITORING:-true}"
ENABLE_CERT_MANAGER="${ENABLE_CERT_MANAGER:-false}"

wait_for_api() {
  echo "[wait] kubernetes api ready..."
  for _ in $(seq 1 60); do
    # shellcheck disable=SC2086
    if $K version --request-timeout=5s >/dev/null 2>&1; then
      echo "[ok] kubernetes api ready"
      return 0
    fi
    sleep 2
  done

  echo "[fail] kubernetes api not ready" >&2
  return 1
}

scale_if_exists() {
  local deployment="$1"
  local replicas="$2"

  # shellcheck disable=SC2086
  if $K -n "$NS" get deploy/"$deployment" >/dev/null 2>&1; then
    # shellcheck disable=SC2086
    $K -n "$NS" scale deploy/"$deployment" --replicas="$replicas"
  fi
}

rollout_if_exists() {
  local deployment="$1"

  # shellcheck disable=SC2086
  if $K -n "$NS" get deploy/"$deployment" >/dev/null 2>&1; then
    # shellcheck disable=SC2086
    $K -n "$NS" rollout status deploy/"$deployment" --timeout=600s
  fi
}

case "$APP_SET" in
  core|all)
    ;;
  *)
    echo "[fail] 지원하지 않는 APP_SET=$APP_SET (core|all)" >&2
    exit 1
    ;;
esac

wait_for_api

INGRESS_MODE="$INGRESS_MODE" \
ENABLE_MONITORING="$ENABLE_MONITORING" \
ENABLE_CERT_MANAGER="$ENABLE_CERT_MANAGER" \
K="$K" \
NS="$NS" \
bash scripts/apply.sh

echo "[scale] apps and platform down"
for deployment in auth user product order coupon payment deposit settlement ai prometheus grafana log-consumer; do
  scale_if_exists "$deployment" 0
done

echo "[deps] up first"
for deployment in postgres redis redpanda elasticsearch mongodb; do
  scale_if_exists "$deployment" 1
  rollout_if_exists "$deployment"
done

if [ "$ENABLE_MONITORING" = "true" ]; then
  echo "[platform] up"
  for deployment in prometheus grafana log-consumer; do
    scale_if_exists "$deployment" 1
    rollout_if_exists "$deployment"
  done
fi

echo "[core] up minimal path (auth/user/product)"
for deployment in auth user product; do
  scale_if_exists "$deployment" 1
  rollout_if_exists "$deployment"
done

if [ "$APP_SET" = "all" ]; then
  echo "[apps] up full stack"
  for deployment in order payment coupon deposit settlement ai; do
    scale_if_exists "$deployment" 1
    rollout_if_exists "$deployment"
  done
fi

echo "[done] restore completed"
