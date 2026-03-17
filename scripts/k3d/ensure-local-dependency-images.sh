#!/usr/bin/env bash
# k3d 로컬 클러스터에서 필요한 커스텀 의존 이미지를 host에서 준비하고 import합니다.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

K="${K:-kubectl}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-dukku/semicolon-postgres:latest}"
FORCE_BUILD_POSTGRES_IMAGE="${FORCE_BUILD_POSTGRES_IMAGE:-false}"

CURRENT_CONTEXT="$($K config current-context 2>/dev/null || true)"
if [ -z "$CURRENT_CONTEXT" ]; then
  echo "[skip] 현재 kubectl context를 찾을 수 없어 로컬 의존 이미지 준비를 건너뜁니다."
  exit 0
fi

case "$CURRENT_CONTEXT" in
  k3d-*)
    ;;
  *)
    echo "[skip] 현재 context '$CURRENT_CONTEXT' 는 k3d가 아니므로 로컬 의존 이미지 준비를 건너뜁니다."
    exit 0
    ;;
esac

if ! command -v docker >/dev/null 2>&1; then
  echo "[fail] docker 명령을 찾을 수 없습니다." >&2
  exit 1
fi

if ! command -v k3d >/dev/null 2>&1; then
  echo "[fail] k3d 명령을 찾을 수 없습니다." >&2
  exit 1
fi

K3D_CLUSTER_NAME="${K3D_CLUSTER_NAME:-${CURRENT_CONTEXT#k3d-}}"

if [ "$FORCE_BUILD_POSTGRES_IMAGE" = "true" ] || ! docker image inspect "$POSTGRES_IMAGE" >/dev/null 2>&1; then
  echo "[build] postgres image '$POSTGRES_IMAGE'"
  docker build -f "$ROOT_DIR/postgres/Dockerfile" -t "$POSTGRES_IMAGE" "$ROOT_DIR/postgres"
else
  echo "[reuse] postgres image '$POSTGRES_IMAGE' 가 이미 존재합니다."
fi

echo "[import] $POSTGRES_IMAGE -> $K3D_CLUSTER_NAME"
k3d image import "$POSTGRES_IMAGE" -c "$K3D_CLUSTER_NAME"
