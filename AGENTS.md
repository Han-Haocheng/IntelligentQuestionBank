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

## 提交风格

Conventional Commits（`feat/fix/docs/chore/ci/refactor/style` + 中文说明），原子化提交。
