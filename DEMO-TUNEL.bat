@echo off
setlocal
title Joyeria Morgan - Demo publica (tunel)
cd /d "%~dp0"

set "CF=%LOCALAPPDATA%\cloudflared\cloudflared.exe"
set "PUERTO=4201"

echo ============================================================
echo  JOYERIA MORGAN - DEMO PUBLICA
echo ============================================================
echo.

REM 1) Docker encendido
docker info >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Docker Desktop no esta iniciado.
  echo         Abrelo y espera a que diga "Engine running".
  pause
  exit /b 1
)

REM 2) Contenedor del frontend de produccion
docker inspect joyeria-front-demo >nul 2>&1
if errorlevel 1 (
  docker image inspect joyeria-front:demo >nul 2>&1
  if errorlevel 1 (
    echo [ERROR] No existe la imagen joyeria-front:demo.
    echo         Construyela una vez con:
    echo         docker build -t joyeria-front:demo frontend\joyeria-front
    pause
    exit /b 1
  )
  docker run -d --name joyeria-front-demo -p %PUERTO%:80 --add-host api-gateway:host-gateway joyeria-front:demo >nul
) else (
  docker start joyeria-front-demo >nul 2>&1
)

docker inspect -f "{{.State.Running}}" joyeria-front-demo 2>nul | findstr /i "true" >nul
if errorlevel 1 (
  echo [ERROR] El contenedor joyeria-front-demo no pudo iniciarse.
  pause
  exit /b 1
)
echo [OK] Frontend de produccion en http://localhost:%PUERTO%

REM 3) Aviso si el gateway no responde
powershell -NoProfile -Command "$c = New-Object Net.Sockets.TcpClient; try { $c.Connect('localhost',8762); exit 0 } catch { exit 1 } finally { $c.Dispose() }" >nul 2>&1
if errorlevel 1 (
  echo [AVISO] El API Gateway no responde en 8762.
  echo         Inicia api-gateway y los 5 microservicios en IntelliJ.
  echo.
)

REM 4) Tunel
if not exist "%CF%" (
  echo [ERROR] No se encontro cloudflared en:
  echo         %CF%
  echo         Descargalo de: https://github.com/cloudflare/cloudflared/releases
  pause
  exit /b 1
)

echo.
echo ------------------------------------------------------------
echo  Abriendo tunel... espera la linea:
echo      https://xxxx-xxxx-xxxx.trycloudflare.com
echo  Esa es la URL que debes enviar a tu grupo.
echo ------------------------------------------------------------
echo.
echo  Deja esta ventana ABIERTA mientras dure la demo.
echo  Para cortar el tunel: Ctrl+C
echo.

"%CF%" tunnel --url http://localhost:%PUERTO% --no-autoupdate

echo.
echo El tunel se cerro.
pause
