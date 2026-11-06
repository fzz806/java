@echo off
setlocal enabledelayedexpansion
rem ============================================================
rem  觉醒伪神 · 一键停止
rem  按端口结束进程：前端 5173 / 5174（Vite 备选端口）、后端 8080
rem ============================================================
title 觉醒伪神 · 停止器
echo.
echo  正在停止觉醒伪神服务...
echo.

set "FOUND=0"
for %%p in (5173 5174 8080) do (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%%p" ^| findstr "LISTENING"') do (
        echo    [前端/后端] 端口 %%p 结束进程 PID=%%a
        taskkill /f /pid %%a >nul 2>nul
        set "FOUND=1"
    )
)

if "!FOUND!"=="0" (
    echo    未发现运行中的服务（端口 5173 / 5174 / 8080 均空闲）
) else (
    echo.
    echo    已停止。
)

echo.
ping -n 3 127.0.0.1 >nul
exit /b 0
