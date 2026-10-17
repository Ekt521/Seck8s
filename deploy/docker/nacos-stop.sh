#!/usr/bin/env bash
# 停止 Nacos 容器（保留容器，可再 start）
set -e
CONTAINER_NAME=seck8s-nacos
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
  docker stop ${CONTAINER_NAME}
  echo "[Nacos] 已停止 ${CONTAINER_NAME}"
else
  echo "[Nacos] 容器不存在"
fi
