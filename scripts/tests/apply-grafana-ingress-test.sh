#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TMP_DIR="$(mktemp -d)"
SANITIZED_APPLY="$ROOT_DIR/scripts/.apply-test.sh"
trap 'rm -rf "$TMP_DIR"; rm -f "$SANITIZED_APPLY"' EXIT

tr -d '\r' < "$ROOT_DIR/scripts/apply.sh" > "$SANITIZED_APPLY"
chmod +x "$SANITIZED_APPLY"

cat > "$TMP_DIR/env" <<'EOF'
PUBLIC_GRAFANA_HOST=grafana-dukku.earlydreamer.dev
SHOWCASE_RESET_ENABLED=false
EOF

cat > "$TMP_DIR/kubectl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

STATE_DIR="${MOCK_STATE_DIR:?}"
DROP_GRAFANA_APPLY="${MOCK_DROP_GRAFANA_APPLY:-false}"

strip_namespace() {
  if [ "${1:-}" = "-n" ]; then
    shift 2
  fi
  echo "$*"
}

cmd="$(strip_namespace "$@")"

case "$cmd" in
  "apply -f k8s/semicolon/00-namespace.yml")
    exit 0
    ;;
  "apply --recursive -f "*)
    exit 0
    ;;
  "apply -f "*)
    file_path="${cmd#apply -f }"
    if [ -f "$file_path" ] && grep -q "name: grafana-ingress" "$file_path"; then
      if [ "$DROP_GRAFANA_APPLY" != "true" ]; then
        touch "$STATE_DIR/grafana-ingress"
      fi
      exit 0
    fi

    if [ -f "$file_path" ] && grep -q "name: api-gateway-ingress" "$file_path"; then
      touch "$STATE_DIR/api-gateway-ingress"
      exit 0
    fi

    exit 0
    ;;
  "get ingress grafana-ingress")
    [ -f "$STATE_DIR/grafana-ingress" ]
    ;;
  "get deploy -o wide"|"get svc -o wide")
    exit 0
    ;;
  "get ingress -o wide")
    if [ -f "$STATE_DIR/grafana-ingress" ]; then
      cat <<'OUT'
NAME                  CLASS     HOSTS                            ADDRESS      PORTS   AGE
api-gateway-ingress   traefik   *                                172.18.0.2   80      12d
grafana-ingress       traefik   grafana-dukku.earlydreamer.dev   172.18.0.2   80      1m
OUT
    else
      cat <<'OUT'
NAME                  CLASS     HOSTS   ADDRESS      PORTS   AGE
api-gateway-ingress   traefik   *       172.18.0.2   80      12d
OUT
    fi
    exit 0
    ;;
  *)
    echo "unexpected kubectl call: $cmd" >&2
    exit 1
    ;;
esac
EOF
chmod +x "$TMP_DIR/kubectl"

run_apply() {
  local drop_grafana_apply="$1"
  local output_file="$2"

  (
    cd "$ROOT_DIR"
    MOCK_STATE_DIR="$TMP_DIR/state" \
    MOCK_DROP_GRAFANA_APPLY="$drop_grafana_apply" \
    K="$TMP_DIR/kubectl" \
    NS="semicolon" \
    ENV_FILE="$TMP_DIR/env" \
    INGRESS_MODE="local" \
    ENABLE_MONITORING="true" \
    ENABLE_CERT_MANAGER="false" \
    bash "$SANITIZED_APPLY"
  ) >"$output_file" 2>&1
}

mkdir -p "$TMP_DIR/state"
SUCCESS_LOG="$TMP_DIR/success.log"
run_apply "false" "$SUCCESS_LOG"

grep -q "\[info\] grafana ingress 적용: host=grafana-dukku.earlydreamer.dev" "$SUCCESS_LOG"
grep -q "\[info\] grafana-ingress 확인 완료: host=grafana-dukku.earlydreamer.dev" "$SUCCESS_LOG"

rm -rf "$TMP_DIR/state"
mkdir -p "$TMP_DIR/state"
FAIL_LOG="$TMP_DIR/fail.log"

if run_apply "true" "$FAIL_LOG"; then
  echo "expected apply.sh to fail when grafana ingress is missing" >&2
  exit 1
fi

grep -q "\[fail\] grafana-ingress 가 존재하지 않습니다." "$FAIL_LOG"

echo "apply-grafana-ingress-test: ok"
