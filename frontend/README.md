# 军事通信效能评估系统 - 前端快速启动

## 安装依赖

```bash
cd frontend
npm install
```

## 启动开发服务器

```bash
npm run dev
```

访问：http://localhost:3000

## 构建生产版本

```bash
npm run build
```

构建产物在 `dist/` 目录

## 项目结构

```
frontend/
├── src/
│   ├── views/              # 页面
│   ├── components/         # 组件
│   ├── api/               # API接口
│   ├── styles/            # 样式
│   ├── router/            # 路由
│   ├── utils/             # 工具函数
│   ├── App.vue            # 根组件
│   └── main.js            # 入口文件
├── index.html
├── package.json
└── vite.config.js
```

## 技术栈

- Vue 3 + Composition API
- Element Plus UI组件库
- ECharts 图表库
- Vite 构建工具
- Axios HTTP客户端

## 海军蓝主题

主题文件：`src/styles/navy-theme.scss`

主色调：
- 深海军蓝 #001f3f
- 亮海军蓝 #0074D9
- 浅蓝 #7FDBFF
- 金色 #FFD700（强调色）
