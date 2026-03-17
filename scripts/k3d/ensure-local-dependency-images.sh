#!/usr/bin/env bash
# k3d 로컬 클러스터에서 필요한 커스텀 이미지(의존 인프라 + 앱)를 host에서 준비하고 import합니다.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

K="${K:-kubectl}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-dukku/semicolon-postgres:latest}"
FORCE_BUILD_POSTGRES_IMAGE="${FORCE_BUILD_POSTGRES_IMAGE:-false}"
BUILD_LOCAL_APP_IMAGES_MODE="${BUILD_LOCAL_APP_IMAGES_MODE:-always}"
LOCAL_APP_MODULES="${LOCAL_APP_MODULES:-auth user product order coupon payment deposit settlement ai log-consumer}"

build_image() {
  local image="$1"
  shift

  if docker buildx version >/dev/null 2>&1; then
    docker buildx build --load --platform "$TARGET_PLATFORM" -t "$image" "$@"
  else
    docker build --platform "$TARGET_PLATFORM" -t "$image" "$@"
  fi
}

append_if_missing() {
  local candidate="$1"
  local existing

  for existing in "${IMAGES_TO_IMPORT[@]:-}"; do
    if [ "$existing" = "$candidate" ]; then
      return 0
    fi
  done

  IMAGES_TO_IMPORT+=("$candidate")
}

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

case "$BUILD_LOCAL_APP_IMAGES_MODE" in
  always|missing|skip)
    ;;
  *)
    echo "[fail] 지원하지 않는 BUILD_LOCAL_APP_IMAGES_MODE=$BUILD_LOCAL_APP_IMAGES_MODE (always|missing|skip)" >&2
    exit 1
    ;;
esac

K3D_CLUSTER_NAME="${K3D_CLUSTER_NAME:-${CURRENT_CONTEXT#k3d-}}"
NODE_ARCH="$($K get nodes -o jsonpath='{.items[0].status.nodeInfo.architecture}' 2>/dev/null || true)"

case "$NODE_ARCH" in
  amd64|arm64)
    TARGET_PLATFORM="linux/$NODE_ARCH"
    ;;
  *)
    TARGET_PLATFORM="${LOCAL_K8S_IMAGE_PLATFORM:-linux/arm64}"
    ;;
esac

declare -a IMAGES_TO_IMPORT=()

if [ "$FORCE_BUILD_POSTGRES_IMAGE" = "true" ] || ! docker image inspect "$POSTGRES_IMAGE" >/dev/null 2>&1; then
  echo "[build] postgres image '$POSTGRES_IMAGE' ($TARGET_PLATFORM)"
  build_image "$POSTGRES_IMAGE" -f "$ROOT_DIR/postgres/Dockerfile" "$ROOT_DIR/postgres"
else
  echo "[reuse] postgres image '$POSTGRES_IMAGE' 가 이미 존재합니다."
fi

append_if_missing "$POSTGRES_IMAGE"

if [ "$BUILD_LOCAL_APP_IMAGES_MODE" != "skip" ]; then
  read -r -a LOCAL_APP_MODULE_ARRAY <<< "$LOCAL_APP_MODULES"

  for module in "${LOCAL_APP_MODULE_ARRAY[@]}"; do
    image="dukku/semicolon-${module}:latest"

    if [ "$BUILD_LOCAL_APP_IMAGES_MODE" = "always" ] || ! docker image inspect "$image" >/dev/null 2>&1; then
      echo "[build] app image '$image' from module '$module' ($TARGET_PLATFORM)"
      build_image "$image" \
        -f "$ROOT_DIR/Dockerfile" \
        --build-arg "MODULE_NAME=$module" \
        "$ROOT_DIR"
    else
      echo "[reuse] app image '$image' 가 이미 존재합니다."
    fi

    append_if_missing "$image"
  done
fi

echo "[import] ${IMAGES_TO_IMPORT[*]} -> $K3D_CLUSTER_NAME"
k3d image import "${IMAGES_TO_IMPORT[@]}" -c "$K3D_CLUSTER_NAME"
