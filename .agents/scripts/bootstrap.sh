#!/usr/bin/env bash
# bootstrap.sh — 安装 git 钩子并做环境自检（幂等）
# 用法：bash .agents/scripts/bootstrap.sh [--check]
#   --check  只验证不写：hooksPath 已设、钩子可执行、平台入口一致；任一不满足 exit 1
set -u

CHECK_ONLY=0
for arg in "$@"; do
  case "$arg" in
    --check) CHECK_ONLY=1 ;;
    -h|--help)
      echo "usage: bash .agents/scripts/bootstrap.sh [--check]"
      exit 0
      ;;
    *)
      echo "[gate] 未知参数：$arg" >&2
      exit 2
      ;;
  esac
done

fail=0
say() { echo "[gate] $*"; }
warn() { echo "[gate] $*" >&2; }

root=$(git rev-parse --show-toplevel 2>/dev/null) || {
  warn "不在 git 仓库内，无法安装钩子"
  exit 1
}
cd "$root" || { warn "无法进入仓库根目录：$root"; exit 1; }
say "root: $root"
say "mode: $([ "$CHECK_ONLY" -eq 1 ] && echo check-only || echo install)"

# ---------- 依赖检查（git / bash / python3） ----------
for tool in git bash; do
  if command -v "$tool" >/dev/null 2>&1; then
    say "$tool: $("$tool" --version 2>&1 | head -1)"
  else
    warn "缺少 $tool（必需）"
    fail=1
  fi
done
if command -v python3 >/dev/null 2>&1; then
  say "python3: $(python3 --version 2>&1 | head -1)"
  has_python=1
else
  has_python=0
  warn "未找到 python3：平台入口同源校验与结构化门禁降级跳过（提交格式、受保护路径仍由 pre-commit 的 bash 检查兜底）"
fi

# ---------- core.hooksPath ----------
want_hooks=".githooks"
cur_hooks=$(git config --get core.hooksPath 2>/dev/null || true)
norm_hooks=$(printf '%s' "$cur_hooks" | sed 's#^\./##; s#/\$##')
if [ "$CHECK_ONLY" -eq 0 ]; then
  git config core.hooksPath "$want_hooks" || { warn "设置 core.hooksPath 失败"; fail=1; }
  cur_hooks=$(git config --get core.hooksPath 2>/dev/null || true)
  norm_hooks=$(printf '%s' "$cur_hooks" | sed 's#^\./##; s#/\$##')
fi
if [ "$norm_hooks" = "$want_hooks" ]; then
  say "core.hooksPath: $cur_hooks（已就位）"
else
  warn "core.hooksPath: $cur_hooks（期望 $want_hooks）"
  fail=1
fi

# ---------- 钩子可执行 ----------
if [ "$CHECK_ONLY" -eq 0 ]; then
  chmod +x .githooks/* 2>/dev/null || true
fi
if [ -d .githooks ]; then
  for f in .githooks/*; do
    [ -e "$f" ] || continue
    if [ -x "$f" ]; then
      say "hook: $f 可执行"
    else
      warn "hook: $f 不可执行"
      fail=1
    fi
  done
else
  warn "缺少 .githooks 目录"
  fail=1
fi

# ---------- 平台入口同源 ----------
pe=".agents/scripts/platform_entries.py"
pe_rc=0
if [ "$has_python" -eq 1 ] && [ -f "$pe" ]; then
  say "platform_entries: $pe check"
  if python3 "$pe" check; then
    say "platform_entries: ok"
  else
    pe_rc=$?
    warn "platform_entries: 不一致（exit $pe_rc），运行 python3 $pe generate 后提交"
    fail=1
  fi
elif [ ! -f "$pe" ]; then
  warn "未找到 $pe：平台入口同源校验降级跳过"
else
  warn "缺少 python3：平台入口同源校验降级跳过"
fi

if [ "$fail" -ne 0 ]; then
  warn "bootstrap: 存在问题（见上）"
  exit 1
fi
if [ "$CHECK_ONLY" -eq 1 ]; then
  say "OK"
fi
exit 0
