@echo off
rem ============================================================
rem  前端启动脚本（Vue3 + Vite）
rem  作用：node_modules 被删除时自动重装依赖，再启动开发服务器
rem  ============================================================
cd /d "%~dp0"

rem ---- 检测依赖：node_modules 缺失则自动安装 ----
if not exist "node_modules" (
    echo [frontend] 检测到 node_modules 不存在，正在 npm install，请稍候...
    call npm install
    if errorlevel 1 (
        echo [frontend] npm install 失败，请检查网络或 node 环境。
        pause
        exit /b 1
    )
)

echo [frontend] 启动开发服务器 http://localhost:5173 ...
call npm run dev
pause
