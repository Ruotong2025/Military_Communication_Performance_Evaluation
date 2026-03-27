#!/bin/bash
# ============================================================
# 生产环境打包脚本
# 使用：./scripts/build-all.sh
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "  军事通信效能评估系统 - 生产打包"
echo "=========================================="
echo ""

# ---------- 1. 编译后端 ----------
echo "[1/4] 编译后端..."
mvn -q clean package -DskipTests
echo "  [OK] 完成"

# ---------- 2. 构建前端 ----------
echo "[2/4] 构建前端..."
cd frontend && npm run build && cd ..
echo "  [OK] 完成"

# ---------- 3. 准备部署目录 ----------
echo "[3/4] 准备部署..."

DEPLOY_DIR="$SCRIPT_DIR/deploy-package"
rm -rf "$DEPLOY_DIR"
mkdir -p "$DEPLOY_DIR"

# 复制文件
cp src/main/resources/application.yml "$DEPLOY_DIR/"
cp src/main/resources/application-prod.yml "$DEPLOY_DIR/"
JAR_NAME=$(ls target/*.jar | grep -v '.original' | head -1)
cp "$JAR_NAME" "$DEPLOY_DIR/app.jar"
cp -r frontend/dist "$DEPLOY_DIR/"

# 创建启动脚本
cat > "$DEPLOY_DIR/start.sh" << 'STARTSCRIPT'
#!/bin/bash
nohup java -Xms512m -Xmx1024m -jar app.jar --spring.profiles.active=prod > app.log 2>&1 &
echo $! > app.pid
echo "服务已启动 (PID: $(cat app.pid))"
STARTSCRIPT

# 创建停止脚本
cat > "$DEPLOY_DIR/stop.sh" << 'STOPSCRIPT'
#!/bin/bash
if [ -f app.pid ]; then
    kill $(cat app.pid) 2>/dev/null
    rm -f app.pid
    echo "服务已停止"
else
    echo "服务未运行"
fi
STOPSCRIPT

# 创建状态脚本
cat > "$DEPLOY_DIR/status.sh" << 'STATUSSCRIPT'
#!/bin/bash
if [ -f app.pid ] && kill -0 $(cat app.pid) 2>/dev/null; then
    echo "[运行中] PID: $(cat app.pid)"
    tail -n 5 app.log
else
    echo "[已停止]"
fi
STATUSSCRIPT

# 创建部署说明
cat > "$DEPLOY_DIR/DEPLOY.txt" << 'DEPLOYTEXT'
============================================
军事通信效能评估系统 - 部署说明
============================================

【1】首次部署
--------------
cd /opt/military-evaluation
unzip deploy.zip
chmod +x *.sh
vim application-prod.yml    # 修改数据库账号密码
./start.sh

【2】后续更新
--------------
./stop.sh
# 上传新的 JAR 文件覆盖 app.jar
./start.sh

【3】服务管理
--------------
./start.sh    启动
./stop.sh     停止
./status.sh   状态

【4】日志
--------------
tail -f app.log
DEPLOYTEXT

chmod +x "$DEPLOY_DIR"/*.sh
echo "  [OK] 完成"

# ---------- 4. 压缩打包 ----------
echo "[4/4] 压缩打包..."
cd "$DEPLOY_DIR"
zip -r "$SCRIPT_DIR/deploy.zip" . .
cd "$SCRIPT_DIR"
rm -rf "$DEPLOY_DIR"

echo ""
echo "=========================================="
echo "  打包完成！deploy.zip"
echo "=========================================="
