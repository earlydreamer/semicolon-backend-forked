@echo off
setlocal enabledelayedexpansion

rem =========================================================
rem [개요]
rem - 로컬 nginx Docker Compose 제어 스크립트
rem - 인자 없이 실행하면 toggle 동작 (running이면 down, 아니면 up)
rem - up 실행 시 PEM 인증서가 없으면 self-signed 인증서 자동 생성
rem =========================================================

rem [기본 경로]
set "SCRIPT_DIR=%~dp0"
set "COMPOSE_FILE=%SCRIPT_DIR%docker-compose.local.nginx.yml"
set "CERTS_DIR=%SCRIPT_DIR%nginx-conf\certs"
set "CERT_FULLCHAIN=%CERTS_DIR%\fullchain.pem"
set "CERT_PRIVKEY=%CERTS_DIR%\privkey.pem"

rem [네트워크 이름]
rem BACKEND_DOCKER_NETWORK가 있으면 해당 값을, 없으면 기본값 사용
if "%BACKEND_DOCKER_NETWORK%"=="" (
    set "NETWORK_NAME=beadv4_4_semicolon_be_default"
) else (
    set "NETWORK_NAME=%BACKEND_DOCKER_NETWORK%"
)

rem [액션]
rem 첫 번째 인자를 액션으로 사용, 없으면 toggle
set "ACTION=%~1"
if "%ACTION%"=="" set "ACTION=toggle"

rem [옵션]
rem - pause / -p: 종료 전 창 유지
rem - -y / --yes: down 확인 프롬프트 생략
set "PAUSE_AFTER=0"
set "FORCE_DOWN=0"
if "%~1"=="" set "PAUSE_AFTER=1"
for %%A in (%*) do (
    if /I "%%~A"=="pause" set "PAUSE_AFTER=1"
    if /I "%%~A"=="-p" set "PAUSE_AFTER=1"
    if /I "%%~A"=="-y" set "FORCE_DOWN=1"
    if /I "%%~A"=="--yes" set "FORCE_DOWN=1"
)

rem [액션 분기]
if /I "%ACTION%"=="up" goto :up
if /I "%ACTION%"=="down" goto :down
if /I "%ACTION%"=="restart" goto :restart
if /I "%ACTION%"=="reload" goto :reload
if /I "%ACTION%"=="logs" goto :logs
if /I "%ACTION%"=="ps" goto :ps
if /I "%ACTION%"=="toggle" goto :toggle

echo [nginx-local] Unknown action: %ACTION%
echo Usage: nginx-local.bat [up^|down^|restart^|reload^|logs^|ps^|toggle] [pause^-p] [-y^|--yes]
set "EXIT_CODE=1"
goto :finish

:ensure_network
rem 외부 네트워크가 없으면 자동 생성
docker network inspect "%NETWORK_NAME%" >nul 2>&1
if errorlevel 1 (
    echo [nginx-local] Docker network "%NETWORK_NAME%" not found. Creating...
    docker network create "%NETWORK_NAME%" >nul
    if errorlevel 1 exit /b 1
)
goto :eof

:is_running
rem semicolon-nginx가 running 상태인지 확인
set "RUNNING_ID="
for /f "usebackq delims=" %%I in (`docker ps --filter "name=semicolon-nginx" --filter "status=running" -q`) do (
    set "RUNNING_ID=%%I"
    goto :eof
)
goto :eof

:ensure_certs
rem PEM 인증서가 없으면 self-signed 인증서 생성
if not exist "%CERTS_DIR%" (
    mkdir "%CERTS_DIR%" >nul 2>&1
)

if exist "%CERT_FULLCHAIN%" if exist "%CERT_PRIVKEY%" goto :eof

echo [nginx-local] TLS cert not found. Generating self-signed certs...
docker run --rm -v "%CERTS_DIR%:/out" alpine:3.20 sh -c "apk add --no-cache openssl >/dev/null && openssl req -x509 -nodes -newkey rsa:2048 -keyout /out/privkey.pem -out /out/fullchain.pem -days 365 -subj '/CN=api.dukku.shop' -addext 'subjectAltName=DNS:api.dukku.shop,DNS:localhost,IP:127.0.0.1'"
if errorlevel 1 exit /b 1

if not exist "%CERT_FULLCHAIN%" exit /b 1
if not exist "%CERT_PRIVKEY%" exit /b 1
goto :eof

:up
rem 네트워크/인증서 준비 후 nginx 기동
call :ensure_network
if errorlevel 1 (
    set "EXIT_CODE=1"
    goto :finish
)

call :ensure_certs
if errorlevel 1 (
    echo [nginx-local] Failed to prepare cert files.
    set "EXIT_CODE=1"
    goto :finish
)

docker compose -f "%COMPOSE_FILE%" up -d nginx
set "EXIT_CODE=%errorlevel%"
goto :finish

:down
rem running 상태면 사용자 확인 후 down
call :is_running
if defined RUNNING_ID goto :down_detected
goto :down_execute

:down_detected
if not "%FORCE_DOWN%"=="1" (
    echo [nginx-local] semicolon-nginx is currently UP.
    set /p CONFIRM_DOWN=Proceed with down? ^(y/N^): 
    if /I not "!CONFIRM_DOWN!"=="y" (
        echo [nginx-local] Canceled.
        set "EXIT_CODE=0"
        goto :finish
    )
)

:down_execute
docker compose -f "%COMPOSE_FILE%" down
set "EXIT_CODE=%errorlevel%"
goto :finish

:toggle
rem running이면 down, 아니면 up
call :is_running
if defined RUNNING_ID (
    echo [nginx-local] semicolon-nginx is running. Stopping...
    docker compose -f "%COMPOSE_FILE%" down
    set "EXIT_CODE=%errorlevel%"
    goto :finish
)

echo [nginx-local] semicolon-nginx is not running. Starting...
goto :up

:restart
docker compose -f "%COMPOSE_FILE%" restart nginx
set "EXIT_CODE=%errorlevel%"
goto :finish

:reload
docker exec semicolon-nginx nginx -t || (
    set "EXIT_CODE=1"
    goto :finish
)
docker exec semicolon-nginx nginx -s reload
set "EXIT_CODE=%errorlevel%"
goto :finish

:logs
docker compose -f "%COMPOSE_FILE%" logs -f nginx
set "EXIT_CODE=%errorlevel%"
goto :finish

:ps
docker compose -f "%COMPOSE_FILE%" ps
set "EXIT_CODE=%errorlevel%"
goto :finish

:finish
if not "%~1"=="" set "EXIT_CODE=%~1"
if "%EXIT_CODE%"=="" set "EXIT_CODE=0"
if "%PAUSE_AFTER%"=="1" (
    echo.
    pause
)
exit /b %EXIT_CODE%