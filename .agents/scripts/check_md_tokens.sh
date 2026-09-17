#!/usr/bin/env bash
# MD3 令牌一致性子集校验
#   校验 frontend(Vue 版) 与 qbank-web(Thymeleaf 版) 共有的 --md-* 设计令牌是否逐位同值。
#   依据: docs/13-frontend-md3-审计与迁移方案.md 第 9 节（令牌单一事实源）；docs/12 §1「两边视觉一致」目标。
#
# 用法:
#   bash .agents/scripts/check_md_tokens.sh            # 校验（建议提交前与验收时各跑一次）
#   bash .agents/scripts/check_md_tokens.sh --quiet    # 仅输出 WARN/FAIL 与结论行
#   路径覆盖（供自测/他仓复用）: QBANK_FE_CSS=... QBANK_QW_CSS=... bash .agents/scripts/check_md_tokens.sh
#
# 退出码: 0 = 共有令牌全部一致; 1 = 存在值不一致(FAIL); 2 = 环境/用法错误
# 依赖: bash + awk + sort + comm（Windows 请在 Git for Windows 自带 Git Bash 或 WSL 中执行）
set -u

# sort/comm 必须用同一字节序排序域, 否则跨 locale 下 comm 会误判(实测 Windows Git Bash 触发)
export LC_ALL=C

QUIET=0
[ "${1:-}" = "--quiet" ] && QUIET=1

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit 2
repo_root=$(cd "$script_dir/../.." && pwd) || exit 2
cd "$repo_root" || exit 2

FE="${QBANK_FE_CSS:-frontend/src/styles.css}"
QW="${QBANK_QW_CSS:-qbank-web/src/main/resources/static/css/app.css}"

for f in "$FE" "$QW"; do
  if [ ! -f "$f" ]; then
    echo "[ERROR] 找不到令牌文件: $f" >&2
    exit 2
  fi
done

# 抽取 --md-*: value; 声明（同一令牌取首次声明；值小写化以便比较）
extract () {
  tr -d '\r' < "$1" | awk '
    {
      line = $0
      while (match(line, /--md-[a-z0-9-]+[ \t]*:[^;]*;/)) {
        s = substr(line, RSTART, RLENGTH)
        eq = index(s, ":")
        tok = substr(s, 1, eq - 1); gsub(/[ \t]/, "", tok)
        val = substr(s, eq + 1); sub(/;$/, "", val)
        gsub(/^[ \t]+|[ \t]+$/, "", val)
        val = tolower(val)
        if (!(tok in seen)) { seen[tok] = 1; printf "%s\t%s\n", tok, val }
        line = substr(line, RSTART + RLENGTH)
      }
    }'
}

tmp=$(mktemp -d) || exit 2
trap 'rm -rf "$tmp"' EXIT
extract "$FE" | sort > "$tmp/fe.tsv"
extract "$QW" | sort > "$tmp/qw.tsv"
cut -f1 "$tmp/fe.tsv" > "$tmp/fe.tokens"
cut -f1 "$tmp/qw.tsv" > "$tmp/qw.tokens"
comm -12 "$tmp/fe.tokens" "$tmp/qw.tokens" > "$tmp/common.tokens"
comm -23 "$tmp/fe.tokens" "$tmp/qw.tokens" > "$tmp/fe.only"
comm -13 "$tmp/fe.tokens" "$tmp/qw.tokens" > "$tmp/qw.only"

n_fe=$(wc -l < "$tmp/fe.tokens" | tr -d ' ')
n_qw=$(wc -l < "$tmp/qw.tokens" | tr -d ' ')
n_common=$(wc -l < "$tmp/common.tokens" | tr -d ' ')
n_fe_only=$(wc -l < "$tmp/fe.only" | tr -d ' ')
n_qw_only=$(wc -l < "$tmp/qw.only" | tr -d ' ')

fail=0
: > "$tmp/mismatch.txt"
while IFS= read -r tok; do
  [ -z "$tok" ] && continue
  v_fe=$(awk -F'\t' -v t="$tok" '$1 == t { print $2 }' "$tmp/fe.tsv")
  v_qw=$(awk -F'\t' -v t="$tok" '$1 == t { print $2 }' "$tmp/qw.tsv")
  if [ "$v_fe" != "$v_qw" ]; then
    printf '  %-28s 前端=%-22s qbank-web=%s\n' "$tok" "$v_fe" "$v_qw" >> "$tmp/mismatch.txt"
    fail=$((fail + 1))
  fi
done < "$tmp/common.tokens"

if [ "$QUIET" -eq 0 ]; then
  echo "[INFO] 前端令牌 $n_fe 个 / qbank-web 令牌 $n_qw 个；共有 $n_common 个"
fi

if [ "$n_fe_only" -gt 0 ]; then
  echo "[WARN] 仅前端定义 $n_fe_only 个（qbank-web 无需同步者忽略，需同步者列入 docs/13 P4）:"
  sed 's/^/       /' "$tmp/fe.only"
fi
if [ "$n_qw_only" -gt 0 ]; then
  echo "[WARN] 仅 qbank-web 定义 $n_qw_only 个（命名分歧或需回补前端，列入 docs/13 P4）:"
  sed 's/^/       /' "$tmp/qw.only"
fi

if [ "$fail" -gt 0 ]; then
  echo "[FAIL] 共有令牌中 $fail 个值不一致（以 $FE 为权威侧修正 $QW）:"
  cat "$tmp/mismatch.txt"
  exit 1
fi

echo "[OK] 共有 $n_common 个 --md-* 令牌取值一致"
exit 0
