@echo off
setlocal
rem ============================================================
rem  后端启动脚本（Spring Boot + MyBatis-Plus）
rem  作用：target 被删除时自动用 Maven 构建，再启动 jar
rem  ============================================================
cd /d "%~dp0"

rem ---- 环境变量：Java 21（Zulu） ----
set "JAVA_HOME=C:\Program Files\Zulu\zulu-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem ---- Maven：使用 wrapper 缓存里的发行版（如不存在请修正） ----
set "MVN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin\4nf9hui3q3djbarqar9g711ggc\apache-maven-3.9.9"
if not exist "%MVN_HOME%\bin\mvn.cmd" (
    rem 回退：尝试 3.9.16 版本
    set "MVN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16"
)
if not exist "%MVN_HOME%\bin\mvn.cmd" (
    echo [backend] 未找到 Maven，请检查 %USERPROFILE%\.m2\wrapper\dists 目录。
    pause
    exit /b 1
)
set "MVN=%MVN_HOME%\bin\mvn.cmd"

set "JAR=%~dp0target\false-god-backend-1.0.0.jar"

rem ---- 检测构建产物：target 缺失则自动构建 ----
if not exist "%JAR%" (
    echo [backend] 未找到 jar，正在用 Maven 构建（首次会下载依赖，较慢）...
    call "%MVN%" -q -DskipTests clean package
    if errorlevel 1 (
        echo [backend] Maven 构建失败，请检查上方报错。
        pause
        exit /b 1
    )
)

echo [backend] 启动服务 http://localhost:8080 ...
java -jar "%JAR%" --server.port=8080
pause
