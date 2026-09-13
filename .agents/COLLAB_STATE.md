# COLLAB_STATE（智能题库项目协作台账）

> 多人多 AI 共享的协作事实源，入仓（state_policy: git）。禁止写入密钥/token/密码；不写本地绝对路径。
> 每次改动同步 `updated_at`；超过 64KB 先把 Active 内容归档到 Archive。
> 仅协调者写入；子代理用独立心跳/状态文件上报（`.agents/heartbeat/<session>.json`，不入 git）。
> 损坏时用 git 历史 / reflog / 最近任务书重建，重建前不执行。
> baseline 语义 = 最近一次非台账交付的 HEAD；台账自身提交会使 baseline 滞后一个提交，check_state 的 WARN 属预期——恢复时 `git log --oneline <baseline>..HEAD` 领先提交仅为台账时忽略。
> **写台账前必 `git fetch` 核对远端（硬步骤，技能通用 2.3 / 项目 2.2 固化，2026-09-13）；push 前同样须 fetch 复核**。
> **工作环境声明**：`.dsh/`、`/.trae/`、`/.tools/` 等为本机工具链运行环境目录（DSH 等的会话快照/本地计划），**各本机保留、一律不入 git**；勿因协作资产已迁至 `.agents/` 而删除对应的 `.gitignore` 忽略项（2026-09-13 曾发生，已补回）。

state_version: 1
skill_version: 通用 2.3 / 项目 2.2
state_policy: git
ledger_lock: （空）
lock_until: （空）
updated_at: 2026-09-13 19:50
last_session: coord-20260913-1450 / trae-cn-2026-09-13 / trae-session-20260913-v21-migration（并行协调者，union 合并）
environment:
  profile: trae-cn（最近更新；另一方 dsh-default）
  model: TRAE 会话内置模型（以当前会话为准；另一方 deepseek-v4-flash）
  git_version: git version 2.55.0.windows.5（另一方 2.55.0）
  workspace: 仓库根 IntelligentQuestionBank（四模块 root，相对描述）

baseline: 9a205c5aec4ee12d57cc4d23ebdb201d045cae1a
last_known_good: 9a205c5aec4ee12d57cc4d23ebdb201d045cae1a

## Session Log

- 2026-09-13 19:50 coord-20260913-1450（协调者/DSH）：**v1.4.0 正式版发布完成**（API 核实：dev=main=fe10ba1、package.json=1.4.0、标签 v1.4.0 已打；release.yml 打包进行中）。#12 已关联 v1.4.1 milestone（排期承诺工程化登记）。本地 git fetch 受 TLS 阻断（gh API 通道正常），本地同步待网络恢复后 merge（禁 rebase）+ 推送 15c092c 等本地提交。
- 2026-09-13 19:35 coord-20260913-1450（协调者/DSH）：按用户指示关闭全部待验收 issue（#4/#5/#6/#7/#8/#9/#10/#11/#13/#14/#15/#16，共 12 个，gh close 附修复提交与验收状态评论；#5 注明真实库验证待补可重开）；剩余打开仅 #12（backlog）。
- 2026-09-13 19:15 coord-20260913-1450（协调者/DSH）：无人值守轮 8——issue #6 死令牌核查完成（真实零消费仅 tertiary-container/on-tertiary-container 2 个已删；其余均有消费；11 字段主题模型为设计选择维持现状，primary=#6750a4 走官方色板基线）；前端门禁 ✓。
- 2026-09-13 19:05 coord-20260913-1450（协调者/DSH）：无人值守轮 7——issue #4/#15 深色主题 EP 覆盖补全（theme.js 按 pageBg 明暗写入/清理 EP text/bg/overlay/border + MD 表面阶梯，暗夜深蓝下 EP 文本表格弹层可读）；前端门禁 npm run build ✓。
- 2026-09-13 18:55 coord-20260913-1450（协调者/DSH）：无人值守轮 6——issue #10 登录态失效（LoginInterceptor 每请求查库校验 status，禁用/删除即时失效并同步最新资料、密码 hash 不入 session；WebConfig 构造器注入 Mapper）；门禁 worktree BUILD SUCCESS ✓。
- 2026-09-13 18:45 coord-20260913-1450（协调者/DSH）：无人值守轮 5——issue #16 键盘可访问性与小屏（全局 :focus-visible 焦点环、侧栏折叠按钮 role=button+tabindex+Enter/Space 语义化、640px 断点收紧留白）；前端门禁 npm run build ✓。遗留：完整小屏布局重构（移动抽屉）列 backlog。
- 2026-09-13 18:35 coord-20260913-1450（协调者/DSH）：无人值守轮 4——issue #8 限流绕过修复（loginKey 小写归一防大小写变体；XFF 默认不信任，需 qbank-web.rate-limit.trust-x-forwarded-for=true 显式开启；锁定到期惰性清零）；门禁 worktree BUILD SUCCESS ✓。
- 2026-09-13 18:25 coord-20260913-1450（协调者/DSH）：无人值守轮 3——issue #9 会话安全加固（SessionID 登录/注册轮换防固定、SameSite=Lax+HttpOnly 配置、tracking-modes=cookie 杜绝 URL jsessionid；CSRF 以 SameSite 缓解，完整 CSRF token 留 backlog 注明）；门禁 worktree BUILD SUCCESS ✓。
- 2026-09-13 18:15 coord-20260913-1450（协调者/DSH）：无人值守轮 2——issue #11 登录页修复完成（538e05f：口令不落明文/失败回填用户名/站内回跳 isSafeNext 防开放重定向/LoginInterceptor 带 next 编码跳转；qbank-web 门禁 worktree 验证 BUILD SUCCESS ✓）；途中集成 beta.6 bump（2419311，合并 5e2c9f2）。
- 2026-09-13 18:05 coord-20260913-1450（协调者/DSH）：无人值守轮 1——issue #7 残留硬编码色值改读 EP 语义令牌（6c10fbc，7 文件：状态色→el-color-*/边框→el-border-color/占位→placeholder；前端门禁 npm ci+build 通过 ✓）；issue #13 顶栏标题补齐 pageTitle（9505c08，5 控制器：登录/注册/仪表盘/题库管理/分类管理/题目管理/题目表单；qbank-web 门禁在干净 worktree 验证通过 ✓——工作树 target 受同机 Trae/IDE 干扰，构建验证已用 worktree 对照确认与改动无关）。
- 2026-09-13 17:55 coord-20260913-1450（协调者/DSH）：**无人值守模式启动**（用户指示"现在开始无人执手，修复后续的问题"）——任务周期内 fast-forward 推 dev 预授权当次确认；按自主执行规则逐 issue 修复、checkpoint 推送、需要人工决策时暂停汇报。「禁默认 rebase」条款落地（AGENTS.md 并行细则 + 技能）；首次 rebase 为当时用户确认的例外，发现并行后一律 merge+union。
- 2026-09-13 17:45 coord-20260913-1450（协调者/DSH）：**TASK-002 完成**（430cae3）——bootstrap 5.3.3 + icons 1.11.3（含字体）入 vendor，layout.html 3 处改本地 th: 引用；bootcdn 零命中、路径一致、mvn clean package 通过（首次失败系工作树 target 陈旧产物，clean 修复；干净 HEAD worktree 对照确认与改动无关）。issue 修复核查：#3 已关闭，TASK-001/002 已交付。

- 2026-09-13 16:40 coord-20260913-1450（协调者/DSH）：**TASK-001 完成**（2e210d0）——upgrade.sql 追加 v6.1 种子主题幂等刷新（仅当 id=1 未被管理员改动时刷新为 MD3 紫，重复导入不再触发）；config 与 init.sql 逐字符一致、引号平衡、幂等推演成立；真实库运行验证待有权限环境（本机 MySQL root 无密码被拒，未猜凭据）。issue #3 已由 gh 关闭（COMPLETED，关联 67730c3）。

- 2026-09-13 16:31 trae-session-20260913-v21-migration（协调者/Trae CN·另机，git 2.39.1）：第五次分叉对齐（重复迁移型；用户当次确认）。本会话从滞后基线 a203a6a 重复实施 .agents 迁移，本地 4 提交（c541330/3068063/7ce8c18/71b16c4）内容被远端 1a0008b 起的版本完全覆盖；处置：备份分支 backup/local-v21-dup-20260913 保留后 `reset --hard origin/dev`（a8147e1，零丢失、未 force）。附带事实：①外发 v2.1 源头草稿（聊天接收副本，仓库外）缺固定作者/仅 beta/跨模块拆提交三条硬规则，已就地补回，用户将回传分发渠道；同目录更老旧草稿按用户决定原样保留；②本机技能安装目录为通用 2.2/项目 2.1，滞后上游标准 2.3/2.2，待指定来源同步；③styles.css/LoginView.vue 文件头重复 BOM 已回退（无净变更）；④本机代理 192.168.0.147:7890 离线，直连可用但抖动；reset 时 LFS 文件 docs/02-演示PPT.pptx（18MB）直连下载挂起，已 GIT_LFS_SKIP_SMUDGE 跳过（指针落盘），待代理恢复 `git lfs pull`。
- 2026-09-13 16:25 coord-20260913-1450（协调者/DSH）：**并行细则落地**（用户确认"按你的建议来"）——文件域分工并行 OK、台账写锁（ledger_lock/lock_until，30 分钟过期，写前 fetch 读锁）、台账低频更新、冲突兜底（保留双方全部条目）；固化于 AGENTS.md「并行协作细则」、台账模板与技能。当前锁空置。
- 2026-09-13 16:10 coord-20260913-1450（协调者/DSH）：第四次集成完成（用户确认接管）——merge ggt 15:50 心跳作业（7988fc9）与 beta.4 bump（7b4f995，github-actions[bot]，dev 累积达标自动发布 v1.4.0-beta.4，版本声明 5 处已随合并同步）；台账以双方全部条目合并（工作树曾被本机 IDE/Trae 侧并发写，用户确认由本会话接管）。工作环境声明（.dsh 等运行目录勿删忽略项）已固化于 AGENTS.md/技能/台账/.gitignore。
- 2026-09-13 16:00 coord-20260913-1450（协调者/DSH）：第三次集成完成（merge ggt 65d040d/9a205c5/08ff930，保留双方全部条目）；工作环境声明固化——`.dsh/` 为本机 DSH 运行环境目录（非废弃约定），.gitignore 忽略项已补回并说明；教训：push 前也须 fetch（本次分叉发生在 push 瞬间）。**发现同工作树并发写迹象**：冲突文件 15:56:25 被外部进程改写（reflog 无提交），已按纪律冻结并上报用户。
- 2026-09-13 15:55 coord-20260913-1450（协调者/DSH）：开始第三次集成——merge ggt 15:47-15:48 提交（65d040d/9a205c5/08ff930）。
- 2026-09-13 15:50 trae-cn-2026-09-13（协调者/Trae CN）：**心跳作业**（用户指示）。写台账前先 `git fetch origin dev` 核对得 `0 0`（合规新硬步骤）后动笔。落地本机心跳文件 `.agents/heartbeat/trae-cn-2026-09-13.json`（不入 git）。同步修正 `.agents/templates/collab-state.md` 滞后的 `skill_version`（→ 通用 2.3/项目 2.2）；修正 `baseline`/`last_known_good` 滞后（39fed75 → **9a205c5**）。
- 2026-09-13 15:50 coord-20260913-1450（协调者/DSH）：与 trae-cn 线合并对齐（git merge origin/dev，冲突仅台账，保留双方全部条目）；采纳复盘教训——heartbeat_sync.sh 增加"写台账前自动 fetch 核对"步骤；自动化落地（heartbeat_sync/预授权条款）随本次合并入线。
- 2026-09-13 15:47 trae-cn-2026-09-13（协调者/Trae CN）：经用户当次确认清理 `.gitignore` 第 22 行废弃忽略项 `.dsh/`（协作资产已收拢至 `.agents/`，该目录约定作废）；推送 dev（含 65d040d 台账提交）。
- 2026-09-13 15:46 trae-cn-2026-09-13（协调者/Trae CN）：按用户指示「按建议来」固化防分叉硬步骤。写台账前先 `git fetch origin dev` 核对，得 `0 0`（无分叉）后动笔。更新技能：通用技能 2.2→**2.3**；项目技能 2.1→**2.2**（对齐通用 2.3）。技能文件位于技能安装目录，不入 git。同步台账 `skill_version` 为「通用 2.3 / 项目 2.2」。
- 2026-09-13 15:41 trae-cn-2026-09-13（协调者/Trae CN）：恢复协议核对发现本地 dev 与 `origin/dev` **再次分叉**（本地独有 3：39fed75/aac8c6b/8bab271；远端独有 2：d332735/3b6f967，均为台账提交；merge-base 70cfc3f）。经用户当次确认执行「合并对齐」：`git merge origin/dev` 并手工合并台账（保留双方全部条目，未丢弃任何提交），随后推送 dev。
- 2026-09-13 15:30 trae-cn-2026-09-13（协调者/Trae CN）：本地分支清理。经用户当次确认删除本地 `main`（81fb016，删除前校验 `origin/main..main = 0`，无独有提交、零丢失）；本地 `beta` 分支原本不存在。本地现仅剩 `dev`。未 push。
- 2026-09-13 15:25 coord-20260913-1450（协调者/DSH）：同步自动化落地——heartbeat_sync.sh 一键心跳+台账提交（--push 需预授权/当次确认）；任务书预授权条款（周期内 fast-forward 推 dev）写入 AGENTS.md/技能/台账模板。
- 2026-09-13 15:24 trae-cn-2026-09-13（协调者/Trae CN）：心跳对齐。本地 dev 与远端分叉，经用户当次确认以 `origin/dev` 重建，丢弃本地 3 个重复提交；补回远端缺失的 `.gitattributes` 行尾规则（`*.sh text eol=lf`，39fed75）；登记本会话心跳。无活跃任务。
- 2026-09-13 14:50 coord-20260913-1450（协调者/DSH）：完成技能 v2.1 重做与多人协作修正（.agents/ 入仓、Windows Git Bash 标注、基线滞后语义）；无活跃任务，等待新目标。心跳对齐。

## Active Tasks

### TASK-001（issue #5：upgrade.sql 同步 MD3 紫默认主题）
- brief: .agents/tasks/TASK-001.md
- scope: db/upgrade.sql（init.sql 只读核对）
- interface_boundaries: app_theme 种子 ↔ theme.js isBaseline（primary='#6750a4'）；与 init.sql config 完全一致；幂等且不覆盖管理员改动
- owner_session: coord-20260913-1450
- pending_takeover: （空）
- heartbeat: 1789287300
- status: completed
- next_action: （已完成）真实库运行验证待有权限环境；发布周期提示执行 upgrade.sql
- verification: db 幂等契约（重复导入不报错）；init.sql 一致性 grep；本任务不涉及前后端构建
- completed_commit: 2e210d0

### TASK-002（issue #14：qbank-web BootCDN 资源本地化）
- brief: .agents/tasks/TASK-002.md
- scope: qbank-web/src/main/resources/static/vendor/**（新增）; templates/fragments/layout.html
- interface_boundaries: layout.html 是全部 14 页面公共片段；版本保持 bootstrap 5.3.3 / icons 1.11.3 不变；icons css 需同步 fonts
- owner_session: coord-20260913-1450
- pending_takeover: （空）
- heartbeat: 1789287300
- status: confirmed
- next_action: （已完成 430cae3）bootcdn 零命中、mvn clean package 通过（首败为 target 陈旧产物，clean 后解决）
- verification: qbank-web mvn package；bootcdn 零命中 grep；vendor 文件存在性
- completed_commit: 430cae3

## Waiting For User

- 2026-09-13 19:25 coord-20260913-1450（协调者/DSH）：无人值守轮 9（收尾）——自主修复全部完成（#3 关闭；#5/#7/#8/#9/#10/#11/#13/#16/#4/#15/#6 共 11 个已交付，见 Session Log 轮 1-8）。#12（qbank-web 全量 MD3）决策：属 14 模板重写大工程、半吊子迁移反增混乱，列 backlog 建议独立里程碑（含小屏布局重构）。待人工验收项：TASK-001 真实库幂等验证、#4/#15/#11/#16 运行期目测、qbank-web 各修复在部署环境回归。issue 关闭由用户验收后执行（未自动关）。
- 本机（trae-session-20260913-v21-migration 工作机）技能安装目录为通用 2.2/项目 2.1，滞后上游标准 2.3/2.2；技能文件不入 git，待用户指定来源后同步。
- 代理 192.168.0.147:7890 恢复后在该工作机执行 `git lfs pull` 补齐 docs/02-演示PPT.pptx（18MB；本次 reset 以指针文件落盘）。

## Archive

### Decisions
### Decisions
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