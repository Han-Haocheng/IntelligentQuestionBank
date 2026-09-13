#!/usr/bin/env bash
# .agents/scripts/collab_sync.sh — 一键对齐：fetch / 分叉检测 / 可选集成与推送
# 用法: bash .agents/scripts/collab_sync.sh [--merge] [--push]
# 授权: --push 需任务书预授权或用户当次确认；仅 fast-forward；push 前再次 fetch 复核（硬步骤）
# 失败保留现场；不自动绕代理、不自动跳过 LFS 锁（仅提示）
# 需 bash 环境：Windows 协作者用 Git for Windows 自带 Git Bash（或 WSL）执行
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

MERGE=0
PUSH=0
for a in "$@"; do
  case "$a" in
    --merge) MERGE=1 ;;
    --push) PUSH=1 ;;
    *) echo "未知参数: $a（支持 --merge --push）" >&2; exit 2 ;;
  esac
done

echo "[1/3] fetch 核对远端..."
if ! git fetch origin 2>/dev/null; then
  echo "[FAIL] fetch 失败（网络/代理问题）。检查代理 127.0.0.1:7890；或直连试：git -c http.proxy= fetch origin" >&2
  exit 1
fi

if ! git rev-parse --verify -q origin/dev >/dev/null 2>&1; then
  echo "[FAIL] 无 origin/dev 引用" >&2
  exit 1
fi

read -r left right < <(git rev-list --left-right --count origin/dev...HEAD)
echo "[分叉] 远端独有 ${left} / 本地独有 ${right}"

if [ "$left" -gt 0 ]; then
  echo "[新增] 远端有本地没有的提交："
  git log --oneline HEAD..origin/dev | head -10
  if [ "$MERGE" -eq 1 ]; then
    echo "[2/3] 集成 origin/dev..."
    if git merge origin/dev --no-edit 2>/dev/null; then
      echo "[merge] 集成成功"
    else
      if grep -q '^<<<<<<<' .agents/COLLAB_STATE.md 2>/dev/null; then
        echo "[merge] 台账冲突：运行 bash .agents/scripts/ledger_union_merge.sh 保留双方全部条目后提交" >&2
      fi
      echo "[FAIL] merge 未完成。保留现场，不要重复 merge；人工处理后继续" >&2
      exit 1
    fi
  else
    echo "[提示] 未集成（可用 --merge 自动集成，或人工处理）；不集成则不可推送"
  fi
else
  echo "[OK] 无远端新增"
fi

if [ "$PUSH" -eq 1 ]; then
  echo "[3/3] 推送（需任务书预授权或用户当次确认；仅 fast-forward）..."
  if ! git fetch origin 2>/dev/null; then
    echo "[FAIL] push 前复核 fetch 失败，未推送" >&2
    exit 1
  fi
  if ! git merge-base --is-ancestor origin/dev HEAD; then
    echo "[FAIL] 推送瞬间又出现新分叉，本次未推送；请重新 --merge" >&2
    exit 1
  fi
  lfs_hits="$(git diff --name-only origin/dev...HEAD | grep -E '\.(pptx|ppt|docx|xlsx|pdf|zip)$' || true)"
  if [ -n "$lfs_hits" ]; then
    echo "[WARN] 推送内容含可能走 LFS 的文件：${lfs_hits}" >&2
  fi
  if ! git push origin dev > /tmp/collab_push.log 2>&1; then
    tail -3 /tmp/collab_push.log >&2
    if grep -q "lfs/locks" /tmp/collab_push.log 2>/dev/null; then
      echo "[提示] 疑似 LFS 锁校验网络问题。若本次推送不含新 LFS 文件，可对该次推送用一次性参数：git -c http.proxy= -c \"lfs.https://github.com/Han-Haocheng/IntelligentQuestionBank.git/info/lfs.locksverify=false\" push origin dev" >&2
    fi
    echo "[FAIL] 推送失败，保留现场" >&2
    exit 1
  fi
  tail -2 /tmp/collab_push.log
  echo "[OK] 推送完成: origin/dev = $(git rev-parse --short origin/dev)"
else
  echo "[待办] 未推送；需要时以 --push 重跑（如已获授权）"
fi

echo "[OK] sync 完成"