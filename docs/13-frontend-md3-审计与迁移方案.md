# 前端（frontend/）MD3 差距审计与 L2 迁移方案

> 目标版本：v1.4.1 周期（与 issue #12 / docs/12 的 qbank-web MD3 迁移同周期）
> 立项依据：用户 2026-09-17 判断「前后端分离版（frontend/ + backend/）的前端未完整使用 MD3 风格」——本文为该判断的**核查结论**与**实施蓝本**
> 评审方式：协调者汇总 → 用户确认后按 P1-P5 实施；实施期间文档随进展回填
> 审计基线：dev `19e834d`（2026-09-17，工作区干净）

## 1. 结论与合规层级定义

**结论一（事实层，判断成立）**：`frontend/` 当前落的是「**MD3 令牌层 + 部分组件皮肤**」，不是完整 MD3。差距可枚举、有 EP 源码级依据（见第 3 节）。

**结论二（规范层，必须纠正）**：在 Element Plus 之上**做不到"完整 MD3"**。MD3 的完整实现要求组件库本身是 MD3 实现，而本项目已深度依赖 EP（52 个在用组件、53 处 EP 图标）。因此"完整"必须先定层级，否则无法验收：

| 层级 | 含义 | 可达性 | 成本 |
|---|---|---|---|
| **L1 令牌级** | 色/形/字/影/动效全部令牌化并映射到组件库 | ✅ 可完全达成 | 低（纯 CSS + theme.js） |
| **L2 组件皮肤级** | 全部在用组件覆盖到 MD3 形态（本方案目标） | ⚠️ 约 90%，少数项只能近似（见第 4 节） | 中（纯 CSS，不换库） |
| **L3 组件库级** | 换 MD3 组件库（Material Web / Vuetify3 MD3）+ 换图标集 | 需改依赖 + 重写 14 视图 | 高，**本方案不做** |

**本次范围**：L2 收口。不改依赖、不改版本声明、不写 CHANGELOG、不动后端 API。

## 2. 现状盘点（可复核）

| 项 | 现状 | 复核方式 |
|---|---|---|
| 令牌层 | `frontend/src/styles.css` 736 行；`:root` 内 **53 个 `--md-*`** 令牌（色/形/影/字体族） | `grep -o -- '--md-[a-z0-9-]*' frontend/src/styles.css \| sort -u` |
| 组件皮肤 | **54 个 `.el-*` 选择器**覆盖（约 20 个组件形态化） | `grep -oE '\.el-[a-z0-9-]+' frontend/src/styles.css \| sort -u` |
| 视图内样式 | 13 个 `.vue` 的 scoped 样式合计 739 行 | 逐个 `<style>` 块统计 |
| EP 组件在用 | **52 个**（`el-button` 89 / `el-form-item` 74 / `el-tag` 32 / `el-tooltip` 28 / `el-select` 20 / `el-color-picker` 10 …） | `grep -rho '<el-[a-z-]*' frontend/src \| sort \| uniq -c` |
| EP 变量重映射 | 仅 **46 个 `--el-*`**；未映射引用 **474 处 / 去重 273 个变量** | 见第 3 节方法 |
| 动态主题 | `stores/theme.js` 写 `--md-*` 与 `--el-color-primary` 系（light-3/5/7/8/9、dark-2）；`--el-text-color-*`/`--el-bg-*`/`--el-border-color*` **仅暗色主题写入** | `sed -n '90,170p' frontend/src/stores/theme.js` |
| 布局 | 导航抽屉（surface + secondary-container 选中胶囊）、顶栏 elevation-0 + 1px 分隔线、登录页渐变、40px 圆形图标按钮 | `frontend/src/views/LayoutView.vue` |
| 键盘可访问性 | 全局 `:focus-visible` 焦点环（issue #16 已交付） | `styles.css` 第 1-7 行 |

**差距发现方法**（可重跑）：提取在用组件 → 读 `frontend/node_modules/element-plus/theme-chalk/el-<组件>.css` 消费的 `var(--el-*)` 集合 → 与项目重映射集合求差 → 差集即"落到 EP 默认皮肤"的变量。

## 3. 差距清单

| # | 差距 | EP 源码依据（`theme-chalk`） | 影响面 | 阶段 |
|---|---|---|---|---|
| **G1** | EP **语义色整套未映射** → 泄露 EP 品牌色 | `base.css`：`--el-color-success:#67c23a`/`warning:#e6a23c`/`danger,error:#f56c6c`/`info:#909399`；`el-alert.css`：`.el-alert--success.is-light{color:var(--el-color-success)}` | `el-alert`(7)、`el-progress`(2)、`el-result`、`el-steps`/`el-step`、`el-rate`、`el-link`；**同一控件内 MD3 容器色 + EP 前景色并存** | P1 |
| **G2** | **表单标签/必填星号/校验文案仍是 EP 色** | `el-form-item.css`：`.el-form-item__label{color:var(--el-text-color-regular /*#606266*/)}`、`.el-form-item__error{color:var(--el-color-danger)}`、必填星号同用 `--el-color-danger` | `el-form-item` **74 处**= 全站表单 | P1 |
| **G3** | **`el-select` 下拉面板完全未覆盖** | `el-select.css`：`.el-select__popper.el-popper{background:var(--el-bg-color-overlay);border:1px solid var(--el-border-color-light);box-shadow:var(--el-box-shadow-light)}`、`.el-select-dropdown__item.is-hovering{background-color:var(--el-fill-color-light /*#f5f7fa*/)}`、`.is-selected{font-weight:700}` | `el-select` **20 处 / 7 个视图**（题目管理筛选栏、主题管理页等）；`styles.css` 覆盖的是 `.el-dropdown-menu`，**是另一个组件** | P2 |
| **G4** | Loading 遮罩非 MD3 scrim | `el-loading.css`：`.el-loading-mask{background-color:var(--el-mask-color)}`=`#ffffffe6`；MD3 scrim = `rgba(0,0,0,.4)` | `v-loading` **8 个视图** | P1 |
| **G5** | **无 MD3 字阶（type scale）** | 仅有 `--md-font` 字体族；无 15 个 type role；字号散落 12/13/14/16/22/24/28px + EP `--el-font-size-base:14px`/`line-height:24px`（MD3 body-medium = 14/20） | 全站排版 | P3 |
| **G6** | 动效未 MD3 化 | `base.css`：`--el-transition-duration:.3s`、`--el-transition-function-ease-in-out-bezier:cubic-bezier(.645,.045,.355,1)` 均未映射；MD3 要求 standard/emphasized 缓动 + 50-600ms 时长档 | 全站交互反馈；现有 14 处 ad-hoc `0.2s/0.25s ease` | P3 |
| **G7** | 状态层只覆盖 primary、无 ripple | 有 `--md-primary-hover/focus`、`--md-primary-state-8/10`；缺 on-surface 中性 8%/12%、pressed 16%；无 ripple 实现 | 按钮/菜单项/列表项/图标按钮 | P3 |
| **G8** | 组件几何未 MD3 化（只改色） | `el-switch.css`：默认 `min-width:40px;height:20px`、滑块 16px（MD3 = 52×32、滑块 24px 带图标）；`el-checkbox/radio` 仅改描边色；MD3 outlined text field 的 floating label/supporting text 在 EP 无对应 DOM | 设置类页面、登录页 | P3 |
| **G9** | 未覆盖的可见组件 | `el-color-picker`(10，**主题管理页**)、`el-steps/step`(导入对话框)、`el-progress`(2)、`el-rate`、`el-collapse`(登录页)、`el-upload/upload-dragger`(导入对话框)、`el-avatar`、`el-result`、`el-input-number`、`el-popper.is-light`(28 处 tooltip 的浅色态)、`el-message-box` 内部 | 逐项可目测 | P2 |
| **G10** | 高度层级只有 3 级 | `--md-elev-1/2/3`，缺 4/5；且 MD3 惯例以 surface 容器色替代卡片阴影，当前卡片为「白底 + elev-1」 | 视觉调性 | P4 |
| **G11** | 图标集非 Material Symbols | `@element-plus/icons-vue`，53 处使用 | 图标字形（L3 边界内只能保留） | — |
| **G12** | 主题系统与 MD3 规范冲突 | 管理端可配任意色板（10 字段），container 色用**线性 mix** 派生（`theme.js` 自注"线性混色无法还原"）；无 MD3 官方暗色 scheme（surface `#141218` / primary `#D0BCFF`） | 非基线主题必然偏离 MD3 | P4（记录+约束，不改功能） |

## 4. L2 边界内"只能近似"的项（不承诺 100%）

1. **Text field 的 floating label / supporting text**：EP 输入框无此 DOM 结构，只能用 CSS 模拟（label 位置/hint 区域），语义与 MD3 不完全一致。
2. **State layer 与 ripple**：MD3 规范用 `state-layer` 伪元素 + ripple 动画；本项目用**预算混合色**（`mix(primary,#fff,0.08)`）近似，且只对 primary 生成。P3 可补中性层，ripple 建议不做（收益低、与 EP 焦点环重复）。
3. **Switch 几何**：可在 CSS 内改到 52×32/24px 滑块，但选中态内嵌图标需 DOM 改造，**不做**。
4. **Material Symbols 图标**：需新增依赖（代理无权决策），**本期不做**；以 size/tint 对齐视觉权重。
5. **HCT 色调板派生**：MD3 要求由 HCT 色调板生成角色色（tone 40/90/100…），前端仅线性混合。**基线紫使用官方值**（已实现）；非基线主题的偏离在 P4 用"最低对比度约束"缓解，不引入色彩库。

## 5. 分阶段实施计划（每阶段独立提交 + 门禁）

| 阶段 | 内容 | 改动面 | 门禁/验收 |
|---|---|---|---|
| **P1 令牌收口** | 映射 `--el-color-{success,warning,danger,error,info}` 及 light-3/5/7/8/9 → MD3 语义色系；浅色主题也写 `--el-text-color-*` → MD3 中性角色；`--el-mask-color` → scrim；`--el-transition-*` → MD3 时长/缓动 | `frontend/src/styles.css`（:root 新增）、`frontend/src/stores/theme.js`（动态主题下重算语义派生色） | `npm ci && npm run build`；登录页/表单页/提醒组件目测（G1/G2/G4/G6） |
| **P2 未覆盖组件皮肤** | select 下拉面板与 option、浅色 popper、loading、progress/result/steps/rate/link/collapse/upload/avatar/input-number/color-picker/message-box 内部 | `frontend/src/styles.css`（追加 3.x 小节） | 构建 + 7 个含 select 的视图展开核对（G3/G9） |
| **P3 字阶与状态层** | MD3 15 个 type role 令牌 + 应用到标题/正文/标签/表单；补 on-surface 中性状态层；switch/checkbox/radio 几何对齐 | `styles.css`、必要时各视图 scoped 样式 | 构建 + 全站字号抽查（G5/G7/G8） |
| **P4 高度/主题约束** | elev-4/5 补全与卡片策略（容器色 vs 阴影）；主题派生加最低对比度约束；记录 L3 边界（图标集/HCT） | `styles.css`、`stores/theme.js` | 构建 + 暗色主题目测（G10/G12） |
| **P5 走查收口** | 14 视图逐一核对 + 截图核对 + 与 qbank-web 视觉对齐复核 | 全 `frontend/src/**` | 构建 + 人工目测 + `check_md_tokens.sh` |

**跨阶段硬约束**（每个阶段都适用）：
- 不改依赖（`frontend/package.json` 禁改）、不改版本声明 5 处、不写 CHANGELOG/README 版本历史。
- 不破坏 EP 既有行为钩子（`el-` 类名与 `data-*` 保持可解析）；不引入 `!important` 之外的激进覆盖（优先靠 `--el-*` 变量层）。
- 每阶段改完必须同步 qbank-web 令牌（见第 9 节），或明确记录"仅前端新增、qbank-web 无需同步"。

## 6. 验收标准

**静态（必过）**
1. `(cd frontend && npm ci && npm run build)` 通过。
2. `bash .agents/scripts/check_md_tokens.sh` 通过（令牌一致性子集）。
3. `grep -c 'var(--el-color-d\|var(--el-color-succ\|var(--el-color-warn\|var(--el-color-info' frontend/src/styles.css` 无新增裸用（语义色必须走 MD3 令牌）。

**视觉（新增手段）**
4. 截图核对：`firefox --headless --screenshot`（本机 `/usr/bin/firefox`，**零新增依赖**）按"环境变量门控探针"模式做成脚本（`QBANK_MD3_SHOTS=1` 才执行、默认零影响、超时判 FAIL），对登录页 + 可离线渲染的页面截图，人工核对后归档到 `.agents/heartbeat/`（不入 git）。
5. 人工目测清单（14 视图）：按钮四态（normal/hover/focus/pressed）、表单（label/必填星号/校验文案三色一致）、select 展开态、loading 遮罩、表格空态、提醒四变体、暗色主题下全部上述项。

**不达标判定**：出现下列任一即阶段不予验收——同一控件内混用 MD3 与 EP 色系；浅色主题下表单 label/错误文案取 EP 默认值；select 下拉肉眼可见 EP 冷灰 `#f5f7fa` hover/EP 阴影。

## 7. 影响域与接口边界（任务书登记用）

- **文件域**：`frontend/src/styles.css`、`frontend/src/stores/theme.js`、`frontend/src/views/**`、`frontend/src/components/**`（**不含** `frontend/package.json` 与 `frontend/package-lock.json`）。
- **影响域**：
  - `stores/theme.js` ↔ `styles.css` 令牌契约（动态主题覆盖静态基线；暗色分支先清后写）。
  - 主题系统 ↔ EP 变量层（管理员可配任意主色，语义色须随主色可推导，不得退回 EP 默认）。
  - **qbank-web 令牌同步**：`qbank-web/src/main/resources/static/css/app.css` 与前端共有令牌必须同值（第 9 节脚本守护）。
  - 不改后端 API 契约（401/403、JWT 白名单）与 `db/`。
- **回退点**：每阶段提交后 `git checkout -- <file>` 可回到上前一阶段；令牌层改动集中在 `:root`，回退粒度清晰。

## 8. 风险与回退

| 风险 | 处置 |
|---|---|
| EP 升级后内部类名/变量变化 → 覆盖失效 | 优先用 `--el-*` 变量层（公开契约）而非深层类名；深层覆盖集中标注 `/* EP 内部结构 */` 便于升级排查 |
| 暗色主题下语义色对比度不足 | `theme.js` 暗色分支同步写入语义色（不只 primary），P1 一并处理 |
| 管理员自定义主色导致 container 色不达 MD3 观感 | 基线紫用官方值（已实现）；其余走 P4 的最低对比度约束；在主题管理页加说明文案（不改接口） |
| 与 qbank-web 再次分叉 | 第 9 节脚本 + 每阶段同步（docs/12 的验收目标即"两边视觉一致"） |
| 视觉验收依赖人工 | 第 6 节截图探针 + 目测清单；本环境无浏览器自动化依赖，探针不通过则阶段不验收 |

## 9. 令牌单一事实源与一致性守护（D1/D2，本次已修）

**问题**：令牌在 `frontend/src/styles.css` 与 `qbank-web/.../app.css` 两处硬拷贝，无任何校验；2026-09-17 核查发现共有 28 个令牌中 **5 个值不一致**（直接违反 docs/12 §1"与前端视觉一致"）。

| 令牌 | 前端（权威） | qbank-web 修正前 | 问题 |
|---|---|---|---|
| `--md-shape-xl` | `28px`（MD3 规范） | `24px` | 规范值错（错源：docs/12 §3.1）+ 两边不一致 + 该令牌在 qbank-web **无消费（死令牌）** |
| `--md-primary-hover` | `#735eab`（提亮 = 内容色 8% 状态层） | `#5d4f8a`（加深） | **hover 方向相反**（MD3 filled 按钮 hover 应变亮） |
| `--md-success-hover` | `#26803c` | `#115c27` | 同上 |
| `--md-success-container` | `#c4eed0` | `#b8f1c1` | 值不一致 |
| `--md-warning-container` | `#ffdf99` | `#ffdf9d` | 值不一致 |

**处置（本次）**：以**前端为权威侧**修正 qbank-web 五处值；`--md-primary-active`/`--md-success-active` 同步改为 MD3 pressed（10%）态，避免"hover 变亮、pressed 变暗"的断裂。

**同源第二处不一致（静态基线 vs 运行时派生，本次一并修）**：`styles.css` 的静态 state-layer 令牌与 `stores/theme.js` 的 `mix()` 派生值有 3 个不逐位相等（primary 系 4 个恰好相等，说明当年是手算而非复算）：

| 令牌 | 静态基线（原值） | theme.js 运行时派生（实际生效） | 处置 |
|---|---|---|---|
| `--md-error-hover` | `#bd4038` | `#b93730` | 静态值改为派生值 |
| `--md-success-hover` | `#26803c` | `#27783f` | 同上 |
| `--md-warning-hover` | `#8a6a0a` | `#856614` | 同上 |

`boot()` 在 `App.vue` 挂载时执行，运行时派生值始终覆盖静态基线，故不一致只影响主题加载前的首屏首绘；但它让"静态基线"这一事实源失真，且会与 qbank-web 的静态值形成跨端漂移，故一并修正为派生值（复算方式：`mix(基色, '#ffffff', 0.08)`，已写入 `styles.css` 注释约束后续改动）。

**本次共修正 8 处令牌值**：qbank-web 5 处（跨端漂移）+ 前端 3 处（静态/运行时不一致）+ 2 处 pressed 态派生值（`--md-primary-active`/`--md-success-active`）。

**守护**：新增 `bash .agents/scripts/check_md_tokens.sh`
- 共有令牌值不一致 → **FAIL**（退出码 1）；
- 前端独有 / qbank-web 独有令牌 → **WARN**（列出，供 P4 决定是否补齐）；
- 纯 bash + grep/sed/sort（无 python/node 依赖，Windows Git Bash 可跑）。

**遗留（P4）**：命名分歧（qbank-web 用 `--active`、前端用 `-focus`）；前端 25 个独有令牌是否需要在 qbank-web 补齐——待 P4 一并决策。

## 10. 关联归档

- issue #12（qbank-web MD3 迁移，P1-P4 已交付，**待人工视觉验收**）；docs/12（qbank-web 方案）
- 台账：`.agents/COLLAB_STATE.md`（本次审计 + D1/D2 修复登记）
- 用户决策（2026-09-17）：① 目标层级 = L2 收口（不换组件库）；② 先出本文档评审后再实施；③ 令牌漂移本次一并修（含 qbank-web）并加一致性校验
