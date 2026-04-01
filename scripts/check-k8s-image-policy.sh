#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

status=0

while IFS= read -r deploy_file; do
  if grep -Eq 'imagePullPolicy:[[:space:]]*Always' "$deploy_file"; then
    echo "[fail] $deploy_file uses imagePullPolicy: Always"
    status=1
  fi
done < <(find k8s/semicolon/services -mindepth 2 -maxdepth 2 -name deploy.yml | sort)

if [ "$status" -ne 0 ]; then
  echo "[hint] App deployments should avoid imagePullPolicy: Always to prevent repeated registry pulls during scale or reset flows."
  exit "$status"
fi

echo "[ok] Kubernetes app deployment pull policy check passed"
