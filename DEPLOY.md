# 部署文档

## 一、本地打包

**Windows:**
```powershell
.\scripts\windows\build-all.ps1
```

**Linux/macOS:**
```bash
./scripts/build-all.sh
```

生成 `deploy.zip` 文件。

---


```

---

## 三、首次部署

SSH 登录服务器后：

```bash
cd /opt/military-evaluation
unzip -o deploy.zip
chmod +x *.sh

# 修改数据库配置
vim application-prod.yml

# 启动服务
./start.sh
```

---

## 四、后续更新

```bash
cd /opt/military-evaluation
./stop.sh
# 上传新的 JAR 覆盖 app.jar，或解压新 ZIP
./start.sh
```

---

## 五、服务管理

```bash
./start.sh     启动
./stop.sh      停止
./status.sh    状态
tail -f app.log   日志
```

---

## 六、访问地址

- API: http://172.0.249.12:8080/api
- Swagger: http://172.0.249.12:8080/api/swagger-ui.html
