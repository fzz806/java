@echo off
rem ============================================================
rem  一键启动前后端（觉醒伪神 · 登录注册）
rem  启动顺序：先后端（构建/启动 jar），再前端（装依赖/启动 vite）
rem  用相对路径，双击即可运行，无需改任何路径
rem  ============================================================
cd /d "%~dp0"

echo ========== 启动后端（Spring Boot :8080） ==========
start "false-god-backend" cmd /k "%~dp0false-god-backend\start-backend.bat"

echo 等待后端构建与启动（首次下载依赖较慢）...
timeout /t 15 /nobreak >nul

echo ========== 启动前端（Vue3 + Vite :5173） ==========
start "false-god-frontend" cmd /k "%~dp0html\start-frontend.bat"

echo.
echo  ------------------------------------------------------------
echo   后端   : http://localhost:8080
echo   前端   : http://localhost:5173
echo   测试账号: chenhaoyu / 123456
echo   停止    : 双击 stop-all.bat
echo  ------------------------------------------------------------
echo.
pause
