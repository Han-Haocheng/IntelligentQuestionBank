# TASK-001 任务书 — db/upgrade.sql 同步 MD3 紫默认主题

> 对应 Issue：#5 [bug] upgrade.sql 未同步 MD3 默认主题且 INSERT IGNORE 不覆盖
> 任务书类型：普通任务；确认后执行，范围内不再逐条确认；范围外动作回到用户当次确认

## 任务

- 目标：已部署库通过 upgrade.sql 升级后，默认主题（is_default=1）为 MD3 紫（#6750a4），与 init.sql 一致；且不覆盖管理员已改过的主题
- 工作目录：仓库根 `/mnt/d/Project/IntelligentQuestionBank`（dev 分支）
- 基线确认（`git log -1 --oneline`）：9c51b26 后续新提交（开工时以实际 HEAD 为准）
- 文件归属边界：仅 `db/upgrade.sql`（+ 必要时的 `db/init.sql` 一致性核对，只读）
- 影响域 / 接口边界：
  - `app_theme` 表种子数据 ↔ `frontend/src/stores/theme.js`（`isBaseline` 用 `primary === '#6750a4'` 判断 MD3 官方色板）
  - init.sql 的 MD3 紫 config（`db/init.sql:264-266`）必须与 upgrade.sql 刷新后的 config 完全一致
  - 幂等性：重复导入不报错、不重复覆盖管理员改动
- 技术要点预研结论（来自 issue #5 核查评论）：
  - 现状：`db/upgrade.sql:97-107` 用 `INSERT IGNORE`（默认蓝 `#409eff`），已存在记录永不更新
  - 建议：改为幂等"刷新种子"——仅当管理员未改动时覆盖，例如 `INSERT … ON DUPLICATE KEY UPDATE config=VALUES(config)` 对 `id` 固定种子行生效，或按 update_time 判断；先确认 `app_theme` 表是否有 update_time 列（无则用 ID 固定 + 明确注释"仅覆盖未改过的种子行"的折中，需在任务书上写明）

## 恢复核对（派工/恢复前必填）

- 当前 HEAD：以开工时 `git rev-parse HEAD` 为准
- 台账任务 id：TASK-001；owner_session：可开工时登记
- 工作区是否有未归属改动：开工前 `git status --short` 核对
- 远端 / CI 是否已变化：开工前 `bash .agents/scripts/collab_status.sh` 核对分叉

## 验证命令（引用项目契约条目）

- 契约条目：db 段 —— 改 `db/init.sql`/`upgrade.sql` 必须验证幂等（重复导入不报错）
- 本任务至少必跑：
  1. `bash -n` 不适用（SQL）；对 upgrade.sql 做语法级人工核查（无 lint/typecheck，构建不覆盖 SQL）
  2. 幂等验证：对 upgrade.sql 提取的 app_theme 语句在本地/测试库执行两次，第二次不报错且不覆盖管理员改动（如无可用库，用 sqlite/mysql 容器或向协调者说明验证方式）
  3. `grep` 核对 init.sql 与 upgrade.sql 的 MD3 紫 config 完全一致
- 其余契约门禁（前端 build / 后端 mvn）本任务不涉及（仅改 SQL 种子），但改动提交前跑一次 db 幂等检查

## 提交规范

- 提交类型：`fix(db)` + 中文说明，原子化单提交
- 不写 CHANGELOG / README 版本历史（项目纪律）
- 提交命令：`git commit -m "fix(db): upgrade.sql 同步 MD3 紫默认主题并幂等刷新种子" -- db/upgrade.sql`；提交前 `bash .agents/scripts/check_staged_files.sh db/upgrade.sql` 自检

## 禁止项

- push / 切分支 / fetch（协调者统一处理推送）
- `git add -A`、修改归属外文件
- 安装依赖 / 使用契约外工具
- 改版本声明、改 CI/发布工作流、写 CHANGELOG
- 失败后盲目重试：门禁变红/脚本失败保留现场上报协调者
- 底部行：如发现任务书不合理必须反驳协调者

## 停止条件

- 幂等验证失败 / 与 init.sql 不一致 / 出现不可预期行为：保留现场，停止并上报协调者

## 完成报告格式

- 改动清单（upgrade.sql 具体语句）
- 行为说明（刷新策略：何时覆盖、何时保留）
- 验证摘要（幂等两次导入结果、与 init.sql 一致性 grep 结果）
- 提交 hash
- 影响域 / 接口兼容性验证摘要（theme.js isBaseline 逻辑不受影响）
- 归属外文件（如有，必须单独说明）