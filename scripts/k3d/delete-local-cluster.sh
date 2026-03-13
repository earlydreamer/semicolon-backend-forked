#!/usr/bin/env bash
set -euo pipefail

K3D_CLUSTER_NAME="${K3D_CLUSTER_NAME:-semicolon-local}"

if ! command -v k3d >/dev/null 2>&1; then
  echo "k3d 가 설치되어 있지 않습니다." >&2
  exit 1
fi

if ! k3d cluster list 2>/dev/null | awk 'NR > 1 {print $1}' | grep -Fxq "$K3D_CLUSTER_NAME"; then
  echo "k3d 클러스터 '$K3D_CLUSTER_NAME' 가 존재하지 않습니다."
  exit 0
fi

k3d cluster delete "$K3D_CLUSTER_NAME"

echo "k3d 클러스터 '$K3D_CLUSTER_NAME' 를 삭제했습니다."
