@echo off
rem ============================================================
rem  一键停止前后端
rem  按端口关闭：后端 8080，前端 5173
rem  ============================================================
echo 正在停止服务...

rem ---- 停止后端（8080） ----
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    echo [backend] 结束进程 PID=%%a
    taskkill /f /pid %%a >nul 2>&1
)

rem ---- 停止前端（5173） ----
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING"') do (
    echo [frontend] 结束进程 PID=%%a
    taskkill /f /pid %%a >nul 2>&1
)

echo.
echo 已停止前后端服务。
pause
