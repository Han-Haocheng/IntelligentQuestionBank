# COLLAB_STATE（智能题库项目协作台账）

> 多人多 AI 共享的协作事实源，入仓（state_policy: git）。禁止写入密钥/token/密码；不写本地绝对路径。
> 每次改动同步 `updated_at`；超过 64KB 先把 Active 内容归档到 Archive。
> 仅协调者写入；子代理用独立心跳/状态文件上报（`.agents/heartbeat/<session>.json`，不入 git）。
> 损坏时用 git 历史 / reflog / 最近任务书重建，重建前不执行。
> baseline 语义 = 最近一次非台账交付的 HEAD；台账自身提交会使 baseline 滞后一个提交，check_state 的 WARN 属预期——恢复时 `git log --oneline <baseline>..HEAD` 领先提交仅为台账时忽略。

state_version: 1
skill_version: 通用 2.2 / 项目 2.1
state_policy: git
updated_at: 2026-09-13 15:24
last_session: trae-cn-2026-09-13
environment:
  profile: trae-cn
  model: TRAE 会话内置模型（以当前会话为准）
  git_version: git version 2.55.0.windows.5
  workspace: 仓库根 IntelligentQuestionBank（四模块 root，相对描述）

baseline: 39fed75ff75886b8a0fcf1bf2ab75b175c6766d4
last_known_good: 39fed75ff75886b8a0fcf1bf2ab75b175c6766d4

## Session Log

- 2026-09-13 15:24 trae-cn-2026-09-13（协调者/Trae CN）：心跳对齐。本地 dev 与远端分叉（ahead 3 / behind 10）经用户当次确认后以 `origin/dev` 重建，丢弃本地 3 个重复提交（与远端 1a0008b 重复劳动）；补回远端缺失的 `.gitattributes` 行尾规则（`*.sh text eol=lf`，39fed75）；登记本会话心跳 `.agents/heartbeat/trae-cn-2026-09-13.json`（不入 git）。无活跃任务。
- 2026-09-13 14:50 coord-20260913-1450（协调者/DSH）：完成技能 v2.1 重做与多人协作修正（.agents/ 入仓、Windows Git Bash 标注、基线滞后语义）；无活跃任务，等待新目标。心跳对齐。

## Active Tasks

（空；本会话为本地分叉对齐 + 心跳对齐，属协作元数据维护）

## Waiting For User

- 本地仍存在 `main` 分支（81fb016 `chore(release): v1.2.0`，跟踪 `origin/main`），与纪律「本地已删除 main/beta」不符；删除分支属敏感操作，需用户当次确认。

## Archive

### Decisions
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
- 2026-09-13 补回 `.gitattributes` 行尾规则（39fed75，修复远端缺失导致的 Git Bash 执行失败）
- 2026-09-13 package.json allowScripts 提交（3852e59，用户确认归属后）

### Retro（每 N 提交或里程碑）
- 无效交付：本地 64c9d30/a3c4663/f8e0988 三个提交与远端 1a0008b 重复（同一目标两处并行产出），已 reset 丢弃；教训——开工前必须先 `git fetch` 核对远端，离线期间的重复劳动无法入仓。
- 会话中断：-
- 越界改动：-
- 重复陷阱：同上（.agents 骨架被两个会话并行创建）；恢复协议应在动手前完成"外部世界核对"。
- 技能更新建议：项目中已删除 `.dsh/` 约定，但远端 `.gitignore` 第 22 行仍保留 `.dsh/` 忽略项，建议清理。