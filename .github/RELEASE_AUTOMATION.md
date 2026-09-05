# 发布自动化与分支规则

> 方案: GitHub Actions(行为自动化) + 规则集(纪律保护) 双层

## 机制总览

| 工作流 | 触发 | 行为 |
| --- | --- | --- |
| Beta Release (beta-release.yml) | push dev 自动 / Actions 手动 force | dev 自上次 beta 跟进累积 ≥10 提交 → 升版本(-beta.N+1; 正式版后按提交语义开新周期) → 提交 dev → 合并 dev→beta → 打 vX.Y.Z-beta.N 标签 → 派发 Release 打包发布(prerelease) |
| Formal Release (formal-release.yml) | Actions 手动 | 去掉 -beta.N 后缀定正式版号 → 升版本提交 dev → 快进更新 main → 打 vX.Y.Z 标签 → 派发 Release 打包发布(latest) |
| Release (release.yml, 已有) | push v* 标签 / 手动填标签 | 后端 jar + 浏览器版 + 三平台桌面安装包 + 发布说明(prerelease 判定已内置) |

- 阈值: `beta-release.yml` 的 `RELEASE_THRESHOLD`(默认 10); 里程碑发布用 Actions 页面的 force 开关, 不凑提交数
- 版本语义: `-beta.N` 就 N+1; 从正式版起新周期按提交语义定级(breaking→major / feat→minor / 其余→patch)再 -beta.1

## 启用步骤

1. 推送 dev 合并本目录文件(工作流与脚本随 dev 进入 beta/main 由发布流程自动带过去)
2. 首次正式发布前, main/beta 上还没有新工作流文件: 手动派发时在分支下拉里选 `dev`;
   此后 main/beta 已含工作流文件, 直接派发即可
3. 规则集(仓库 Settings → Rules → Rulesets → New ruleset → Branch):
   - 目标: `refs/heads/main` 与 `refs/heads/beta`
   - 开启: 禁止强制推送 (Block force pushes) + 禁止删除 (Restrict deletions)
   - 不建议开启"要求 PR 合并": CI 以合并方式直推更新; 若要 PR 保护, 必须给 CI 配 bypass
     (专用 PAT secret 或 github-actions[bot]), 否则 CI 推送会报 GH006
   - dev 不设规则(直推开发); 若也想防误操作可对 dev 加同样的禁强推/禁删

## 注意事项与已知约定

- GITHUB_TOKEN 的推送不会触发新的 workflow 事件, 所以 tag 推送后由工作流显式
  `gh workflow run` 派发 Release —— 整套流程不需要 PAT
- bump 提交推 dev 会再触发本工作流, 但此时 beta 已同步(计数=0)自动跳过, 不会死循环
- 发布提交自动改 5 处: frontend/package.json, frontend/package-lock.json,
  backend/pom.xml, qbank-web/pom.xml, README.MD(启动 jar 名)
- CHANGELOG 段、README「版本历史」高亮行仍由人工维护(Release 说明由 generate-release-notes.sh 自动生成)
- 正式版发布后 dev 版本号保持正式版值, 下一个 beta 周期由自动语义判定起步
