# ============================================================
# Production Build Script
# Usage: .\scripts\windows\build-all.ps1
# ============================================================

$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"

Write-Host "=========================================="
Write-Host "  Military Communication System - Build"
Write-Host "=========================================="
Write-Host ""

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$FrontendDir = Join-Path $ProjectRoot "frontend"
$DeployDir = Join-Path $ProjectRoot "deploy-package"

# ---------- 1. Build Backend ----------
Write-Host "[1/4] Building backend..."
Push-Location $ProjectRoot
mvn -q clean package -DskipTests
Pop-Location
Write-Host "  [OK] Done"

# ---------- 2. Build Frontend ----------
Write-Host "[2/4] Building frontend..."
if (-not (Test-Path $FrontendDir)) {
    Write-Host "  [ERROR] Frontend directory not found: $FrontendDir"
    exit 1
}
Push-Location $FrontendDir
npm run build
Pop-Location
Write-Host "  [OK] Done"

# ---------- 3. Prepare Deploy Package ----------
Write-Host "[3/4] Preparing deploy package..."

if (Test-Path $DeployDir) { Remove-Item -Path $DeployDir -Recurse -Force }
New-Item -Path $DeployDir -ItemType Directory | Out-Null

# Copy config files
Copy-Item "$ProjectRoot\src\main\resources\application.yml" "$DeployDir\"
Copy-Item "$ProjectRoot\src\main\resources\application-prod.yml" "$DeployDir\"

# Copy JAR file
$jarFile = Get-ChildItem "$ProjectRoot\target\*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
if ($jarFile) {
    Copy-Item $jarFile.FullName "$DeployDir\app.jar"
} else {
    Write-Host "  [ERROR] No JAR file found in target/"
    exit 1
}

# Copy frontend dist
if (Test-Path "$FrontendDir\dist") {
    Copy-Item "$FrontendDir\dist" "$DeployDir\dist" -Recurse
} else {
    Write-Host "  [ERROR] Frontend dist not found"
    exit 1
}

# Copy Python scripts (for server deployment)
$pythonDir = Join-Path $ProjectRoot "military_operational_effectiveness_evaluation"
if (Test-Path $pythonDir) {
    Copy-Item $pythonDir "$DeployDir\military_operational_effectiveness_evaluation" -Recurse
    Write-Host "  [OK] Python scripts included"
} else {
    Write-Host "  [WARN] Python scripts not found: $pythonDir"
}

# Create start script (use single quotes to prevent variable expansion)
$startScript = @'
#!/bin/bash
# Portable Java 17 (项目自带，不影响系统其他服务)
JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$0")"))/java17"

# 首次部署需要下载 Java 17
if [ ! -d "$JAVA_HOME" ]; then
    echo "首次部署，正在下载 JDK 17..."
    mkdir -p "$(dirname "$JAVA_HOME")"
    cd "$(dirname "$JAVA_HOME")"

    # 下载 Azul Zulu JDK 17
    wget -q https://cdn.azul.com/zulu/bin/zulu17.50.19-ca-jdk17.0.11-linux_x64.tar.gz
    tar -xzf zulu17.50.19-ca-jdk17.0.11-linux_x64.tar.gz
    mv zulu17.50.19-ca-jdk17.0.11-linux_x64 java17
    rm zulu17.50.19-ca-jdk17.0.11-linux_x64.tar.gz
    echo "JDK 17 安装完成"
    cd "$(dirname "$(dirname "$(readlink -f "$0"))")"
fi

# 启动服务
cd /opt/military-evaluation
nohup $JAVA_HOME/bin/java -Xms512m -Xmx1024m -jar app.jar --spring.profiles.active=prod > app.log 2>&1 &
echo $! > app.pid
echo "Service started (PID: $(cat app.pid))"
'@
$startScript | Out-File -FilePath "$DeployDir\start.sh" -Encoding ascii

# Create stop script
$stopScript = @'
#!/bin/bash
if [ -f app.pid ]; then
    kill $(cat app.pid) 2>/dev/null
    rm -f app.pid
    echo "Service stopped"
else
    echo "Service not running"
fi
'@
$stopScript | Out-File -FilePath "$DeployDir\stop.sh" -Encoding ascii

# Create status script
$statusScript = @'
#!/bin/bash
if [ -f app.pid ] && kill -0 $(cat app.pid) 2>/dev/null; then
    echo "[Running] PID: $(cat app.pid)"
    tail -n 5 app.log
else
    echo "[Stopped]"
fi
'@
$statusScript | Out-File -FilePath "$DeployDir\status.sh" -Encoding ascii

# Create deploy README
$deployText = @'
============================================
Military Communication System - Deploy Guide
============================================

[1] First Deploy (自动下载 JDK 17)
--------------
cd /opt/military-evaluation
unzip deploy.zip
chmod +x *.sh

# 编辑配置文件
vim application-prod.yml

# Python 脚本位置:
# military_operational_effectiveness_evaluation/generate/generate_all_data.py

./start.sh   # 首次会自动下载 JDK 17

[2] Update (只更新后端)
--------------
./stop.sh
unzip -o deploy.zip
./start.sh

[3] Update (只更新前端)
--------------
rm -rf dist
unzip -o deploy.zip "dist/*"
./stop.sh && ./start.sh

[4] Management
--------------
./start.sh    Start
./stop.sh     Stop
./status.sh   Status

[5] Logs
--------------
tail -f app.log

[6] Check Running
--------------
ps aux | grep app.jar | grep -v grep

[7] Cleanup (如果需要)
--------------
# 停止服务
./stop.sh

# 删除整个目录（完全清理）
cd /opt
rm -rf military-evaluation
'@
$deployText | Out-File -FilePath "$DeployDir\DEPLOY.txt" -Encoding ascii

Write-Host "  [OK] Done"

# ---------- 4. Create ZIP ----------
Write-Host "[4/4] Creating ZIP package..."
$ZipFile = Join-Path $ProjectRoot "deploy.zip"
if (Test-Path $ZipFile) { Remove-Item $ZipFile -Force }
Compress-Archive -Path "$DeployDir\*" -DestinationPath $ZipFile
Remove-Item -Path $DeployDir -Recurse -Force

Write-Host ""
Write-Host "=========================================="
Write-Host "  Build complete! deploy.zip"
Write-Host "=========================================="
