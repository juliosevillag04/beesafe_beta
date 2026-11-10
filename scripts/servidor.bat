@echo off
setlocal
cd /d "%~dp0.."
if not exist "logs" mkdir "logs"
java.exe -Dfile.encoding=UTF-8 -cp "backend\bin" BeeSafeServer 1>>"logs\beesafe-out.log" 2>>"logs\beesafe-error.log"
if errorlevel 1 (
 echo BeeSafe finalizo con error. Consulta logs\beesafe-error.log
 pause
)
endlocal
