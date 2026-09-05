#!/usr/bin/env bash
# 计算下一个 beta 版本号并输出到 stdout
# 规则:
#   当前 X.Y.Z-beta.N            -> X.Y.Z-beta.(N+1)
#   当前为正式版 X.Y.Z           -> 分析自上个 v* 标签以来的提交语义定级:
#        BREAKING CHANGE / feat! -> (X+1).0.0-beta.1
#        feat                    -> X.(Y+1).0-beta.1
#        其余                    -> X.Y.(Z+1)-beta.1
# 环境变量 CUR 可覆盖当前版本(测试用)
set -euo pipefail

CUR="${CUR:-$(node -p "require('./frontend/package.json').version")}"

if [[ "${CUR}" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)-beta\.([0-9]+)$ ]]; then
  echo "${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.${BASH_REMATCH[3]}-beta.$((BASH_REMATCH[4] + 1))"
  exit 0
fi
if [[ ! "${CUR}" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  echo "::error::无法识别当前版本: ${CUR}" >&2
  exit 1
fi
MAJ="${BASH_REMATCH[1]}"
MIN="${BASH_REMATCH[2]}"
PAT="${BASH_REMATCH[3]}"

BASE_TAG="$(git tag --list 'v*' --sort=-version:refname | head -1 || true)"
if [ -z "${BASE_TAG}" ]; then
  BASE_TAG="$(git rev-list --max-parents=0 HEAD)"
fi
RANGE="${BASE_TAG}..HEAD"

SUBJ="$(git log --format='%s' "${RANGE}" 2>/dev/null || true)"
BODY="$(git log --format='%B' "${RANGE}" 2>/dev/null || true)"

LEVEL="patch"
if printf '%s\n' "${BODY}" | grep -qE '^(BREAKING CHANGE:|[A-Za-z]+!:)'; then
  LEVEL="major"
elif printf '%s\n' "${SUBJ}" | grep -qE '^feat(\(|:|!|\s)'; then
  LEVEL="minor"
fi

case "${LEVEL}" in
  major) MAJ=$((MAJ + 1)); MIN=0; PAT=0 ;;
  minor) MIN=$((MIN + 1)); PAT=0 ;;
  *)     PAT=$((PAT + 1)) ;;
esac
echo "${MAJ}.${MIN}.${PAT}-beta.1"
