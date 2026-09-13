# qbank-web 全量 MD3 迁移方案（issue #12 / v1.4.1 milestone）

> 目标版本：v1.4.1 完全解决（用户排期承诺，issue #12 已关联 v1.4.1 milestone）
> 方案评审方式：协调者汇总 → 用户/另一侧协调者确认后按阶段实施

## 1. 目标与范围

- 把 qbank-web（SpringBoot + Thymeleaf 服务端渲染，14 个页面）的视觉从 **Bootstrap 默认皮肤** 统一到 **Material Design 3（MD3）令牌体系**，与前端 Vue 版（MD3 紫 #6750a4 基线）视觉一致
- **不做**：布局机制重写（保留 Bootstrap 5 栅格/CSS 机制）、JS 交互重写（collapse/modal 沿用 bootstrap.bundle，仅样式覆盖）、暗色主题（qbank-web 无主题切换，固定 MD3 浅色基线）
- **保留**：TASK-002 已本地化的 vendor 资产；`qk-*` 自定义布局类（侧栏/顶栏/闪存）作为 MD3 化核心对象

## 2. 现状依赖面（2026-09-13 盘点）

| 项 | 现状 |
|---|---|
| 模板 | 14 个 html（login/register/error/dashboard/questions/question_form/banks/categories/practice/practice_do/practice_result/practice_records/wrongbook + fragments/layout），共约 1602 行 |
| 布局 | 自定义 `qk-sidebar/qk-brand/qk-nav/qk-nav-group/qk-topbar/qk-page-title/qk-flash`（可从 Bootstrap 类剥离） |
| Bootstrap 组件类（用到的） | `btn/btn-sm/btn-primary/btn-outline-*` 39 处、`form-control/form-label`、`table/table-hover/pagination(-sm)`、`alert(-dismissible)`、`badge/text-bg-*`、`shadow`、栅格 `col-sm-6/col-md-2`、工具类 `d-flex/text-*/bg-*/gap-*` |
| Bootstrap JS 组件 | collapse ×7（banks/categories/wrongbook）、modal ×1 |
| 自有 CSS | `static/css/app.css`（165 行，qk-* 主题层） |
| 依赖 | 三方静态资源已 vendor 化（bootstrap 5.3.3 / icons 1.11.3，TASK-002） |

## 3. MD3 落地方案（qbank-web 版）

### 3.1 令牌层（`app.css` 顶部新增 `:root`）
与前端一致的基础令牌（深/浅固定一套 = MD3 紫 utilities 基线）：
- 色彩：`--md-primary:#6750a4`、`--md-on-primary:#fff`、`--md-primary-container:#eaddff`、`--md-on-primary-container:#21005d`、`--md-secondary-container:#e8def8`、`--md-on-secondary-container:#1d192b`、`--md-surface:#fef7ff`、`--md-surface-container*（#f3edf7..#e6e0e9 阶梯）`、`--md-on-surface:#1d1b20`、`--md-error:#b3261e`、`--md-success:#146c2e`、`--md-warning:#7a5900`
- 形状：`--md-shape-sm:8px`、`--md-shape-md:12px`、`--md-shape-lg:16px`、`--md-shape-xl:24px`
- （数值直接复用 `frontend/src/styles.css` 的 MD3 静态令牌，保证两边视觉一致）

### 3.2 组件覆盖层（`app.css` 内，Bootstrap 类名 + MD3 值）
用令牌重写被用到的 Bootstrap 组件（不引入新 DOM 结构）：
- `.btn`（filled/text/outlined 语义 → primary/secondary/light 映射 MD3 按钮态：hover/focus state-layer 色）
- `.form-control/.form-select/.form-check-input`（surface 底、on-surface 字、focus 用 `--md-primary` 环与 2px outline）
- `.table/.table-hover/.pagination`（surface-container 表头、on-surface 文字、分页选中 = primary-container）
- `.badge/.text-bg-primary/success/info/secondary`（MD3 容器色语义映射）
- `.alert`（info→secondary-container、danger→error-container、success→success-container，含 dismiss）
- `.card`（若模板用 Bootstrap `.card`：surface-container-lowest + 阴影按 elevation）
- `qk-sidebar/qk-nav/qk-topbar/qk-flash/qk-page-title`（nav 用 surface/on-surface 与选中态 secondary-container + on-secondary-container；品牌区 primary-container）
- 全局焦点：`:focus-visible { outline:2px solid var(--md-primary); }`（与前端 #16 修复一致）

### 3.3 保留层
- Bootstrap 栅格/工具类（布局不重写）；`bootstrap.bundle.min.js`（collapse/modal 行为不变）
- 图标：bootstrap-icons 保留（MD3 无等效免费图标集，视觉上以 size/tint 对齐）

## 4. 分阶段实施计划（v1.4.1 内按序交付，每阶段独立提交+验证）

| 阶段 | 内容 | 改动面 | 验证 |
|---|---|---|---|
| P1 令牌+基础表单/按钮 | `:root` 令牌层；`.btn/.form-control` 系覆盖 | app.css | mvn package + 登录/注册/表单页目测 |
| P2 布局与导航 | `qk-*` 侧栏/顶栏/闪存 MD3 化；全局 focus 环 | app.css（+layout.html 微调） | 各主页面目测 + 激活态对比 |
| P3 数据与交互组件 | `.table/.pagination/.badge/.alert/.modal` 覆盖；collapse 面板 MD3 化 | app.css（+banks/categories/wrongbook 微调） | 列表/分页/折叠/删除确认目测 |
| P4 走查收口 | 间距/字体/图标尺寸/error/login/register 细节；对比前端视觉核对 | app.css/模板微调 | 全 14 页走查 + mvn 门禁 |

### 每阶段门禁
- `mvn -B -DskipTests clean package --file qbank-web/pom.xml`（工作树 target 受 IDE 干扰时用 worktree 验证，以干净环境为准）
- `grep -c "vendor/boot" layout.html` 不回归（vendor 引用不被破坏）
- 阶段变更不得引入新依赖、不改版本声明、不写 CHANGELOG

## 5. 风险与验收

- **风险**：模板与 app.css 耦合（class 改动可能影响 JS hooks：data-bs-toggle/dismiss 关联类名保持不动）；前后端视觉一致性需人工目测对比
- **验收标准**：14 页在默认浏览器无 Bootstrap 默认感（按钮/表单/表格/导航均为 MD3 形态）；键盘聚焦可见；mvn 门禁绿；无新增外部依赖
- **里程碑**：v1.4.1（正式版流程同 v1.4.0：dev 累积 → beta → 手动 formal）

## 6. 关联归档
- issue #12（打开，milestone v1.4.1）
- 处理记录：无人值守轮 9 决策（#12 不做半吊子迁移，列 backlog）；本方案为 v1.4.1 启动蓝本