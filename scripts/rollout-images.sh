#!/usr/bin/env bash
set -euo pipefail

K="${K:-kubectl}"
NS="${NS:-semicolon}"
DOCKER_USERNAME="${DOCKER_USERNAME:?DOCKER_USERNAME is required}"
IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG is required}"
MODS="${MODS:?MODS is required}"
REPO="${REPO:?REPO is required}"
RUN_ID="${RUN_ID:?RUN_ID is required}"

for module in $MODS; do
  image="${DOCKER_USERNAME}/semicolon-${module}:${IMAGE_TAG}"

  if ! $K -n "$NS" get deploy/"$module" >/dev/null 2>&1; then
    echo "[fail] Deployment '$module' 가 없습니다. 최초 1회 bootstrap 후 다시 배포하세요." >&2
    exit 1
  fi

  echo "[deploy] ${module} -> ${image}"
  $K -n "$NS" set image "deploy/${module}" "${module}=${image}"

  $K -n "$NS" patch "deploy/${module}" -p "{
    \"spec\": {
      \"template\": {
        \"metadata\": {
          \"annotations\": {
            \"deploy.git.sha\": \"${IMAGE_TAG}\",
            \"deploy.git.repo\": \"${REPO}\",
            \"deploy.git.run\": \"https://github.com/${REPO}/actions/runs/${RUN_ID}\"
          }
        }
      }
    }
  }"

  $K -n "$NS" rollout status "deploy/${module}" --timeout=600s
done
