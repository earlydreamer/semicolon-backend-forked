#!/usr/bin/env bash
set -euo pipefail

# =========================================================
# 로컬 nginx 제어 스크립트 (macOS / Linux / WSL)
#
# 용도
# - docker compose 를 이용해 semicolon-nginx를 띄우고/중지/재시작
# - up/down/restart/reload/logs/ps/toggle 동작 지원
# - 인증서가 없으면 mkcert/openssl으로 자동 생성
# - 네트워크 미생성 시 생성, 필요 시 orphan 컨테이너 정리 옵션 제공
# - 기본 동작은 오류 시 일시정지로 결과 확인
# =========================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.local.nginx.yml"
CERTS_DIR="${SCRIPT_DIR}/nginx-conf/certs"
CERT_FULLCHAIN="${CERTS_DIR}/fullchain.pem"
CERT_PRIVKEY="${CERTS_DIR}/privkey.pem"
NETWORK_NAME="${BACKEND_DOCKER_NETWORK:-beadv4_4_semicolon_be_default}"

# 기본값
ACTION="toggle"
PAUSE_AFTER=1
FORCE_DOWN=0
SKIP_PAUSE=0
REMOVE_ORPHANS=0
COMPOSE_CMD=""

# 인자 없이 실행하면 기본 동작(토글)에 대해 사용자 확인 시간을 둠
if (( $# == 0 )); then
  PAUSE_AFTER=1
fi

# 공통 compose 실행기
compose_cmd() {
  $COMPOSE_CMD -f "$COMPOSE_FILE" "$@"
}

compose_down() {
  compose_cmd down "$@"
}

# 업스킵 옵션 포함 up
compose_up() {
  local extra=()
  if (( REMOVE_ORPHANS == 1 )); then
    extra+=(--remove-orphans)
  fi
  compose_cmd up -d "${extra[@]}" "$@"
}

compose_logs() { compose_cmd logs -f "$@"; }
compose_ps() { compose_cmd ps "$@"; }
compose_restart() { compose_cmd restart "$@"; }

# docker compose 명령 탐색
ensure_compose() {
  if docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
    return 0
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD="docker-compose"
    return 0
  fi

  echo "[nginx-local] Docker compose command not found. Install Docker Compose plugin or docker-compose."
  return 1
}

# Docker 데몬 연결 확인
ensure_docker() {
  if ! docker version >/dev/null 2>&1; then
    echo "[nginx-local] Docker is not running or unavailable. Start Docker Desktop and retry."
    return 1
  fi
  return 0
}

# 네트워크 존재 보장
ensure_network() {
  if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
    echo "[nginx-local] Docker network '$NETWORK_NAME' not found. Creating..."
    if ! docker network create "$NETWORK_NAME" >/dev/null; then
      echo "[nginx-local] Failed to create docker network '$NETWORK_NAME'."
      return 1
    fi
  fi
  return 0
}

# 실행 중 컨테이너 존재 여부
is_running() {
  docker ps --filter "name=semicolon-nginx" --filter "status=running" -q | grep -q .
}

# 테스트/리로드에 사용할 실제 실행 컨테이너명
container_running_name() {
  docker ps --filter "name=semicolon-nginx" --filter "status=running" --format '{{.Names}}' | head -n 1
}

# TLS 인증서 준비
ensure_certs() {
  # certs 디렉터리 생성
  mkdir -p "$CERTS_DIR"

  # 이미 준비되어 있으면 스킵
  if [[ -f "$CERT_FULLCHAIN" && -f "$CERT_PRIVKEY" ]]; then
    return 0
  fi

  echo "[nginx-local] TLS cert not found. Generating certs..."

  # 우선순위: mkcert -> openssl -> dockerized openssl
  if command -v mkcert >/dev/null 2>&1; then
    if ! mkcert -cert-file "$CERT_FULLCHAIN" -key-file "$CERT_PRIVKEY" api.dukku.shop localhost 127.0.0.1 >/dev/null 2>&1; then
      echo "[nginx-local] mkcert generation failed."
      return 1
    fi
  elif command -v openssl >/dev/null 2>&1; then
    if ! openssl req -x509 -nodes -newkey rsa:2048 \
      -keyout "$CERT_PRIVKEY" -out "$CERT_FULLCHAIN" -days 365 \
      -subj "/CN=localhost" >/dev/null 2>&1; then
      echo "[nginx-local] OpenSSL generation failed."
      return 1
    fi
  else
    echo "[nginx-local] mkcert/openssl not found. Using dockerized openssl fallback..."
    if ! docker run --rm -v "${CERTS_DIR}:/out" alpine:3.20 sh -lc "apk add --no-cache openssl >/dev/null && openssl req -x509 -nodes -newkey rsa:2048 -keyout /out/privkey.pem -out /out/fullchain.pem -days 365 -subj '/CN=localhost'"; then
      echo "[nginx-local] Dockerized OpenSSL generation failed."
      return 1
    fi
  fi

  # 산출물 확인
  if [[ ! -f "$CERT_FULLCHAIN" || ! -f "$CERT_PRIVKEY" ]]; then
    echo "[nginx-local] Cert generation finished but cert files were not created."
    return 1
  fi
  return 0
}

# 종료 처리 및 pause 정책
finish() {
  local code=${1:-0}
  if (( code != 0 )); then
    PAUSE_AFTER=1
  fi

  if (( PAUSE_AFTER == 1 )) && (( SKIP_PAUSE == 0 )); then
    echo
    read -r -p "Press Enter to close... " _
  fi
  exit "$code"
}

# ===============================
# 인자 파싱
# ===============================
while (($# > 0)); do
  case "$1" in
    up|down|restart|reload|logs|ps|toggle)
      ACTION="$1"
      ;;
    pause|-p)
      PAUSE_AFTER=1
      ;;
    -y|--yes)
      FORCE_DOWN=1
      ;;
    --no-pause)
      SKIP_PAUSE=1
      ;;
    --remove-orphans|--orphan)
      REMOVE_ORPHANS=1
      ;;
    -h|--help|help)
      ACTION="help"
      ;;
    *)
      echo "[nginx-local] Unknown argument: $1"
      ACTION="help"
      ;;
  esac
  shift
done

if (( SKIP_PAUSE == 1 )); then
  PAUSE_AFTER=0
fi

# ===============================
# 사용법 출력
# ===============================
if [[ "$ACTION" == "help" ]]; then
  cat <<'USAGE'
[nginx-local] Usage:
  ./nginx-local.sh [up|down|restart|reload|logs|ps|toggle] [pause|-p] [-y|--yes] [--remove-orphans|--orphan] [--no-pause]
  toggle: stop when running, start when stopped
  down: stop semicolon-nginx (confirmation required unless -y/--yes)
USAGE
  finish 1
fi

# 실행 전 사전 체크
ensure_docker
ensure_compose

# ===============================
# action 분기
# ===============================
case "$ACTION" in
  up)
    # 신규 기동: 네트워크/인증서 보장 후 nginx 서비스 up
    ensure_network || finish 1
    ensure_certs || { echo "[nginx-local] Failed to prepare cert files."; finish 1; }
    compose_up nginx
    finish $? ;;

  down)
    # 종료 전 확인(강제 플래그가 없으면 y/N)
    if is_running && (( FORCE_DOWN == 0 )); then
      echo -n "[nginx-local] semicolon-nginx is currently UP. Proceed with down? (y/N): "
      read -r CONFIRM
      if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
        echo "[nginx-local] Canceled."
        finish 0
      fi
    fi
    compose_down
    finish $? ;;

  restart)
    # 실행중인 nginx 컨테이너만 재시작
    compose_restart nginx
    finish $? ;;

  reload)
    # 설정만 재적용
    CONTAINER_NAME="$(container_running_name)"
    if [[ -z "$CONTAINER_NAME" ]]; then
      echo "[nginx-local] semicolon-nginx is not running."
      finish 1
    fi
    docker exec "$CONTAINER_NAME" nginx -t || finish 1
    docker exec "$CONTAINER_NAME" nginx -s reload
    finish $? ;;

  logs)
    # 실시간 로그 보기
    compose_logs nginx
    finish $? ;;

  ps)
    # 컨테이너 상태
    compose_ps
    finish $? ;;

  toggle)
    # 실행 중이면 down, 아니면 up
    if is_running; then
      echo "[nginx-local] semicolon-nginx is running. Stopping..."
      compose_down
      finish $? 
    fi

    echo "[nginx-local] semicolon-nginx is not running. Starting..."
    ensure_network || finish 1
    ensure_certs || { echo "[nginx-local] Failed to prepare cert files."; finish 1; }
    compose_up nginx
    finish $? ;;

  *)
    echo "[nginx-local] Unknown action: $ACTION"
    finish 1 ;;
esac
