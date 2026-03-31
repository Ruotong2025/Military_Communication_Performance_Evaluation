## 手动启动

```bash
# 1. 编译项目
mvn clean install

# 2. 运行项目
mvn spring-boot:run
```

```bash
# 进入前端目录
cd frontend

# 安装依赖（首次运行）
npm install

# 启动前端
npm run dev
```

## 二、上传到服务器



```bash
.\scripts\windows\build-all.ps1

scp -P 1122 deploy.zip user4@172.0.249.12:/opt/military-evaluation/
!Hzsfdxlzl!

ssh user4@172.0.249.12 -p 1122
!Hzsfdxlzl!
cd /opt/military-evaluation
unzip -o deploy.zip
chmod +x *.sh
pkill -f app.jar

nohup ./java17/bin/java -Xms512m -Xmx1024m -jar app.jar --spring.profiles.active=prod > app.log 2>&1 &
```

netstat -ano | findstr :8080
taskkill /PID 22260 /F
