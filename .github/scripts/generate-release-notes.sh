#!/usr/bin/env bash
# 生成 GitHub Release 发布说明
# 用法: TAG=v1.0.2 bash generate-release-notes.sh  (输出 release-notes.md)
# 级别判定与范围:
#   patch: 上一标签 -> 当前标签, 展示全部提交
#   minor: 上一 minor 基线(如 v1.0.0) -> 当前, 展示核心提交(feat/fix/perf/refactor)
#   major: 上一 major 基线(如 v1.0.0) -> 当前, 突出架构/结构性变化
set -euo pipefail

CUR="${TAG:?需要 TAG 环境变量, 如 v1.0.2}"
OUT="${NOTES_FILE:-release-notes.md}"

# ---- 版本解析 ----
VER="${CUR#v}"
MAJ="${VER%%.*}"
REST="${VER#*.}"
MIN="${REST%%.*}"
PAT="${REST#*.}"

# ---- 标签收集(版本倒序, 排除当前) ----
TAGS=$(git tag --list 'v*' --sort=-version:refname | grep -v "^${CUR}$" || true)
PREV=$(echo "$TAGS" | head -1)

# ---- 级别判定 ----
LEVEL="first"
if [ -n "$PREV" ]; then
  PVER="${PREV#v}"
  PMAJ="${PVER%%.*}"
  PREST="${PVER#*.}"
  PMIN="${PREST%%.*}"
  if [ "$MAJ" -gt "$PMAJ" ]; then
    LEVEL="major"
  elif [ "$MIN" -gt "$PMIN" ]; then
    LEVEL="minor"
  else
    LEVEL="patch"
  fi
fi

# ---- 基线选择 ----
pick_base() {
  local want="$1"
  echo "$TAGS" | while read -r t; do
    [ -z "$t" ] && continue
    local v m mi p
    v="${t#v}"; m="${v%%.*}"; r="${v#*.}"; mi="${r%%.*}"; p="${r#*.}"
    if [ "$want" = "major" ]; then
      [ "$m" -lt "$MAJ" ] && [ "$mi" -eq 0 ] && [ "$p" -eq 0 ] && { echo "$t"; break; }
    elif [ "$want" = "minor" ]; then
      [ "$m" -eq "$MAJ" ] && [ "$mi" -eq $((MIN - 1)) ] && [ "$p" -eq 0 ] && { echo "$t"; break; }
    fi
  done | head -1
}

BASE=""
case "$LEVEL" in
  major) BASE=$(pick_base major) ;;
  minor) BASE=$(pick_base minor) ;;
  patch) BASE="$PREV" ;;
esac
[ -z "$BASE" ] && BASE="$PREV"

# ---- 收集提交 ----
ALL=""
N=0
if [ -n "$BASE" ] && git rev-parse --verify "$CUR" >/dev/null 2>&1; then
  ALL=$(git log --oneline --no-merges "$BASE..$CUR" 2>/dev/null || true)
  [ -n "$ALL" ] && N=$(printf '%s\n' "$ALL" | grep -c . || true)
fi
RANGE_INFO="首个版本"
[ -n "$BASE" ] && RANGE_INFO="${BASE} → ${CUR} · 共 ${N:-0} 个提交"

# ---- 组装 Markdown ----
{
  echo "# ${CUR} 发布说明"
  echo ""
  echo "> ${RANGE_INFO} · 发布级别: ${LEVEL}"
  echo ""
  if [ "$LEVEL" = "major" ] && [ -n "$ALL" ]; then
    echo "## 🏗️ 架构/结构性变化"
    arch=$(printf '%s\n' "$ALL" | while read -r h rest; do
      [ -z "$h" ] && continue
      kw=$(echo "$rest" | grep -icE '架构|重构|模块|权限|统一|合并|迁移|升级|体系|接口|DB|数据库|表|界面|路由' || true)
      dirs=$(git show --name-only --format= "$h" 2>/dev/null | sed 's#/.*##' | sort -u | wc -l)
      if [ "$kw" -gt 0 ] || [ "$dirs" -ge 3 ]; then
        echo "- \`${h:0:7}\` ${rest}"
      fi
    done)
    if [ -n "$arch" ]; then echo "$arch"; else echo "- (无显著架构变更)"; fi
    echo ""
  fi
  if [ -n "$ALL" ]; then
    for entry in "feat:✨ 新功能" "fix:🐛 修复" "perf:⚡ 性能优化" "refactor:🔧 重构"; do
      type="${entry%%:*}"
      title="${entry#*:}"
      list=$(printf '%s\n' "$ALL" | grep -E " ${type}(\\(|:| )" || true)
      if [ -n "$list" ]; then
        echo "## ${title}"
        echo "$list"
        echo ""
      fi
    done
    others=$(printf '%s\n' "$ALL" | grep -vE " (feat|fix|perf|refactor)(\\(|:| )" || true)
    if [ -n "$others" ]; then
      echo "## 📄 其他(文档/CI/维护)"
      echo "$others"
    fi
  else
    echo "_无可用提交记录_"
  fi
} > "$OUT"
echo "release notes written to $OUT"
