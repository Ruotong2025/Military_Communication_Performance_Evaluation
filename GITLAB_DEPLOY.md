# GitLab 部署文档

## 服务器信息
- **服务器IP**: 172.0.249.12
- **SSH端口**: 1122
- **访问地址**: http://172.0.249.12

---

## 一、系统环境检查

SSH 登录到服务器：

```bash
ssh user4@172.0.249.12 -p 1122
# 密码: !Hzsfdxlzl!
```

检查系统信息：

```bash
# 查看操作系统版本
cat /etc/os-release

# 查看内存
free -h

# 查看磁盘空间
df -h

# 查看 CPU 信息
cat /proc/cpuinfo | grep "model name" | head -1
```

**最低配置要求：**
- CPU: 4 核以上
- 内存: 4GB 以上（推荐 8GB）
- 磁盘: 20GB 以上

---

## 二、安装依赖包

```bash
# 更新系统包
sudo yum update -y

# 安装依赖
sudo yum install -y curl policycoreutils-python postfix cronie
```

---

## 三、安装 Postfix（邮件服务）

GitLab 需要邮件服务发送通知：

```bash
# 安装 postfix
sudo yum install -y postfix

# 设置开机启动
sudo systemctl enable postfix

# 启动 postfix
sudo systemctl start postfix
```

---

## 四、添加 GitLab 仓库

### 方法一：使用 GitLab 官方脚本（推荐）

```bash
# 下载 GitLab 安装脚本
curl -fsSL https://packages.gitlab.com/install/repositories/gitlab/gitlab-ee/script.rpm.sh | sudo bash
```

### 或者手动添加仓库

```bash
# 创建仓库文件
sudo vim /etc/yum.repos.d/gitlab_gitlab-ee.repo
```

添加以下内容：

```ini
[gitlab_gitlab-ee]
name=GitLab Enterprise Edition
baseurl=https://packages.gitlab.com/install/repositories/gitlab/gitlab-ee/el/7/$basearch
repo_gpgcheck=0
gpgcheck=0
enabled=1
gpgkey=https://packages.gitlab.com/gpg.key
sslverify=0
```

保存退出（`:wq`）

---

## 五、安装 GitLab

```bash
# 安装 GitLab（指定域名）
sudo EXTERNAL_URL="http://172.0.249.12" yum install -y gitlab-ee
```

**注意：** 安装过程可能需要 10-20 分钟，取决于网络速度。

---

## 六、配置 GitLab

### 6.1 修改 GitLab 配置文件

```bash
# 编辑 GitLab 配置文件
sudo vim /etc/gitlab/gitlab.rb
```

找到并修改以下配置：

```ruby
# 修改外部访问 URL
external_url 'http://172.0.249.12'

# 修改 GitLab 监听端口（如果 80 端口被占用）
nginx['listen_port'] = 8080

# 修改 SSH 端口（如果服务器 SSH 不是 22）
gitlab_rails['gitlab_shell_ssh_port'] = 1122

# 关闭 HTTPS（如果不需要）
nginx['enable'] = true

# 设置时区
gitlab_rails['time_zone'] = 'Asia/Shanghai'
```

保存退出（`:wq`）

### 6.2 重新配置 GitLab

```bash
# 重新配置 GitLab
sudo gitlab-ctl reconfigure
```

此命令需要几分钟完成。

---

## 七、防火墙配置

```bash
# 检查防火墙状态
sudo systemctl status firewalld

# 如果防火墙开启，放行端口
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --permanent --add-port=8080/tcp

# 重载防火墙
sudo firewall-cmd --reload

# 查看已开放的端口
sudo firewall-cmd --list-all
```

**如果使用 iptables：**

```bash
# 添加规则
sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 8080 -j ACCEPT

# 保存规则
sudo service iptables save
```

---

## 八、启动和检查服务

```bash
# 检查 GitLab 状态
sudo gitlab-ctl status

# 如果未运行，启动服务
sudo gitlab-ctl start

# 查看所有服务状态
sudo gitlab-ctl status nginx postgresql redis gitlab-workhorse gitaly
```

---

## 九、访问 GitLab

### 9.1 首次访问

打开浏览器访问：
```
http://172.0.249.12
```

### 9.2 获取初始密码

```bash
# 查看初始 root 密码
sudo cat /etc/gitlab/initial_root_password
```

默认账号：`root`
密码：文件中的密码

**首次登录后请立即修改密码！**

---

## 十、创建新用户和项目

### 10.1 登录后创建新用户

1. 点击右上角用户图标
2. 选择 "Admin Area"
3. 点击 "New User"
4. 填写信息后点击 "Create user"

### 10.2 创建 SSH 密钥（客户端）

在本地 Windows/PowerShell 上执行：

```powershell
# 生成 SSH 密钥
ssh-keygen -t ed25519 -C "your_email@example.com"

# 查看公钥
cat ~/.ssh/id_ed25519.pub
```

将公钥添加到 GitLab 用户设置中。

### 10.3 Clone 代码示例

```bash
# 克隆仓库（使用 SSH）
git clone git@172.0.249.12:username/projectname.git

# 或使用 HTTP
git clone http://172.0.249.12/username/projectname.git
```

---

## 十一、常用管理命令

```bash
# 启动所有服务
sudo gitlab-ctl start

# 停止所有服务
sudo gitlab-ctl stop

# 重启所有服务
sudo gitlab-ctl restart

# 检查状态
sudo gitlab-ctl status

# 查看日志
sudo gitlab-ctl tail

# 查看 nginx 日志
sudo gitlab-ctl tail nginx

# 查看 gitlab 日志
sudo gitlab-ctl tail gitlab-rails

# 重新加载配置（修改配置后执行）
sudo gitlab-ctl reconfigure

# 检查 GitLab 配置
sudo gitlab-rake gitlab:check

# 查看 GitLab 版本
sudo gitlab-rake gitlab:env:info
```

---

## 十二、备份和恢复

### 12.1 创建备份

```bash
# 创建备份
sudo gitlab-rake gitlab:backup:create

# 备份文件位置
sudo ls -la /var/opt/gitlab/backups/
```

### 12.2 自动备份（可选）

创建 cron 任务：

```bash
# 编辑 crontab
sudo crontab -e
```

添加以下行（每天凌晨 2 点备份）：

```cron
0 2 * * * /opt/gitlab/bin/gitlab-rake gitlab:backup:create CRON=1
```

### 12.3 恢复备份

```bash
# 停止相关服务
sudo gitlab-ctl stop unicorn
sudo gitlab-ctl stop sidekiq

# 恢复备份（指定备份文件）
sudo gitlab-rake gitlab:backup:restore BACKUP=备份文件名

# 重启服务
sudo gitlab-ctl start
```

---

## 十三、优化配置（可选）

### 13.1 修改内存限制

```bash
sudo vim /etc/gitlab/gitlab.rb
```

```ruby
# Unicorn 工作进程数
unicorn['worker_processes'] = 4

# Sidekiq 并发数
sidekiq['concurrency'] = 10

# Postgresql 内存
postgresql['shared_buffers'] = "256MB"
```

### 13.2 修改仓库存储路径

```ruby
git_data_dirs({
  "default" => {
    "path" => "/mnt/git-data"
  }
})
```

修改后执行：

```bash
sudo gitlab-ctl reconfigure
```

---

## 十四、卸载 GitLab（如果需要）

```bash
# 停止服务
sudo gitlab-ctl stop

# 卸载 gitlab
sudo rpm -e gitlab-ee

# 清理配置文件
sudo rm -rf /opt/gitlab
sudo rm -rf /etc/gitlab
sudo rm -rf /var/opt/gitlab
sudo rm -rf /var/log/gitlab

# 清理用户
sudo userdel git
```

---

## 十五、故障排查

### 15.1 查看服务状态

```bash
sudo gitlab-ctl status
sudo gitlab-ctl tail
```

### 15.2 检查端口占用

```bash
sudo netstat -tlnp | grep -E ":(80|443|22|8080)"
```

### 15.3 检查日志

```bash
# Nginx 日志
sudo tail -f /var/log/gitlab/nginx/gitlab_access.log
sudo tail -f /var/log/gitlab/nginx/gitlab_error.log

# Rails 日志
sudo tail -f /var/log/gitlab/gitlab-rails/production.log
```

### 15.4 常见问题

**问题：安装后无法访问**
```bash
# 检查防火墙
sudo systemctl status firewalld
sudo firewall-cmd --list-all

# 检查 nginx
sudo gitlab-ctl status nginx
```

**问题：502 错误**
```bash
# 等待几秒后重试，或检查内存是否不足
free -h
# 增加内存或增加 swap
```

**问题：忘记 root 密码**
```bash
# 重置 root 密码
sudo gitlab-rails console -e production
```
```ruby
user = User.where(id: 1).first
user.password = '新密码'
user.password_confirmation = '新密码'
user.save!
exit
```

---

## 十六、访问信息汇总

| 项目 | 地址 |
|------|------|
| GitLab 首页 | http://172.0.249.12 |
| 管理界面 | http://172.0.249.12/admin |
| 用户设置 | http://172.0.249.12/-/profile |
| API 文档 | http://172.0.249.12/api/v4 |
| SSH 端口 | 1122 |
| Web 端口 | 80 (或 8080) |

---

## 十七、快速部署脚本

将以下内容保存为 `install_gitlab.sh`：

```bash
#!/bin/bash

# GitLab 安装脚本
# 适用于 CentOS 7/RHEL 7

set -e

GITLAB_URL="http://172.0.249.12"
SSH_PORT=1122

echo "=== 开始安装 GitLab ==="

# 1. 更新系统
echo "[1/7] 更新系统..."
sudo yum update -y

# 2. 安装依赖
echo "[2/7] 安装依赖..."
sudo yum install -y curl policycoreutils-python postfix cronie

# 3. 启动 postfix
echo "[3/7] 配置 postfix..."
sudo systemctl enable postfix
sudo systemctl start postfix

# 4. 添加 GitLab 仓库
echo "[4/7] 添加 GitLab 仓库..."
curl -fsSL https://packages.gitlab.com/install/repositories/gitlab/gitlab-ee/script.rpm.sh | sudo bash

# 5. 安装 GitLab
echo "[5/7] 安装 GitLab..."
sudo EXTERNAL_URL="$GITLAB_URL" yum install -y gitlab-ee

# 6. 配置防火墙
echo "[6/7] 配置防火墙..."
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-port=${SSH_PORT}/tcp
sudo firewall-cmd --reload

# 7. 完成
echo "[7/7] 安装完成！"
echo "========================================"
echo "访问地址: $GITLAB_URL"
echo "初始密码: sudo cat /etc/gitlab/initial_root_password"
echo "========================================"
```

使用方法：
```bash
chmod +x install_gitlab.sh
sudo ./install_gitlab.sh
```

---

## 十八、后续维护

### 18.1 升级 GitLab

```bash
# 备份
sudo gitlab-rake gitlab:backup:create

# 停止服务
sudo gitlab-ctl stop unicorn
sudo gitlab-ctl stop sidekiq
sudo gitlab-ctl stop nginx

# 升级
sudo yum update -y gitlab-ee

# 重启
sudo gitlab-ctl restart
```

### 18.2 常用维护任务

```bash
# 检查 GitLab 健康状态
sudo gitlab-rake gitlab:health:check

# 检查 GitLab 配置
sudo gitlab-rake gitlab:check

# 清理缓存
sudo gitlab-rake cache:clear

# 清理过期分支
sudo gitlab-rake gitlab:cleanup:stale_projects
```
