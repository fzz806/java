@echo off
rem ===== false-god frontend (Vue) one-click start =====
cd /d "%~dp0"
echo [frontend] starting on http://localhost:5173 ...
call npm run dev
pause
