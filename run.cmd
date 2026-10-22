@echo off
setlocal
cd /d "%~dp0"
if exist "dist\0.2.0\Passworder\Passworder.exe" (
    start "" "dist\0.2.0\Passworder\Passworder.exe"
    exit /b 0
)
if exist "dist\Passworder\Passworder.exe" (
    start "" "dist\Passworder\Passworder.exe"
    exit /b 0
)
if not exist "target\Passworder.jar" (
    call mvnw.cmd -B -ntp package
    if errorlevel 1 exit /b 1
)
start "" javaw -jar "target\Passworder.jar"
