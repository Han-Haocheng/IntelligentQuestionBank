#!/usr/bin/env python3
"""range_check: 校验提交范围内的标题格式与受保护路径改动（L2 权威层）。

用法：
  python3 .agents/scripts/range_check.py [--range A..B] [--last N] [--json]

退出码：0 通过；1 违规；2 参数/范围非法。
"""
import argparse
import json
import re
import subprocess
import sys
sys.dont_write_bytecode = True
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import precision_common as pc

DEFAULT_COMMIT_TYPES = ["feat", "fix", "docs", "chore", "ci", "refactor", "style"]
DEFAULT_PROTECTED = [
    "CHANGELOG.md",
    "frontend/package.json",
    "frontend/package-lock.json",
    "backend/pom.xml",
    "qbank-web/pom.xml",
]
DEFAULT_RELEASE_WORKFLOWS = [
    ".github/workflows/beta-release.yml",
    ".github/workflows/formal-release.yml",
    ".github/workflows/release.yml",
]
DEFAULT_RANGE = "origin/dev..HEAD"

SUBJECT_RE = re.compile(r"^(?P<type>[a-z]+)(?:\((?P<scope>[^()]+)\))?: (?P<rest>.+)$")

# README 版本行（启动 jar 名）与 gate-config.json 的 version_checks 保持同一正则
README_PATH = "README.MD"
README_VERSION_RE = re.compile(r"qbank-[0-9][^\s\"\)]*\.jar")


def git(root, args):
    return subprocess.run(["git"] + args, cwd=str(root), text=True, capture_output=True)


def die(root, code, path, msg, as_json, command="range-check"):
    """参数/范围非法：按 gate.py 的行格式打印并返回退出码 2。"""
    if as_json:
        print(json.dumps(pc.envelope(command, False, None,
                                     [{"path": path, "code": code, "message": msg}]),
                         ensure_ascii=False, indent=2))
    else:
        print(f"{path}: {code}: {msg}", file=sys.stderr)
    return 2


def resolve_ref(root, ref):
    r = git(root, ["rev-parse", "--verify", "--quiet", ref + "^{commit}"])
    return r.returncode == 0


def parse_range(root, spec):
    """返回 (spec, sha_range) 或 (None, 错误信息)。"""
    if "..." in spec:
        return None, "范围写法不支持三点（...），请用 A..B"
    if ".." not in spec:
        return None, "范围必须形如 A..B"
    left, right = spec.split("..", 1)
    if not left or not right:
        return None, "范围两端都不能为空，必须形如 A..B"
    for ref in (left, right):
        if not resolve_ref(root, ref):
            return None, f"无法解析范围端点：{ref}"
    return spec, None


def collect_meta(root, rev_args):
    """一次 git log 取出 作者/邮箱/标题/Agent trailer。"""
    fmt = "%x1e%H%x1f%an%x1f%ae%x1f%s%x1f%(trailers:key=Agent,valueonly)"
    r = git(root, ["log", "--no-merges", "--format=" + fmt] + rev_args)
    if r.returncode != 0:
        return None, (r.stderr or r.stdout).strip()
    out = []
    for rec in r.stdout.split("\x1e"):
        rec = rec.strip("\n")
        if not rec:
            continue
        parts = rec.split("\x1f", 4)
        while len(parts) < 5:
            parts.append("")
        out.append({
            "sha": parts[0].strip(),
            "author": parts[1].strip(),
            "email": parts[2].strip(),
            "subject": parts[3].strip(),
            "agent_trailer": parts[4].strip(),
        })
    return out, None


def collect_files(root, rev_args):
    """一次 git log --name-only 取出每个提交触及的文件。"""
    r = git(root, ["log", "--no-merges", "--name-only", "--format=%x01%H"] + rev_args)
    if r.returncode != 0:
        return None, (r.stderr or r.stdout).strip()
    files, cur = {}, None
    for line in r.stdout.splitlines():
        if line.startswith("\x01"):
            cur = line[1:].strip()
            files.setdefault(cur, [])
        elif line.strip() and cur:
            files[cur].append(pc.norm(line))
    return files, None


def load_rules(cfg):
    types = [str(x).strip().lower() for x in (cfg.get("commit_types") or DEFAULT_COMMIT_TYPES)]
    types = [x for x in types if x] or list(DEFAULT_COMMIT_TYPES)
    prot = cfg.get("protected") or {}
    paths = [pc.norm(p) for p in (prot.get("paths") or DEFAULT_PROTECTED) if str(p).strip()]
    paths += [pc.norm(p) for p in (cfg.get("release_workflows") or DEFAULT_RELEASE_WORKFLOWS)
              if str(p).strip()]
    authors = {str(a).strip().lower() for a in (prot.get("release_authors") or ["github-actions[bot]"])}
    prefixes = [str(s).strip() for s in (prot.get("release_subject_prefix") or ["chore(release)"])]
    return types, paths, authors, prefixes


def is_release_author(commit, authors):
    return (commit["author"].lower() in authors) or (commit["email"].lower() in authors)


def is_release_subject(subject, prefixes):
    return any(subject.startswith(p) for p in prefixes if p)


def readme_version(root, rev):
    """取指定版本的 README.MD 启动 jar 名令牌；读不到返回空串。"""
    r = git(root, ["show", f"{rev}:{README_PATH}"])
    if r.returncode != 0:
        return ""
    m = README_VERSION_RE.search(r.stdout or "")
    return m.group(0) if m else ""


def main():
    ap = argparse.ArgumentParser(prog="range_check", description="提交范围校验（标题格式/受保护路径）")
    g = ap.add_mutually_exclusive_group()
    g.add_argument("--range", dest="range_spec", default=None, help="范围 A..B（默认 origin/dev..HEAD）")
    g.add_argument("--last", dest="last", type=int, default=None, help="HEAD 最近 N 个非 merge 提交")
    ap.add_argument("--json", action="store_true", help="JSON 输出")
    ap.add_argument("--root", default=None, help="仓库根（默认自动探测）")
    args = ap.parse_args()

    root = pc.repo_root(args.root)
    cfg = pc.load_config(root)
    types, protected, authors, prefixes = load_rules(cfg)

    if args.last is not None:
        if args.last < 1:
            return die(root, 2, "range", "--last N 的 N 必须 >= 1", args.json)
        if not resolve_ref(root, "HEAD"):
            return die(root, 2, "range", "无法解析 HEAD", args.json)
        rev_args = ["-n", str(args.last), "HEAD"]
        spec_label = f"HEAD~{args.last}"
    else:
        spec_label = args.range_spec or DEFAULT_RANGE
        resolved, err = parse_range(root, spec_label)
        if err:
            return die(root, 2, "range", err, args.json)
        spec_label = resolved
        rev_args = [spec_label]

    count_r = git(root, ["rev-list", "--no-merges", "--count"] + rev_args)
    if count_r.returncode != 0:
        return die(root, 2, "range", (count_r.stderr or count_r.stdout).strip() or "范围解析失败",
                   args.json)
    total = int((count_r.stdout or "0").strip() or "0")

    if total == 0:
        if args.json:
            print(json.dumps(pc.envelope("range-check", True, None, [], [],
                                         {"range": spec_label, "commits": 0,
                                          "message": "OK (empty range)"}),
                             ensure_ascii=False, indent=2))
        else:
            print("OK (empty range)")
        return 0

    commits, err = collect_meta(root, rev_args)
    if err:
        return die(root, 2, "range", f"读取提交失败：{err}", args.json)
    files_map, err = collect_files(root, rev_args)
    if err:
        return die(root, 2, "range", f"读取提交文件失败：{err}", args.json)

    errs, warns = [], []

    def e(path, code, msg):
        errs.append({"path": path, "code": code, "message": msg})

    def w(path, code, msg):
        warns.append({"path": path, "code": code, "message": msg})

    for c in commits:
        sha = c["sha"]
        short = sha[:7]
        subject = c["subject"]
        m = SUBJECT_RE.match(subject)
        bad = True
        if m and m.group("type") in types and m.group("rest").strip():
            bad = False
        if bad:
            e(short, "SUBJECT_FORMAT",
              f"提交标题不符合 Conventional Commits（类型需属于 {'/'.join(types)}）：{subject}")

        if not c["agent_trailer"]:
            w(short, "AGENT_TRAILER_MISSING", f"缺少 Agent: trailer：{subject}")

        whitelisted = is_release_author(c, authors) or is_release_subject(subject, prefixes)

        touched = files_map.get(sha) or []
        hit_protected = [p for p in touched if p in protected]
        if hit_protected and not whitelisted:
            for p in hit_protected:
                e(p, "PROTECTED_PATH",
                  f"受保护路径被非白名单提交改动（{short}：{subject}）；"
                  f"仅 {', '.join(sorted(authors))} 或标题前缀 "
                  f"{', '.join(prefixes)} 放行，人工发布走 .github/scripts/bump-version.sh")

        # README.MD 只拦版本行（启动 jar 名）的变化，正常文档改动放行
        if README_PATH in touched and not whitelisted:
            old_ver = readme_version(root, sha + "^")
            new_ver = readme_version(root, sha)
            if old_ver and new_ver and old_ver != new_ver:
                e(README_PATH, "README_VERSION_CHANGED",
                  f"README 启动 jar 名被非白名单提交改动（{short}：{subject}）："
                  f"{old_ver} -> {new_ver}；仅 {', '.join(sorted(authors))} 或标题前缀 "
                  f"{', '.join(prefixes)} 放行，人工发布走 .github/scripts/bump-version.sh")

    if args.json:
        print(json.dumps(pc.envelope("range-check", not errs, None, errs, warns,
                                     {"range": spec_label, "commits": total}),
                         ensure_ascii=False, indent=2))
    else:
        for x in errs:
            print(f"{x['path']}: {x['code']}: {x['message']}", file=sys.stderr)
        for x in warns:
            print(f"WARN {x['code']}: {x['message']}", file=sys.stderr)
        if not errs:
            print(f"OK (checked {total} commits)")
    return 1 if errs else 0


if __name__ == "__main__":
    sys.exit(main())
