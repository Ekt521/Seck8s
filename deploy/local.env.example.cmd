@echo off
REM ============================================================
REM 复制本文件为 local.env.cmd，填入你的真实值后使用
REM local.env.cmd 已被 .gitignore 忽略，不会提交到 Git
REM start-all.cmd 会自动加载它
REM ============================================================
set "SECK8S_MYSQL_PASSWORD=change-me"
set "SECK8S_REDIS_PASSWORD=change-me"
set "SECK8S_RABBITMQ_PASSWORD=change-me"
set "SECK8S_JWT_SECRET=change-me-to-a-random-string-at-least-32-bytes"
