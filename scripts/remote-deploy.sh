#!/usr/bin/env bash
set -euo pipefail

REMOTE_WORKSPACE="${REMOTE_WORKSPACE:?REMOTE_WORKSPACE is required}"
NS="${NS:-semicolon}"
DOCKER_USERNAME="${DOCKER_USERNAME:-}"
IMAGE_TAG="${IMAGE_TAG:-}"
MODS="${MODS:-}"
REPO="${REPO:-}"
RUN_ID="${RUN_ID:-}"
INGRESS_MODE="${INGRESS_MODE:-local}"
ENABLE_MONITORING="${ENABLE_MONITORING:-true}"
ENABLE_CERT_MANAGER="${ENABLE_CERT_MANAGER:-false}"

ENV_FILE="${REMOTE_WORKSPACE}/semicolon.env" \
K=kubectl \
NAMESPACE="$NS" \
bash "${REMOTE_WORKSPACE}/k8s/semicolon/secrets/create-secrets.sh"

K=kubectl \
NS="$NS" \
ENV_FILE="${REMOTE_WORKSPACE}/semicolon.env" \
INGRESS_MODE="$INGRESS_MODE" \
ENABLE_MONITORING="$ENABLE_MONITORING" \
ENABLE_CERT_MANAGER="$ENABLE_CERT_MANAGER" \
bash "${REMOTE_WORKSPACE}/scripts/apply.sh"

if [ -n "$MODS" ]; then
  : "${DOCKER_USERNAME:?DOCKER_USERNAME is required when rolling out images}"
  : "${IMAGE_TAG:?IMAGE_TAG is required when rolling out images}"
  : "${REPO:?REPO is required when rolling out images}"
  : "${RUN_ID:?RUN_ID is required when rolling out images}"

  K=kubectl \
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
