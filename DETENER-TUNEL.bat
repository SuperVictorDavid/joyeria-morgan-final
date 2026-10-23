@echo off
title Joyeria Morgan - Detener tunel
echo Deteniendo el tunel publico...
taskkill /IM cloudflared.exe /F >nul 2>&1
if errorlevel 1 (
  echo No habia ningun tunel activo.
) else (
  echo Tunel detenido. La URL publica dejo de funcionar.
)
echo.
echo El contenedor del frontend y los microservicios siguen funcionando en local.
pause
