# AGENTS.md — AI 协作强制约定（唯一事实源）

> 适用：本仓库全部 AI 代理与成员（DSH / Trae / Claude Code / Copilot / Cursor / Cline / Gemini 等）。
> 本文件是规则唯一事实源。各平台入口文件由 `.agents/scripts/platform_entries.py` 从下方 `RULES` 块渲染，**禁止手写正文**；未登记即出现的入口文件由 CI 判红。
> 修改规则：只改 `RULES` 块 → `generate` → `check` 通过 → 提交。

<!-- RULES:BEGIN -->
## 硬性约定（所有平台、所有成员，违反即事故）

1. **先对齐**：实质请求先输出「理解 / 异议 / 动作」。方案有错误、风险或会累积技术债时必须反驳并给替代方案；不可逆、敏感、跨契约、需求歧义 → 停下等确认。
2. **只在 dev**：本地唯一开发分支；`main` / `beta` 只由发布工作流以合并或快进更新。禁止 push、切分支、打标签、force、rebase（协调者持预授权时的 fast-forward 推 dev 除外）。
3. **提交带 pathspec**：`git commit -m "..." -- <文件>`；禁止 `git add -A`；误暂存用 `git restore --staged <文件>` 摘出，不动工作树；禁止 `--no-verify`。
4. **禁改清单**：版本声明文件（见「版本与发布」）、`CHANGELOG.md`、依赖与锁文件、`.github/workflows/` 发布三件（beta / formal / release）、他人未提交改动。人工发布走 `.github/scripts/bump-version.sh`。
5. **提交前必跑**：`python3 .agents/scripts/platform_entries.py check`；有结构化任务时加 `python3 .agents/scripts/task check <任务id>` 与 `python3 .agents/scripts/gate.py check`；推送前 `python3 .agents/scripts/gate.py push`（仅协调者）。
6. **写域互斥**：同一文件域同一时刻只允许一个写者，动手前声明写域；全量构建与门禁由协调者统一执行。
7. **台账单写**：`.agents/COLLAB_STATE.md` 只由持锁协调者写；写前 `git fetch` 读 `ledger_lock` / `lock_until`；锁未过期只读；冲突按 union 保留双方全部条目。
8. **越界即停**：方案偏离、门禁变红、契约冲突、无归属改动 → 保留现场上报，不盲目重试，不自行扩大范围。
<!-- RULES:END -->

## 平台入口

| 项 | 约定 |
|---|---|
| 规则源 | 本文件 `RULES` 块，唯一可编辑处 |
| 生成 | `python3 .agents/scripts/platform_entries.py generate` |
| 校验 | `python3 .agents/scripts/platform_entries.py check`：0 通过 / 1 漂移或未登记 / 2 配置错误；CI 与 pre-commit 均执行 |
| 新平台接入 | 成员在 `.agents/platforms.json` 登记后生成；仓库内出现未登记的已知入口文件即判红 |
| 本机专用入口 | `install --local`（如 `.trae/rules/project_rules.md`），写入本机，不入 git |
| 能力差异 | 任务板、多代理编排等仅部分平台具备；任一强制约定不得依赖平台独有能力，共同下限 = git + bash + python3 |

平台入口不预置：用哪个平台由该成员自行登记并生成，不替他人决定文件形态。

## 验证门禁（全绿才提交；缺项须显式写「本项目无此项」）

- 前端：`(cd frontend && npm ci && npm run build)`
- 后端：`mvn -B -DskipTests package --file backend/pom.xml`
- qbank-web：`mvn -B -DskipTests package --file qbank-web/pom.xml`
- 样式令牌：`bash .agents/scripts/check_md_tokens.sh`
- DB：改 `db/init.sql` 需验证可重复导入（幂等）

## 门槛阶梯

| 层 | 位置 | 强度 | 内容 |
|---|---|---|---|
| L1 | `.githooks/` | 提前发现 | `bash .agents/scripts/bootstrap.sh` 安装 `core.hooksPath`；无任务上下文也检查提交格式、受保护路径、平台入口同源 |
| L2 | `.github/workflows/discipline.yml` | 权威，不可绕过 | 提交范围校验（格式 / 禁改文件 / 白名单）、平台入口同源、任务与门禁自检 |
| L3 | 协调者验收 | 兜底 | 复查提交内容、全仓复跑门禁、检查影响域接口兼容 |

版本放行白名单：作者 `github-actions[bot]` 或标题前缀 `chore(release)`；其余提交触及版本声明或 `CHANGELOG.md` 一律由 L2 阻断。

## 版本与发布

- 版本声明：`frontend/package.json`、`frontend/package-lock.json`、`backend/pom.xml`、`qbank-web/pom.xml`、`README.MD`（启动 jar 名）
- beta 自动：dev 自上次跟进 ≥10 提交 → `.github/workflows/beta-release.yml`；正式版：手动派发 `.github/workflows/formal-release.yml`；打包：`.github/workflows/release.yml`

## 协作台账与脚本

- 台账 `.agents/COLLAB_STATE.md`：Active ≤30 行、Session Log 留 5 条；超出部分移入 `.agents/COLLAB_STATE.archive.md`，条目不删除
- `bootstrap.sh` 装钩子与环境自检；`check_staged_files.sh` 暂存区归属自检；`check_state.sh` 恢复前体检
- `collab_status.sh` 盘点；`collab_sync.sh [--merge] [--push]` 对齐；`heartbeat_sync.sh [--push]` 心跳；`ledger_union_merge.sh` union 合并
- 心跳文件 `.agents/heartbeat/<session>.json`（本地，不入 git）

## 并行协作

- 并行前提：文件域两两不相交、任务书写明边界、协调者统一提交
- 共享热点单写：`AGENTS.md`、`COLLAB_STATE.md`、`gate-config.json`、`platforms.json`、发布相关文件
- 多 clone：push 前必 `git fetch` 复核；禁止默认 rebase；冲突用 merge + union；无归属改动先冻结上报

## 工作环境

- `.dsh/`、`.dsh-ops/`、`.trae/`、`.tools/`、`.agents/heartbeat/` 为本机运行环境，不入 git；删除前确认无本机进程使用
- Windows 成员用 Git for Windows 的 Git Bash 或 WSL 执行 bash / python3 脚本；cmd 与 PowerShell 无原生 bash

## 提交风格

- Conventional Commits：`feat|fix|docs|chore|ci|refactor|style` + 可选范围 + 中文说明
- body 带 `Agent: <平台>/<模型>`；结构化任务带 `Task: T-YYYYMMDD-NN`
- 原子提交：一个可独立验证的步骤一个提交
