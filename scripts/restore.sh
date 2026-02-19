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

echo "[done] restore completed"
