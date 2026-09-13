#!/usr/bin/env bash
# .agents/scripts/ledger_union_merge.sh — 台账冲突 union 合并（保留双方全部条目）
# 用法: bash .agents/scripts/ledger_union_merge.sh
# 仅处理 .agents/COLLAB_STATE.md；其余文件冲突不自动处理（保留现场人工解决）
# 合并后需人工通读去重（可能产生语义重复条目）再 git add + commit
# 需 bash 环境：Windows 协作者用 Git for Windows 自带 Git Bash（或 WSL）执行
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"
F=".agents/COLLAB_STATE.md"

if [ ! -f "$F" ]; then
  echo "[FAIL] 台账文件不存在: $F" >&2
  exit 1
fi

if ! grep -q '^<<<<<<<' "$F"; then
  echo "[OK] 无冲突标记，无需合并"
  exit 0
fi

echo "[提示] 处理前请确认：写锁为空或归你所有（见 AGENTS.md 并行细则），且无其他协调者正在写台账"
conflicts=$(grep -c '^<<<<<<<' "$F")
echo "[info] 检测到 ${conflicts} 个冲突块，执行 union 合并（保留双方全部条目：ours 段 + theirs 段）"

sed -E '/^<{7}/d; /^={7}/d; /^>{7}/d' "$F" > "$F.union"

if grep -q '^<<<<<<<' "$F.union"; then
  echo "[FAIL] 合并结果仍含冲突标记，停止（人工处理，不要覆盖）" >&2
  rm -f "$F.union"
  exit 1
fi

mv "$F.union" "$F"
echo "[OK] 已生成 union 版本（冲突块 = ours 段 + theirs 段）"
echo "[下一步] 通读核对（重点：重复条目去重、时间顺序、updated_at），确认后执行："
echo "  git add .agents/COLLAB_STATE.md && git commit --no-edit"
echo "[纪律] 保留双方全部条目为底线；语义重复（如同名 Decision/Session Log）人工去重后再提交"