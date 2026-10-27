#!/usr/bin/env python3
"""platform_entries: 从 AGENTS.md 的 RULES 块渲染各 agent 平台入口文件。

单一事实源 = AGENTS.md 的 <!-- RULES:BEGIN --> .. <!-- RULES:END --> 块。
本脚本只读该块并按 .agents/platforms.json 的登记渲染目标文件，正文禁止手写；
仓库不预置平台入口，由使用方 add 登记后 generate。

退出码（CI 与 pre-commit 依赖）：0 一致 / 1 漂移·缺失·未登记 / 2 配置错误。
"""
import argparse
import json
import os
import re
import subprocess
import sys
from pathlib import Path

sys.dont_write_bytecode = True

CONFIG_REL = ".agents/platforms.json"
SKIP_DIRS = {".git", "node_modules", "target", "dist"}
FORMATS = ("md", "mdc")
SCOPES = ("repo", "local")
DEFAULT_DISPLAY = {
    "claude": "Claude Code",
    "gemini": "Gemini CLI",
    "conventions": "通用 CONVENTIONS",
    "copilot": "GitHub Copilot",
    "cursor": "Cursor",
    "cline": "Cline",
    "windsurf": "Windsurf",
    "trae": "Trae",
}

class ConfigError(Exception):
    """platforms.json 结构/取值错误 → check 退出 2。"""

class BlockError(Exception):
    """RULES 块标记缺失或内容为空 → check 退出 2。"""

def repo_root(start=None):
    p = Path(start or os.getcwd()).resolve()
    try:
        out = subprocess.check_output(
            ["git", "-C", str(p), "rev-parse", "--show-toplevel"],
            text=True, stderr=subprocess.DEVNULL
        ).strip()
        return Path(out)
    except Exception:
        return p

def norm(p):
    p = str(p).replace("\\", "/").strip()
    while p.startswith("./"):
        p = p[2:]
    return p.rstrip("/")

def glob_regex(pat):
    """把仓库相对 glob 编译成正则：** 跨目录，* / ? 不跨 /。"""
    pat, out, i = norm(pat), [], 0
    while i < len(pat):
        c = pat[i]
        if c == "*" and pat[i:i + 2] == "**":
            out.append(".*")
            i += 2
            if pat[i:i + 1] == "/":
                i += 1
            continue
        if c == "*":
            out.append("[^/]*")
        elif c == "?":
            out.append("[^/]")
        else:
            out.append(re.escape(c))
        i += 1
    return re.compile("^" + "".join(out) + "$")

def config_path(root):
    return Path(root) / CONFIG_REL

def load_config(root):
    path = config_path(root)
    if not path.is_file():
        raise ConfigError("配置文件不存在：%s" % CONFIG_REL)
    try:
        cfg = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ConfigError("JSON 解析失败 %s: %s" % (CONFIG_REL, exc))
    except UnicodeDecodeError as exc:
        raise ConfigError("配置文件非 UTF-8：%s" % exc)
    if not isinstance(cfg, dict):
        raise ConfigError("根节点必须是对象")
    for key in ("rules_file", "rules_begin", "rules_end", "header_note", "footer_note"):
        if not isinstance(cfg.get(key), str) or not cfg[key].strip():
            raise ConfigError("缺少非空字符串字段：%s" % key)
    targets = cfg.get("targets")
    if targets is None:
        targets = cfg["targets"] = []
    if not isinstance(targets, list):
        raise ConfigError("targets 必须是数组")
    known = cfg.get("known_platforms")
    if known is None:
        known = cfg["known_platforms"] = []
    if not isinstance(known, list):
        raise ConfigError("known_platforms 必须是数组")
    seen = set()
    for idx, t in enumerate(targets):
        where = "targets[%d]" % idx
        if not isinstance(t, dict):
            raise ConfigError("%s 必须是对象" % where)
        plat, path, scope = t.get("platform"), t.get("path"), t.get("scope", "repo")
        fmt = t.get("format", "md")
        if not isinstance(plat, str) or not plat.strip():
            raise ConfigError("%s 缺少 platform" % where)
        if not isinstance(path, str) or not path.strip():
            raise ConfigError("%s 缺少 path" % where)
        if scope not in SCOPES:
            raise ConfigError("%s scope 非法：%s" % (where, scope))
        if fmt not in FORMATS:
            raise ConfigError("%s format 非法：%s" % (where, fmt))
        t["platform"], t["path"], t["scope"], t["format"] = plat.strip(), norm(path), scope, fmt
        if Path(t["path"]).is_absolute():
            if scope != "local":
                raise ConfigError("%s repo scope 不允许绝对路径：%s" % (where, t["path"]))
        elif ".." in t["path"].split("/"):
            raise ConfigError("%s 路径不得越出仓库根：%s" % (where, t["path"]))
        if t["path"] in seen:
            raise ConfigError("%s 路径重复登记：%s" % (where, t["path"]))
        seen.add(t["path"])
    for idx, k in enumerate(known):
        if not isinstance(k, dict) or not isinstance(k.get("globs"), list):
            raise ConfigError("known_platforms[%d] 必须含 globs 数组" % idx)
        for gpat in k["globs"]:
            if not isinstance(gpat, str) or not gpat.strip():
                raise ConfigError("known_platforms[%d] glob 非法" % idx)
    return cfg

def rules_block(root, cfg):
    """抽取 RULES 块原文（含 BEGIN/END 标记行），输入按 LF 归一。"""
    path = Path(root) / cfg["rules_file"]
    if not path.is_file():
        raise BlockError("规则文件不存在：%s" % cfg["rules_file"])
    try:
        text = path.read_bytes().decode("utf-8")
    except UnicodeDecodeError as exc:
        raise BlockError("规则文件非 UTF-8：%s" % exc)
    lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    begin, end = cfg["rules_begin"].strip(), cfg["rules_end"].strip()
    bi = next((i for i, l in enumerate(lines) if l.strip() == begin), None)
    if bi is None:
        raise BlockError("缺少开始标记 %s" % begin)
    ei = next((i for i, l in enumerate(lines[bi + 1:], bi + 1) if l.strip() == end), None)
    if ei is None:
        raise BlockError("缺少结束标记 %s" % end)
    body = "\n".join(lines[bi + 1:ei])
    if not body.strip():
        raise BlockError("RULES 块为空")
    return "\n".join(lines[bi:ei + 1])

def display_for(cfg, platform):
    for k in cfg.get("known_platforms") or []:
        if norm(k.get("platform")) == platform and k.get("display"):
            return str(k["display"])
    return DEFAULT_DISPLAY.get(platform, platform)

def render(cfg, block, target):
    """渲染目标文件全文；md = 注释头 + 标题 + RULES 块 + 引用尾。"""
    plat = target["platform"]
    name = display_for(cfg, plat)
    lines = []
    if target["format"] == "mdc":
        lines.extend([
            "---",
            "description: %s 协作约定（由 AGENTS.md 的 RULES 块生成，禁止手改）" % name,
            'globs: ["**/*"]',
            "alwaysApply: true",
            "---",
            "",
        ])
    lines.extend([
        "<!-- %s -->" % cfg["header_note"].strip(),
        "# %s 协作约定 — 智能题库仓库" % name,
        "",
        block,
        "",
        "> %s" % cfg["footer_note"].strip(),
        "",
    ])
    return "\n".join(lines)

def target_file(root, target):
    p = Path(target["path"])
    return p if p.is_absolute() else Path(root) / p

def write_if_changed(path, content):
    """UTF-8、LF、无行尾转换；内容相同则不写（幂等）。"""
    data = content.encode("utf-8")
    try:
        if path.is_file() and path.read_bytes() == data:
            return False
    except OSError:
        pass
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_name(path.name + ".tmp-platform-entries")
    try:
        with open(tmp, "wb") as f:
            f.write(data)
        os.replace(tmp, path)
    finally:
        if tmp.exists():
            try:
                tmp.unlink()
            except OSError:
                pass
    return True

def save_config(root, cfg):
    path = config_path(root)
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_name(path.name + ".tmp-platform-entries")
    try:
        with open(tmp, "w", encoding="utf-8", newline="\n") as f:
            json.dump(cfg, f, ensure_ascii=False, indent=2)
            f.write("\n")
        os.replace(tmp, path)
    finally:
        if tmp.exists():
            try:
                tmp.unlink()
            except OSError:
                pass

def cmd_list(root, cfg):
    for t in cfg["targets"]:
        print("%s\t%s\tscope=%s\tformat=%s" % (t["platform"], t["path"], t["scope"], t["format"]))
    return 0

def cmd_generate(root, cfg, dry_run=False):
    block = rules_block(root, cfg)
    for t in cfg["targets"]:
        if t["scope"] != "repo":
            continue
        content = render(cfg, block, t)
        path = target_file(root, t)
        if dry_run:
            data = content.encode("utf-8")
            same = path.is_file() and path.read_bytes() == data
            if not same:
                print("WOULD_WRITE %s" % t["path"])
            continue
        if write_if_changed(path, content):
            print("WRITE %s" % t["path"])
    return 0

def scan_unmanaged(root, cfg):
    registered = {t["path"] for t in cfg["targets"]}
    patterns = []
    for k in cfg.get("known_platforms") or []:
        if k.get("local_only"):
            continue
        for gpat in k["globs"]:
            patterns.append(glob_regex(norm(gpat)))
    hits, root = [], Path(root)
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = sorted(d for d in dirnames if d not in SKIP_DIRS)
        for fn in sorted(filenames):
            rel = Path(dirpath, fn).relative_to(root).as_posix()
            if rel in registered:
                continue
            if any(rx.match(rel) for rx in patterns):
                hits.append(rel)
    return sorted(hits)

def cmd_check(root, cfg):
    block = rules_block(root, cfg)
    problems = []
    for t in cfg["targets"]:
        if t["scope"] != "repo":
            continue
        path, content = target_file(root, t), render(cfg, block, t)
        if not path.is_file():
            problems.append("MISSING %s" % t["path"])
        elif path.read_bytes() != content.encode("utf-8"):
            problems.append("DRIFT %s: 与 AGENTS.md RULES 块渲染结果不一致" % t["path"])
    for rel in scan_unmanaged(root, cfg):
        problems.append("UNMANAGED %s: 已知平台入口未在 platforms.json 登记" % rel)
    for line in problems:
        print(line)
    if problems:
        return 1
    print("OK")
    return 0

def cmd_install(root, cfg, use_local):
    if not use_local:
        print("install 需要 --local（写入本机专用入口）", file=sys.stderr)
        return 2
    block = rules_block(root, cfg)
    rest = [t for t in cfg["targets"] if t["scope"] == "local"]
    for t in rest:
        path, content = target_file(root, t), render(cfg, block, t)
        changed = write_if_changed(path, content)
        print("%s %s" % ("LOCAL_WRITE" if changed else "LOCAL_SKIP", t["path"]))
    return 0

def infer_format(cfg, platform, path):
    for k in cfg.get("known_platforms") or []:
        if str(k.get("platform") or "").strip() != platform:
            continue
        for gpat in k.get("globs") or []:
            if glob_regex(norm(gpat)).match(path):
                fmt = k.get("format")
                return fmt if fmt in FORMATS else ("mdc" if path.endswith(".mdc") else "md")
    return "mdc" if path.endswith(".mdc") else "md"

def cmd_add(root, cfg, platform, path, scope, fmt):
    platform, path = platform.strip(), norm(path)
    if not platform:
        print("ADD 需要 --platform", file=sys.stderr)
        return 2
    if not path:
        print("ADD 需要 --path", file=sys.stderr)
        return 2
    if scope not in SCOPES:
        print("scope 非法：%s" % scope, file=sys.stderr)
        return 2
    if fmt and fmt not in FORMATS:
        print("format 非法：%s" % fmt, file=sys.stderr)
        return 2
    is_abs = Path(path).is_absolute()
    if is_abs and scope != "local":
        print("repo scope 不允许绝对路径：%s" % path, file=sys.stderr)
        return 2
    if not is_abs and ".." in path.split("/"):
        print("路径不得越出仓库根：%s" % path, file=sys.stderr)
        return 2
    for t in cfg["targets"]:
        if t["path"] == path:
            print("DUPLICATE %s: 已由平台 %s 登记" % (path, t["platform"]), file=sys.stderr)
            return 2
    target = {
        "platform": platform,
        "path": path,
        "scope": scope,
        "format": fmt or infer_format(cfg, platform, path),
    }
    cfg["targets"].append(target)
    save_config(root, cfg)
    block = rules_block(root, cfg)
    changed = write_if_changed(target_file(root, target), render(cfg, block, target))
    print("ADD %s\t%s\tscope=%s\tformat=%s" % (platform, path, scope, target["format"]))
    if changed:
        print("WRITE %s" % path)
    return 0

def main(argv=None):
    ap = argparse.ArgumentParser(
        prog="platform_entries",
        description="从 AGENTS.md 的 RULES 块渲染各平台入口文件（单一事实源，正文禁止手写）",
    )
    ap.add_argument("--root", default=None, help="仓库根目录（默认 git toplevel）")
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("list", help="列出已登记目标（平台 / 路径 / scope / format）")
    g = sub.add_parser("generate", help="按 targets 渲染并写 repo scope 文件（幂等）")
    g.add_argument("--dry-run", action="store_true", help="只打印将变更的路径")
    sub.add_parser("check", help="校验同源：0 一致 / 1 漂移·缺失·未登记 / 2 配置错误")
    i = sub.add_parser("install", help="把 scope=local 目标写入本机路径")
    i.add_argument("--local", action="store_true", help="写本机专用入口（忽略 gitignore）")
    a = sub.add_parser("add", help="追加登记并立即生成")
    a.add_argument("--platform", required=True)
    a.add_argument("--path", required=True)
    a.add_argument("--scope", choices=SCOPES, default="repo")
    a.add_argument("--format", choices=FORMATS, default=None)
    args = ap.parse_args(argv)

    root = repo_root(args.root)
    try:
        cfg = load_config(root)
        if args.cmd == "list":
            return cmd_list(root, cfg)
        if args.cmd == "generate":
            return cmd_generate(root, cfg, args.dry_run)
        if args.cmd == "check":
            return cmd_check(root, cfg)
        if args.cmd == "install":
            return cmd_install(root, cfg, args.local)
        if args.cmd == "add":
            return cmd_add(root, cfg, args.platform, args.path, args.scope, args.format)
    except ConfigError as exc:
        print("CONFIG %s" % exc)
        return 2
    except BlockError as exc:
        print("RULES_BLOCK %s" % exc)
        return 2
    return 2

if __name__ == "__main__":
    sys.exit(main())
