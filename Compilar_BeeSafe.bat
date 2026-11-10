@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
 echo ERROR: No se encontro el compilador JDK javac en PATH.
 pause
 exit /b 1
)
if not exist "backend\bin" mkdir "backend\bin"
javac --release 17 -encoding UTF-8 -d "backend\bin" "backend\src\BeeSafeServer.java"
if errorlevel 1 (
 echo ERROR: Fallo la compilacion.
 pause
 exit /b 1
)
echo Servidor BeeSafe compilado correctamente.
pause
endlocal
