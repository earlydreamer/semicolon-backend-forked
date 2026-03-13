#!/usr/bin/env bash
set -euo pipefail

K3D_CLUSTER_NAME="${K3D_CLUSTER_NAME:-semicolon-local}"
HTTP_PORT="${HTTP_PORT:-8080}"
HTTPS_PORT="${HTTPS_PORT:-8443}"
AGENTS="${AGENTS:-0}"

if ! command -v k3d >/dev/null 2>&1; then
  echo "k3d 가 설치되어 있지 않습니다." >&2
  exit 1
fi

if k3d cluster list 2>/dev/null | awk 'NR > 1 {print $1}' | grep -Fxq "$K3D_CLUSTER_NAME"; then
  echo "k3d 클러스터 '$K3D_CLUSTER_NAME' 가 이미 존재합니다."
  exit 0
fi

k3d cluster create "$K3D_CLUSTER_NAME" \
  --servers 1 \
  --agents "$AGENTS" \
  --port "${HTTP_PORT}:80@loadbalancer" \
  --port "${HTTPS_PORT}:443@loadbalancer" \
  --wait

echo "k3d 클러스터 '$K3D_CLUSTER_NAME' 를 생성했습니다."
echo "HTTP ingress:  http://localhost:${HTTP_PORT}"
echo "HTTPS ingress: https://localhost:${HTTPS_PORT}"
