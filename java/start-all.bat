@echo off
rem ===== Start both backend and frontend =====
start "false-god-backend" cmd /k "C:\Users\Administrator\Desktop\false-god-backend\start-backend.bat"
timeout /t 10 /nobreak >nul
start "false-god-frontend" cmd /k "C:\Users\Administrator\Desktop\html\start-frontend.bat"
echo.
echo   Backend : http://localhost:8080
echo   Frontend: http://localhost:5173
echo   Test account: chenhaoyu / 123456
echo.
