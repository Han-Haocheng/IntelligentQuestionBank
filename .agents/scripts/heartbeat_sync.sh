#!/usr/bin/env bash
# .agents/scripts/heartbeat_sync.sh — 一键心跳 + 台账提交（可选推送）
# 用法: bash .agents/scripts/heartbeat_sync.sh [-m <提交消息>] [--session <id>] [--push]
# 纪律:
#   - 只 add 台账文件（pathspec），不做 git add -A，提交前自检
#   - --push 需任务书预授权或用户当次确认；仅 fast-forward 推送 dev；失败保留现场不重试
# 需 bash 环境：Windows 协作者用 Git for Windows 自带 Git Bash（或 WSL）执行
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
LEDGER="$ROOT/.agents/COLLAB_STATE.md"
HEARTBEAT_DIR="$ROOT/.agents/heartbeat"
MSG=""
SESSION=""
PUSH=0

usage() {
  echo "用法: $0 [-m <提交消息>] [--session <id>] [--push]" >&2
  exit 2
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    -m) MSG="${2:?}"; shift 2 ;;
    --session) SESSION="${2:?}"; shift 2 ;;
    --push) PUSH=1; shift ;;
    -h|--help) usage ;;
    *) usage ;;
  esac
done

# 1) 心跳文件：指定 --session 或更新最新一个；都没有则新建
hb_file=""
if [ -n "$SESSION" ]; then
  hb_file="$HEARTBEAT_DIR/$SESSION.json"
fi
if [ -z "$hb_file" ] || [ ! -f "$hb_file" ]; then
  latest="$(ls -t "$HEARTBEAT_DIR"/*.json 2>/dev/null | head -1 || true)"
  if [ -n "$latest" ]; then
    hb_file="$latest"
  fi
fi
if [ -z "$hb_file" ]; then
  hb_file="$HEARTBEAT_DIR/coord-$(date +%Y%m%d-%H%M).json"
fi
mkdir -p "$HEARTBEAT_DIR"

head_sha="$(git rev-parse --short HEAD)"
remote_sha=""
if git rev-parse --verify -q origin/dev >/dev/null 2>&1; then
  remote_sha="$(git rev-parse --short origin/dev)"
fi
now="$(date '+%Y-%m-%d %H:%M')"
epoch="$(date +%s)"

if [ -f "$hb_file" ]; then
  sed -i \
    -e "s|\"updated_at\": .*|\"updated_at\": \"$now\",|" \
    -e "s|\"heartbeat\": .*|\"heartbeat\": $epoch,|" \
    -e "s|\"head_local\": .*|\"head_local\": \"$head_sha\",|" \
    -e "s|\"head_remote\": .*|\"head_remote\": \"$remote_sha\",|" \
    "$hb_file"
else
  cat > "$hb_file" <<EOF
{
  "version": 1,
  "role": "coordinator",
  "session_id": "$(basename "$hb_file" .json)",
  "updated_at": "$now",
  "heartbeat": $epoch,
  "repo": "$(basename "$ROOT")",
  "branch": "dev",
  "head_local": "$head_sha",
  "head_remote": "$remote_sha",
  "push_status": "local",
  "status": "heartbeat",
  "last_action": "heartbeat_sync 一键心跳",
  "next_action": "",
  "note": "心跳文件仅本机/本地共享，不入 git；跨机对齐以 .agents/COLLAB_STATE.md 为准"
}
EOF
fi
echo "[OK] 心跳已更新: ${hb_file#$ROOT/}"

# 2) 台账提交（pathspec + 自检）
git add -- "$LEDGER"
bash "$ROOT/.agents/scripts/check_staged_files.sh" ".agents/COLLAB_STATE.md"
if [ -z "$MSG" ]; then
  MSG="chore(agents): 心跳同步（heartbeat_sync）"
fi
git commit -m "$MSG" -- ".agents/COLLAB_STATE.md"
echo "[OK] 台账已提交: $(git rev-parse --short HEAD)"

# 3) 可选推送（fast-forward 校验；授权由流程负责）
if [ "$PUSH" -eq 1 ]; then
  echo "[--push] 需任务书预授权或用户当次确认；仅 fast-forward 推送 dev"
  if ! git fetch origin 2>/dev/null; then
    echo "[FAIL] fetch 失败（网络/代理问题），未推送，保留现场；可尝试 git -c http.proxy= push origin dev" >&2
    exit 1
  fi
  if git merge-base --is-ancestor origin/dev HEAD; then
    git push origin dev
    echo "[OK] dev 已推送: $(git rev-parse --short origin/dev)"
  else
    echo "[FAIL] 非 fast-forward（远端有本地没有的提交），未推送；先 fetch 并按纪律确认处理" >&2
    exit 1
  fi
fi

echo "[OK] 心跳同步完成"