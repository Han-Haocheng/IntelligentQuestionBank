# 智能题库管理系统

一个接入 AI 分析的题库系统，提供完整的题目保存、共享、分析和练习。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.4 / MyBatis / PageHelper / Maven |
| 数据库 | MySQL 8.0+ (utf8mb4) |
| 前端 | Vue 3 / Vite / Element Plus / ECharts / Pinia / Vue Router |
| 桌面端 | Electron |
| AI | OpenAI 兼容接口(默认 DeepSeek), 未配置 Key 自动降级本地规则分析 |

## 项目结构

```
IntelligentQuestionBank/
├── db/init.sql                  # 建库建表 + 演示数据(一键导入)
├── backend/                     # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/qbank/
│       ├── common/              # 统一响应/全局异常/常量
│       ├── config/              # Web配置/CORS/Jackson
│       ├── entity/              # 9 张表实体
│       ├── dto/                 # 入参出参
│       ├── mapper/              # MyBatis 接口 (XML 在 resources/mapper)
│       ├── service/             # 业务服务(9 个)
│       ├── controller/          # REST 接口(9 个)
│       ├── interceptor/         # 登录鉴权拦截器
│       └── util/                # Token/密码/AI客户端
└── frontend/                    # Vue3 + Electron
    ├── electron/main.js         # 桌面壳
    └── src/
        ├── api/                 # axios 封装 + 全部接口
        ├── stores/              # Pinia 用户状态
        ├── router/              # 路由与登录守卫
        └── views/               # 13 个页面
```

## 快速开始

### 1. 初始化数据库

要求 MySQL 8.0+。导入脚本会自动创建 question_bank 库、9 张表和演示数据:

```bash
mysql -uroot -p < db/init.sql
```

默认账号(密码均为 123456):

- admin / 123456 —— 管理员
- demo / 123456 —— 普通用户

后端数据库连接配置在 backend/src/main/resources/application.yml (默认 root/123456),
若本机 root 密码不同请同步修改。

### 2. 启动后端 (端口 8080)

```bash
cd backend
mvn spring-boot:run          # 方式一: 开发运行
# 或
mvn -DskipTests package      # 方式二: 打包后运行
java -jar target/qbank-1.0.0.jar
```

### 3. 启动前端 (端口 5173)

```bash
cd frontend
npm install
npm run dev                  # 浏览器访问 http://localhost:5173
```

开发模式下 /api 请求由 Vite 代理到 `http://localhost:8080`。

### 4. Electron 桌面端

```bash
cd frontend
npm run build                # 先打包 dist
npm run electron:dev         # 开发模式
npm run electron:build       # 生产模式(加载 dist)
```

桌面端生产模式默认请求 `http://localhost:8080/api`；如后端地址不同，
可在应用内控制台执行 `localStorage.setItem("qbank_api_base", "http://主机:端口/api")` 后刷新。

## 功能模块

- **题目管理**: 增删改查，支持单选/多选/填空/判断/简答 5 种题型、难度、知识点标签、分类、关键词检索、分页
- **分类管理**: 二级树形分类，增删改查（有子分类或题目时禁止删除）
- **用户管理**: 注册/登录/个人资料/修改密码；管理员可新增/禁用/重置密码/删除用户
- **收藏**: 一键收藏/取消，收藏夹分页浏览
- **共享**: 共享给指定用户或公开共享，发出/收到列表，可取消共享
- **练习**: 顺序/随机/错题重做三种模式，可按分类/题型/难度筛选；练习中不返回答案防作弊，
  交卷自动判分（单选/多选/判断精确匹配，填空多空用 ||| 分隔，简答宽松匹配）
- **错题本**: 答错自动入本并累计错误次数，支持标记掌握/恢复/移除、错题重练
- **统计**: 题目/分类/收藏/错题/练习总览，题型与难度分布、分类占比、近 14 天正确率趋势、易错分类 Top5
- **AI 分析**: 题目质量分析（考点/难度/表述建议/解题提示）与个人学情报告（薄弱点/学习建议/练习计划），
  分析结果落库可查历史；配置见 backend/src/main/resources/AI-CONFIG.md

## API 概览 (统一前缀 /api, 返回 {code, msg, data})

```
POST /api/user/register|login|logout|add      登录注册(登录返回token)
GET  /api/user/info|list                      个人信息/用户列表(管理员)
PUT  /api/user/update|status|reset-password   资料/状态/重置密码
GET  /api/category/tree|list                  分类树/平铺列表
POST PUT DELETE /api/category                 分类增删改
GET  /api/question/list|{id}                  题目分页查询/详情
POST PUT /api/question, POST /question/delete 题目增改/批量删
POST /api/favorite/{id}/toggle                收藏切换
POST /api/share, GET /share/sent|received     共享与列表
POST /api/practice/start|submit               开始练习(隐藏答案)/交卷判分
GET  /api/practice/records|records/{id}       练习记录/详情
GET  /api/wrong/list, PUT /api/wrong/master/{id}  错题本/掌握切换
GET  /api/stats/overview|question-by-type|question-by-difficulty
GET  /api/stats/question-by-category|practice-trend|wrong-by-category
POST /api/ai/analyze/question/{id}|report     AI题目分析/学情报告
GET  /api/ai/history                          AI分析历史
```

鉴权方式: 登录后请求头携带 Authorization: Bearer {token}（前端已自动处理）。

## 常见问题

- **登录报“用户名或密码错误”**: 确认已导入 db/init.sql 且种子数据未被修改。
- **后端启动失败**: 检查 MySQL 是否运行、application.yml 中的账号密码是否正确。
- **AI 分析显示“本地规则分析”**: 未配置 api-key，属正常降级；配置后重启后端即可（见 AI-CONFIG.md）。
- **npm install 时 electron 下载慢**: 可设置镜像 `export ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/` 后执行 npm rebuild electron。
