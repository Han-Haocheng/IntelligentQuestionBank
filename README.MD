# 智能题库管理系统

一套集 **管题 — 练题 — 分析** 于一体的智能题库软件：老师轻松建题库、批量导题、共享协作，学生高效刷题、错题重练，
并借助 AI 大模型获得题目质量分析与个人学情报告。支持私有化部署、数据自主可控、开源免费，可打包为 Windows / macOS / Linux 桌面应用。

> 完整变更记录见 [CHANGELOG.md](CHANGELOG.md)

## ✨ 核心特性

- **题目全生命周期**：5 种题型（单选/多选/填空/判断/简答）录入、知识点标签、二级分类、题库分组、Excel/CSV 批量导入（错误行标红、可 AI 补全）
- **练学闭环**：顺序/随机/错题重做三种练习模式，交卷自动判分，错题自动进错题本并累计次数，支持标记掌握与错题重练
- **共享协作**：题目/题库共享（指定用户或公开），只读/可编辑两级权限，收件人可订阅实时跟随或拷贝为本人副本
- **数据洞察**：统计看板展示正确率趋势、题型/难度分布、易错分类 Top5；管理员可查看全站或指定用户统计
- **AI 分析**：题目质量分析（考点/难度/表述建议/解题提示）与个人学情报告（薄弱点/学习建议/练习计划），浏览器本地直连任意 OpenAI 兼容大模型
- **界面主题(v1.2)**：管理员维护多套前端样式（主色/侧栏/顶栏/圆角等），一键设为全局默认，用户可自行切换
- **私有化部署**：Spring Boot + MySQL + Vue3 全套开源，Electron 桌面端开箱即用

## 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [功能模块](#功能模块)
- [API 概览](#api-概览)
- [部署与配置](#部署与配置)
- [常见问题](#常见问题)
- [版本历史](#版本历史)
- [开源许可](#开源许可)

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.4 / MyBatis / PageHelper / Maven |
| 数据库 | MySQL 8.0+ (utf8mb4) |
| 前端 | Vue 3 / Vite / Element Plus / ECharts / Pinia / Vue Router |
| 桌面端 | Electron (electron-builder 三平台打包) |
| AI | 浏览器本地直连 OpenAI 兼容接口(默认 DeepSeek), 配置存于本机浏览器 |

## 项目结构

```
IntelligentQuestionBank/
├── db/init.sql                  # 建库建表 + 演示数据(一键导入, 幂等可重跑)
├── db/upgrade.sql               # 存量库升级脚本(v2-v6 合并, 一次性执行)
├── backend/                     # Spring Boot 后端
│   └── src/main/java/com/qbank/
│       ├── common/              # 统一响应/全局异常/常量
│       ├── config/              # Web 配置/CORS/Jackson
│       ├── entity/              # 11 个数据库实体(user/题目/共享/主题等)
│       ├── dto/                 # 入参出参
│       ├── mapper/              # MyBatis 接口 (XML 在 resources/mapper)
│       ├── service/             # 业务服务(11 个)
│       ├── controller/          # REST 接口(10 个)
│       ├── interceptor/         # 登录鉴权拦截器
│       └── util/                # JWT/密码/限流/判分
├── frontend/                    # Vue3 + Electron
│   ├── electron/main.js         # 桌面壳
│   └── src/
│       ├── api/                 # axios 封装 + 全部接口
│       ├── stores/              # Pinia 状态(用户/主题)
│       ├── router/              # 路由与登录守卫
│       ├── utils/               # AI 客户端/确认框/常量
│       ├── components/          # 通用组件(批量导入向导/主题选择器等)
│       └── views/               # 14 个页面(看板/题目/练习/管理/我的等)
└── .github/workflows/release.yml # 推送 v* 标签自动构建三平台产物并发布 Release
```

## 快速开始

### 1. 初始化数据库 (MySQL 8.0+)

一键导入会创建 `question_bank` 库、12 张表和演示数据：

```bash
mysql -uroot -p < db/init.sql
```

> - 脚本幂等可重复执行（数据部分只重置种子行，不影响用户自建数据）
> - 存量 v1 库请改用 `db/upgrade.sql` 一次性升级

默认账号（密码均为 `123456`）：

| 账号 | 角色 | 说明 |
| --- | --- | --- |
| `admin` | 管理员 | 用户/分类/主题管理，全站统计 |
| `demo` | 普通用户 | 常规练习用户 |

### 2. 启动后端 (端口 8080)

```bash
cd backend
mvn spring-boot:run          # 方式一: 开发运行
# 或
mvn -DskipTests package      # 方式二: 打包后运行
java -jar target/qbank-1.2.0.jar
```

数据库连接配置在 `backend/src/main/resources/application.yml`，默认账号 `qbank / qbank123`，可通过环境变量覆盖（见[部署与配置](#部署与配置)）。

### 3. 启动前端 (端口 5173)

```bash
cd frontend
npm install
npm run dev                  # 浏览器访问 http://localhost:5173
```

开发模式下 `/api` 请求由 Vite 代理到 `http://localhost:8080`。

### 4. Electron 桌面端

```bash
cd frontend
npm run build                # 先打包 dist
npm run electron:dev         # 开发模式(加载 dist)
npm run dist                 # 打包安装包(exe/dmg/AppImage/deb)
```

桌面端生产模式默认请求 `http://localhost:8080/api`；如后端部署在其他地址，可在**登录页底部「⚙️ 后端地址设置」**选择“自定义远程后端”填入地址
（保存在本机浏览器，登录/注册及所有请求即发往该地址；远程后端需放行跨域 CORS）。

## 功能模块

- **题目管理**: 增删改查，支持单选/多选/填空/判断/简答 5 种题型、难度、知识点标签、分类、题库归属、关键词检索、分页
- **题库管理(v2)**: 题目分组到题库，题库增删改查与题目计数；删除题库时题目保留仅解除归属
- **批量导入(v2)**: 下载 Excel 模板或按固定格式准备 .xlsx/.csv，三步向导(上传→预览校验→导入)，错误行自动标红跳过；
  预览时可选用 AI 补全缺失的答案与解析(单次上限 50 行)
- **分类管理(v1.1)**: 二级树形分类，**仅管理员可增删改**；分类全局共享（所有用户共用，有子分类或题目时禁止删除），支持拖拽排序/合并/影响面统计
- **用户管理**: 注册/登录/个人资料/修改密码；管理员可新增/禁用/重置密码/删除用户
- **界面主题(v1.2)**: 管理员在「管理 → 界面主题」维护**多套前端样式**（名称/标识/主色/侧栏/顶栏/页面背景/登录页渐变/圆角，内置默认蓝/暗夜深蓝/清新绿模板），
  支持新增/编辑/启停/删除，设为默认即全局生效；停用或删除默认主题时默认自动移交给其他启用主题；
  所有用户可在右上角「切换界面主题」选择启用的样式（个人选择保存在本机浏览器，未选择时跟随管理员默认）
- **收藏**: 一键收藏/取消，收藏夹分页浏览
- **共享(v1.1)**: 题目/题库共享给指定用户或公开共享；**权限两级**——只读/可编辑（指定用户可选，公开固定只读，可编辑写回共享者原记录，删除权仅共享者）；
  收件人可**订阅(实时跟随)/退订/重新订阅/拷贝**(拷贝为本人副本并记录来源)；管理员可查看全部共享
- **练习(v1.1)**: 顺序/随机/错题重做三种模式，可按分类/题库/题型/难度筛选；快捷题目数量并显示总题数；开始练习与练习记录同页；
  练习中不返回答案防作弊，交卷自动判分（单选/多选/判断精确匹配，填空多空用 `|||` 分隔，简答宽松匹配）
- **错题本**: 答错自动入本并累计错误次数，支持标记掌握/恢复/移除、错题重练
- **统计(v1.1)**: 题目/分类/收藏/错题/练习总览，题型与难度分布、分类占比、近 14 天正确率趋势、易错分类 Top5；管理员可查看全站或指定用户统计
- **AI 分析(v2)**: 题目质量分析（考点/难度/表述建议/解题提示）与个人学情报告（薄弱点/学习建议/练习计划），由前端本地直连大模型完成；
  配置（API Key/模型/接口地址）保存在本机浏览器，入口为「我的 → AI 设置」，支持常用服务商下拉、模型自动拉取、测试连接与本地历史(最近 50 条)

## API 概览 (统一前缀 /api, 返回 {code, msg, data})

```HTTP
POST /api/user/register|login|logout|add      登录注册(登录返回token)
GET  /api/user/info|list                      个人信息/用户列表(管理员)
PUT  /api/user/update|status|reset-password   资料/状态/重置密码
GET  /api/theme/active                         当前全局生效主题(无需登录)
GET  /api/theme/enabled                        启用的主题列表(登录用户)
GET  /api/theme/list                           全部主题(管理员)
POST PUT /api/theme, PUT /api/theme/{id}/status|default, DELETE /api/theme/{id}
                                                 主题增改/启停/设默认/删除(仅管理员)
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

> v2 起 AI 分析不再走后端接口，由前端本地直连大模型（见 `frontend/src/utils/ai.js`）。

**鉴权方式**: 登录后请求头携带 `Authorization: Bearer {token}`（前端已自动处理）；未登录/令牌失效返回 401，无权限返回 500(业务提示)。
令牌有效期 7 天，且由后端内存白名单校验——**后端重启后所有已签发令牌失效，用户需重新登录**；登出立即生效。

## 部署与配置

### 环境变量（覆盖 application.yml 默认值）

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/question_bank?...` | 数据库连接串 |
| `DB_USERNAME` | `qbank` | 数据库账号 |
| `DB_PASSWORD` | `qbank123` | 数据库密码 |
| `QBANK_JWT_SECRET` | 开发用固定串 | JWT 签名密钥，**生产必须改为高强度随机串** |
| `QBANK_CORS_ALLOWED_ORIGINS` | `*` | 允许的跨域来源(逗号分隔)，公网部署建议收紧为前端域名 |

### 生产部署建议

- 通过环境变量注入数据库口令与 JWT 密钥，勿使用默认值
- 前端 `npm run build` 后由 Nginx 等托管 `frontend/dist`，并反向代理 `/api` 到后端
- CORS 收紧为实际前端域名：`QBANK_CORS_ALLOWED_ORIGINS=https://qbank.example.com`
- HTTPS 建议由反向代理终结（后端本身 HTTP 即可）
- 登录会话白名单存于后端内存：单实例部署无影响；多实例需改为共享存储（如 Redis），否则实例间令牌互不认
- 桌面端连接远程后端时，登录页「后端地址设置」填入公网地址，并在服务端放行对应来源的 CORS

## 常见问题

- **登录报“用户名或密码错误”**: 确认已导入 `db/init.sql` 且种子数据未被修改；或密码少于 6 位时注册被拦截。
- **后端启动失败**: 检查 MySQL 是否运行、`DB_URL/DB_USERNAME/DB_PASSWORD` 是否正确（默认 `qbank/qbank123`）。
- **为什么后端重启后所有人都要重新登录?**: 令牌由后端内存白名单校验，重启即清空（v1.2 安全增强）；重新登录即可。
- **AI 分析未生效**: 在「我的 → AI 设置」填写 API Key 并保存（本机浏览器存储，不经过后端）；开发模式默认经 Vite 代理转发，
  Electron 生产模式直连；可用「测试连接」验证，也可下拉选择服务商或自定义 OpenAI 兼容地址（本地 Ollama 可留空 Key）。
- **存量库从 v1 升级**: 执行 `mysql -uroot -p < db/upgrade.sql`（v2-v6 合并: 题库分组/练习快照/索引唯一键/权限订阅/前端样式主题，并移除已废弃的 ai_analysis 表）；
  新环境直接导入 `init.sql` 即可。
- **批量导入模板在哪**: 题目管理页「批量导入」向导第一步可下载 Excel 模板；CSV 需 UTF-8 编码(自动兼容 GBK)，列顺序与模板一致。
- **npm install 时 electron 下载慢**: 设置镜像 `export ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/` 后重新安装。

## 版本历史

| 版本 | 亮点 |
| --- | --- |
| v1.2.0 | 界面主题系统、JWT 会话白名单(重启即失效)、登录安全增强 |
| v1.1.2 | 批量导入三种模板、交互健壮性、按需引入体积优化 |
| v1.1.1 | 「我的」页整合、登录页远程后端地址、分类管理增强 |
| v1.1.0 | 共享权限/订阅/拷贝、分类仅管理员、统计按用户 |
| v1.0.2 | BCrypt 密码、登录限流、越权修复、性能优化 |
| v1.0.1 | 首版：题目/题库/导入/AI 分析/Electron 打包 |

完整变更记录见 [CHANGELOG.md](CHANGELOG.md)。

## 开源许可

本项目基于 [MIT License](LICENSE) 开源，可自由使用、修改与分发（含商用）。