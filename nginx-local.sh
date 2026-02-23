#!/usr/bin/env bash
set -u

# =========================================================
# [개요]
# - 로컬 nginx Docker Compose 제어 스크립트 (macOS/Linux)
# - 인자 없이 실행하면 toggle (running이면 down, 아니면 up)
# - up 실행 시 PEM 인증서가 없으면 self-signed 인증서 자동 생성
# =========================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.local.nginx.yml"
CERTS_DIR="${SCRIPT_DIR}/nginx-conf/certs"
CERT_FULLCHAIN="${CERTS_DIR}/fullchain.pem"
CERT_PRIVKEY="${CERTS_DIR}/privkey.pem"
NETWORK_NAME="${BACKEND_DOCKER_NETWORK:-beadv4_4_semicolon_be_default}"

ACTION="${1:-toggle}"
PAUSE_AFTER=0
FORCE_DOWN=0

if [[ $# -eq 0 ]]; then
  PAUSE_AFTER=1
fi

# [옵션]
# - pause / -p: 종료 전 대기
# - -y / --yes: down 확인 프롬프트 생략
for arg in "$@"; do
  case "$arg" in
    pause|-p) PAUSE_AFTER=1 ;;
    -y|--yes) FORCE_DOWN=1 ;;
  esac
done

compose() {
  docker compose -f "$COMPOSE_FILE" "$@"
}

ensure_network() {
  # 외부 네트워크가 없으면 자동 생성
  if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
    echo "[nginx-local] Docker network '$NETWORK_NAME' not found. Creating..."
    docker network create "$NETWORK_NAME" >/dev/null
  fi
}

is_running() {
  # semicolon-nginx가 running 상태인지 확인
  docker ps --filter "name=semicolon-nginx" --filter "status=running" -q | grep -q .
}

ensure_certs() {
  # PEM 인증서가 없으면 self-signed 인증서 자동 생성
  mkdir -p "$CERTS_DIR"

  if [[ -f "$CERT_FULLCHAIN" && -f "$CERT_PRIVKEY" ]]; then
    return 0
  fi

  echo "[nginx-local] TLS cert not found. Generating self-signed certs..."
  docker run --rm -v "${CERTS_DIR}:/out" alpine:3.20 sh -c \
    "apk add --no-cache openssl >/dev/null && \
     openssl req -x509 -nodes -newkey rsa:2048 \
     -keyout /out/privkey.pem \
     -out /out/fullchain.pem \
     -days 365 \
     -subj '/CN=api.dukku.shop' \
     -addext 'subjectAltName=DNS:api.dukku.shop,DNS:localhost,IP:127.0.0.1'"

  [[ -f "$CERT_FULLCHAIN" && -f "$CERT_PRIVKEY" ]]
}

finish() {
  local code="${1:-0}"
  if [[ "$PAUSE_AFTER" == "1" ]]; then
    echo
    read -r -p "엔터를 누르면 종료합니다..." _
  fi
  exit "$code"
}

case "$ACTION" in
  up)
    # 네트워크/인증서 준비 후 nginx만 기동
    ensure_network || finish 1
    ensure_certs || {
      echo "[nginx-local] Failed to prepare cert files."
      finish 1
    }
    compose up -d nginx
    finish $?
    ;;

  down)
    # running 상태면 확인 후 down
    if is_running && [[ "$FORCE_DOWN" != "1" ]]; then
      read -r -p "[nginx-local] semicolon-nginx is currently UP. Proceed with down? (y/N): " confirm
      if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
        echo "[nginx-local] Canceled."
        finish 0
      fi
    fi
    compose down
    finish $?
    ;;

  toggle)
    # running이면 down, 아니면 up
    if is_running; then
      echo "[nginx-local] semicolon-nginx is running. Stopping..."
      compose down
      finish $?
    fi
    echo "[nginx-local] semicolon-nginx is not running. Starting..."
    ensure_network || finish 1
    ensure_certs || {
      echo "[nginx-local] Failed to prepare cert files."
      finish 1
    }
    compose up -d nginx
    finish $?
    ;;

  restart)
    compose restart nginx
    finish $?
    ;;

  reload)
    docker exec semicolon-nginx nginx -t || finish 1
    docker exec semicolon-nginx nginx -s reload
    finish $?
    ;;

  logs)
    compose logs -f nginx
    finish $?
    ;;

  ps)
    compose ps
    finish $?
    ;;

  *)
    echo "[nginx-local] Unknown action: $ACTION"
    echo "Usage: ./nginx-local.sh [up|down|restart|reload|logs|ps|toggle] [pause|-p] [-y|--yes]"
    finish 1
    ;;
esac
