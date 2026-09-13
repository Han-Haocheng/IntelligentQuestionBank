# AGENTS.md — 本仓库 AI 协作纪律

> 约束作用于本仓库的所有 AI 代理（子代理 / Copilot / Cursor / Claude Code 等）。详细规则见技能 intelligent-question-bank-workflow 与 .github/RELEASE_AUTOMATION.md。

## 硬性规则（违反即事故）

1. **一切开发只在 dev**：本地无 main/beta（已删除）；远端 main/beta 仅由发布工作流合并/快进更新。
   禁止：push 任何分支、手动改版本号、打标签、force、切分支。
2. **提交必带 pathspec**：`git commit -m "..." -- <文件>`；禁止 `git add -A`；提交前 `git diff --cached --stat` 自检。
3. **禁止 npm install / 改依赖**（多代理共享环境，需人工决策）。
4. **不写 CHANGELOG.md 与 README「版本历史」**：由发布周期统一维护。
5. 版本号由发布工作流管理，人工操作必须用 `.github/scripts/bump-version.sh <版本>`（同步 5 处声明）。

## 验证门禁（全绿才提交）

- 前端：`(cd frontend && npm ci && npm run build)`
- 后端：`mvn -B -DskipTests package --file backend/pom.xml`
- 改 db/init.sql 需验证幂等（可重复导入）

## 版本声明 5 处

frontend/package.json · frontend/package-lock.json · backend/pom.xml · qbank-web/pom.xml · README.MD（启动 jar 名）

## 发布

- beta 自动：dev 自上次跟进 ≥10 提交 → `.github/workflows/beta-release.yml`（合并 dev→beta，prerelease）
- 正式版：手动派发 `.github/workflows/formal-release.yml`（快进 main，latest）
- 打包：`.github/workflows/release.yml`（v* 标签或手动填标签）

## 协作台账（多人多 AI 共享事实源）

- 源：`.agents/COLLAB_STATE.md`（入仓、随提交更新；仅协调者写入；只写仓库相对路径，禁止密钥/token）
- 脚本：提交前自检 `bash .agents/scripts/check_staged_files.sh <允许路径...>`；恢复前 `bash .agents/scripts/check_state.sh .agents/COLLAB_STATE.md`；一键心跳 `bash .agents/scripts/heartbeat_sync.sh [--push]`（更新心跳+提交台账；`--push` 需任务书预授权或当次确认，仅 fast-forward 推 dev）；一键盘点 `bash .agents/scripts/collab_status.sh`；对齐 `bash .agents/scripts/collab_sync.sh [--merge] [--push]`（`--push` 需授权）；台账冲突 union 合并 `bash .agents/scripts/ledger_union_merge.sh`
- 任务书预授权：任务书可登记「本任务周期内 fast-forward 推 dev」并经用户当次确认；周期内自动执行不再逐次确认；发布分支/打标签/其它 push 不在此列
- 防分叉硬步骤：写台账前必 `git fetch` 核对远端，push 前同样须 fetch 复核（技能通用 2.3 / 项目 2.2 固化）
- **工作环境声明**：`.dsh/`、`/.trae/`、`/.tools/` 等为本机工具链运行环境目录（DSH 等的会话快照/本地计划），各本机保留、一律不入 git；协作资产只以 `.agents/` 为仓库共享域。**删除任一忽略项前必须先确认该目录不被本机运行环境使用**
- 脚本需 bash 环境：Windows 协作者用 Git for Windows 自带 Git Bash（或 WSL）执行；cmd/PowerShell 无原生 bash，无法直接运行
- `.agents/heartbeat/` 为本地心跳，不入 git

## 并行协作细则（多协调者同仓并行）

- **并行 OK**：文件域互不重叠的任务可并行开发（任务书写明文件域边界，波次并行）；git 的分布合并模型天然支持多 clone 并行
- **共享热点单写**：`.agents/COLLAB_STATE.md` 等单一事实源同一时刻只允许一个协调者写入
- **台账写锁**：写台账前必 `git fetch` 并读远端台账 `ledger_lock`/`lock_until`；锁未过期（30 分钟）则不写（等待或只读）；写入时把锁更新为自己 +30 分钟，随台账提交；过期自动释放
- **低频更新**：台账只在里程碑/交接/发布点更新；日常状态用本地心跳 `.agents/heartbeat/<session>.json`
- **冲突兜底**：台账冲突一律按「保留双方全部条目」union 合并，不丢信息；无法自动合并时冻结上报用户
- **禁默认 rebase**：共享 dev + 多协调者场景禁止默认 rebase（历史重写、重放冲突、双方提交"换身份"）；rebase 仅限"独占分支、无并行写者、提交可重放"的例外且需当次确认；并行场景统一 merge + union

## 提交风格

Conventional Commits（`feat/fix/docs/chore/ci/refactor/style` + 中文说明），原子化提交。
