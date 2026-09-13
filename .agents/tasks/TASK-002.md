# TASK-002 任务书 — qbank-web 前端资源本地化（去 BootCDN 依赖）

> 对应 Issue：#14 [enhancement] 前端资源全依赖 BootCDN，且无版本化与本地兜底
> 任务书类型：普通任务；确认后执行，范围内不再逐条确认；范围外动作回到用户当次确认

## 任务

- 目标：qbank-web 引用的 3 处 BootCDN 外部资源改为**本地 vendor**（固定版本、随包分发），不再依赖外网；页面离线/外网故障时样式与交互不失效
- 工作目录：仓库根 `/mnt/d/Project/IntelligentQuestionBank`（dev 分支）
- 基线确认（`git log -1 --oneline`）：e7c6d59 后续新提交（开工时以实际 HEAD 为准）
- 文件归属边界：
  - `qbank-web/src/main/resources/static/vendor/**`（新增：bootstrap 5.3.3 css/js、bootstrap-icons 1.11.3 css/fonts）
  - `qbank-web/src/main/resources/templates/fragments/layout.html`（3 处引用改本地）
- 影响域 / 接口边界：
  - layout.html 是全部 14 个页面的公共片段（布局/导航/脚本）——改引用后所有页面生效
  - Bootstrap/Bootstrap-Icons 版本**保持不变**（5.3.3 / 1.11.3），仅改来源，避免样式回归
  - bootstrap-icons css 内部以相对路径引用字体文件（fonts/），下载时必须**同步字体文件**，否则图标空白
- 技术要点预研结论：
  - 现状：`layout.html:17/19/78` 三处 `cdn.bootcdn.net`（bootstrap.min.css、bootstrap-icons.min.css、bootstrap.bundle.min.js）
  - 方案：`static/vendor/bootstrap-5.3.3/{css,js}` 与 `static/vendor/bootstrap-icons-1.11.3/{css,fonts}`，layout.html 用 `th:href="@{/vendor/...}"`
  - 下载源顺序：bootcdn 原源 → jsdelivr 备源；**下载失败不重试超过 1 次，停止并上报协调者（网络问题）**

## 恢复核对（派工/恢复前必填）

- 当前 HEAD：以开工时 `git rev-parse HEAD` 为准
- 台账任务 id：TASK-002；owner_session：可开工时登记
- 工作区是否有未归属改动：开工前 `git status --short` 核对
- 远端 / CI 是否已变化：开工前 `bash .agents/scripts/collab_status.sh` 核对

## 验证命令（引用项目契约条目）

- 契约条目：改 qbank-web 用 maven 命令；本任务涉及 qbank-web
- 本任务至少必跑：
  1. `mvn -B -DskipTests package --file qbank-web/pom.xml`（构建通过 = 资源路径不破坏打包）
  2. 资源存在性：`find qbank-web/src/main/resources/static/vendor -type f | wc -l` ≥ 5 且各文件 >0 字节（js/css/woff2）
  3. `grep -rn "bootcdn\|cdn.bootcdn" qbank-web/src/main/resources` 应返回**零命中**
  4. 模板引用路径与 `@{/vendor/...}` 一致性核对（layout.html 3 处）
- 前端（frontend/）构建不涉及；后端 mvn 不涉及

## 提交规范

- 提交类型：`fix(qbank-web)` 或 `chore(qbank-web)` + 中文说明；vendor 资源与 layout.html 改动**同一提交**（资源+引用不可分）
- 不写 CHANGELOG / README 版本历史（项目纪律）
- 提交命令示例：`git commit -m "fix(qbank-web): 前端资源本地化——bootstrap 5.3.3/bootstrap-icons 1.11.3 入 vendor，去 BootCDN 外网依赖" -- qbank-web/src/main/resources/static/vendor qbank-web/src/main/resources/templates/fragments/layout.html`；提交前 `bash .agents/scripts/check_staged_files.sh qbank-web/src/main/resources/...` 自检

## 禁止项

- push / 切分支 / fetch（协调者统一处理推送）
- `git add -A`、修改归属外文件（尤其不得改其它页面的公共样式/脚本）
- 安装依赖 / 使用契约外工具（vendor 为静态资源直下，不引入包管理器）
- 改版本声明（bootcdn 版本不算版本声明 5 处，但需在 vendor 目录名/注释固定版本）
- 下载失败盲目重试（超过 1 次即停止上报）
- 发现任务书不合理必须反驳协调者

## 停止条件

- 下载源全部不可达（网络）：保留现场停止，上报协调者（可改期或换源）
- mvn 构建失败 / 模板引用错误：保留现场上报

## 完成报告格式

- 改动清单（vendor 文件、layout.html diff 摘要）
- 行为说明（版本、来源、字体文件同步情况）
- 验证摘要（mvn 结果、bootcdn 零命中、文件数/字节数）
- 提交 hash
- 影响域 / 接口兼容性验证摘要（14 页面共享片段无回归；图标字体可用）
- 归属外文件（如有，必须单独说明）