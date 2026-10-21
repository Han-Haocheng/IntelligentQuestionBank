#!/usr/bin/env python3
"""gate: precision-layer checks for staged changes, push and environment."""
import argparse
import json
import os
import re
import subprocess
import sys
sys.dont_write_bytecode = True
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import precision_common as pc
import doc_check

def git(root, args, check=False):
    return subprocess.run(["git"] + args, cwd=str(root), text=True,
                          capture_output=True, check=check)

def staged_files(root):
    r = git(root, ["diff", "--cached", "--name-only"])
    return [x for x in r.stdout.splitlines() if x.strip()]

def version_check(root, cfg):
    errs, vals = [], {}
    for spec in cfg.get("version_checks") or []:
        name = spec.get("name") or spec.get("file")
        rel = spec.get("file")
        pattern = spec.get("pattern")
        p = Path(root) / rel
        if not p.is_file():
            errs.append({"path": rel, "code": "VERSION_FILE_MISSING", "message": "版本文件不存在"})
            continue
        text = p.read_text(encoding="utf-8")
        m = re.search(pattern, text, re.S)
        if not m:
            errs.append({"path": rel, "code": "VERSION_NOT_FOUND", "message": "未匹配到版本号"})
            continue
        vals[name] = m.group(1)
    if len(set(vals.values())) > 1:
        detail = ", ".join(f"{k}={v}" for k, v in vals.items())
        errs.append({"path": "version", "code": "VERSION_MISMATCH", "message": detail})
    return errs, vals

def doc_type_for(path):
    low = path.lower()
    if "incident" in low:
        return "incident"
    if "release" in low:
        return "release"
    if "task" in low:
        return "task"
    return "design"

def check(root, as_json=False):
    cfg = pc.load_config(root)
    errs, warns = [], []
    def e(path, code, msg):
        errs.append({"path": path, "code": code, "message": msg})
    def w(path, code, msg):
        warns.append({"path": path, "code": code, "message": msg})

    tid = pc.task_id_from_env(root, cfg)
    task = None
    if not tid:
        e("task", "TASK_ID_MISSING", "设置 GATE_TASK_ID 或 .agents/tasks/.current")
    else:
        task = pc.load_task(root, cfg, tid)
        if not task:
            e("task", "TASK_NOT_FOUND", f"任务不存在：{tid}")
        else:
            if task.get("status") != "active":
                e("status", "TASK_NOT_ACTIVE", f"任务状态是 {task.get('status')}，提交前必须 active")
            for x in pc.task_errors(task, cfg, pc.all_tasks(root, cfg)):
                errs.append(x)

    staged = staged_files(root)
    if not staged:
        e("staged", "EMPTY", "暂存区为空")

    if task:
        allow = (task.get("scope") or {}).get("allow") or []
        deny = (task.get("scope") or {}).get("deny") or []
        for f in staged:
            if not any(pc.under(f, a) for a in allow):
                e(f, "SCOPE_OUTSIDE", "文件不在 scope.allow 内")
            if any(pc.under(f, d) for d in deny):
                e(f, "SCOPE_DENY", "文件命中 scope.deny")

    for f in staged:
        if f.endswith(".md") and (f.startswith("docs/") or "/docs/" in f):
            de, dw = doc_check.validate(root / f, doc_type_for(f), root, cfg)
            errs.extend(de)
            warns.extend(dw)

    declared = set((task or {}).get("sync") or [])
    for key in declared:
        rule = (cfg.get("sync") or {}).get(key) or {}
        chk = rule.get("check")
        if chk == "version" or key == "readme":
            ve, _ = version_check(root, cfg)
            errs.extend([x for x in ve if x not in errs])
        elif rule.get("command"):
            r = subprocess.run(rule["command"], cwd=str(root), text=True,
                               capture_output=True)
            if r.returncode != 0:
                e(key, "SYNC_COMMAND_FAILED",
                  (r.stdout + r.stderr).strip()[-500:] or "同步检查命令失败")
        elif key in ("schema", "api"):
            w(key, "SYNC_MANUAL", f"{key} 同步需要人工/契约检查")
        else:
            w(key, "SYNC_UNKNOWN", f"sync 规则未定义：{key}")

    if as_json:
        print(json.dumps(pc.envelope("gate-check", not errs, tid, errs, warns), ensure_ascii=False, indent=2))
    else:
        for x in errs:
            print(f"{x['path']}: {x['code']}: {x['message']}", file=sys.stderr)
        for x in warns:
            print(f"WARN {x['code']}: {x['message']}", file=sys.stderr)
        if not errs:
            print("OK")
    return 1 if errs else 0

def push_check(root):
    errs = []
    branch = git(root, ["rev-parse", "--abbrev-ref", "HEAD"]).stdout.strip()
    if branch != "dev":
        errs.append({"path": "branch", "code": "NOT_DEV", "message": f"当前分支是 {branch}"})
    fetch = git(root, ["fetch", "--quiet", "origin"])
    if fetch.returncode != 0:
        errs.append({"path": "fetch", "code": "FETCH_FAILED", "message": fetch.stderr.strip()})
    else:
        anc = git(root, ["merge-base", "--is-ancestor", "origin/dev", "HEAD"])
        if anc.returncode != 0:
            errs.append({"path": "ff", "code": "NOT_FAST_FORWARD", "message": "origin/dev 不是 HEAD 的祖先"})
    tags = git(root, ["tag", "--points-at", "HEAD"]).stdout.strip()
    if tags:
        errs.append({"path": "tag", "code": "TAG_ON_HEAD", "message": tags})
    dirty = git(root, ["status", "--porcelain"]).stdout.strip()
    if dirty:
        errs.append({"path": "worktree", "code": "DIRTY", "message": "工作区不干净"})
    for x in errs:
        print(f"{x['path']}: {x['code']}: {x['message']}", file=sys.stderr)
    if not errs:
        print("OK")
    return 1 if errs else 0

def doctor(root):
    cfg = pc.load_config(root)
    print(f"root: {root}")
    print(f"python: {sys.version.split()[0]}")
    print(f"config: {(root / '.agents' / 'gate-config.json')}")
    print(f"task_dir: {pc.task_dir(root, cfg)}")
    hooks = git(root, ["config", "--get", "core.hooksPath"]).stdout.strip()
    print(f"core.hooksPath: {hooks or '(unset)'}")
    print(f"tasks: {len(pc.all_tasks(root, cfg))}")
    return 0

def build(root, scope):
    cfg = pc.load_config(root)
    item = (cfg.get("build") or {}).get(scope)
    if not item:
        print(f"[FAIL] 未配置 build scope: {scope}", file=sys.stderr)
        return 2
    cmd = item.get("cmd") or []
    cwd = root / item.get("cwd", ".")
    print("+ " + " ".join(cmd) + f"  (cwd={cwd})")
    return subprocess.call(cmd, cwd=str(cwd))

def main():
    argv = sys.argv[1:]
    if argv and argv[0] == "commit":
        rest = argv[1:]
        root = pc.repo_root()
        rc = check(root)
        if rc != 0:
            return rc
        if "--check-only" in rest:
            return 0
        return subprocess.call(["git", "commit"] + rest, cwd=str(root))

    ap = argparse.ArgumentParser(prog="gate")
    ap.add_argument("--root", default=None)
    sub = ap.add_subparsers(dest="cmd", required=True)
    c = sub.add_parser("check")
    c.add_argument("--json", action="store_true")
    p = sub.add_parser("push")
    p.add_argument("--dry-run", action="store_true")
    sub.add_parser("doctor")
    b = sub.add_parser("build")
    b.add_argument("--scope", required=True)
    r = sub.add_parser("release")
    r.add_argument("--dry-run", action="store_true")
    args = ap.parse_args(argv)
    root = pc.repo_root(args.root)
    if args.cmd == "check":
        return check(root, args.json)
    if args.cmd == "push":
        return push_check(root)
    if args.cmd == "doctor":
        return doctor(root)
    if args.cmd == "build":
        return build(root, args.scope)
    if args.cmd == "release":
        print("[FAIL] 本地不发布；请走工作流。", file=sys.stderr)
        return 2
    return 2

if __name__ == "__main__":
    sys.exit(main())
