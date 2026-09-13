# COLLAB_STATE（协作台账模板）

> 只做本地协作事实源，禁止写入密钥/token/密码。每次改动同步 `updated_at`；超过 64KB 先把 Active 内容归档到 Archive。
> `state_policy` 默认 ignored（不入 git）；**本项目为多人多 AI 协作，契约决定入仓（state_policy: git）**——入仓时不写本地绝对路径与敏感信息。
> 仅协调者写入；子代理用独立心跳/状态文件上报（本项目：`.agents/heartbeat/<session>.json`，不入 git）。损坏时用 git 历史 / reflog / 最近任务书重建，重建前不执行。

state_version: 1
skill_version: 通用 2.3 / 项目 2.2
state_policy: git
ledger_lock: <owner-session；写台账前先 fetch 读远端锁，锁未过期（30 分钟）不写；写入时更新为自己 +30 分钟，随台账提交>
lock_until: <epoch+1800；过期自动释放>
updated_at: <YYYY-MM-DD HH:MM>
last_session: <session-id>
environment:
  profile: <profile 名>
  model: <模型名>
  git_version: <git --version 摘要>
  workspace: <仓库根相对描述>

baseline: <git rev-parse HEAD>
last_known_good: <每目标回滚基线，可多项>

## Session Log

- <日期时间> <session-id>（<角色/工具链>）：<本次会话产出与状态>；<下一步>。老条目归档到 Archive。

## Active Tasks

### TASK-001
- brief: <任务书相对路径>
- scope: <允许路径逐项列出>
- interface_boundaries: <影响域 / 接口契约>
- owner_session: <session-id>
- pending_takeover: <申请接管的 session-id，无则留空>
- heartbeat: <date +%s>
- status: <proposed / confirmed / in_progress / blocked / waiting_user>
- next_action: <恢复后下一步>
- approved_at: <时间> / approved_scope: <范围>（敏感操作不适用）
- push_authorized: <登记范围，如 "dev fast-forward，本任务周期内"；任务书预授权时填，无则留空>
- verification: <验证命令，引用契约条目>
- completed_commit: <hash，未完成留空>

## Waiting For User

- <问题或建议引用；附用户原话/关键限制>

## Archive

### Decisions
- <日期> <决策> <确认来源>

### Rejected Ideas
- <日期> <原始想法> <反驳理由> <替代方案>

### Completed Tasks
- TASK-001 done at <commit>

### Retro（每 N 提交或里程碑）
- 无效交付：
- 会话中断：
- 越界改动：
- 重复陷阱：
- 技能更新建议：