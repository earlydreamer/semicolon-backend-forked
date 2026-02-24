#!/usr/bin/env bash
set -euo pipefail

K="${K:-sudo kubectl}"
NS="${NS:-semicolon}"

sudo systemctl enable --now k3s >/dev/null 2>&1 || true

echo "[wait] k3s api ready..."
for i in $(seq 1 60); do
  if $K version >/dev/null 2>&1; then
    echo "[ok] k3s api ready"
    break
  fi
  sleep 2
done

if ! $K version >/dev/null 2>&1; then
  echo "[fail] k3s api not ready"
  exit 1
fi

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR"

bash scripts/apply.sh

echo "[scale] apps down (keep deps up)"
$K -n "$NS" scale deploy/auth deploy/user deploy/product deploy/order deploy/coupon deploy/payment deploy/deposit deploy/settlement deploy/ai --replicas=0 || true

echo "[deps] up first"
$K -n "$NS" scale deploy/postgres deploy/redis deploy/redpanda --replicas=1 || true
$K -n "$NS" rollout status deploy/postgres --timeout=600s
$K -n "$NS" rollout status deploy/redis --timeout=600s
$K -n "$NS" rollout status deploy/redpanda --timeout=600s

echo "[core] up minimal path (auth/user/product)"
for m in auth user product; do
  $K -n "$NS" scale deploy/${m} --replicas=1
  $K -n "$NS" rollout status deploy/${m} --timeout=600s
done

echo "[done] restore completed"
