#!/usr/bin/env bash
# Seck8s Nacos 注册中心启动脚本（standalone 单机模式，开发用）
# 控制台: http://localhost:8848/nacos  （NACOS_AUTH_ENABLE=false 免登录）
set -e

CONTAINER_NAME=seck8s-nacos
IMAGE=nacos/nacos-server:v2.4.2

# 已存在且运行中则跳过
if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
  echo "[Nacos] 容器 ${CONTAINER_NAME} 已在运行"
  exit 0
fi

# 已存在但停止：直接启动
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
  echo "[Nacos] 容器 ${CONTAINER_NAME} 已存在，启动之"
  docker start ${CONTAINER_NAME}
  exit 0
fi

# 拉镜像（本地已有则秒过）
echo "[Nacos] 拉取镜像 ${IMAGE} ..."
docker pull ${IMAGE}

# 启动容器：8848(HTTP) + 9848(gRPC)，standalone，关闭鉴权方便本地开发
echo "[Nacos] 启动容器 ${CONTAINER_NAME} ..."
docker run -d \
  --name ${CONTAINER_NAME} \
  -p 8848:8848 \
  -p 9848:9848 \
  -e MODE=standalone \
  -e NACOS_AUTH_ENABLE=false \
  --restart unless-stopped \
  ${IMAGE}

echo "[Nacos] 启动完成，等待就绪（约 10~30s）..."
