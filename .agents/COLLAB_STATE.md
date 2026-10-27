# COLLAB_STATE（智能题库项目协作台账）

> 多人多 AI 共享的协作事实源，入仓（state_policy: git）。禁止写入密钥/token/密码；不写本地绝对路径。
> 每次改动同步 `updated_at`；超过 64KB 先把 Active 内容归档到 Archive。Active ≤30 行、Session Log 留 5 条，超出条目逐字移入 `.agents/COLLAB_STATE.archive.md`（不删条目、不改写）。
> 仅协调者写入；子代理用独立心跳/状态文件上报（`.agents/heartbeat/<session>.json`，不入 git）。
> 损坏时用 git 历史 / reflog / 最近任务书重建，重建前不执行。
> baseline 语义 = 最近一次非台账交付的 HEAD；台账自身提交会使 baseline 滞后一个提交，check_state 的 WARN 属预期——恢复时 `git log --oneline <baseline>..HEAD` 领先提交仅为台账时忽略。
> **写台账前必 `git fetch` 核对远端（硬步骤，技能通用 2.3 / 项目 2.2 固化，2026-09-13）；push 前同样须 fetch 复核**。
> **工作环境声明**：`.dsh/`、`/.trae/`、`/.tools/` 等为本机工具链运行环境目录（DSH 等的会话快照/本地计划），**各本机保留、一律不入 git**；勿因协作资产已迁至 `.agents/` 而删除对应的 `.gitignore` 忽略项（2026-09-13 曾发生，已补回）。

state_version: 1
skill_version: 通用 2.9 / 项目 2.6
state_policy: git
ledger_lock: 无（上一持有者 coord-20260917-1126 的锁已过期）
lock_until: 0
updated_at: 2026-10-27 20:40（Asia/Shanghai）
last_session: coord-20261027-2050 / coord-20260917-1126 / coord-20260917-1103 / trae-session-20260914-lfs-done / trae-session-20260914-pull-sync / coord-20260913-1450 / trae-cn-2026-09-13 / trae-session-20260913-v21-migration（并行协调者，union 合并）
environment:
  profile: dsh-default
  model: deepseek-v4-flash
  git_version: git version 2.55.0.windows.5（另一方 2.55.0）
  workspace: 仓库根 IntelligentQuestionBank（四模块 root，相对描述）

baseline: ff0f64fdb88dbd2929e75e49747bab10227ef7f3
last_known_good: ff0f64fdb88dbd2929e75e49747bab10227ef7f3

## Session Log

- 2026-10-27 20:40 coord-20261027-2050（协调者/DSH）：**多平台协作约定重构 + 台账瘦身归档**。① 单一事实源：`AGENTS.md` 收敛为 `RULES` 块（BEGIN/END 标记）唯一可编辑处，各平台入口文件由 `.agents/scripts/platform_entries.py` 从 `RULES` 块渲染，禁止手写正文。② 平台入口按需生成：`.agents/platforms.json` 登记 → `generate` → `check`；未登记的已知入口文件由 CI 判红；本机专用入口走 `install --local`（不入库）。③ L1 钩子扩展：`.githooks/pre-commit` 在无任务上下文（无 `GATE_TASK_ID` 且无 `.agents/tasks/.current`）时也检查提交格式、受保护路径、平台入口同源。④ L2 权威：`.github/workflows/discipline.yml` 新增「平台入口同源 + 提交范围校验（`range_check.py`）」两步，叠加原有 py_compile / `task check --all-active` / `gate.py doctor`；版本声明与 `CHANGELOG.md` 放行白名单 = 作者 `github-actions[bot]` 或标题前缀 `chore(release)`，其余阻断。⑤ `bootstrap.sh` 一条命令接入（装 `core.hooksPath=.githooks` + 环境自检）。⑥ 台账瘦身：Session Log 仅留最新 5 条、Active 压到四要素（≤30 行），更早条目与已完成任务细节逐字移入 `.agents/COLLAB_STATE.archive.md`。后续待办：其他成员按各自平台在 `platforms.json` 登记入口后 `generate`（本机 targets 为空，不预置他人入口）。

> 更早条目见 `.agents/COLLAB_STATE.archive.md`（逐字搬迁，未改写）

- 2026-09-17 11:45 coord-20260917-1126（协调者/DSH）：**前端 MD3 差距审计 + 令牌对齐交付**（33a6bca docs/13 / e9d9455 qbank-web 令牌 / 4ac5f3a 前端静态令牌 / ff0f64f 校验脚本）。① 审计：用户判断"前后端分离版（frontend）未完整使用 MD3"**成立**——可证差距 G1–G12：EP 语义色整套未映射（`--el-color-success/warning/danger/error/info` → alert 文案与图标、`el-form-item` 校验文案与必填星号仍取 EP 品牌色）、表单标签仍 EP `#606266`（`--el-text-color-*` 仅暗色主题写入）、`el-select` 下拉面板未覆盖（20 处/7 视图，`styles.css` 覆盖的是 `.el-dropdown-menu` 另一组件）、loading 遮罩 `#ffffffe6` 非 MD3 scrim、无字阶/动效令牌/ripple、几何未 MD3 化（switch 40×20）、图标集为 EP icons；量化：前端 53 令牌 + 54 个 `.el-*` 选择器 vs 在用 52 个 EP 组件，未映射 `--el-*` 引用 474 处/去重 273 个。② 结论纠正：EP 之上不可达"完整 MD3"，定义 L1 令牌级/L2 组件皮肤级/L3 组件库级，本次取 **L2 收口（不换库）**，方案 docs/13（P1 令牌收口 → P2 未覆盖组件 → P3 字阶与状态层 → P4 高度/主题约束 → P5 走查）**待用户评审后实施**。③ 令牌漂移修复：qbank-web 5 处（`--md-shape-xl` 24→28px[MD3 规范值；该令牌在 qbank-web 无消费]；`--md-primary-hover`/`--md-success-hover` 由"加深"改为 MD3 状态层"提亮"方向；success/warning container 取值）+ pressed 态派生（`--md-primary-active`→`#7662ad`、`--md-success-active`→`#2c7b43`）+ 前端 3 处（静态 hover 令牌与 `theme.js` 的 `mix()` 派生值不逐位相等，改为派生值 `#b93730`/`#27783f`/`#856614`）。④ 新增 `.agents/scripts/check_md_tokens.sh`（纯 bash + `LC_ALL=C` 修 comm 排序域；正反向自测：注入漂移→FAIL/退出码 1、文件缺失→2）。门禁：qbank-web `mvn package` BUILD SUCCESS ✓；前端在**原生 FS 副本** `npm ci` + `vite build` ✓（工作区因 FUSE 孤儿文件阻塞，见 Waiting For User）；CSS 盲区补扫（括号 104/104、61/61；无重复/自引用令牌）✓。
- 2026-09-17 11:14 coord-20260917-1103（协调者/DSH）：#12 **P4 完成，迁移四阶段全部交付**（97d3433）——新增 Bootstrap↔MD3 令牌桥接：把 Bootstrap 的 --bs-*-rgb（primary/secondary/success/danger/warning/light + body/border/link）整体映射到 MD3，使 text-muted(52 处)/text-danger(13)/text-primary(12)/bg-light(10)/bg-white(4)/text-success(7) 等工具类与 error.html 自动落到 MD3 色，不再泄露 Bootstrap 默认蓝/灰；字阶收口 body/表格/卡片/表单/按钮 14px（对齐前端 body-medium），内容区 padding 24px。门禁：括号平衡 61/61、无自引用/无裸色值（157 令牌）、mvn worktree BUILD SUCCESS ✓。**遗留：人工视觉验收（14 页目测）为验收前置，本环境无浏览器故未做**；验收通过即可关闭 issue #12。
- 2026-09-17 11:11 coord-20260917-1103（协调者/DSH）：#12 P3 完成——卡片/表格（表头 #f7f8fa→surface-container）/分页（选中=secondary-container）/徽章（text-bg-* 六变体映射 MD3 容器色）/提醒（四变体）/弹窗（shape-lg+outline）/语义按钮补全（success/danger/warning/light），并把按钮 hover/active 深色变体收敛为 :root 令牌（--md-primary-hover 等）；门禁：括号平衡 57/57、无自引用/无裸色值（137 令牌）、mvn worktree BUILD SUCCESS ✓。P4 走查收口为最后一阶段。
- 2026-09-17 11:02 coord-20260917-1103（协调者/DSH）：#12 P2 完成——侧栏改 MD3 浅色表面（surface-container-low）+ secondary-container 选中态/primary 左边条，顶栏/页面标题/导航卡片/登录卡片圆角 MD3 化；无模板改动（data-bs 钩子不动）；{} 平衡、mvn worktree BUILD SUCCESS ✓。遗留：#f7f8fa 表头归 P3。**跨日恢复**（距 09-13 21:40 已 3 天）：按恢复协议 fetch 复核发现远端新增 2 提交（guozhibin：LFS 补齐、本机同步登记），台账冲突经 `ledger_union_merge.sh` 保留双方条目后由本会话去重字段/修正时间戳。

## Active Tasks

### TASK-003（v1.4.1 里程碑 #12：qbank-web 全量 MD3 迁移）
- 目标：qbank-web 模板与静态样式全量 MD3 迁移（brief: docs/12-qbank-web-md3-迁移方案.md；scope: qbank-web/src/main/resources/templates/**; static/css/app.css；影响域：vendor 引用不回归、data-bs-* JS hooks 类名不动、前端 Vue MD3 令牌一致）
- 状态：stages_done——P1 令牌层+btn/表单/焦点环、P2 布局导航、P3 卡片/表格/分页/徽章/提醒/弹窗/语义按钮、P4 Bootstrap↔MD3 令牌桥接+字阶收口全部完成；2026-09-17 令牌已与前端对齐（e9d9455）
- 阻塞点：待人工视觉验收（14 页目测）；验收通过后关闭 #12 并推进 v1.4.1 正式发布
- 验证：qbank-web `mvn -B -DskipTests package --file qbank-web/pom.xml`（干净 worktree）；14 视图目测

### TASK-004（v1.4.1 周期：frontend 前端 MD3 L2 收口）
- 目标：按 docs/13 实施 P1-P5（brief: docs/13-frontend-md3-审计与迁移方案.md；scope: frontend/src/styles.css、frontend/src/stores/theme.js、frontend/src/views/**、frontend/src/components/**（不含 frontend/package.json 与 package-lock.json）；影响域：theme.js ↔ styles.css 令牌契约、qbank-web 令牌同步、不改后端 API、不改依赖、不改版本声明 5 处、不写 CHANGELOG）
- 状态：plan_delivered（owner_session: coord-20260917-1126；L2 路线 + P1-P5 + 验收标准 + 影响域已写入 docs/13）
- 阻塞点：等用户评审 docs/13 后才动代码；工作区 `frontend/node_modules/@rollup/rollup-linux-x64-gnu` 因 FUSE 孤儿文件（`.fuse_hidden*`）导致 `npm ci` ENOTEMPTY，门禁暂以原生 FS 副本验证
- 验证：`(cd frontend && npm ci && npm run build)`；`bash .agents/scripts/check_md_tokens.sh`；14 视图目测 + `firefox --headless --screenshot` 探针（环境变量门控）

### TASK-001 / TASK-002（已完结，细节见 `.agents/COLLAB_STATE.archive.md` 的 Archive › Completed Tasks）
- TASK-001（issue #5：upgrade.sql 同步 MD3 紫默认主题）：completed，2e210d0；真实库幂等验证待有权限环境
- TASK-002（issue #14：qbank-web BootCDN 资源本地化）：completed，430cae3

## Waiting For User

- 2026-09-17 11:45 coord-20260917-1126（协调者/DSH）：**① docs/13 评审待办**——前端 MD3 L2 收口方案（P1-P5）已交付，评审通过后进入 P1（不改依赖、不换组件库）。**② 环境阻塞（需 Windows 侧动作）**：`frontend/node_modules/@rollup/rollup-linux-x64-gnu` 内存在 FUSE 孤儿文件 `.fuse_hidden0000f09b00000014`（2141464B，9-13 17:50 创建、句柄在 Windows 宿主侧未释放；Linux 侧无进程持有），导致 `npm ci` 恒定 ENOTEMPTY、`vite` 缺失；**处置选项**：(a) 在 Windows 侧关闭占用该目录的进程（如 Windows 版 VS Code / node / esbuild 残留）后由我重跑 `npm ci`；(b) 允许我 `rm -rf frontend/node_modules` 后重装（需当次确认）；(c) 维持现状，门禁继续走原生 FS 副本验证（本次已用 tar 复制 + MD5 核对）。**③ push 待当次确认**：本地 dev 领先 origin/dev **6 提交**（97d3433/19e834d + 本次 33a6bca/e9d9455/4ac5f3a/ff0f64f），未推送。
- 2026-09-17 11:45 coord-20260917-1126（协调者/DSH）：#12（qbank-web）**待人工视觉验收**——P4 令牌桥接 + 本次令牌对齐（e9d9455）后需 14 页目测；验收通过即可关闭 issue #12 并推进 v1.4.1 正式发布。
- 2026-09-13 19:25 coord-20260913-1450（协调者/DSH）：无人值守轮 9（收尾）——自主修复全部完成（#3 关闭；#5/#7/#8/#9/#10/#11/#13/#16/#4/#15/#6 共 11 个已交付，见 Session Log 轮 1-8）。#12（qbank-web 全量 MD3）决策：属 14 模板重写大工程、半吊子迁移反增混乱，列 backlog 建议独立里程碑（含小屏布局重构）。待人工验收项：TASK-001 真实库幂等验证、#4/#15/#11/#16 运行期目测、qbank-web 各修复在部署环境回归。issue 关闭由用户验收后执行（未自动关）。
- 本机（工作机 trae-session-20260914-pull-sync）技能安装目录为通用 2.2/项目 2.1；微信 9/13 技能 zip 经解压核验为通用 2.2（与本机一致）/项目 2.0（更旧），2.3/2.2 正本仅 DSH 机安装目录持有、本机全渠道缺失。用户已定（2026-09-14）：等对方补发 2.3/2.2 技能包后按「备份→逐条 diff 三条硬规则→替换→核验」安装，不手工改版。
- ~~代理恢复后在该工作机执行 `git lfs pull` 补齐 docs/02-演示PPT.pptx~~ **已完成（2026-09-14 14:32）**：直连 `git lfs pull` 成功，文件 18,277,587B 已下载、`git lfs ls-files` 标记 `*`。
- 2026-10-27 20:40 coord-20261027-2050（协调者/DSH）：**其他成员按各自平台登记入口**——`.agents/platforms.json` 的 targets 为空、仓库不预置入口文件；使用某平台者执行 `python3 .agents/scripts/platform_entries.py add <平台>` 后 `generate`，并把结果随提交带入。

## Archive

### Decisions
- 2026-09-17 前端 MD3 收口立项与三项决策（用户当次选择）：① **目标层级 = L2 收口**（令牌 + 全部在用 EP 组件皮肤覆盖，**不换组件库**、不改依赖）；② **先出 docs/13 审计与迁移方案，评审后再实施**；③ **令牌漂移本次一并修**（含 qbank-web 5 处）**并新增一致性子集校验脚本**。依据：用户判断"前后端分离版前端未完整使用 MD3"经审计成立（G1-G12），而 EP 之上不可达"完整 MD3"，故以层级定义替代模糊的"完整"。来源：用户当次确认。
- 2026-09-13 19:40 发布 v1.4.0 正式版（用户指示"发布v1.4.0"）：dev 当前 1.4.0-beta.7，走 formal-release.yml（去 -beta.7 后缀 → bump dev → 快进 main → 打 v1.4.0 标签 → 派发 release.yml 打包 latest）；发布前门禁在干净 worktree 验证通过（本机工作树 target 受 IDE 干扰，门禁以 worktree 为准）。**#12 排期承诺：在 v1.4.1 完全解决**（用户指示）。来源：用户当次确认。
- 2026-09-13 无人值守授权（用户指示"现在开始无人执手，修复后续的问题"）：任务周期内由协调者自主执行——逐 issue 修复、验证门禁、pathspec 提交；「任务周期内 fast-forward 推 dev」预授权当次确认（push 前仍 fetch 复核，分叉一律 merge+union，禁 rebase）；需要人工决策（真实库/凭据/外源下载失败/歧义/门禁红无法定位）时立即暂停并记入 Waiting For User。来源：用户当次确认。
- 2026-09-13 禁默认 rebase 条款（用户"可以"确认）：共享 dev + 多协调者场景禁止默认 rebase；rebase 仅限独占分支无并行写者的例外且需当次确认；并行场景统一 merge + union。固化于 AGENTS.md 并行细则与技能八。来源：用户当次确认。
- 2026-09-13 第五次分叉对齐（重复 .agents 迁移型，协调者 Trae CN·另机）：本地 4 提交（c541330/3068063/7ce8c18/71b16c4）与远端 1a0008b 以来的 .agents 迁移等价且被覆盖，无任何独有交付；经用户当次确认建备份分支 backup/local-v21-dup-20260913 后 `reset --hard origin/dev` 重建（仅本地历史重写，备份保留、零丢失，未对远端 force）。来源：用户当次确认。
- 2026-09-13 并行协作细则落地（用户确认"按你的建议来"）：文件域分工并行 OK；共享热点（台账）单写——写前 fetch 读远端 `ledger_lock`/`lock_until`，锁未过期（30 分钟）不写；写入时锁随台账提交更新，过期自动释放；台账低频更新（仅里程碑/交接/发布点，日常用本地心跳）；冲突一律「保留双方全部条目」union 兜底。固化于 AGENTS.md、台账模板与技能。来源：用户当次确认。
- 2026-09-13 第四次分叉对齐 + 同工作树并发写处置（协调者 DSH，用户确认接管）：merge ggt 7988fc9 与 beta.4 bump 7b4f995；冲突解决期间发现工作树台账被本机 IDE/Trae 侧并发改写（reflog 无提交、文件 mtime 15:56:25），按"同一文件域禁止双写"纪律冻结并上报，用户确认由本会话以双方全部条目完成合并并推送。来源：用户当次确认。
- 2026-09-13 工作环境声明（用户指示"指明当前工作环境，以防其他出现删掉了 .dsh/"）：`.dsh/`、`/.trae/`、`/.tools/` 等是本机工具链运行环境目录（含 DSH 会话快照/本地计划），各本机保留、一律不入 git；协作资产只以 `.agents/` 为仓库共享域。**删除任一忽略项前必须先确认该目录不被本机运行环境使用**。固化位置：.gitignore 注释、AGENTS.md、台账头注释、技能。来源：用户指示。
- 2026-09-13 第三次分叉对齐（协调者 DSH）：本地 push 瞬间远端又新增 ggt 3 提交（65d040d/9a205c5/08ff930），经 merge 集成（保留双方全部条目）；补回 `.dsh/` 忽略（见上条声明）；push 前也须 fetch 的教训并入台账头注释。来源：用户此前对齐意图延续。
- 2026-09-13 同步自动化落地（用户确认"可以"）：新增 `.agents/scripts/heartbeat_sync.sh` 一键心跳+台账提交（--push 需任务书预授权或当次确认，仅 fast-forward 推 dev）；「任务书可登记本任务周期内 fast-forward 推 dev 预授权」条款写入 AGENTS.md、技能〇.2（push 例外）与台账模板 push_authorized 字段；CI 侧 beta 自动发布维持现状。来源：用户当次确认。
- 2026-09-13 固化「写台账前必 `git fetch`」硬步骤（技能通用 2.3 / 项目 2.2）：针对台账两次因多机并行写而分叉的根因，把该前置步骤写入通用技能正文（SKILL.md 第六节、references/session-continuity.md 第 1 节）、模板抬头与项目技能第七/九节，并将通用技能 2.2→2.3、项目技能 2.1→2.2。技能文件在安装目录不入 git。来源：用户指示（按建议来）。
- 2026-09-13 分叉对齐（第二次）：本地 dev 与远端因跨会话并行写台账再次分叉（本地独有 3 / 远端独有 2，冲突仅 `.agents/COLLAB_STATE.md`），经用户当次确认以合并方式对齐——保留本地 `.gitattributes` 行尾修复与双方全部台账条目，未丢弃提交、未 force。来源：用户当次确认（合并对齐并推送）。
- 2026-09-13 外部世界核对：beta 自动发布工作流已基于 e59461e 在远端完成 v1.4.0-beta.3（bump 提交 70cfc3f，github-actions[bot]，推送期间本机代理 TLS 故障）；beta 分支已合并、标签 v1.4.0-beta.3 已打；本地台账提交经用户确认 rebase 至该 bump 之上并推送，版本声明随仓库同步为 1.4.0-beta.3。来源：用户当次确认（rebase 后推送）。
- 2026-09-13 本地分支清理：经用户当次确认（敏感操作·删除）执行 `git branch -D main`，删除前以 `git rev-list --count origin/main..main` 校验为 0（无独有提交）确保零丢失；本地 `beta` 不存在，无需处理。来源：用户当次确认。
- 2026-09-13 本地分叉对齐：本地 dev 领先 3 提交（64c9d30/a3c4663/f8e0988）与远端 1a0008b 属重复劳动，经用户当次确认后执行 `git reset --hard origin/dev` 重建本地 dev（历史重写，用户明确授权），仅补回唯一有价值的本地独有改动 `.gitattributes` 行尾规则。来源：用户当次确认。
- 2026-09-13 心跳对齐（首次）推送记录经 git 复核：台账记为"推送未执行、本地领先 2 提交（68bc610、f91335a）"，但 2026-09-13 15:20 fetch 后确认二者已在 `origin/dev` 上——以 git 为准，台账记录已过期。来源：双源校验差异回馈。
- 2026-09-13 心跳对齐：push dev 的用户当次确认已取得且 fast-forward 校验通过，但用户随后指示"先暂停对齐"——推送未执行（该状态已由上述复核更新）。来源：用户指示（先暂停）。
- 2026-09-13 多人环境适配：协作脚本需 bash 环境，Windows 协作者用 Git for Windows 自带 Git Bash（或 WSL）执行，cmd/PowerShell 不可直接运行；已在 AGENTS.md、技能与脚本头注明。来源：用户补充意见。
- 2026-09-13 项目技能 intelligent-question-bank-workflow 重做为 2.0，对齐通用技能 2.2（反驳授权 / 命令契约 / 波次并行与文件域 / 会话连续性 / 门禁盲区 / 事故案例）。来源：用户当次任务确认。
- 2026-09-13 多人多 AI 修正：协作资产与台账收拢至仓库内 `.agents/` 隐藏目录（入仓共享），全部路径改为仓库相对路径，不再依赖任何本机工具链目录（如 ~/.dsh、/.trae）；原 .dsh/COLLAB_STATE.md 已迁移至此。来源：用户修正指示。
- 2026-09-13 未归属改动 frontend/package.json（allowScripts 增加 electron-winstaller@5.4.0）经用户确认后提交为 3852e59（chore）。来源：用户当次确认。

### Rejected Ideas
- 2026-09-17 「L3：换 MD3 组件库（Material Web / Vuetify3 MD3）+ 换 Material Symbols 图标集，追求"完整 MD3"」：需改依赖（代理无权决策）+ 重写 14 个视图 + 全量回归，且 qbank-web 已按当前 EP+令牌基线完成 P1-P4 对齐，换库会让两端再次分叉；替代方案 = L2 收口（令牌 + 组件皮肤覆盖，Floating label/ripple/switch 几何/图标集四项明确列为"只能近似、本期不做"）。来源：协调者建议，用户当次选择 L2。
- 2026-09-13 「不重建历史，直接在当前本地 dev 上写台账做心跳对齐」：本地台账为第三版约定（`state_policy: tracked` vs 远端 `git`、无 Session Log 段），直接写入会生成与远端不兼容的版本，后续合并必冲突；替代方案为先以 `origin/dev` 重建本地 dev。来源：协调者反驳，用户采纳重建方案。

### Completed Tasks
- 2026-09-13 第四次集成（ggt 心跳作业 + beta.4 bump）
- 2026-09-13 工作环境声明固化（.dsh/ 等本机运行目录忽略项补回并注明，勿再删）
- 2026-09-13 删除本地 `main` 分支（用户当次确认；删除前校验无独有提交）
- 2026-09-13 补回 `.gitattributes` 行尾规则（39fed75，修复远端缺失导致的 Git Bash 执行失败）
- 2026-09-13 清理 `.gitignore` 废弃 `.dsh/` 协作资产忽略项（9a205c5，ggt 侧；本次补回运行快照忽略）
- 2026-09-13 package.json allowScripts 提交（3852e59，用户确认归属后）

### Retro（每 N 提交或里程碑）
- 无效交付：本地 64c9d30/a3c4663/f8e0988 三个提交与远端 1a0008b 重复（同一目标两处并行产出），已 reset 丢弃；教训——开工前必须先 `git fetch` 核对远端，离线期间的重复劳动无法入仓。
- 会话中断：-
- 越界改动：ggt 清 `.dsh/` 忽略项时未确认本机运行环境是否仍在用，已补回并固化"删忽略项前先确认运行目录"声明；另发现本机 IDE/Trae 侧并发写工作树（merge 冲突文件被外部改写），处置为冻结上报、由用户确认单方接管——**多工具同机协作必须串行化（同一文件域单写者）**。
- 重复陷阱：台账**两次**因跨会话/跨机并行写而分叉，根因是写台账前未先 `git fetch`。**已闭合（15:46）**：硬步骤固化（通用 2.3 / 项目 2.2）+ heartbeat_sync.sh；**补充（15:55）push 前同样须 fetch 复核**（第三次分叉发生在 push 瞬间；第四次为 beta.4 bot bump + ggt 心跳作业）。
- 技能更新建议：`.dsh/` 约定已迁移至 `.agents/`，但目录仍为本机运行环境使用——忽略项保留并注明用途；删除任何忽略项前须先确认运行目录归属。
- 门禁陷阱（2026-09-17 新增）：**门禁命令禁止经 `| tail`/`| head` 管道**——本次 `npm ci | tail -8 && … npm run build | tail -20` 的真实失败被管道退出码掩盖（外层报告 exit 0，实际 `npm ci` ENOTEMPTY、`vite` 未找到）；门禁必须以命令自身退出码判定（`cmd; rc=$?`），已建议并入项目技能"命令契约/陷阱"节。
- 环境陷阱（2026-09-17 新增）：仓库所在盘为 **fuseblk/NTFS-FUSE 挂载（Windows 盘）**时，`npm ci` 删除 node_modules 可能因 **FUSE 孤儿文件**（`.fuse_hidden*`：文件已 unlink 但句柄未释放）恒定 ENOTEMPTY，且对该孤儿文件 `rm` 只会改名、无法删除；Linux 侧 `lsof` + `/proc/*/fd` 无持有者即可判定句柄在 Windows 宿主侧。处置：门禁改在**原生 FS 副本**验证（tar 复制 + `md5sum` 证明副本与工作区逐字节一致），根治需 Windows 侧释放句柄。
- 令牌漂移的第二形态（2026-09-17 新增）：除"跨文件漂移"外，`styles.css` **静态令牌**与 `theme.js` **运行时 `mix()` 派生值**也会不一致（primary 系 4 个恰好相等、error/success/warning 3 个不一致），影响主题加载前首绘并与跨端漂移叠加；令牌新增/改动后须用 `node` 复算派生值再与静态值比对。