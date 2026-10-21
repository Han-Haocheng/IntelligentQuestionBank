#!/usr/bin/env python3
"""doc_check: validate generated docs against the optional doc standard."""
import argparse
import json
import re
import sys
sys.dont_write_bytecode = True
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import precision_common as pc

DEFAULT_REQUIRED = {
    "task": ["目标", "范围", "不变量", "验收", "依赖与风险", "停止条件", "检查点", "未决问题"],
    "design": ["问题与目标", "非目标", "现状与约束", "方案", "影响面", "验证与验收", "风险与回滚", "未决问题"],
    "incident": ["时间线", "影响", "直接原因", "根因", "处置", "防复发", "未决问题"],
    "release": ["用户可见变更", "修复", "破坏性变更与升级步骤", "已知问题"]
}
DEFAULT_MAX = {"task": 80, "design": 150, "incident": 100, "release": 60}
DEFAULT_VAGUE = ["大概", "可能", "建议考虑", "应该没问题", "尽量", "适当", "酌情"]

def load_std(root, cfg):
    std = pc.load_json(Path(root) / cfg.get("doc_standard", ".agents/doc-standard.json"), {}) or {}
    std.setdefault("required_sections", DEFAULT_REQUIRED)
    std.setdefault("max_lines", DEFAULT_MAX)
    std.setdefault("vague_words", DEFAULT_VAGUE)
    return std

def parse(path):
    lines = Path(path).read_text(encoding="utf-8").splitlines()
    headings = []
    for i, line in enumerate(lines):
        m = re.match(r"^(#{1,3})\s+(.+?)\s*$", line)
        if m:
            headings.append((i, len(m.group(1)), m.group(2).strip()))
    sections = []
    for idx, (start, level, title) in enumerate(headings):
        end = len(lines)
        for j in range(idx + 1, len(headings)):
            if headings[j][1] <= level:
                end = headings[j][0]
                break
        body = lines[start + 1:end]
        sections.append({"title": title, "body": body, "start": start + 1})
    return lines, sections

def find_section(sections, keyword):
    for s in sections:
        if keyword in s["title"]:
            return s
    return None

def validate(path, dtype, root, cfg):
    errs, warns = [], []
    def e(code, msg): errs.append({"path": str(path), "code": code, "message": msg})
    def w(code, msg): warns.append({"path": str(path), "code": code, "message": msg})

    p = Path(path)
    if not p.is_file():
        e("NOT_FOUND", "文件不存在")
        return errs, warns
    lines, sections = parse(p)
    std = load_std(root, cfg)

    cap = int((std.get("max_lines") or {}).get(dtype, DEFAULT_MAX.get(dtype, 120)))
    if len(lines) > cap:
        e("TOO_LONG", f"{len(lines)} 行 > 上限 {cap}")

    required = (std.get("required_sections") or {}).get(dtype, DEFAULT_REQUIRED.get(dtype, []))
    for req in required:
        s = find_section(sections, req)
        if not s:
            e("SECTION_MISSING", f"缺少章节：{req}")
            continue
        if not any(x.strip() for x in s["body"]):
            e("SECTION_EMPTY", f"章节为空：{req}")

    text = "\n".join(lines)
    for word in std.get("vague_words") or []:
        if word in text:
            e("VAGUE_WORD", f"出现模糊词：{word}")

    if dtype == "design":
        s = find_section(sections, "方案")
        if s:
            body = "\n".join(s["body"])
            if "备选" not in body and "替代" not in body:
                e("NO_ALTERNATIVE", "方案节缺少备选方案与否决理由")
    if dtype == "release":
        s = find_section(sections, "破坏性")
        if s and any(x.strip() for x in s["body"]):
            body = "\n".join(s["body"])
            if "升级" not in body and "迁移" not in body:
                e("MIGRATION_MISSING", "破坏性变更缺少升级/迁移步骤")
    if dtype == "incident":
        s = find_section(sections, "根因")
        if s:
            body = "\n".join(s["body"])
            if "粗心" in body or "疏忽" in body:
                w("ROOT_CAUSE_WEAK", "根因写成粗心/疏忽，需补流程或门禁原因")
    if "未决问题" in required:
        s = find_section(sections, "未决问题")
        if s:
            items = [x.strip() for x in s["body"] if x.strip()]
            for item in items:
                if item in ("无", "无。", "none", "None", "—", "-"):
                    continue
                if "owner" not in item.lower() and "@" not in item:
                    w("OPEN_QUESTION_NO_OWNER", f"未决问题缺少 owner：{item[:40]}")
                    break
    return errs, warns

def main():
    ap = argparse.ArgumentParser(prog="doc_check")
    ap.add_argument("file")
    ap.add_argument("--type", required=True, choices=["task", "design", "incident", "release"])
    ap.add_argument("--root", default=None)
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    root = pc.repo_root(args.root)
    cfg = pc.load_config(root)
    errs, warns = validate(args.file, args.type, root, cfg)
    if args.json:
        print(json.dumps(pc.envelope("doc-check", not errs, None, errs, warns), ensure_ascii=False, indent=2))
    else:
        pc.print_errors(errs)
        for x in warns:
            print(f"WARN {x['code']}: {x['message']}", file=sys.stderr)
        if not errs:
            print("OK")
    return 1 if errs else 0

if __name__ == "__main__":
    sys.exit(main())
