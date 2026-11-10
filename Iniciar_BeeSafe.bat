@echo off
setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0"
title BeeSafe Beta - Iniciador
set "BEESAFE_URL=http://127.0.0.1:8765"
echo.
echo ==========================================
echo          BeeSafe Beta - Iniciador
 echo ==========================================
echo.
if not exist "backend\bin\BeeSafeServer.class" (
  echo No se encontro el servidor compilado. Intentando compilar...
  where javac.exe >nul 2>nul
  if errorlevel 1 goto :no_javac
  if not exist "backend\bin" mkdir "backend\bin"
  javac -encoding UTF-8 --release 17 -d "backend\bin" "backend\src\BeeSafeServer.java"
  if errorlevel 1 goto :compile_error
)
where java.exe >nul 2>nul
if errorlevel 1 goto :no_java
if not exist "logs" mkdir "logs"
if not exist "datos" mkdir "datos"
where curl.exe >nul 2>nul
if errorlevel 1 goto :no_curl
curl.exe -fsS --max-time 2 "%BEESAFE_URL%/api/health" >nul 2>nul
if not errorlevel 1 goto :open
 echo Iniciando servidor Java...
start "BeeSafe Beta - Servidor" /min cmd.exe /d /c ""%~dp0scripts\servidor.bat""
echo Comprobando disponibilidad local...
for /l %%G in (1,1,30) do (
  curl.exe -fsS --max-time 1 "%BEESAFE_URL%/api/health" >nul 2>nul
  if not errorlevel 1 goto :open
  timeout /t 1 /nobreak >nul
)
echo.
echo ERROR: BeeSafe no pudo iniciar. Revisa logs\beesafe-error.log
 echo Si el puerto 8765 esta ocupado, cierra la aplicacion que lo utiliza.
goto :failed
:open
 echo BeeSafe esta lista. Abriendo navegador...
start "" "%BEESAFE_URL%"
echo Puedes cerrar esta ventana: BeeSafe seguira ejecutandose.
exit /b 0
:no_java
echo ERROR: No se encontro Java. Instala JDK 17 o superior y vuelve a abrir este BAT.
goto :failed
:no_javac
echo ERROR: Falta clase compilada y no se encontro javac. Instala JDK 17 o superior.
goto :failed
:no_curl
echo ERROR: curl.exe no esta disponible en Windows. Se necesita Windows 10/11 actualizado.
goto :failed
:compile_error
echo ERROR: No se pudo compilar el servidor Java.
goto :failed
:failed
echo.
pause
exit /b 1
