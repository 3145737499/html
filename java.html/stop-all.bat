@echo off
chcp 936 >nul
title 烟雨阁-停止服务

REM ============================================================
REM  停止服务：按端口 9090(后端) / 5173(前端) 杀进程
REM  不动 MySQL，数据库数据安全
REM ============================================================

echo 正在停止后端(9090)和前端(5173)，不影响 MySQL ...
for %%p in (9090 5173) do (
    for /f "tokens=5" %%i in ('netstat -ano ^| findstr /C:":%%p " ^| findstr "LISTENING"') do (
        echo   停止端口 %%p 上的进程（PID %%i）
        taskkill /F /PID %%i >nul 2>&1
    )
)
echo 完成。
pause
