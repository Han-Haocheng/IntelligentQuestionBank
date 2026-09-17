#!/usr/bin/env bash
# .agents/scripts/collab_status.sh — 一键恢复盘点（恢复协议第一步）
# 用法: bash .agents/scripts/collab_status.sh
# 输出: 分支/HEAD/远端/分叉计数/台账健康与写锁/心跳过期/未归属改动
# 需 bash 环境：Windows 协作者用 Git for Windows 自带 Git Bash（或 WSL）执行
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

echo "===== 协作状态盘点 $(date '+%Y-%m-%d %H:%M') ====="

branch="$(git branch --show-current)"
echo "[分支] ${branch} @ $(git rev-parse --short HEAD)"
git log -1 --format="[最新] %h %s"

if git rev-parse --verify -q origin/dev >/dev/null 2>&1; then
  echo "[远端] origin/dev = $(git rev-parse --short origin/dev)"
else
  echo "[远端] 无 origin/dev（先 git fetch origin）"
fi

if git rev-parse --verify -q origin/dev >/dev/null 2>&1; then
  read -r left right < <(git rev-list --left-right --count origin/dev...HEAD 2>/dev/null || echo "? ?")
  echo "[分叉] 远端独有 ${left} / 本地独有 ${right} （0 0 = 完全对齐）"
fi

if [ -f .agents/COLLAB_STATE.md ]; then
  bash .agents/scripts/check_state.sh .agents/COLLAB_STATE.md >/dev/null 2>&1 && echo "[台账] check_state 通过" || echo "[台账] check_state 未通过（见上条/需重建）"
  # 字段值可能带中文说明（如 "coord-x（本机协调者；30 分钟过期）"），只取主体与会话名/数值前缀
  lock="$(awk '/^ledger_lock:/{ n=$2; sub(/（.*$/, "", n); sub(/\(.*$/, "", n); print n; exit }' .agents/COLLAB_STATE.md)"
  case "$lock" in ""|"（空）"|null) lock="" ;; esac
  until_epoch="$(awk '/^lock_until:/{ if (match($2, /^[0-9]+/)) print substr($2, 1, RLENGTH); exit }' .agents/COLLAB_STATE.md)"
  if [ -n "$until_epoch" ] && [ "${until_epoch}" -gt "$(date +%s)" ] 2>/dev/null; then
    hour="$(date -d "@${until_epoch}" '+%H:%M' 2>/dev/null || echo "$until_epoch")"
    echo "[台账锁] 被 ${lock} 持有，至 ${hour} 过期"
  else
    echo "[台账锁] 空闲${lock:+（${lock} 已过期）}"
  fi
else
  echo "[台账] 缺失 .agents/COLLAB_STATE.md"
fi

if ls .agents/heartbeat/*.json >/dev/null 2>&1; then
  echo "[心跳]"
  for f in .agents/heartbeat/*.json; do
    age_min=$(( ($(date +%s) - $(stat -c %Y "$f")) / 60 ))
    flag=""; [ "$age_min" -gt 30 ] && flag="（已过期）"
    echo "   - $(basename "$f")：${age_min} 分钟前${flag}"
  done
else
  echo "[心跳] 无"
fi

if [ -n "$(git status --porcelain)" ]; then
  echo "[工作树] 未归属改动："
  git status --short
else
  echo "[工作树] 干净"
fi
echo "===== 盘点完 ====="