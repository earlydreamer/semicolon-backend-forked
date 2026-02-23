@echo off
setlocal enabledelayedexpansion

rem =========================================================
rem Local nginx helper for Windows.
rem Usage: nginx-local.bat [up^|down^|restart^|reload^|logs^|ps^|toggle] [pause^-p] [-y^|--yes] [--no-pause]
rem =========================================================

set "SCRIPT_DIR=%~dp0"
set "COMPOSE_FILE=%SCRIPT_DIR%docker-compose.local.nginx.yml"
set "CERTS_DIR=%SCRIPT_DIR%nginx-conf\certs"
set "CERT_FULLCHAIN=%CERTS_DIR%\fullchain.pem"
set "CERT_PRIVKEY=%CERTS_DIR%\privkey.pem"
set "COMPOSE_CMD="

if "%BACKEND_DOCKER_NETWORK%"=="" (
    set "NETWORK_NAME=beadv4_4_semicolon_be_default"
) else (
    set "NETWORK_NAME=%BACKEND_DOCKER_NETWORK%"
)

set "ACTION=toggle"
set "PAUSE_AFTER=1"
set "FORCE_DOWN=0"
set "SKIP_PAUSE=0"
set "REMOVE_ORPHANS=0"
set "UP_EXTRA_ARGS="

for %%A in (%*) do (
    set "ARG=%%~A"
    if /I "%ARG%"=="pause" set "PAUSE_AFTER=1"
    if /I "%ARG%"=="-p" set "PAUSE_AFTER=1"
    if /I "%ARG%"=="-y" set "FORCE_DOWN=1"
    if /I "%ARG%"=="--yes" set "FORCE_DOWN=1"
    if /I "%ARG%"=="--no-pause" set "SKIP_PAUSE=1"
    if /I "%ARG%"=="--remove-orphans" set "REMOVE_ORPHANS=1"
    if /I "%ARG%"=="--orphan" set "REMOVE_ORPHANS=1"
    if /I "%ARG%"=="-h" set "ACTION=help"
    if /I "%ARG%"=="--help" set "ACTION=help"
    if /I "%ARG:~0,1%" NEQ "-" (
        if /I "%ARG%"=="up" set "ACTION=up"
        if /I "%ARG%"=="down" set "ACTION=down"
        if /I "%ARG%"=="restart" set "ACTION=restart"
        if /I "%ARG%"=="reload" set "ACTION=reload"
        if /I "%ARG%"=="logs" set "ACTION=logs"
        if /I "%ARG%"=="ps" set "ACTION=ps"
        if /I "%ARG%"=="toggle" set "ACTION=toggle"
        if /I "%ARG%"=="help" set "ACTION=help"
    )
)

if "%ACTION%"=="help" goto :help

if "%SKIP_PAUSE%"=="1" set "PAUSE_AFTER=0"

call :ensure_docker
if errorlevel 1 goto :finish

docker compose version >nul 2>&1
if not errorlevel 1 (
    set "COMPOSE_CMD=docker compose"
) else (
    where docker-compose >nul 2>&1
    if not errorlevel 1 (
        set "COMPOSE_CMD=docker-compose"
    ) else (
        echo [nginx-local] Docker compose command not found. Install Docker Compose plugin or docker-compose.
        set "EXIT_CODE=1"
        goto :finish
    )
)

goto :unknown_action

:ensure_docker
docker version >nul 2>&1
if errorlevel 1 (
    echo [nginx-local] Docker is not running or unavailable. Start Docker Desktop and retry.
    set "EXIT_CODE=1"
    goto :finish
)
goto :eof

:unknown_action
if /I "%ACTION%"=="up" goto :up
if /I "%ACTION%"=="down" goto :down
if /I "%ACTION%"=="restart" goto :restart
if /I "%ACTION%"=="reload" goto :reload
if /I "%ACTION%"=="logs" goto :logs
if /I "%ACTION%"=="ps" goto :ps
if /I "%ACTION%"=="toggle" goto :toggle

:help
    echo [nginx-local] Unknown or missing action: %ACTION%
echo Usage: nginx-local.bat [up^|down^|restart^|reload^|logs^|ps^|toggle] [pause^-p] [-y^|--yes] [--no-pause]
set "EXIT_CODE=1"
goto :finish

:ensure_certs
if not exist "%CERTS_DIR%" (
    mkdir "%CERTS_DIR%" >nul 2>&1
)

if exist "%CERT_FULLCHAIN%" if exist "%CERT_PRIVKEY%" goto :eof

echo [nginx-local] TLS cert not found. Generating certs...

where mkcert >nul 2>&1
if not errorlevel 1 (
    echo [nginx-local] Using mkcert (browser-trusted)...
    mkcert -cert-file "%CERT_FULLCHAIN%" -key-file "%CERT_PRIVKEY%" api.dukku.shop localhost 127.0.0.1
) else (
    where openssl >nul 2>&1
    if not errorlevel 1 (
        echo [nginx-local] mkcert not found. Using local openssl fallback (self-signed, not browser-trusted)...
        openssl req -x509 -nodes -newkey rsa:2048 -keyout "%CERT_PRIVKEY%" -out "%CERT_FULLCHAIN%" -days 365 -subj /CN=localhost
    ) else (
        echo [nginx-local] mkcert not found. Using dockerized openssl fallback (self-signed, not browser-trusted)...
        docker run --rm -v "%CERTS_DIR%:/out" alpine:3.20 sh -lc "apk add --no-cache openssl > /dev/null ; openssl req -x509 -nodes -newkey rsa:2048 -keyout /out/privkey.pem -out /out/fullchain.pem -days 365 -subj /CN=localhost"
    )
)
if errorlevel 1 (
    echo [nginx-local] Failed to generate certs.
    set "EXIT_CODE=1"
    set "PAUSE_AFTER=1"
    goto :finish
)

if not exist "%CERT_FULLCHAIN%" (
    echo [nginx-local] Cert generation finished but fullchain.pem was not created.
    echo [nginx-local] Checked: "%CERT_FULLCHAIN%"
    dir /b "%CERTS_DIR%" | findstr /i "fullchain.pem privkey.pem"
    set "EXIT_CODE=1"
    set "PAUSE_AFTER=1"
    goto :finish
)

if not exist "%CERT_PRIVKEY%" (
    echo [nginx-local] Cert generation finished but privkey.pem was not created.
    echo [nginx-local] Checked: "%CERT_PRIVKEY%"
    dir /b "%CERTS_DIR%" | findstr /i "fullchain.pem privkey.pem"
    set "EXIT_CODE=1"
    set "PAUSE_AFTER=1"
    goto :finish
)
goto :eof

:ensure_network
docker network inspect "%NETWORK_NAME%" >nul 2>&1
if errorlevel 1 (
    echo [nginx-local] Docker network "%NETWORK_NAME%" not found. Creating...
    docker network create "%NETWORK_NAME%" >nul
    if errorlevel 1 (
        echo [nginx-local] Failed to create docker network "%NETWORK_NAME%".
        set "EXIT_CODE=1"
        goto :finish
    )
)
goto :eof

:up
if "%REMOVE_ORPHANS%"=="1" set "UP_EXTRA_ARGS=--remove-orphans"
docker network inspect "%NETWORK_NAME%" >nul 2>&1
if errorlevel 1 (
    echo [nginx-local] Docker network "%NETWORK_NAME%" not found. Creating...
    docker network create "%NETWORK_NAME%" >nul
    if errorlevel 1 (
        echo [nginx-local] Failed to create docker network "%NETWORK_NAME%".
        set "EXIT_CODE=1"
        goto :finish
    )
)
call :ensure_certs
if errorlevel 1 (
    echo [nginx-local] Failed to prepare cert files.
    goto :finish
)
    %COMPOSE_CMD% -f "%COMPOSE_FILE%" up -d %UP_EXTRA_ARGS% nginx
set "EXIT_CODE=%errorlevel%"
goto :finish

:down
set "RUNNING_ID="
for /f "delims=" %%I in ('docker ps --filter "name=semicolon-nginx" --filter "status=running" -q') do set "RUNNING_ID=%%I"

if defined RUNNING_ID goto :down_detected
goto :down_execute

:down_detected
if not "%FORCE_DOWN%"=="1" (
    echo [nginx-local] semicolon-nginx is currently UP.
    set /p CONFIRM_DOWN=Proceed with down? (y/N): 
    if /I not "!CONFIRM_DOWN!"=="y" (
        echo [nginx-local] Canceled.
        set "EXIT_CODE=0"
        goto :finish
    )
)

:down_execute
%COMPOSE_CMD% -f "%COMPOSE_FILE%" down
set "EXIT_CODE=%errorlevel%"
goto :finish

:toggle
set "RUNNING_ID="
for /f "delims=" %%I in ('docker ps --filter "name=semicolon-nginx" --filter "status=running" -q') do set "RUNNING_ID=%%I"

if defined RUNNING_ID (
    echo [nginx-local] semicolon-nginx is running. Stopping...
    %COMPOSE_CMD% -f "%COMPOSE_FILE%" down
    set "EXIT_CODE=%errorlevel%"
    goto :finish
)

echo [nginx-local] semicolon-nginx is not running. Starting...
if "%REMOVE_ORPHANS%"=="1" set "UP_EXTRA_ARGS=--remove-orphans"
docker network inspect "%NETWORK_NAME%" >nul 2>&1
if errorlevel 1 (
    echo [nginx-local] Docker network "%NETWORK_NAME%" not found. Creating...
    docker network create "%NETWORK_NAME%" >nul
    if errorlevel 1 (
        echo [nginx-local] Failed to create docker network "%NETWORK_NAME%".
        set "EXIT_CODE=1"
        goto :finish
    )
)
call :ensure_certs
if errorlevel 1 (
    echo [nginx-local] Failed to prepare cert files.
    goto :finish
)

goto :up

:restart
%COMPOSE_CMD% -f "%COMPOSE_FILE%" restart nginx
set "EXIT_CODE=%errorlevel%"
goto :finish

:reload
%COMPOSE_CMD% -f "%COMPOSE_FILE%" exec nginx nginx -t || (
    set "EXIT_CODE=1"
    goto :finish
)
%COMPOSE_CMD% -f "%COMPOSE_FILE%" exec nginx nginx -s reload
set "EXIT_CODE=%errorlevel%"
goto :finish

:logs
%COMPOSE_CMD% -f "%COMPOSE_FILE%" logs -f nginx
set "EXIT_CODE=%errorlevel%"
goto :finish

:ps
%COMPOSE_CMD% -f "%COMPOSE_FILE%" ps
set "EXIT_CODE=%errorlevel%"
goto :finish

:finish
if not defined EXIT_CODE set "EXIT_CODE=0"
if not "%EXIT_CODE%"=="0" set "PAUSE_AFTER=1"
if "%PAUSE_AFTER%"=="1" (
    if "%SKIP_PAUSE%"=="1" goto :_nginx_finish_end
    echo.
    pause
)
:_nginx_finish_end
exit /b %EXIT_CODE%
