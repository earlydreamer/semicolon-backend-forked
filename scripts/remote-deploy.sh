#!/usr/bin/env bash
set -euo pipefail

REMOTE_WORKSPACE="${REMOTE_WORKSPACE:?REMOTE_WORKSPACE is required}"
NS="${NS:-semicolon}"
DOCKER_USERNAME="${DOCKER_USERNAME:?DOCKER_USERNAME is required}"
IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG is required}"
MODS="${MODS:?MODS is required}"
REPO="${REPO:?REPO is required}"
RUN_ID="${RUN_ID:?RUN_ID is required}"

ENV_FILE="${REMOTE_WORKSPACE}/semicolon.env" \
K=kubectl \
NAMESPACE="$NS" \
bash "${REMOTE_WORKSPACE}/k8s/semicolon/secrets/create-secrets.sh"

K=kubectl \
NS="$NS" \
INGRESS_MODE=local \
ENABLE_MONITORING=true \
ENABLE_CERT_MANAGER=false \
bash "${REMOTE_WORKSPACE}/scripts/apply.sh"

K=kubectl \
NS="$NS" \
DOCKER_USERNAME="$DOCKER_USERNAME" \
IMAGE_TAG="$IMAGE_TAG" \
MODS="$MODS" \
REPO="$REPO" \
RUN_ID="$RUN_ID" \
bash "${REMOTE_WORKSPACE}/scripts/rollout-images.sh"

rm -f "${REMOTE_WORKSPACE}/semicolon-bundle.tgz" "${REMOTE_WORKSPACE}/semicolon.env"
