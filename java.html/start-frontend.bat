@echo off
chcp 936 >nul
title 烟雨阁-前端启动(5173)
cd /d "%~dp0boonie-vue"

REM ============================================================
REM  前端启动脚本：node_modules 缺失则自动 npm install，再启动
REM  注意：node_modules 每次开机会被删除，本脚本不依赖它存在
REM ============================================================

REM ---- 1. 前端已在运行则跳过 ----
netstat -ano | findstr /C:":5173 " | findstr "LISTENING" >nul 2>&1
if %errorlevel%==0 (
    echo [跳过] 前端已在运行（端口 5173）
    exit /b 0
)

REM ---- 2. node_modules 不存在则先安装依赖 ----
if not exist node_modules (
    echo [安装] node_modules 不存在（开机已重置），执行 npm install ...
    call npm install
    if errorlevel 1 (
        echo [错误] npm install 失败，请检查网络
        pause
        exit /b 1
    )
)

REM ---- 3. 后台启动 Vite（日志写入 vite-log.txt）----
echo [启动] 前端 Vite 启动中...
start "烟雨阁前端" /min cmd /c "npm run dev > vite-log.txt 2>&1"

REM ---- 4. 轮询等待 5173 端口就绪（最多 120 秒）----
set /a tries=0
:wait_fe
netstat -ano | findstr /C:":5173 " | findstr "LISTENING" >nul 2>&1
if %errorlevel%==0 goto fe_ok
set /a tries+=1
if %tries% geq 60 (
    echo [错误] 前端 120 秒仍未就绪，请查看 boonie-vue\vite-log.txt 排查
    pause
    exit /b 1
)
ping -n 3 127.0.0.1 >nul
goto wait_fe
:fe_ok
echo [完成] 前端已就绪：http://localhost:5173
exit /b 0
