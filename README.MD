# 智能题库管理系统

一个接入 AI 分析的题库系统，提供完整的题目保存、题库分组、共享、批量导入和练习。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.4 / MyBatis / PageHelper / Maven |
| 数据库 | MySQL 8.0+ (utf8mb4) |
| 前端 | Vue 3 / Vite / Element Plus / ECharts / Pinia / Vue Router |
| 桌面端 | Electron |
| AI | 浏览器本地直连 OpenAI 兼容接口(默认 DeepSeek), 配置存于前端 |

## 项目结构

```
IntelligentQuestionBank/
├── db/init.sql                  # 建库建表 + 演示数据(一键导入)
├── db/upgrade-v2.sql            # v1 存量库升级脚本(v2 题库分组)
├── backend/                     # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/qbank/
│       ├── common/              # 统一响应/全局异常/常量
│       ├── config/              # Web配置/CORS/Jackson
│       ├── entity/              # 9 张表实体(bank/题目/共享等)
│       ├── dto/                 # 入参出参
│       ├── mapper/              # MyBatis 接口 (XML 在 resources/mapper)
│       ├── service/             # 业务服务(9 个)
│       ├── controller/          # REST 接口(9 个)
│       ├── interceptor/         # 登录鉴权拦截器
│       └── util/                # Token/密码
└── frontend/                    # Vue3 + Electron
    ├── electron/main.js         # 桌面壳
    └── src/
        ├── api/                 # axios 封装 + 全部接口
        ├── stores/              # Pinia 用户状态
        ├── router/              # 路由与登录守卫
        ├── utils/ai.js          # 前端本地 AI 客户端(v2)
        ├── components/          # 通用组件(批量导入向导等)
        └── views/               # 15 个页面
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
java -jar target/qbank-1.1.2.jar
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
可在**登录页底部「⚙️ 后端地址设置」**中选择"自定义远程后端"填入地址（保存在本机浏览器，
登录/注册及所有请求即发往该地址；远程后端需放行跨域 CORS）。

## 功能模块

- **题目管理**: 增删改查，支持单选/多选/填空/判断/简答 5 种题型、难度、知识点标签、分类、题库归属、关键词检索、分页
- **题库管理(v2)**: 题目分组到题库，题库增删改查与题目计数；删除题库时题目保留仅解除归属
- **批量导入(v2)**: 下载 Excel 模板或按固定格式准备 .xlsx/.csv，三步向导(上传→预览校验→导入)，
  错误行自动标红跳过；预览时可选用 AI 补全缺失的答案与解析(单次上限 50 行)
- **分类管理(v1.1)**: 二级树形分类，**仅管理员可增删改**；分类全局共享（所有用户共用，有子分类或题目时禁止删除）
- **用户管理**: 注册/登录/个人资料/修改密码；管理员可新增/禁用/重置密码/删除用户
- **收藏**: 一键收藏/取消，收藏夹分页浏览
- **共享(v1.1)**: 题目/题库共享给指定用户或公开共享；**权限两级**——只读/可编辑（指定用户可选，公开固定只读，可编辑写回共享者原记录，删除权仅共享者）；收件人可**订阅(实时跟随)/退订/重新订阅/拷贝**(拷贝为本人副本并记录来源)；管理员可查看全部共享
- **练习(v1.1)**: 顺序/随机/错题重做三种模式，可按分类/题库/题型/难度筛选；**快捷题目数量(全部/1/2/1/4/自定义)并显示总题数**；开始练习与练习记录同页；练习中不返回答案防作弊，
  交卷自动判分（单选/多选/判断精确匹配，填空多空用 ||| 分隔，简答宽松匹配）
- **错题本**: 答错自动入本并累计错误次数，支持标记掌握/恢复/移除、错题重练
- **统计(v1.1)**: 题目/分类/收藏/错题/练习总览，题型与难度分布、分类占比、近 14 天正确率趋势、易错分类 Top5；**管理员可查看全站或指定用户统计**
- **AI 分析(v2)**: 题目质量分析（考点/难度/表述建议/解题提示）与个人学情报告（薄弱点/学习建议/练习计划），
  由前端本地直连大模型完成，配置(API Key/模型/接口地址)保存在浏览器 localStorage，
  入口为左侧菜单「AI 设置」，支持测试连接与本地历史(最近 50 条)

## API 概览 (统一前缀 /api, 返回 {code, msg, data})

```HTTP
POST /api/user/register|login|logout|add      登录注册(登录返回token)
GET  /api/user/info|list                      个人信息/用户列表(管理员)
PUT  /api/user/update|status|reset-password   资料/状态/重置密码
GET  /api/category/tree|list                  分类树/平铺列表(全局)
POST PUT DELETE /api/category                 分类增删改(仅管理员)
GET  /api/question/list|{id}                  题目分页查询(管理员可传 userId 筛选)/详情
POST PUT /api/question, POST /question/delete 题目增改/批量删
GET  /api/question/import/template            批量导入模板下载(.xlsx)
POST /api/question/import/parse|save          批量导入解析预览/批量入库
GET  /api/bank/list|{id}                      题库列表(含订阅的共享题库, 管理员全量)/详情
POST PUT DELETE /api/bank, DELETE /api/bank/{id}  题库增删改
POST /api/favorite/{id}/toggle                收藏切换
POST /api/share, GET /share/sent|received     共享/发出收到列表; PUT /share/{id}/permission 改权限;
                                               POST /share/{id}/subscribe 订阅退订; POST /share/{id}/copy 拷贝
POST /api/practice/start|submit               开始练习(隐藏答案)/交卷判分
GET  /api/practice/count                      练习筛选下的总题数
GET  /api/practice/records|records/{id}       练习记录/详情
GET  /api/wrong/list, PUT /api/wrong/master/{id}  错题本/掌握切换
GET  /api/stats/overview|question-by-type|question-by-difficulty
GET  /api/stats/question-by-category|practice-trend|wrong-by-category (管理员可传 targetUserId)
```

> v2 起 AI 分析不再走后端接口，由前端本地直连大模型（见 utils/ai.js）。

鉴权方式: 登录后请求头携带 Authorization: Bearer {token}（前端已自动处理）。

## 常见问题

- **登录报“用户名或密码错误”**: 确认已导入 db/init.sql 且种子数据未被修改。
- **后端启动失败**: 检查 MySQL 是否运行、application.yml 中的账号密码是否正确。
- **AI 分析未生效**: 在前端「AI 设置」页填写 API Key 并保存（浏览器本地存储，不经过后端）；
  开发模式默认经 Vite 代理转发，Electron 生产模式直连。可在设置页点「测试连接」验证。
- **存量库从 v1 升级**: 执行 `mysql -uroot -p < db/upgrade-v2.sql`（新增题库表与题目归属，
  并移除已废弃的 ai_analysis 表）；新环境直接导入 init.sql 即可。
- **批量导入模板在哪**: 题目管理页「批量导入」向导第一步可下载 Excel 模板；CSV 需 UTF-8
  编码(自动兼容 GBK)，列顺序与模板一致。
- **npm install 时 electron 下载慢**: 可设置镜像 `export ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/` 后执行 npm rebuild electron。
