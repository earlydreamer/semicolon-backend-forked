#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

K="${K:-kubectl}"
NAMESPACE="${NAMESPACE:-semicolon}"
SECRET_NAME="${SECRET_NAME:-semicolon-env}"
ENV_FILE="${ENV_FILE:-$SCRIPT_DIR/.env.template}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Env 템플릿 $ENV_FILE 을(를) 찾을 수 없습니다." >&2
  exit 1
fi

TMP_ENV="$(mktemp)"
trap 'rm -f "$TMP_ENV"' EXIT

awk '
  function trim(value) {
    sub(/^[[:space:]]+/, "", value)
    sub(/[[:space:]]+$/, "", value)
    return value
  }

  /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }

  {
    line = $0
    sub(/\r$/, "", line)

    split(line, parts, "=")
    key = trim(parts[1])
    value = substr(line, index(line, "=") + 1)
    value = trim(value)

    if (key != "" && value != "") {
      print key "=" value
    }
  }
' "$ENV_FILE" > "$TMP_ENV"

# shellcheck disable=SC2086
$K -n "$NAMESPACE" create secret generic "$SECRET_NAME" \
  --from-env-file="$TMP_ENV" \
  --dry-run=client \
  -o yaml | \
  # shellcheck disable=SC2086
  $K apply -f -

echo "네임스페이스 '$NAMESPACE'에 시크릿 '$SECRET_NAME'을(를) 적용했습니다."
