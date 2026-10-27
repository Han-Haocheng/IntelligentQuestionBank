# COLLAB_STATE.archive（智能题库项目协作台账 · 归档）

> 来源：`.agents/COLLAB_STATE.md`（git 仓内协作台账）逐字搬迁，未删一字、未改写、未调整顺序。
> 搬迁日期：2026-10-27（Asia/Shanghai）。搬迁执行：coord-20261027-2050（协调者/DSH）。
> 约定：本文件只追加；主台账 Active ≤30 行、Session Log 留 5 条，超出条目移入本文件，条目不删除。
> 校验：搬迁条目已与 `git show HEAD:.agents/COLLAB_STATE.md` 的原文逐字节比对，31 条 Session Log 条目全部命中且顺序一致。

## Session Log（历史条目，保留原顺序）

- 2026-09-14 14:32 trae-session-20260914-lfs-done（本机协调者/Trae CN）：LFS 补齐——代理 192.168.0.147:7890 仍离线，直连 `git -c http.proxy= -c https.proxy= lfs pull` 成功；docs/02-演示PPT.pptx 从指针（133B）下载为真实文件（18,277,587B ≈ 17.4MB），`git lfs ls-files` 标记 `*` 已下载。LFS 待办结清。
- 2026-09-14 10:26 trae-session-20260914-pull-sync（本机协调者/Trae CN）：本机同步——工作区 4 文件（.gitattributes/.gitignore/check_state.sh/模板 collab-state.md）曾被某进程于 09:47 还原为备份分支 71b16c4 旧版（逐字节比对确认、零独有内容，用户当次确认丢弃）；写前 fetch 落后 44 提交，fast-forward af22dda..82943c6（含 v1.4.0 正式版、v1.4.1-beta.1、#7–#16 修复、#12 P1 与迁移方案），本地 HEAD/远程跟踪/ls-remote 三向核对一致、工作区干净。网络通道补注（环境差异）：本机所配局域网代理离线、本机无 127.0.0.1:7890 监听——与 2026-09-13 20:40「走代理禁直连」结论所在的 DSH 机环境不同；本次仅直连成功（TLS 间歇抖动需多重试），gh API 通道正常可用；该结论在本机待代理恢复后回归。技能盘点：微信 9/13 两个技能 zip 解压核验为通用 2.2（与本机已装逐字节一致）/项目 2.0（旧于本机 2.1），2.3/2.2 正本本机全渠道（仓库不入库/微信/.trae-cn/工作机）缺失；用户已定等对方补发后按「备份→逐条 diff 三条硬规则→替换→核验」安装，不手工改版冒充正本。备份分支 backup/local-v21-dup-20260913 仍保留本地。
- 2026-09-13 21:40 coord-20260913-1450（协调者/DSH）：网络通道结论——git 走代理 127.0.0.1:7890（local+global 已配），Clash 分流规则已加 github.com→主代理（建议补 githubusercontent.com 覆盖 Release/LFS 资产）；**禁止 `-c http.proxy=` 绕代理**（直连对 GitHub 国内不稳，此前 TLS 抖动根源）；默认代理下 ls-remote/push 秒通。注：本结论限于 DSH 机环境；另机（局域网代理离线的 trae-cn）依赖直连，见 2026-09-14 10:26 条。
- 2026-09-13 21:13 coord-20260913-1450（协调者/DSH）：#12 P1 完成——app.css 加 MD3 令牌层（色板/形状与前端一致）+ .btn/.form-control/.form-check/:focus-visible 覆盖 + 登录渐变与 body 背景统一主色；{} 平衡、mvn worktree BUILD SUCCESS ✓。
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

## Archive

### Completed Tasks（由主台账 Active Tasks 迁出，逐字保留原文）

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
