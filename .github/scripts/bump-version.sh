#!/usr/bin/env bash
# bump-version.sh <新版本> — 同步仓库全部版本声明
# 声明位置: frontend/package.json + package-lock.json (npm 原生),
#           backend/pom.xml, qbank-web/pom.xml (项目版本),
#           README.MD (后端启动 jar 名)
set -euo pipefail

NEW="${1:?用法: bump-version.sh <新版本>}"
if [[ ! "${NEW}" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.-]+)?$ ]]; then
  echo "::error::版本号格式非法: ${NEW}" >&2
  exit 1
fi

OLD="$(node -p "require('./frontend/package.json').version")"
echo "版本升级: ${OLD} -> ${NEW}"

# 1) frontend: package.json + package-lock.json (npm version 保持锁文件结构一致)
( cd frontend && npm version --no-git-tag-version "${NEW}" >/dev/null )

# 2) pom.xml: 项目版本 = 当前版本串的唯一出现处
for pom in backend/pom.xml qbank-web/pom.xml; do
  sed -i "s|<version>${OLD}</version>|<version>${NEW}</version>|" "${pom}"
done

# 3) README.MD: 后端启动 jar 名 (不论当前写的是哪个旧版本都替换)
sed -i -E "s|qbank-[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.-]+)?\.jar|qbank-${NEW}.jar|g" README.MD

echo "---- 待提交文件 ----"
git status --porcelain
