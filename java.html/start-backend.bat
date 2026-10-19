@echo off
chcp 936 >nul
title 烟雨阁-后端启动(9090)
cd /d "%~dp0springBootTest"

REM ============================================================
REM  后端启动脚本：清环境变量 -> 等MySQL -> 初始化DB -> 启动等就绪
REM  注意：target 文件夹每次开机会被删除，本脚本不依赖 target
REM  （mvnw spring-boot:run 会自动重新编译）
REM ============================================================

REM ---- 0. 清除会污染 Spring Boot 配置的环境变量（坑#3）----
set SERVER__PORT=
set SERVER__HOST=

REM ---- 1. 后端已在运行则跳过 ----
netstat -ano | findstr /C:":9090 " | findstr "LISTENING" >nul 2>&1
if %errorlevel%==0 (
    echo [跳过] 后端已在运行（端口 9090）
    exit /b 0
)

REM ---- 2. 等待 MySQL 就绪（最多 60 秒）----
set "MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not exist "%MYSQL%" set "MYSQL=mysql"
set /a tries=0
:wait_mysql
"%MYSQL%" -uroot -p123456 -e "select 1" >nul 2>&1
if %errorlevel%==0 goto mysql_ok
set /a tries+=1
if %tries% geq 30 (
    echo [错误] MySQL 迟迟未就绪，请手动启动 MySQL 服务后重试
    pause
    exit /b 1
)
echo [等待] MySQL 未就绪，第 %tries% 次重试...
ping -n 3 127.0.0.1 >nul
goto wait_mysql
:mysql_ok

REM ---- 3. 数据库 boonie 不存在则自动导入（坑#2：必须 utf8mb4）----
"%MYSQL%" -uroot -p123456 --default-character-set=utf8mb4 -e "use boonie" >nul 2>&1
if not %errorlevel%==0 (
    echo [初始化] 数据库 boonie 不存在，正在导入 sql\boonie.sql ...
    "%MYSQL%" -uroot -p123456 --default-character-set=utf8mb4 < sql\boonie.sql
    if errorlevel 1 (
        echo [错误] SQL 导入失败，请检查 MySQL 是否运行
        pause
        exit /b 1
    )
    echo [初始化] 数据库导入完成
)

REM ---- 4. 后台启动后端（日志写入 start-log.txt）----
echo [启动] 后端编译并启动中（target 已被重置，需重新编译，请耐心等待）...
start "烟雨阁后端" /min cmd /c "mvnw.cmd spring-boot:run > start-log.txt 2>&1"

REM ---- 5. 轮询等待 9090 端口就绪（最多 240 秒）----
set /a tries=0
:wait_backend
netstat -ano | findstr /C:":9090 " | findstr "LISTENING" >nul 2>&1
if %errorlevel%==0 goto backend_ok
set /a tries+=1
if %tries% geq 120 (
    echo [错误] 后端 240 秒仍未就绪，请查看 springBootTest\start-log.txt 排查
    pause
    exit /b 1
)
ping -n 3 127.0.0.1 >nul
goto wait_backend
:backend_ok
echo [完成] 后端已就绪：http://localhost:9090
exit /b 0
