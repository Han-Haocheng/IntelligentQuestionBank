# COLLAB_STATE（智能题库项目协作台账）

> 多人多 AI 共享的协作事实源，入仓（state_policy: git）。禁止写入密钥/token/密码；不写本地绝对路径。
> 每次改动同步 `updated_at`；超过 64KB 先把 Active 内容归档到 Archive。
> 仅协调者写入；子代理用独立心跳/状态文件上报（`.agents/heartbeat/<session>.json`，不入 git）。
> 损坏时用 git 历史 / reflog / 最近任务书重建，重建前不执行。
> baseline 语义 = 最近一次非台账交付的 HEAD；台账自身提交会使 baseline 滞后一个提交，check_state 的 WARN 属预期——恢复时 `git log --oneline <baseline>..HEAD` 领先提交仅为台账时忽略。

state_version: 1
skill_version: 通用 2.3 / 项目 2.2
state_policy: git
updated_at: 2026-09-13 15:47
last_session: trae-cn-2026-09-13
environment:
  profile: trae-cn
  model: TRAE 会话内置模型（以当前会话为准）
  git_version: git version 2.55.0.windows.5
  workspace: 仓库根 IntelligentQuestionBank（四模块 root，相对描述）

baseline: 39fed75ff75886b8a0fcf1bf2ab75b175c6766d4
last_known_good: 39fed75ff75886b8a0fcf1bf2ab75b175c6766d4

## Session Log

- 2026-09-13 15:47 trae-cn-2026-09-13（协调者/Trae CN）：经用户当次确认清理 `.gitignore` 第 22 行废弃忽略项 `.dsh/`（协作资产已收拢至 `.agents/`，该目录约定作废）；推送 dev（含 65d040d 台账提交）。
- 2026-09-13 15:46 trae-cn-2026-09-13（协调者/Trae CN）：按用户指示「按建议来」固化防分叉硬步骤。写台账前先 `git fetch origin dev` 核对，得 `0 0`（无分叉）后动笔。更新技能：通用技能 2.2→**2.3**（SKILL.md「外部事实源」条 + references/session-continuity.md 第 1 节新增「写前必取远端（硬步骤）」+ 模板 `skill_version` 同步 2.3）；项目技能 2.1→**2.2**（对齐通用 2.3，第七节外部事实源条 + 第九节事故案例同步）。技能文件位于技能安装目录，不入 git，本次仅改本地技能。同步台账 `skill_version` 为「通用 2.3 / 项目 2.2」。
- 2026-09-13 15:41 trae-cn-2026-09-13（协调者/Trae CN）：恢复协议核对发现本地 dev 与 `origin/dev` **再次分叉**（本地独有 3：39fed75/aac8c6b/8bab271；远端独有 2：d332735/3b6f967，均为台账提交；merge-base 70cfc3f）。经用户当次确认执行「合并对齐」：`git merge origin/dev` 并手工合并台账（保留双方全部条目，未丢弃任何提交），随后推送 dev。
- 2026-09-13 15:30 trae-cn-2026-09-13（协调者/Trae CN）：本地分支清理。经用户当次确认删除本地 `main`（81fb016，删除前校验 `origin/main..main = 0`，无独有提交、零丢失）；本地 `beta` 分支原本不存在。本地现仅剩 `dev`，与纪律「本地无 main/beta」一致。未 push。
- 2026-09-13 15:24 trae-cn-2026-09-13（协调者/Trae CN）：心跳对齐。本地 dev 与远端分叉（ahead 3 / behind 10）经用户当次确认后以 `origin/dev` 重建，丢弃本地 3 个重复提交（与远端 1a0008b 重复劳动）；补回远端缺失的 `.gitattributes` 行尾规则（`*.sh text eol=lf`，39fed75）；登记本会话心跳 `.agents/heartbeat/trae-cn-2026-09-13.json`（不入 git）。无活跃任务。
- 2026-09-13 14:50 coord-20260913-1450（协调者/DSH）：完成技能 v2.1 重做与多人协作修正（.agents/ 入仓、Windows Git Bash 标注、基线滞后语义）；无活跃任务，等待新目标。心跳对齐。

## Active Tasks

（空；本会话为本地/远端分叉合并对齐，属协作元数据维护）

## Waiting For User

（空）

## Archive

### Decisions
- 2026-09-13 固化「写台账前必 `git fetch`」硬步骤（技能 2.3/2.2）：针对台账两次因多机并行写而分叉的根因，把该前置步骤写入通用技能正文（SKILL.md 第六节、references/session-continuity.md 第 1 节）、模板抬头与项目技能第七/九节，并将通用技能 2.2→2.3、项目技能 2.1→2.2。技能文件在安装目录不入 git。来源：用户指示（按建议来）。
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
- 2026-09-13 删除本地 `main` 分支（用户当次确认；删除前校验无独有提交）
- 2026-09-13 补回 `.gitattributes` 行尾规则（39fed75，修复远端缺失导致的 Git Bash 执行失败）
- 2026-09-13 package.json allowScripts 提交（3852e59，用户确认归属后）

### Retro（每 N 提交或里程碑）
- 无效交付：本地 64c9d30/a3c4663/f8e0988 三个提交与远端 1a0008b 重复（同一目标两处并行产出），已 reset 丢弃；教训——开工前必须先 `git fetch` 核对远端，离线期间的重复劳动无法入仓。
- 会话中断：-
- 越界改动：-
- 重复陷阱：台账**两次**因跨会话/跨机并行写而分叉（第一次 .agents 骨架，第二次台账条目），根因是写台账前未先 `git fetch`，"仅协调者写入"在多机场景未真正落实。**已闭合（2026-09-13 15:46）**：「写台账前必 `git fetch` 并确认无新远端提交」已固化进技能正文（通用 2.3 / 项目 2.2），后续写台账须先核 `git rev-list --left-right --count origin/dev...HEAD` 得 `0 0`。
- 技能更新建议：**已闭合（2026-09-13 15:47）**——`.gitignore` 第 22 行废弃忽略项 `.dsh/` 已经用户确认清理（协作资产收拢至 `.agents/`）；技能正文防分叉硬步骤亦已固化（见上条）。