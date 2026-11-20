@echo off
setlocal enabledelayedexpansion
rem ============================================================
rem  FalseGod - headless one-click launcher
rem  For WorkBuddy / CI / automation. Differs from start-all.bat:
rem    1. No popup windows (start /b); logs go to *-run.log
rem    2. No pause; exits as soon as ports are ready
rem    3. Clears injected SERVER__PORT so Spring keeps port 8080
rem  For desktop double-click, keep using start-all.bat.
rem  To stop: stop-all.bat
rem  NOTE: this file is pure ASCII on purpose (GBK/UTF-8 safe).
rem ============================================================
cd /d "%~dp0"
title FalseGod - Headless Launcher

echo [headless] FalseGod headless launcher

rem ---- Critical: host may inject SERVER__PORT; Spring relaxed binding overrides 8080 ----
set "SERVER__PORT="
set "SERVER__HOST="
set "SERVER_PORT="
set "SERVER_ADDRESS="

rem ---- 1. Java ----
set "JAVA_EXE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE if exist "C:\Program Files\Zulu\zulu-21\bin\java.exe" set "JAVA_EXE=C:\Program Files\Zulu\zulu-21\bin\java.exe"
if not defined JAVA_EXE if exist "C:\Program Files\Java\jdk-17\bin\java.exe" set "JAVA_EXE=C:\Program Files\Java\jdk-17\bin\java.exe"
if not defined JAVA_EXE (for /f "delims=" %%j in ('where java 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%j")
if not defined JAVA_EXE (echo [X] Java not found & exit /b 1)
echo    [OK] Java: %JAVA_EXE%

rem ---- 2. Maven (three-level wrapper cache) ----
set "MVN="
for /d %%d in ("%USERPROFILE%\.m2\wrapper\dists\apache-maven-*") do (
    for /d %%h in ("%%d\*") do (
        if exist "%%h\bin\mvn.cmd" if not defined MVN set "MVN=%%h\bin\mvn.cmd"
        for /d %%v in ("%%h\*") do (if exist "%%v\bin\mvn.cmd" if not defined MVN set "MVN=%%v\bin\mvn.cmd")
    )
)
if not defined MVN (for /f "delims=" %%m in ('where mvn 2^>nul') do if not defined MVN set "MVN=%%m")

rem ---- 3. Node ----
where npm >nul 2>nul
if errorlevel 1 (echo [X] npm not found & exit /b 1)

rem ---- 4. Kill stale processes on 5173 / 8080 ----
call :kill_port 5173
call :kill_port 8080

rem ---- 5. MySQL + database ----
set "MYSQL_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not defined MYSQL_EXE (for /f "delims=" %%m in ('where mysql 2^>nul') do if not defined MYSQL_EXE set "MYSQL_EXE=%%m")
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul 2>nul
if errorlevel 1 (
    net start MySQL80 >nul 2>nul
    ping -n 6 127.0.0.1 >nul
)
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul 2>nul
if errorlevel 1 (echo [X] MySQL not listening on 3306 & exit /b 1)
if defined MYSQL_EXE (
    "%MYSQL_EXE%" -uroot -p123456 -N -e "use information_schema; select table_name from tables where table_schema='false_god' and table_name='t_message';" 2>nul | findstr "t_message" >nul
    if errorlevel 1 (
        "%MYSQL_EXE%" -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS false_god DEFAULT CHARACTER SET utf8mb4;" 2>nul
        for %%s in (false_god.sql false_god_content.sql false_god_social.sql false_god_power.sql false_god_task.sql) do (
            if exist "%~dp0false-god-backend\sql\%%s" "%MYSQL_EXE%" -uroot -p123456 --default-character-set=utf8mb4 false_god < "%~dp0false-god-backend\sql\%%s" 2>nul
        )
        echo    [OK] database initialized
    )
)

rem ---- 6. Build backend if jar missing ----
set "JAR=%~dp0false-god-backend\target\false-god-backend-1.0.0.jar"
if not exist "%JAR%" (
    if not defined MVN (echo [X] jar missing and no Maven & exit /b 1)
    pushd "%~dp0false-god-backend"
    call "%MVN%" -q -B -DskipTests clean package
    popd
    if not exist "%JAR%" (echo [X] build failed & exit /b 1)
)

rem ---- 7. Start backend (no window, log to file) ----
echo    [..] starting backend :8080
start "" /b cmd /c ""%JAVA_EXE%" -jar "%JAR%" --server.port=8080 --server.address=0.0.0.0 > "%~dp0false-god-backend\backend-run.log" 2>&1"
set /a TRY=0
:wait_backend
set /a TRY+=1
if !TRY! GTR 40 (echo [X] backend timeout, see backend-run.log & exit /b 1)
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>nul
if errorlevel 1 (ping -n 2 127.0.0.1 >nul & goto :wait_backend)
echo    [OK] backend :8080

rem ---- 8. Start frontend ----
if not exist "%~dp0html\node_modules" (
    pushd "%~dp0html"
    call npm install
    popd
    if not exist "%~dp0html\node_modules" (echo [X] npm install failed & exit /b 1)
)
echo    [..] starting frontend :5173
start "" /b cmd /c "cd /d "%~dp0html" && npm run dev > "%~dp0html\frontend-run.log" 2>&1"
set /a TRY=0
:wait_frontend
set /a TRY+=1
if !TRY! GTR 30 (echo [X] frontend timeout, see frontend-run.log & exit /b 1)
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul 2>nul
if errorlevel 1 (ping -n 2 127.0.0.1 >nul & goto :wait_frontend)
echo    [OK] frontend :5173

echo.
echo   [DONE] front http://192.168.68.49:5173   back http://192.168.68.49:8080
echo   test account: chenhaoyu / 123456
exit /b 0

:kill_port
set "P=%~1"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%P%" ^| findstr "LISTENING"') do (taskkill /f /pid %%a >nul 2>nul)
exit /b 0
