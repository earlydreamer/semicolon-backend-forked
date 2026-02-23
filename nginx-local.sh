#!/usr/bin/env bash
set -euo pipefail
SCRIPT_NAME="$(basename "$0")"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.local.nginx.yml"
CERTS_DIR="${SCRIPT_DIR}/nginx-conf/certs"
CERT_FULLCHAIN="${CERTS_DIR}/fullchain.pem"
CERT_PRIVKEY="${CERTS_DIR}/privkey.pem"
NETWORK_NAME="${BACKEND_DOCKER_NETWORK:-beadv4_4_semicolon_be_default}"
COMPOSE_MODE=""

ACTION="toggle"
PAUSE_AFTER=1
FORCE_DOWN=0
SKIP_PAUSE=0
REMOVE_ORPHANS=0

handle_error() {
  local exit_code="$1"
  local lineno="${2:-$LINENO}"
  echo "[${SCRIPT_NAME}] Error: command failed at line ${lineno} (exit ${exit_code})." >&2
}
trap 'handle_error $? ${BASH_LINENO[0]}' ERR

if (( $# == 0 )); then
  PAUSE_AFTER=1
fi

is_tty() {
  [[ -t 0 ]]
}

compose_cmd() {
  if [[ -z "${COMPOSE_MODE}" ]]; then
    echo "[${SCRIPT_NAME}] Compose mode is not initialized." >&2
    return 1
  fi

  case "${COMPOSE_MODE}" in
    plugin)
      docker compose -f "$COMPOSE_FILE" "$@"
      ;;
    legacy)
      docker-compose -f "$COMPOSE_FILE" "$@"
      ;;
    *)
      echo "[${SCRIPT_NAME}] Unsupported compose mode: ${COMPOSE_MODE}" >&2
      return 1
      ;;
  esac
}

compose_down() {
  compose_cmd down "$@"
}

compose_up() {
  local -a extra=( )
  if (( REMOVE_ORPHANS == 1 )); then
    extra+=(--remove-orphans)
  fi
  compose_cmd up -d "${extra[@]}" "$@"
}

compose_logs() { compose_cmd logs -f "$@"; }
compose_ps() { compose_cmd ps "$@"; }
compose_restart() { compose_cmd restart "$@"; }

ensure_compose() {
  if [[ ! -f "$COMPOSE_FILE" ]]; then
    echo "[${SCRIPT_NAME}] Compose file not found: $COMPOSE_FILE" >&2
    return 1
  fi

  if docker compose version >/dev/null 2>&1; then
    COMPOSE_MODE="plugin"
    return 0
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_MODE="legacy"
    return 0
  fi

  echo "[${SCRIPT_NAME}] Docker Compose command not found. Install Docker Compose plugin or docker-compose." >&2
  return 1
}

ensure_docker() {
  if ! docker version >/dev/null 2>&1; then
    echo "[${SCRIPT_NAME}] Docker is not running or unavailable. Start Docker Desktop and retry." >&2
    return 1
  fi
}

ensure_network() {
  if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
    echo "[${SCRIPT_NAME}] Docker network '$NETWORK_NAME' not found. Creating..."
    if ! docker network create "$NETWORK_NAME" >/dev/null; then
      echo "[${SCRIPT_NAME}] Failed to create docker network '$NETWORK_NAME'." >&2
      return 1
    fi
  fi
}

is_running() {
  docker ps --filter "name=semicolon-nginx" --filter "status=running" -q | grep -q .
}

container_running_name() {
  docker ps --filter "name=semicolon-nginx" --filter "status=running" --format '{{.Names}}' | head -n 1
}

ensure_certs() {
  mkdir -p "$CERTS_DIR"

  rm -f "$CERT_FULLCHAIN" "$CERT_PRIVKEY"
  echo "[${SCRIPT_NAME}] Generating TLS certs..."

  if command -v mkcert >/dev/null 2>&1; then
    echo "[${SCRIPT_NAME}] Using mkcert ..."
    mkcert -install >/dev/null 2>&1 || echo "[${SCRIPT_NAME}] mkcert -install failed or requires elevated privileges. Certificate may not be trusted."
    if ! mkcert -cert-file "$CERT_FULLCHAIN" -key-file "$CERT_PRIVKEY" api.dukku.shop localhost 127.0.0.1 >/dev/null 2>&1; then
      echo "[${SCRIPT_NAME}] mkcert generation failed." >&2
      return 1
    fi
  elif command -v openssl >/dev/null 2>&1; then
    if ! openssl req -x509 -nodes -newkey rsa:2048 \
      -keyout "$CERT_PRIVKEY" -out "$CERT_FULLCHAIN" -days 365 \
      -subj "/CN=localhost" >/dev/null 2>&1; then
      echo "[${SCRIPT_NAME}] OpenSSL generation failed." >&2
      return 1
    fi
  else
    echo "[${SCRIPT_NAME}] mkcert/openssl not found. Using dockerized openssl fallback..."
    if ! docker run --rm -v "${CERTS_DIR}:/out" alpine:3.20 sh -lc "apk add --no-cache openssl >/dev/null && openssl req -x509 -nodes -newkey rsa:2048 -keyout /out/privkey.pem -out /out/fullchain.pem -days 365 -subj '/CN=localhost'"; then
      echo "[${SCRIPT_NAME}] Dockerized OpenSSL generation failed." >&2
      return 1
    fi
  fi

  if [[ ! -f "$CERT_FULLCHAIN" || ! -f "$CERT_PRIVKEY" ]]; then
    echo "[${SCRIPT_NAME}] Cert generation finished but cert files were not created." >&2
    return 1
  fi

  chmod 600 "$CERT_PRIVKEY" 2>/dev/null || true
  chmod 644 "$CERT_FULLCHAIN" 2>/dev/null || true
}

finish() {
  local code=${1:-0}
  if (( code != 0 )); then
    PAUSE_AFTER=1
  fi

  if (( PAUSE_AFTER == 1 )) && (( SKIP_PAUSE == 0 )) && is_tty; then
    echo
    read -r -p "Press Enter to close... " _
  fi
  exit "$code"
}

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
      echo "[${SCRIPT_NAME}] Unknown argument: $1"
      ACTION="help"
      ;;
  esac
  shift
 done

if (( SKIP_PAUSE == 1 )); then
  PAUSE_AFTER=0
fi
if [[ "$ACTION" == "logs" || "$ACTION" == "ps" ]]; then
  SKIP_PAUSE=1
  PAUSE_AFTER=0
fi

if [[ "$ACTION" == "help" ]]; then
  cat <<'USAGE'
[nginx-local] Usage:
  ./nginx-local.sh [up|down|restart|reload|logs|ps|toggle] [pause|-p] [-y|--yes] [--remove-orphans|--orphan] [--no-pause]
  toggle: stop when running, start when stopped
  down: stop semicolon-nginx (confirmation required unless -y/--yes)
USAGE
  finish 1
fi

ensure_docker
ensure_compose

case "$ACTION" in
  up)
    ensure_network || finish 1
    ensure_certs || { echo "[${SCRIPT_NAME}] Failed to prepare cert files."; finish 1; }
    compose_up nginx
    finish $? ;;

  down)
    if is_running && (( FORCE_DOWN == 0 )); then
      if is_tty; then
        echo -n "[${SCRIPT_NAME}] semicolon-nginx is currently UP. Proceed with down? (y/N): "
        read -r CONFIRM
        if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
          echo "[${SCRIPT_NAME}] Canceled."
          finish 0
        fi
      else
        echo "[${SCRIPT_NAME}] semicolon-nginx is running. Use -y/--yes in non-interactive mode." >&2
        finish 1
      fi
    fi
    compose_down
    finish $? ;;

  restart)
    compose_restart nginx
    finish $? ;;

  reload)
    CONTAINER_NAME="$(container_running_name)"
    if [[ -z "$CONTAINER_NAME" ]]; then
      echo "[${SCRIPT_NAME}] semicolon-nginx is not running."
      finish 1
    fi
    docker exec "$CONTAINER_NAME" nginx -t || finish 1
    docker exec "$CONTAINER_NAME" nginx -s reload
    finish $? ;;

  logs)
    compose_logs nginx
    finish $? ;;

  ps)
    compose_ps
    finish $? ;;

  toggle)
    if is_running; then
      echo "[${SCRIPT_NAME}] semicolon-nginx is running. Stopping..."
      compose_down
      finish $?
    fi

    echo "[${SCRIPT_NAME}] semicolon-nginx is not running. Starting..."
    ensure_network || finish 1
    ensure_certs || { echo "[${SCRIPT_NAME}] Failed to prepare cert files."; finish 1; }
    compose_up nginx
    finish $? ;;

  *)
    echo "[${SCRIPT_NAME}] Unknown action: $ACTION"
    finish 1 ;;
esac
