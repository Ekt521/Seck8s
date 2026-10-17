@echo off
REM ============================================================
REM Seck8s 一键启动：Nacos(需已启动) + 8 个服务
REM 前提：mvn clean package -DskipTests 已执行；Nacos 已启动(8848)
REM 每个服务独立窗口启动，日志在 logs\<服务>.log
REM ============================================================
cd /d %~dp0\..
REM 加载本机私有配置（真实密码不入库；文件不存在时使用系统环境变量）
if exist "%~dp0local.env.cmd" call "%~dp0local.env.cmd"
if not exist logs mkdir logs

echo [1/8] user-service (8081)
start "seck8s-user" java -jar user-service\target\user-service-1.0-SNAPSHOT.jar
echo [2/8] ticket-service (8082)
start "seck8s-ticket" java -jar ticket-service\target\ticket-service-1.0-SNAPSHOT.jar
echo [3/8] activity-service (8083)
start "seck8s-activity" java -jar activity-service\target\activity-service-1.0-SNAPSHOT.jar
echo [4/8] inventory-service (8084)
start "seck8s-inventory" java -jar inventory-service\target\inventory-service-1.0-SNAPSHOT.jar
echo [5/8] order-service (8085)
start "seck8s-order" java -jar order-service\target\order-service-1.0-SNAPSHOT.jar
echo [6/8] payment-service (8086)
start "seck8s-payment" java -jar payment-service\target\payment-service-1.0-SNAPSHOT.jar
echo [7/8] notification-service (8087)
start "seck8s-notification" java -jar notification-service\target\notification-service-1.0-SNAPSHOT.jar
echo [8/8] gateway (8080)
start "seck8s-gateway" java -jar seck8s-gateway\target\seck8s-gateway-1.0-SNAPSHOT.jar
echo.
echo 全部已启动，等待 30-60s 完成注册。Nacos 控制台: http://127.0.0.1:8848/nacos
pause
