#!/usr/bin/env bash
set -euo pipefail

REMOTE_WORKSPACE="${REMOTE_WORKSPACE:?REMOTE_WORKSPACE is required}"
NS="${NS:-semicolon}"
K="${K:-kubectl}"
DOCKER_USERNAME="${DOCKER_USERNAME:-}"
DOCKER_PASSWORD="${DOCKER_PASSWORD:-}"
IMAGE_TAG="${IMAGE_TAG:-}"
MODS="${MODS:-}"
REPO="${REPO:-}"
RUN_ID="${RUN_ID:-}"
INGRESS_MODE="${INGRESS_MODE:-local}"
ENABLE_MONITORING="${ENABLE_MONITORING:-true}"
ENABLE_CERT_MANAGER="${ENABLE_CERT_MANAGER:-false}"
REJECT_APP_LATEST_IMAGE="${REJECT_APP_LATEST_IMAGE:-true}"
DOCKER_REGISTRY_SERVER="${DOCKER_REGISTRY_SERVER:-https://index.docker.io/v1/}"
IMAGE_PULL_SECRET_NAME="${IMAGE_PULL_SECRET_NAME:-dockerhub-creds}"

resolve_kubectl() {
  local candidate

  for candidate in \
    "$K" \
    kubectl \
    /opt/homebrew/bin/kubectl \
    /usr/local/bin/kubectl \
    /usr/bin/kubectl \
    /snap/bin/kubectl; do
    [ -n "$candidate" ] || continue

    if [[ "$candidate" == */* ]]; then
      if [ -x "$candidate" ]; then
        printf '%s\n' "$candidate"
        return 0
      fi
      continue
    fi

    if command -v "$candidate" >/dev/null 2>&1; then
      command -v "$candidate"
      return 0
    fi
  done

  echo "[fail] kubectl 실행 파일을 찾을 수 없습니다. K 환경변수 또는 PATH를 확인하세요." >&2
  exit 127
}

K_BIN="$(resolve_kubectl)"

if [ -n "$MODS" ]; then
  : "${DOCKER_USERNAME:?DOCKER_USERNAME is required when rolling out images}"
  : "${IMAGE_TAG:?IMAGE_TAG is required when rolling out images}"

  for module in $MODS; do
    image_env_key="$(printf '%s_IMAGE' "$(printf '%s' "$module" | tr '[:lower:]-' '[:upper:]_')")"
    export "${image_env_key}=${DOCKER_USERNAME}/semicolon-${module}:${IMAGE_TAG}"
  done
fi

ENV_FILE="${REMOTE_WORKSPACE}/semicolon.env" \
K="$K_BIN" \
NAMESPACE="$NS" \
DOCKER_USERNAME="$DOCKER_USERNAME" \
DOCKER_PASSWORD="$DOCKER_PASSWORD" \
DOCKER_REGISTRY_SERVER="$DOCKER_REGISTRY_SERVER" \
IMAGE_PULL_SECRET_NAME="$IMAGE_PULL_SECRET_NAME" \
bash "${REMOTE_WORKSPACE}/k8s/semicolon/secrets/create-secrets.sh"

K="$K_BIN" \
NS="$NS" \
ENV_FILE="${REMOTE_WORKSPACE}/semicolon.env" \
INGRESS_MODE="$INGRESS_MODE" \
ENABLE_MONITORING="$ENABLE_MONITORING" \
ENABLE_CERT_MANAGER="$ENABLE_CERT_MANAGER" \
REJECT_APP_LATEST_IMAGE="$REJECT_APP_LATEST_IMAGE" \
bash "${REMOTE_WORKSPACE}/scripts/apply.sh"

if [ -n "$MODS" ]; then
  : "${DOCKER_USERNAME:?DOCKER_USERNAME is required when rolling out images}"
  : "${IMAGE_TAG:?IMAGE_TAG is required when rolling out images}"
  : "${REPO:?REPO is required when rolling out images}"
  : "${RUN_ID:?RUN_ID is required when rolling out images}"

  K="$K_BIN" \
  NS="$NS" \
  DOCKER_USERNAME="$DOCKER_USERNAME" \
  IMAGE_TAG="$IMAGE_TAG" \
  MODS="$MODS" \
  REPO="$REPO" \
  RUN_ID="$RUN_ID" \
  bash "${REMOTE_WORKSPACE}/scripts/rollout-images.sh"
else
  echo "[skip] 롤아웃할 앱 이미지가 없어 infra-only 배포만 수행합니다."
fi

rm -f "${REMOTE_WORKSPACE}/semicolon-bundle.tgz" "${REMOTE_WORKSPACE}/semicolon.env"
