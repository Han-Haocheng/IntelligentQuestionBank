#!/usr/bin/env bash
# .agents/scripts/check_state.sh — 恢复前做协作台账健康检查
# 用法: bash .agents/scripts/check_state.sh .agents/COLLAB_STATE.md
# 入仓副本（多人/多 AI 环境通用，敏感扫描用 grep -E，不依赖 rg）；源模板: ai-collaborative-dev-workflow 技能 scripts/
set -euo pipefail

usage() {
  echo "Usage: $0 <COLLAB_STATE.md 路径>" >&2
  exit 2
}

state_path=${1:-}
[ -n "$state_path" ] || usage
[ -f "$state_path" ] || {
  echo "[FAIL] 状态文件不存在: $state_path" >&2
  exit 1
}

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "[FAIL] 当前不在 git 工作树内，无法做双源校验" >&2
  exit 1
fi

size=$(wc -c < "$state_path")
if [ "$size" -gt 65536 ]; then
  echo "[WARN] 状态文件过大（${size} bytes），建议先把 Active 内容归档到 Archive"
fi

state_version=$(awk '/^state_version:/{print $2; exit}' "$state_path")
updated_at=$(awk '/^updated_at:/{sub(/^updated_at: */, ""); print; exit}' "$state_path")
baseline=$(awk '/^baseline:/{print $2; exit}' "$state_path")

missing=0
if [ -z "$state_version" ]; then
  echo "[FAIL] 台账缺少 state_version，先按重建协议用 git 历史/reflog/最近任务书重建" >&2
  missing=1
fi
if [ -z "$updated_at" ]; then
  echo "[FAIL] 台账缺少 updated_at，先按重建协议补齐时间戳" >&2
  missing=1
fi
if [ -z "$baseline" ]; then
  echo "[FAIL] 台账缺少 baseline，先按重建协议用 git log 重建" >&2
  missing=1
fi
if [ "$missing" -ne 0 ]; then
  exit 1
fi

if [ -n "$baseline" ]; then
  head_sha=$(git rev-parse HEAD 2>/dev/null || true)
  if [ -n "$head_sha" ] && [ "$baseline" != "$head_sha" ]; then
    echo "[WARN] 状态基线 $baseline != 当前 HEAD $head_sha，续作前先核对仓库状态"
  fi
fi

if grep -nEi '^[[:space:]]*(api[_-]?key|secret|password|token)[[:space:]]*[:=]' "$state_path" >/dev/null 2>&1; then
  echo "[FAIL] 状态文件疑似包含敏感信息，请改为“见项目配置”" >&2
  exit 1
fi

echo "[OK] 状态文件基础检查通过"