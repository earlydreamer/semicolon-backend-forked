@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem nginx-local.bat - Windows helper for semicolon-nginx
rem Usage: nginx-local.bat [up|down|restart|reload|logs|ps|toggle] [pause|-p] [-y|--yes] [--no-pause] [--remove-orphans|--orphan]

set "SCRIPT_DIR=%~dp0"
set "COMPOSE_FILE=%SCRIPT_DIR%docker-compose.local.nginx.yml"
set "CERTS_DIR=%SCRIPT_DIR%nginx-conf\certs"
set "CERT_FULLCHAIN=%CERTS_DIR%\fullchain.pem"
set "CERT_PRIVKEY=%CERTS_DIR%\privkey.pem"
set "NETWORK_NAME=%BACKEND_DOCKER_NETWORK%"
if not defined NETWORK_NAME set "NETWORK_NAME=beadv4_4_semicolon_be_default"

set "COMPOSE_CMD="
set "ACTION=toggle"
set "PAUSE_AFTER=1"
set "SKIP_PAUSE=0"
set "FORCE_DOWN=0"
set "REMOVE_ORPHANS=0"
set "UP_EXTRA_ARGS="
set "EXIT_CODE=0"

rem ----------------------------
rem Parse arguments
rem ----------------------------
if "%*"=="" (
  rem default: toggle
  goto :after_parse
)

for %%A in (%*) do (
  if /I "%%~A"=="up" set "ACTION=up"
  if /I "%%~A"=="down" set "ACTION=down"
  if /I "%%~A"=="restart" set "ACTION=restart"
  if /I "%%~A"=="reload" set "ACTION=reload"
  if /I "%%~A"=="logs" set "ACTION=logs"
  if /I "%%~A"=="ps" set "ACTION=ps"
  if /I "%%~A"=="toggle" set "ACTION=toggle"
  if /I "%%~A"=="help" set "ACTION=help"
  if /I "%%~A"=="pause" set "PAUSE_AFTER=1"
  if "%%~A"=="-p" set "PAUSE_AFTER=1"
  if "%%~A"=="-y" set "FORCE_DOWN=1"
  if "%%~A"=="--yes" set "FORCE_DOWN=1"
  if "%%~A"=="--no-pause" set "SKIP_PAUSE=1"
  if "%%~A"=="--remove-orphans" set "REMOVE_ORPHANS=1"
  if "%%~A"=="--orphan" set "REMOVE_ORPHANS=1"
  if "%%~A"=="-h" set "ACTION=help"
  if "%%~A"=="--help" set "ACTION=help"
)

:after_parse
if "%SKIP_PAUSE%"=="1" set "PAUSE_AFTER=0"
if "%ACTION%"=="logs" set "SKIP_PAUSE=1" & set "PAUSE_AFTER=0"
if "%ACTION%"=="ps" set "SKIP_PAUSE=1" & set "PAUSE_AFTER=0"
if /I not "%ACTION%"=="up" if /I not "%ACTION%"=="down" if /I not "%ACTION%"=="restart" if /I not "%ACTION%"=="reload" if /I not "%ACTION%"=="logs" if /I not "%ACTION%"=="ps" if /I not "%ACTION%"=="toggle" (
  if /I not "%ACTION%"=="help" (
    set "ACTION=help"
  )
)

if "%ACTION%"=="help" goto :help

rem ----------------------------
rem Pre-checks
rem ----------------------------
call :ensure_docker
if errorlevel 1 goto :finish

call :ensure_compose
if errorlevel 1 goto :finish

if "%REMOVE_ORPHANS%"=="1" set "UP_EXTRA_ARGS=--remove-orphans"

goto :action_%ACTION%

:help
  echo [nginx-local] Usage: nginx-local.bat [up^|down^|restart^|reload^|logs^|ps^|toggle] [pause^-p] [-y^|--yes] [--remove-orphans^|--orphan] [--no-pause]
  echo [nginx-local] Unknown or missing action: %ACTION%
  set "EXIT_CODE=1"
  goto :finish

:ensure_docker
  docker version >nul 2>&1
  if errorlevel 1 (
    echo [nginx-local] Docker is not running or unavailable. Start Docker Desktop and retry.
    set "EXIT_CODE=1"
    exit /b 1
  )
  exit /b 0

:ensure_compose
  docker compose version >nul 2>&1
  if errorlevel 0 (
    set "COMPOSE_CMD=docker compose"
    exit /b 0
  )

  where docker-compose >nul 2>&1
  if errorlevel 1 (
    echo [nginx-local] Docker Compose command not found. Install Docker Compose plugin or docker-compose.
    set "EXIT_CODE=1"
    exit /b 1
  )
  set "COMPOSE_CMD=docker-compose"
  exit /b 0

:ensure_network
  docker network inspect "%NETWORK_NAME%" >nul 2>&1
  if errorlevel 1 (
    echo [nginx-local] Docker network "%NETWORK_NAME%" not found. Creating...
    docker network create "%NETWORK_NAME%" >nul 2>&1
    if errorlevel 1 (
      echo [nginx-local] Failed to create docker network "%NETWORK_NAME%".
      exit /b 1
    )
  )
  exit /b 0

:ensure_certs
  if not exist "%CERTS_DIR%" mkdir "%CERTS_DIR%" >nul 2>&1

  if exist "%CERT_FULLCHAIN%" del /f /q "%CERT_FULLCHAIN%" >nul 2>&1
  if exist "%CERT_PRIVKEY%" del /f /q "%CERT_PRIVKEY%" >nul 2>&1

  echo [nginx-local] Generating TLS certs...

  where mkcert >nul 2>&1
  if errorlevel 0 (
    echo [nginx-local] Using mkcert ...
    echo [nginx-local] Ensuring mkcert root CA is trusted...
    mkcert -install >nul 2>&1
    if errorlevel 1 (
      echo [nginx-local] mkcert -install failed or requires admin rights. Certificate may not be trusted.
    )
    mkcert -cert-file "%CERT_FULLCHAIN%" -key-file "%CERT_PRIVKEY%" api.dukku.shop localhost 127.0.0.1
    if errorlevel 1 (
      echo [nginx-local] mkcert generation failed.
      exit /b 1
    )
  ) else (
    where openssl >nul 2>&1
    if errorlevel 0 (
      echo [nginx-local] mkcert not found. Using local openssl fallback...
      openssl req -x509 -nodes -newkey rsa:2048 -keyout "%CERT_PRIVKEY%" -out "%CERT_FULLCHAIN%" -days 365 -subj /CN=localhost
      if errorlevel 1 (
        echo [nginx-local] OpenSSL generation failed.
        exit /b 1
      )
    ) else (
      echo [nginx-local] mkcert/openssl not found. Using dockerized openssl fallback...
      docker run --rm -v "%CERTS_DIR%:/out" alpine:3.20 sh -c "apk add --no-cache openssl >/dev/null && openssl req -x509 -nodes -newkey rsa:2048 -keyout /out/privkey.pem -out /out/fullchain.pem -days 365 -subj /CN=localhost"
      if errorlevel 1 (
        echo [nginx-local] Dockerized OpenSSL generation failed.
        exit /b 1
      )
    )
  )

  if not exist "%CERT_FULLCHAIN%" (
    echo [nginx-local] Cert generation finished but fullchain.pem was not created.
    set "EXIT_CODE=1"
    exit /b 1
  )
  if not exist "%CERT_PRIVKEY%" (
    echo [nginx-local] Cert generation finished but privkey.pem was not created.
    set "EXIT_CODE=1"
    exit /b 1
  )
  exit /b 0

:action_up
  call :ensure_network
  if errorlevel 1 goto :finish
  call :ensure_certs
  if errorlevel 1 (
    echo [nginx-local] Failed to prepare cert files.
    set "EXIT_CODE=1"
    goto :finish
  )
  %COMPOSE_CMD% -f "%COMPOSE_FILE%" up -d %UP_EXTRA_ARGS% nginx
  set "EXIT_CODE=%errorlevel%"
  goto :finish

:action_down
  set "RUNNING_ID="
  for /f "delims=" %%I in ('docker ps --filter "name=semicolon-nginx" --filter "status=running" -q') do set "RUNNING_ID=%%I"
  if defined RUNNING_ID (
    if "%FORCE_DOWN%"=="0" (
      echo [nginx-local] semicolon-nginx is currently UP.
      set /p "CONFIRM_DOWN=Proceed with down? (y/N): "
      if /I not "%CONFIRM_DOWN%"=="y" (
        echo [nginx-local] Canceled.
        set "EXIT_CODE=0"
        goto :finish
      )
    )
  )
  %COMPOSE_CMD% -f "%COMPOSE_FILE%" down
  set "EXIT_CODE=%errorlevel%"
  goto :finish

:action_restart
  %COMPOSE_CMD% -f "%COMPOSE_FILE%" restart nginx
  set "EXIT_CODE=%errorlevel%"
  goto :finish

:action_reload
  set "RUNNING_ID="
  for /f "delims=" %%I in ('docker ps --filter "name=semicolon-nginx" --filter "status=running" -q') do set "RUNNING_ID=%%I"
  if not defined RUNNING_ID (
    echo [nginx-local] semicolon-nginx is not running.
    set "EXIT_CODE=1"
    goto :finish
  )
  %COMPOSE_CMD% -f "%COMPOSE_FILE%" exec nginx nginx -t
  if errorlevel 1 (
    set "EXIT_CODE=1"
    goto :finish
  )
  %COMPOSE_CMD% -f "%COMPOSE_FILE%" exec nginx nginx -s reload
  set "EXIT_CODE=%errorlevel%"
  goto :finish

:action_logs
  %COMPOSE_CMD% -f "%COMPOSE_FILE%" logs -f nginx
  set "EXIT_CODE=%errorlevel%"
  goto :finish

:action_ps
  %COMPOSE_CMD% -f "%COMPOSE_FILE%" ps
  set "EXIT_CODE=%errorlevel%"
  goto :finish

:action_toggle
  set "RUNNING_ID="
  for /f "delims=" %%I in ('docker ps --filter "name=semicolon-nginx" --filter "status=running" -q') do set "RUNNING_ID=%%I"
  if defined RUNNING_ID (
    echo [nginx-local] semicolon-nginx is running. Stopping...
    %COMPOSE_CMD% -f "%COMPOSE_FILE%" down
    set "EXIT_CODE=%errorlevel%"
    goto :finish
  )

  echo [nginx-local] semicolon-nginx is not running. Starting...
  call :ensure_network
  if errorlevel 1 goto :finish
  call :ensure_certs
  if errorlevel 1 (
    echo [nginx-local] Failed to prepare cert files.
    set "EXIT_CODE=1"
    goto :finish
  )
  %COMPOSE_CMD% -f "%COMPOSE_FILE%" up -d %UP_EXTRA_ARGS% nginx
  set "EXIT_CODE=%errorlevel%"
  goto :finish

:finish
if not defined EXIT_CODE set "EXIT_CODE=0"
if not "%EXIT_CODE%"=="0" set "PAUSE_AFTER=1"
if "%PAUSE_AFTER%"=="1" (
  if not "%SKIP_PAUSE%"=="1" (
    echo.
    pause
  )
)
exit /b %EXIT_CODE%

