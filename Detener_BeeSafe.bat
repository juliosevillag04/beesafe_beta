@echo off
setlocal
cd /d "%~dp0"
title BeeSafe Beta - Detener
echo Solicitando cierre de BeeSafe...
where curl.exe >nul 2>nul
if errorlevel 1 goto :error
curl.exe -fsS --max-time 5 -X POST -H "X-Demo-Role: tecnico" -H "Content-Type: application/json" -d "{}" "http://127.0.0.1:8765/api/shutdown"
if errorlevel 1 goto :error
echo.
echo Servidor detenido. Los datos guardados se conservan.
exit /b 0
:error
echo.
echo No fue posible cerrar BeeSafe. Puede que ya este cerrado.
pause
exit /b 1
