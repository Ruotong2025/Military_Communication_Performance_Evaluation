#!/bin/bash
# ============================================================
# 军事通信效能评估系统 — 一键部署脚本
#
# 使用方法：
#   bash deploy.sh              # 完整部署（拉取最新代码 + 重建容器）
#   bash deploy.sh --no-pull   # 仅重建容器（不拉取代码，用于本地调试）
# ============================================================

set -e

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_DIR"

# 获取服务器 IP（用于最后显示访问地址）
SERVER_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "172.0.249.12")

echo "=========================================="
echo "  军事通信效能评估系统 — Docker 部署脚本"
echo "=========================================="
echo ""

# 是否跳过 git pull
if [ "$1" != "--no-pull" ]; then
    echo "========== [1/6] 拉取最新代码 =========="
    git pull origin main
    echo ""
fi

echo "========== [2/6] 停止旧容器 =========="
docker-compose down || true
echo ""

echo "========== [3/6] 构建镜像（首次约 15-20 分钟，后续增量约 3-5 分钟）==========="
docker-compose build --parallel
echo ""

echo "========== [4/6] 启动所有服务 =========="
docker-compose up -d
echo ""

echo "========== [5/6] 等待 MySQL 健康检查通过 =========="
for i in $(seq 1 30); do
  if docker-compose ps mysql 2>/dev/null | grep -q "(healthy)"; then
    echo "✅ MySQL 健康检查通过！"
    break
  fi
  echo "   等待中... ($i/30)"
  sleep 3
done
echo ""

echo "========== [6/6] 检查所有容器状态 =========="
docker-compose ps
echo ""

echo "=============================================="
echo "🎉 部署完成！"
echo ""
echo "  前端页面：http://${SERVER_IP}/"
echo "  Swagger 文档：http://${SERVER_IP}/api/swagger-ui.html"
echo ""
echo "  常用命令："
echo "    查看日志：docker-compose logs -f"
echo "    停止服务：docker-compose down"
echo "    重新部署：bash deploy.sh"
echo "=============================================="
