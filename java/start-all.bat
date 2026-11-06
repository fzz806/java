@echo off
setlocal enabledelayedexpansion
rem ============================================================
rem  觉醒伪神 · 一键启动（智能版）
rem  特性：
rem    1. 自动检测并清理被占用的 5173 / 8080 端口
rem    2. 自动检测 MySQL 是否在跑，没跑就尝试启动服务
rem    3. 自动检测 false_god 库，缺失则自动建库导入 sql
rem    4. 自动寻找 Java / Maven（不写死路径，多路回退）
rem    5. 缺 jar 才构建，缺 node_modules 才装依赖
rem    6. 后端健康检查通过后才开前端，避免白屏
rem ============================================================
cd /d "%~dp0"
title 觉醒伪神 · 启动器

echo.
echo  ============================================================
echo    觉醒伪神：神明弃我，我自为神  · 一键启动
echo  ============================================================
echo.

rem ---------------- 1. 环境检测 ----------------
echo [1/6] 检测运行环境...

rem ---- Java ----
set "JAVA_EXE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE if exist "C:\Program Files\Zulu\zulu-21\bin\java.exe" set "JAVA_EXE=C:\Program Files\Zulu\zulu-21\bin\java.exe"
if not defined JAVA_EXE if exist "C:\Program Files\Java\jdk-17\bin\java.exe" set "JAVA_EXE=C:\Program Files\Java\jdk-17\bin\java.exe"
if not defined JAVA_EXE (
    for /f "delims=" %%j in ('where java 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%j"
)
if not defined JAVA_EXE (
    echo    [X] 未找到 Java。请安装 JDK 17+ 或设置 JAVA_HOME。
    goto :fail
)
echo    [OK] Java: %JAVA_EXE%

rem ---- Maven ----
rem 注意实际路径是三层：apache-maven-X-bin\<hash>\apache-maven-X\bin\mvn.cmd
set "MVN="
if exist "%~dp0false-god-backend\mvnw.cmd" set "MVN=%~dp0false-god-backend\mvnw.cmd"
if not defined MVN (
    for /d %%d in ("%USERPROFILE%\.m2\wrapper\dists\apache-maven-*") do (
        for /d %%h in ("%%d\*") do (
            if exist "%%h\bin\mvn.cmd" if not defined MVN set "MVN=%%h\bin\mvn.cmd"
            for /d %%v in ("%%h\*") do (
                if exist "%%v\bin\mvn.cmd" if not defined MVN set "MVN=%%v\bin\mvn.cmd"
            )
        )
    )
)
if not defined MVN (
    for /f "delims=" %%m in ('where mvn 2^>nul') do if not defined MVN set "MVN=%%m"
)
if defined MVN (
    echo    [OK] Maven: !MVN!
) else (
    echo    [!] 未找到 Maven。若 jar 已存在则不影响启动。
)

rem ---- Node ----
where npm >nul 2>nul
if errorlevel 1 (
    echo    [X] 未找到 npm。请安装 Node.js 18+。
    goto :fail
)
echo    [OK] Node: 已就绪

rem ---------------- 2. 清理旧进程 ----------------
echo.
echo [2/6] 清理被占用的端口（5173 / 8080）...
call :kill_port 5173
call :kill_port 8080

rem ---------------- 3. MySQL 与数据库 ----------------
echo.
echo [3/6] 检测 MySQL...
set "MYSQL_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not defined MYSQL_EXE (
    for /f "delims=" %%m in ('where mysql 2^>nul') do if not defined MYSQL_EXE set "MYSQL_EXE=%%m"
)

netstat -ano | findstr ":3306" | findstr "LISTENING" >nul 2>nul
if errorlevel 1 (
    echo      [!] MySQL 未运行，尝试启动服务...
    net start MySQL80 >nul 2>nul || net start MySQL >nul 2>nul
    ping -n 6 127.0.0.1 >nul
    netstat -ano | findstr ":3306" | findstr "LISTENING" >nul 2>nul
    if errorlevel 1 (
        echo    [X] MySQL 启动失败。请手动启动 MySQL 服务后重试。
        goto :fail
    )
)
echo    [OK] MySQL 已在 3306 运行

if defined MYSQL_EXE (
    "%MYSQL_EXE%" -uroot -p123456 -N -e "show databases like 'false_god';" 2>nul | findstr "false_god" >nul
    if errorlevel 1 (
        echo      [!] false_god 库不存在，自动建库导入...
        "%MYSQL_EXE%" -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS false_god DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>nul
        if exist "%~dp0false-god-backend\sql\false_god.sql" (
            "%MYSQL_EXE%" -uroot -p123456 --default-character-set=utf8mb4 false_god < "%~dp0false-god-backend\sql\false_god.sql" 2>nul
            echo    [OK] 数据库已初始化
        ) else (
            echo    [!] 未找到 sql 脚本，请手动导入 false-god-backend\sql\false_god.sql
        )
    ) else (
        echo    [OK] false_god 库已存在
    )
) else (
    echo    [!] 未找到 mysql 客户端，跳过建库检查
)

rem ---------------- 4. 构建后端 ----------------
echo.
echo [4/6] 检查后端构建产物...
set "JAR=%~dp0false-god-backend\target\false-god-backend-1.0.0.jar"
if not exist "%JAR%" (
    if not defined MVN (
        echo    [X] jar 不存在且无 Maven 可用，无法构建。
        goto :fail
    )
    echo     未找到 jar，正在 Maven 构建（首次较慢）...
    pushd "%~dp0false-god-backend"
    call "!MVN!" -q -B -DskipTests clean package
    popd
    if not exist "%JAR%" (
        echo    [X] 后端构建失败，请检查上方报错。
        goto :fail
    )
)
echo    [OK] jar 已就绪

rem ---------------- 5. 启动后端 ----------------
echo.
echo [5/6] 启动后端（Spring Boot :8080）...
start "false-god-backend" cmd /k ""!JAVA_EXE!" -jar "%JAR%" --server.port=8080"

echo      等待后端就绪...
set /a TRY=0
:wait_backend
set /a TRY+=1
if !TRY! GTR 40 goto :backend_timeout
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>nul
if errorlevel 1 (
    ping -n 2 127.0.0.1 >nul
    goto :wait_backend
)
echo    [OK] 后端已监听 8080

rem ---------------- 6. 启动前端 ----------------
echo.
echo [6/6] 启动前端（Vue3 + Vite :5173）...
if not exist "%~dp0html\node_modules" (
    echo     未找到 node_modules，正在 npm install...
    pushd "%~dp0html"
    call npm install
    popd
    if not exist "%~dp0html\node_modules" (
        echo    [X] npm install 失败，请检查网络。
        goto :fail
    )
)
start "false-god-frontend" cmd /k "cd /d "%~dp0html" && npm run dev"

echo      等待前端就绪...
set /a TRY=0
:wait_frontend
set /a TRY+=1
if !TRY! GTR 30 goto :frontend_timeout
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul 2>nul
if errorlevel 1 (
    ping -n 2 127.0.0.1 >nul
    goto :wait_frontend
)

echo.
echo  ============================================================
echo    启动完成  觉醒伪神：神明弃我，我自为神
echo  ------------------------------------------------------------
echo    前端   : http://localhost:5173
echo    后端   : http://localhost:8080
echo    测试号 : chenhaoyu / 123456
echo    停止   : 双击 stop-all.bat
echo  ============================================================
echo.
start "" http://localhost:5173
ping -n 4 127.0.0.1 >nul
exit /b 0

:backend_timeout
echo    [X] 后端 40 秒内未监听 8080，请查看后端窗口报错。
goto :fail
:frontend_timeout
echo    [X] 前端 30 秒内未监听 5173，请查看前端窗口报错。
goto :fail

:fail
echo.
echo  启动中断。按任意键关闭。
pause >nul
exit /b 1

rem ---------------- 工具函数：按端口杀进程 ----------------
:kill_port
set "P=%~1"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%P%" ^| findstr "LISTENING"') do (
    echo     结束占用 %P% 的进程 PID=%%a
    taskkill /f /pid %%a >nul 2>nul
)
exit /b 0
