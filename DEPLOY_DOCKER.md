# 军事通信效能评估系统 — Docker 部署指南（运维小白版）

> **目标服务器**：`ssh user4@172.0.249.12 -p`
> **操作系统**：Linux（CentOS / Ubuntu / Debian 均适用）
> **部署方式**：Docker + Docker Compose（全自动，一行命令启动）

---

## 目录

1. [一、服务器环境准备](#一服务器环境准备)
2. [二、Git 仓库克隆（推荐，免打包上传）](#二git-仓库克隆推荐免打包上传)
3. [三、修改数据库密码（重要）](#三修改数据库密码重要)
4. [四、一键启动所有服务](#四一键启动所有服务)
5. [五、验证部署是否成功](#五验证部署是否成功)
6. [六、初始化数据库表结构](#六初始化数据库表结构)
7. [七、常用运维命令](#七常用运维命令)
8. [八、代码更新（Git 免打包部署）](#八代码更新git-免打包部署)
9. [九、常见问题与解决方案](#九常见问题与解决方案)
10. [十、附录：项目架构说明](#十附录项目架构说明)

---

## 一、服务器环境准备

### 1.1 检查服务器是否有 Docker

登录服务器后，在终端输入以下命令检查：

```bash
docker --version
docker-compose --version
```

### 1.2 版本兼容性说明（重要！）

| 你服务器上的版本 | 状态 | 说明 |
|------|------|------|
| Docker `20.10.9` | ✅ 完美兼容 | 最新 Docker 24.x 才刚出，20.x 完全够用 |
| docker-compose `1.25.4` | ✅ 兼容 | **经典 v1 版本**，所有特性完全支持 |

> ⚠️ **特别注意**：`1.25.4` 是经典版（带 hyphen），命令格式为 `docker-compose`（中间有横杠），
> **不是** Docker Desktop 自带的 v2（`docker compose`，中间无横杠）。
> 本文档所有命令均使用 `docker-compose` 格式，**直接复制粘贴即可**。

如果显示版本号说明已安装，跳到 [二、Git 仓库克隆](#二git-仓库克隆推荐免打包上传)。

### 1.3 如果没有安装 Docker，请按以下步骤安装

> **以 Ubuntu / Debian 为例**（你的服务器 Z370 很可能是 Ubuntu/Debian）

```bash
# Step 1：更新系统包
sudo apt update && sudo apt upgrade -y

# Step 2：安装依赖
sudo apt install -y ca-certificates curl gnupg lsb-release

# Step 3：添加 Docker 官方 GPG 密钥
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Step 4：添加 Docker 仓库
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Step 5：安装 Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose

# Step 6：启动 Docker 并设置开机自启
sudo systemctl start docker
sudo systemctl enable docker

# Step 7：验证
docker --version
sudo docker run hello-world
```

> **以 CentOS 7/8 为例**

```bash
sudo yum install -y yum-utils device-mapper-persistent-data lvm2
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io docker-compose
sudo systemctl start docker
sudo systemctl enable docker
docker --version
```

### 1.4 给当前用户授权（避免每次 sudo）

```bash
# 将当前用户加入 docker 组（你的用户名是 user4，无需修改）
sudo usermod -aG docker user4

# 重新登录后生效（或者执行以下命令立即生效）
newgrp docker

# 验证：不用 sudo 能否运行 docker
docker ps
```

---

## 二、Git 仓库克隆（推荐，免打包上传）

> **优点**：每次代码更新后，在服务器上执行一条 `git pull` 即可，无需打包上传。

### 2.1 首次设置：克隆仓库到服务器

```bash
# SSH 登录到服务器
ssh user4@172.0.249.12

# 创建项目目录并进入
sudo mkdir -p /opt/military-evaluation
sudo chown user4:user4 /opt/military-evaluation   # 目录归属当前用户，避免权限问题

cd /opt/military-evaluation

# ★★★ 重要：替换为你的实际 Git 仓库地址 ★★★
# 方式 A：HTTPS（推荐，简单）
git init
git remote add origin https://your-git-repo-url/military-evaluation.git
git pull origin main

# 方式 B：SSH（需要先配置 SSH 公钥，更安全）
git init
git remote add origin git@your-git-repo-url:military-evaluation.git
git pull origin main
```

### 2.2 确认克隆成功

```bash
ls -la /opt/military-evaluation/
```

正常情况下应该看到以下关键文件：

```
├── docker-compose.yml          ← 核心：一键部署配置
├── Dockerfile.springboot       ← 后端镜像构建文件
├── Dockerfile.frontend         ← 前端镜像构建文件
├── nginx.conf                  ← Nginx 配置
├── pom.xml                     ← Maven 配置
├── deploy.sh                   ← 一键部署脚本（首次部署后自动生成）
├── SQL/                        ← 数据库脚本目录
├── src/                        ← 后端 Java 源代码
└── frontend/                   ← 前端 Vue 源代码
```

### 2.3 一键部署脚本（自动创建，无需手动编写）

> 服务器上执行一次即可，以后更新代码只需运行它。

在服务器上创建一键部署脚本：

```bash
nano /opt/military-evaluation/deploy.sh
```

粘贴以下**完整内容**，保存（`Ctrl+O`，回车，`Ctrl+X`）：

```bash
#!/bin/bash
# ============================================================
# 军事通信效能评估系统 — 一键部署脚本
# 使用方法：bash deploy.sh
# ============================================================

set -e

APP_DIR="/opt/military-evaluation"
cd "$APP_DIR"

echo "========== [1/5] 拉取最新代码 =========="
git pull origin main

echo "========== [2/5] 停止旧容器 =========="
docker-compose down || true

echo "========== [3/5] 重新构建并启动 =========="
docker-compose up -d --build

echo "========== [4/5] 等待 MySQL 健康检查 =========="
for i in $(seq 1 30); do
  if docker-compose ps mysql 2>/dev/null | grep -q "(healthy)"; then
    echo "✅ MySQL 健康检查通过！"
    break
  fi
  echo "   等待中... ($i/30)"
  sleep 3
done

echo "========== [5/5] 检查所有容器状态 =========="
docker-compose ps

echo ""
echo "==============================================="
echo "🎉 部署完成！访问 http://$(hostname -I | awk '{print $1}')/"
echo "==============================================="
```

添加执行权限：

```bash
chmod +x /opt/military-evaluation/deploy.sh
```

### 2.4 以后代码更新流程（极简 2 步）

```bash
# Step 1：在本机（Windows/Mac）push 代码到 Git 仓库
git add . && git commit -m "更新内容" && git push origin main

# Step 2：在服务器上执行一条命令即可完成部署
ssh user4@172.0.249.12 "cd /opt/military-evaluation && bash deploy.sh"
```

> 💡 **更懒的方式**：在服务器上设置**定时自动部署**，代码 push 后服务器自动拉取（见 [八、代码更新](#八代码更新git-免打包部署)）。

---

## 三、修改数据库密码（重要）

> **安全提醒**：默认密码 `root123456` 仅供测试使用，正式环境请务必修改为强密码。

### 3.1 修改 docker-compose.yml 中的数据库密码

```bash
cd /opt/military-evaluation/
nano docker-compose.yml
```

找到以下两处 `MYSQL_ROOT_PASSWORD` 和 `SPRING_DATASOURCE_PASSWORD`，将 `root123456` 改为你想要的新密码（建议包含大小写字母和数字，长度 ≥ 8 位）：

```yaml
mysql:
    environment:
      MYSQL_ROOT_PASSWORD: root123456   # ← 改成你的强密码

backend:
    environment:
      SPRING_DATASOURCE_PASSWORD: root123456   # ← 和上面保持一致
```

### 3.2 如果之前已经运行过，需要清除旧数据

> **注意：以下操作会删除所有历史数据，如果是正式环境请三思而后行！**

```bash
cd /opt/military-evaluation/

# 停止所有容器
docker-compose down

# 删除 MySQL 数据卷（清除旧数据库数据，重新初始化）
docker volume rm military-evaluation_mysql-data 2>/dev/null || true

# 删除旧镜像（可选，下次 up 时会自动重新构建）
docker images | grep military
docker rmi military-evaluation_backend   2>/dev/null || true
docker rmi military-evaluation_frontend   2>/dev/null || true
```

---

## 四、一键启动所有服务

```bash
cd /opt/military-evaluation/

# 构建并启动所有容器（首次运行需要下载镜像和编译，时间较长约 10-20 分钟）
docker-compose up -d --build
```

### 参数说明

| 参数 | 含义 |
|------|------|
| `up` | 启动所有服务 |
| `-d` | 后台运行（不加会前台打印日志） |
| `--build` | 强制重新构建镜像（首次必须，后续更新代码时使用） |

### 查看启动状态

```bash
# 查看所有容器状态（Up = 运行中）
docker-compose ps

# 查看实时日志（实时监控后端启动情况）
docker-compose logs -f backend

# 等 MySQL 就绪后再查看（Ctrl+C 退出日志）
docker-compose logs -f mysql
```

---

## 五、验证部署是否成功

### 5.1 等待所有服务启动（约 3-5 分钟）

```bash
# 等待 60 秒后检查所有容器是否都 Up
sleep 60 && docker-compose ps
```

正常情况下应该看到三个容器都是 `Up` 状态：

```
NAME                STATUS
military-mysql      Up (healthy)
military-backend    Up
military-frontend   Up
```

### 5.2 验证各个服务端口

```bash
# 查看容器监听的端口
docker-compose ps

# 测试 MySQL 连接
docker exec military-mysql mysql -uroot -p -e "SELECT VERSION();"

# 测试后端 API（返回 JSON 说明正常）
curl http://localhost:8080/api/actuator/health 2>/dev/null || \
curl http://localhost:8080/api/v3/api-docs 2>/dev/null | head -c 200

# 测试前端 Nginx（返回 HTML 说明正常）
curl http://localhost/ | head -c 300
```

### 5.3 浏览器访问验证

打开浏览器，访问以下地址：

| 服务 | 地址 | 说明 |
|------|------|------|
| **前端页面** | `http://172.0.249.12/` | 军事通信效能评估系统 Web 界面 |
| **Swagger API 文档** | `http://172.0.249.12/api/swagger-ui.html` | 后端所有 API 接口说明（可在线测试） |
| **API Docs JSON** | `http://172.0.249.12/api/v3/api-docs` | OpenAPI 3.0 规范 JSON |

> **如果访问 `http://172.0.249.12/` 提示 502 Bad Gateway**，说明后端还在启动中，请等待 1-2 分钟后再试。

---

## 六、初始化数据库表结构

> **重要**：首次部署时，`docker-compose.yml` 中的 `volumes: ./SQL:/docker-entrypoint-initdb.d:ro` 配置会在 MySQL 容器**首次创建时**自动执行 `SQL/` 目录下的所有 `.sql` 文件。
>
> 如果 SQL 目录为空或启动时没有自动执行，需要手动执行。

### 6.1 查看当前数据库有哪些表

```bash
docker exec military-mysql mysql -uroot -p -e "USE military_operational_effectiveness_evaluation; SHOW TABLES;"
```

### 6.2 手动执行 SQL 脚本

```bash
cd /opt/military-evaluation/

# 执行建表脚本
docker exec -i military-mysql mysql -uroot -p'myStrongPassword123' \
    military_operational_effectiveness_evaluation < SQL/metrics_military_comm_effect.sql

# 执行数据插入脚本（可选）
docker exec -i military-mysql mysql -uroot -p'myStrongPassword123' \
    military_operational_effectiveness_evaluation < SQL/insert_military_effectiveness_evaluation.sql
```

> **注意**：将命令中的 `myStrongPassword123` 替换为你实际设置的数据库密码。

---

## 七、常用运维命令

### 7.1 启动 / 停止 / 重启

```bash
# 停止所有服务（数据保留在 volume 中）
docker-compose stop

# 启动已停止的服务
docker-compose start

# 重启所有服务
docker-compose restart

# 完全停止并删除容器（数据不丢失，数据在 volume 中）
docker-compose down

# 完全停止并删除容器 + 删除 MySQL 数据（慎用！）
docker-compose down -v
```

### 7.2 查看日志

```bash
# 查看所有服务实时日志
docker-compose logs -f

# 只看后端日志
docker-compose logs -f backend

# 只看最近 100 行日志
docker-compose logs --tail=100 backend

# 搜索日志中的错误
docker-compose logs | grep -i error
```

### 7.3 进入容器内部

```bash
# 进入 MySQL 容器
docker exec -it military-mysql bash

# 进入后端 Java 容器
docker exec -it military-backend bash

# 进入前端 Nginx 容器
docker exec -it military-frontend sh

# 在容器内连接数据库
mysql -uroot -p
```

### 7.4 查看资源占用

```bash
# 查看所有容器资源占用
docker stats

# 查看磁盘占用
docker system df
```

### 7.5 安全设置防火墙（如有需要）

如果服务器有防火墙（firewalld / ufw），需要开放端口：

```bash
# CentOS/RHEL - firewalld
sudo firewall-cmd --permanent --add-port=80/tcp   # HTTP
sudo firewall-cmd --permanent --add-port=3306/tcp  # MySQL（可选，仅开发调试用）
sudo firewall-cmd --reload

# Ubuntu/Debian - ufw
sudo ufw allow 80/tcp
sudo ufw allow 3306/tcp   # 可选
sudo ufw reload
```

---

## 八、代码更新（Git 免打包部署）

### 方案一：手动一键更新（推荐）

代码 push 到 Git 仓库后，在服务器上执行一键脚本即可：

```bash
ssh user4@172.0.249.12 "cd /opt/military-evaluation && bash deploy.sh"
```

### 方案二：定时自动部署（适合开发测试环境）

服务器定时（如每 5 分钟）自动拉取最新代码并重启容器：

```bash
# 服务器上编辑定时任务
crontab -e

# 添加以下两行（每 5 分钟检查一次，有更新则自动部署）
*/5 * * * * cd /opt/military-evaluation && git pull origin main >> /var/log/git-deploy.log 2>&1
*/5 * * * * cd /opt/military-evaluation && docker-compose up -d --build >> /var/log/docker-deploy.log 2>&1
```

### 方案三：Git Webhook（生产环境推荐，实时更新）

代码 push 后 Git 服务器自动通知服务器拉取并部署：

```bash
# Step 1：服务器上安装 webhook 工具
sudo apt install -y webhook

# Step 2：创建 webhook 触发脚本
cat > /opt/military-evaluation/webhook-deploy.sh << 'EOF'
#!/bin/bash
cd /opt/military-evaluation
git pull origin main
docker-compose up -d --build
EOF
chmod +x /opt/military-evaluation/webhook-deploy.sh

# Step 3：配置 webhook（监听 Git 服务器的 POST 请求）
cat > /etc/webhook.conf << 'EOF'
[
  {
    "id": "deploy",
    "execute-command": "/opt/military-evaluation/webhook-deploy.sh",
    "command-working-directory": "/opt/military-evaluation",
    "response-message": "部署已触发，正在构建..."
  }
]
EOF

# Step 4：启动 webhook 服务（端口 9000，可自行修改）
nohup webhook -hooks /etc/webhook.conf -port 9000 &

# Step 5：在 Git 服务器（如 Gitea/GitLab/Gitee）配置 Webhook
# URL 填：http://172.0.249.12:9000/hooks/deploy
# 触发事件：Push events（代码推送时触发）
```

### 单独更新前端或后端

```bash
# 只重新构建前端
docker-compose build frontend
docker-compose up -d frontend

# 只重新构建后端
docker-compose build backend
docker-compose up -d backend
```

### 备份与回滚

```bash
# 备份当前版本（带时间戳）
sudo cp -r /opt/military-evaluation /opt/military-evaluation.bak.$(date +%Y%m%d%H%M%S)

# 回滚到上一个版本（Git 版本控制的好处）
cd /opt/military-evaluation
git log --oneline -5         # 查看最近 5 次提交
git reset --hard HEAD~1      # 回滚到上一个提交
docker-compose up -d --build
```

---

## 九、常见问题与解决方案

### Q1：MySQL 容器一直处于 `starting` 状态

**原因**：可能是挂载的 SQL 文件有语法错误或 MySQL 数据卷权限问题。

**解决**：

```bash
# 查看 MySQL 详细日志
docker-compose logs mysql

# 如果是首次启动失败，删除数据卷重新初始化
docker-compose down -v
docker-compose up -d
```

### Q2：后端容器 `Exit (1)` 启动失败

**原因**：数据库连接失败或配置错误。

**解决**：

```bash
# 查看后端详细日志
docker-compose logs backend

# 常见错误：
# - "Access denied"：数据库密码不匹配，检查 docker-compose.yml 中的密码
# - "Connection refused"：MySQL 未就绪，确保 mysql healthcheck 通过
# - "Unknown database"：数据库不存在，确认 SQL 建表脚本已执行
```

### Q3：前端显示 `502 Bad Gateway`

**原因**：后端未启动或 Nginx 配置错误。

**解决**：

```bash
# 确认后端已启动
docker-compose ps backend

# 查看前端 Nginx 日志
docker-compose logs frontend

# 检查 Nginx 能否访问后端（容器内测试）
docker exec military-frontend wget -qO- http://backend:8080/api/v3/api-docs | head -c 100
```

### Q4：页面样式错乱或静态资源 404

**原因**：前端构建时 `base` 配置与部署路径不匹配。

**解决**：检查 `frontend/vite.config.js` 中的 `base` 配置，如果部署在子目录（如 `/app/`），需要修改为 `/` 或对应的子路径，并同步修改 `nginx.conf` 中的 `root` 配置。

### Q5：容器启动正常但 API 返回 500

**原因**：后端 Java 代码运行时异常。

**解决**：

```bash
# 查看后端日志，找到 ERROR 级别的堆栈信息
docker-compose logs backend | grep -A 20 "ERROR"

# 如果是 Python 脚本相关错误，检查项目根目录下是否存在 python_service/evaluation_service.py
docker exec military-backend ls -la /app/
```

### Q6：如何修改后端访问的数据库地址？

如果你需要让后端连接**外部数据库**（而非 Docker 容器内的 MySQL），修改 `docker-compose.yml` 中 backend 的环境变量：

```yaml
backend:
    environment:
      # ★★★ 使用外部数据库时取消注释并填写 ★★★
      # SPRING_DATASOURCE_URL: jdbc:mysql://192.168.1.100:3306/military_operational_effectiveness_evaluation?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      # SPRING_DATASOURCE_USERNAME: root
      # SPRING_DATASOURCE_PASSWORD: yourExternalPassword
```

### Q7：如何配置 HTTPS（SSL 证书）？

在生产环境中启用 HTTPS，推荐使用 **Nginx 反代 + Let's Encrypt 免费证书**：

```bash
# 安装 certbot
sudo apt install -y certbot python3-certbot-nginx

# 获取 SSL 证书（需要域名解析已生效）
sudo certbot --nginx -d your-domain.com

# 自动续期（certbot 会设置 cron 任务）
sudo certbot renew --dry-run
```

然后修改 `nginx.conf`，在 `listen 80;` 后添加：

```nginx
listen 443 ssl http2;
ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
```

### Q8：如何设置开机自启动？

```bash
# 方法 1：使用 systemd（推荐）
sudo nano /etc/systemd/system/military-evaluation.service
```

写入以下内容：

```ini
[Unit]
Description=Military Evaluation System
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/military-evaluation
ExecStart=/usr/local/bin/docker-compose up -d
ExecStop=/usr/local/bin/docker-compose down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
```

然后启用服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable military-evaluation.service
sudo systemctl start military-evaluation.service
sudo systemctl status military-evaluation.service
```

---

## 十、附录：项目架构说明

### 10.1 整体架构图

```
                        ┌──────────────────────────────────────────┐
                        │           用户浏览器 (Browser)             │
                        │    http://172.0.249.12  → Vue 前端        │
                        └──────────────┬───────────────────────────┘
                                       │ HTTP (80 端口)
                        ┌──────────────▼───────────────────────────┐
                        │         Nginx (frontend 容器)            │
                        │   ┌──────────────────────────────┐      │
                        │   │  静态文件 (index.html/JS/CSS) │      │
                        │   └──────────┬───────────────────┘      │
                        │              │ /api/* 请求               │
                        │   ┌──────────▼───────────────────┐      │
                        │   │  反向代理 → backend:8080     │      │
                        │   └──────────────────────────────┘      │
                        └──────────────┬───────────────────────────┘
                                       │ 容器内网络
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
                    ▼                  ▼                  ▼
           ┌────────────────┐  ┌────────────────┐  ┌────────────────┐
           │  MySQL 8.0     │  │  Spring Boot   │  │   Python       │
           │  (mysql 容器)  │  │  (backend 容器)│  │  (后端进程)    │
           │   3306        │  │    8080        │  │  评估脚本       │
           │               │  │  Java 8        │  │               │
           └───────────────┘  └────────────────┘  └───────────────┘
```

### 10.2 容器说明

| 容器名 | 镜像 | 端口 | 说明 |
|--------|------|------|------|
| `military-mysql` | `mysql:8.0` | `3306` | MySQL 数据库 |
| `military-backend` | 自定义 | `8080` | Spring Boot 后端 API |
| `military-frontend` | `nginx:1.25-alpine` | `80` | Vue 前端 + Nginx 反代 |

### 10.3 前端 API 请求流程

```
浏览器 → http://172.0.249.12/   → Nginx 容器（静态文件 + 反代）
                               ↓
        http://172.0.249.12/api/evaluation/xxx
                               ↓
                    Nginx → http://backend:8080/api/evaluation/xxx
                               ↓
                         Spring Boot 后端处理
                               ↓
                         MySQL 数据库 / Python 脚本
```

### 10.4 数据持久化

| 挂载点 | 说明 | 备份方式 |
|--------|------|----------|
| MySQL 数据 (`mysql-data` 卷) | 所有业务数据 | `docker run --rm -v military-evaluation_mysql-data:/data -v /backup:/backup alpine tar czvf /backup/mysql-backup.tar.gz -C /data .` |

---

## 快速检查清单（部署前必读）

- [ ] 服务器已安装 Docker 和 docker-compose
- [ ] 项目文件已上传到 `/opt/military-evaluation/`
- [ ] `docker-compose.yml` 中的数据库密码已修改
- [ ] `SQL/` 目录下已放入所有 `.sql` 初始化脚本
- [ ] 服务器防火墙已开放 80 端口
- [ ] 浏览器测试访问 `http://172.0.249.12/` 正常

---

**文档版本**：v1.0
**适用项目**：军事通信效能评估系统
**Docker Compose 版本**：v2.x / v1.x 均兼容
**最后更新**：2026-03-25
