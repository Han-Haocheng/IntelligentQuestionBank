#!/usr/bin/env bash
# .agents/scripts/check_staged_files.sh — 提交前核对暂存区是否全部位于允许路径内
# 用法: bash .agents/scripts/check_staged_files.sh <精确路径|目录/>...
# 入仓副本（多人/多 AI 环境通用）；源模板: ai-collaborative-dev-workflow 技能 scripts/
set -euo pipefail

usage() {
  echo "Usage: $0 <精确路径|目录/>..." >&2
  echo "检查暂存区是否全部位于允许的路径/目录内。" >&2
  exit 2
}

[ "$#" -gt 0 ] || usage

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "[FAIL] 当前不在 git 工作树内" >&2
  exit 1
fi

mapfile -t staged < <(git diff --cached --name-only)
if [ "${#staged[@]}" -eq 0 ]; then
  echo "[FAIL] 暂存区为空（先逐文件 git add）" >&2
  exit 1
fi

unexpected=()
for file in "${staged[@]}"; do
  allowed=0
  for prefix in "$@"; do
    prefix=${prefix%/}
    case "$file" in
      "$prefix" | "$prefix/"*)
        allowed=1
        ;;
    esac
  done
  if [ "$allowed" -eq 0 ]; then
    unexpected+=("$file")
  fi
done

if [ "${#unexpected[@]}" -gt 0 ]; then
  echo "[FAIL] 暂存区包含归属外文件：" >&2
  printf '  %s\n' "${unexpected[@]}" >&2
  echo "--- git diff --cached --stat ---" >&2
  git diff --cached --stat >&2
  exit 1
fi

echo "[OK] 暂存区全部在允许清单内"
git diff --cached --stat