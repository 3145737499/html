@echo off
chcp 936 >nul
title 烟雨阁-一键启动
cd /d "%~dp0"

REM ============================================================
REM  一键启动：后端(9090) + 前端(5173)
REM  幂等：已运行的服务自动跳过，重复双击无副作用
REM  node_modules / target 每次开机被删除，脚本已内置自动恢复
REM ============================================================

echo ============================================
echo    离恨烟·烟雨阁 一键启动
echo ============================================

call "%~dp0start-backend.bat"
if errorlevel 1 (
    echo [中止] 后端启动失败
    pause
    exit /b 1
)

call "%~dp0start-frontend.bat"
if errorlevel 1 (
    echo [中止] 前端启动失败
    pause
    exit /b 1
)

echo.
echo ============================================
echo    全部就绪！访问 http://localhost:5173
echo    演示账号：demo@boonie.com / 123456
echo    停止服务：双击 stop-all.bat（不影响 MySQL）
echo ============================================
pause
