@echo off
rem Wrapper minimo do Maven para Windows (logica em .mvn\wrapper\baixar-maven.ps1).
rem Pode ser trocado pelo oficial a qualquer momento com: mvn -N wrapper:wrapper
setlocal
set "MVN_CMD="
for /f "usebackq delims=" %%m in (`powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0.mvn\wrapper\baixar-maven.ps1"`) do set "MVN_CMD=%%m"
if not defined MVN_CMD exit /b 1
"%MVN_CMD%" %*
