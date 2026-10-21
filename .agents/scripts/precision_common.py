#!/usr/bin/env python3
"""Shared helpers for the optional precision layer (task/doc/gate)."""
import json
import os
import re
import subprocess
import sys
import tempfile
from datetime import datetime
from pathlib import Path

def repo_root(start=None):
    env = os.environ.get("TASK_ROOT")
    if env:
        return Path(env).resolve()
    p = Path(start or os.getcwd()).resolve()
    try:
        out = subprocess.check_output(
            ["git", "-C", str(p), "rev-parse", "--show-toplevel"],
            text=True, stderr=subprocess.DEVNULL
        ).strip()
        return Path(out)
    except Exception:
        return p

def now():
    env = os.environ.get("TASK_NOW")
    if env:
        try:
            return datetime.fromisoformat(env)
        except ValueError:
            pass
    return datetime.now().astimezone()

def iso(dt):
    return dt.strftime("%Y-%m-%dT%H:%M:%S%z")

def load_json(path, default=None):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        return default
    except json.JSONDecodeError as exc:
        raise SystemExit(f"[FAIL] JSON 解析失败 {path}: {exc}")

def write_json(path, obj):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp = tempfile.mkstemp(prefix=".tmp-", dir=str(path.parent))
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as f:
            json.dump(obj, f, ensure_ascii=False, indent=2)
            f.write("\n")
        os.replace(tmp, path)
    finally:
        if os.path.exists(tmp):
            os.unlink(tmp)

def norm(p):
    p = str(p).replace("\\", "/").strip()
    while p.startswith("./"):
        p = p[2:]
    return p.rstrip("/")

def under(path, prefix):
    path, prefix = norm(path), norm(prefix)
    return path == prefix or path.startswith(prefix + "/")

def overlap(a, b):
    return under(a, b) or under(b, a)

def load_config(root):
    cfg = load_json(Path(root) / ".agents" / "gate-config.json", {}) or {}
    cfg.setdefault("task_dir", ".agents/tasks")
    cfg.setdefault("doc_standard", ".agents/doc-standard.json")
    cfg.setdefault("sync", {})
    cfg.setdefault("version_checks", [])
    cfg.setdefault("scopes", [])
    cfg.setdefault("allowed_domains", [])
    cfg.setdefault("build", {})
    return cfg

def task_dir(root, cfg):
    return Path(root) / cfg["task_dir"]

def task_path(root, cfg, tid):
    return task_dir(root, cfg) / (tid + ".json")

def load_task(root, cfg, tid):
    return load_json(task_path(root, cfg, tid))

def all_tasks(root, cfg):
    out = []
    d = task_dir(root, cfg)
    if d.is_dir():
        for p in sorted(d.glob("T-*.json")):
            t = load_json(p)
            if isinstance(t, dict):
                out.append(t)
    return out

def task_id_from_env(root, cfg):
    tid = os.environ.get("GATE_TASK_ID")
    if tid:
        return tid.strip()
    cur = Path(root) / cfg["task_dir"] / ".current"
    if cur.is_file():
        return cur.read_text(encoding="utf-8").strip()
    return None

def task_errors(t, cfg, others=None):
    errs = []
    def e(path, code, msg):
        errs.append({"path": path, "code": code, "message": msg})

    required = ["schema", "id", "title", "status", "owner", "created_at",
                "updated_at", "baseline", "workdir", "goal", "scope",
                "interfaces", "invariants", "acceptance", "forbidden",
                "stop_conditions", "preflight"]
    for k in required:
        if k not in t:
            e(k, "REQUIRED", "缺少必填字段")

    if t.get("schema") != 1:
        e("schema", "SCHEMA", "schema 必须为 1")
    if not re.match(r"^T-\d{8}-\d{2}$", str(t.get("id", ""))):
        e("id", "ID_FORMAT", "id 应形如 T-YYYYMMDD-NN")
    if t.get("status") not in ("draft", "active", "blocked", "done", "archived"):
        e("status", "STATUS", "非法状态")

    scope = t.get("scope") or {}
    allow = scope.get("allow") or []
    deny = scope.get("deny") or []
    if not allow:
        e("scope.allow", "EMPTY", "scope.allow 不能为空")
    for p in allow + deny:
        if not isinstance(p, str) or p.startswith("/") or ".." in Path(p).parts:
            e("scope", "PATH", "路径必须相对且不含 ..")

    if t.get("status") in ("active", "done") and not t.get("baseline"):
        e("baseline", "BASELINE", "active/done 需要 baseline")

    pf = t.get("preflight") or {}
    if not pf.get("understanding"):
        e("preflight.understanding", "PREFLIGHT", "缺少理解")
    if t.get("status") == "active" and pf.get("resolution") == "pending":
        e("preflight.resolution", "PREFLIGHT_PENDING", "active 任务不得有未解决异议")
    if pf.get("objections") and pf.get("resolution") not in ("resolved",):
        e("preflight.resolution", "PREFLIGHT_UNRESOLVED", "有异议但未解决")

    acc = t.get("acceptance") or []
    if not acc:
        e("acceptance", "EMPTY", "至少一条验收")
    for i, a in enumerate(acc):
        cmd = a.get("cmd") if isinstance(a, dict) else None
        exp = a.get("expect") if isinstance(a, dict) else None
        if not cmd:
            e(f"acceptance[{i}].cmd", "REQUIRED", "缺少 cmd")
        if not exp:
            e(f"acceptance[{i}].expect", "REQUIRED", "缺少 expect")
        elif not (exp == "exit 0" or exp == "empty" or re.match(r"^>= \d+$", str(exp))):
            e(f"acceptance[{i}].expect", "EXPECT", "expect 只能是 exit 0、empty、>= N")

    if len(t.get("invariants") or []) > 5:
        e("invariants", "TOO_MANY", "不变量最多 5 条")
    notes = t.get("notes") or ""
    if len(notes.splitlines()) > 20 or len(notes) > 2000:
        e("notes", "TOO_LONG", "notes 超限")

    if t.get("status") == "done":
        if not t.get("commits"):
            e("commits", "DONE_WITHOUT_COMMIT", "done 需要至少一个提交")
        cps = t.get("checkpoints") or []
        if not any(isinstance(c, dict) and c.get("gate") == "pass" for c in cps):
            e("checkpoints", "DONE_WITHOUT_GATE", "done 需要至少一个 pass 检查点")

    declared = set(t.get("sync") or [])
    for key, rule in (cfg.get("sync") or {}).items():
        matches = rule.get("match") or []
        if any(any(under(p, m) for m in matches) for p in allow):
            if key not in declared:
                e("sync", "SYNC_MISSING", f"scope 命中 {key}，但 sync 未声明")

    if t.get("status") == "active" and others:
        for o in others:
            if o.get("id") == t.get("id") or o.get("status") != "active":
                continue
            oa = (o.get("scope") or {}).get("allow") or []
            for a in allow:
                for b in oa:
                    if overlap(a, b):
                        e("scope", "SCOPE_OVERLAP",
                          f"与 {o.get('id')} 的 scope 重叠：{a} / {b}")
    return errs

def envelope(command, ok, tid=None, errors=None, warnings=None, data=None):
    out = {"ok": ok, "command": command}
    if tid:
        out["id"] = tid
    out["errors"] = errors or []
    out["warnings"] = warnings or []
    if data is not None:
        out["data"] = data
    return out

def print_errors(errs):
    for x in errs:
        print(f"{x['path']}: {x['code']}: {x['message']}", file=sys.stderr)
