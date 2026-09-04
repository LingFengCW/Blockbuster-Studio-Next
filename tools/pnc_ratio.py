#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PNC License §5 — Modification Ratio Calculator (base logic).

This tool RETURNS and RETAINS the base calculation logic required by
PNC License §5‑A. Distributors MAY extend it (add metrics, dead‑code
detection, custom rules, …) but MUST NOT remove, alter, or delete the
base logic implemented in:

    count_valid_lines()
    modified_ratio()
    classify_file()
    compute_aggregate()

BASE ALGORITHM (per PNC §5)
  * "Valid source line" = a line carrying executable source logic.
    Blank lines, line breaks, whitespace‑only indentation, comments, and
    annotation lines are NOT counted.
  * Per file derived from the original Software, compared against the
    original unmodified source file:
        - modified valid-line ratio > 50%  -> independently authored (relicenseable)
        - modified valid-line ratio <= 50% -> must stay MIT / BSD-3 / PNC
  * Aggregate ratio =
        (valid lines of unmodified / <=50%-modified original files)
        -------------------------------------------------------------
        (total valid lines of the whole derivative codebase,
         including all incorporated third-party library source)
    If aggregate ratio > 30% -> no commercial activity allowed.

New files (present in CURRENT but absent from ORIGINAL) are 100% new code,
classified as independently authored and not subject to the <=50% rule.

Usage:
    python tools/pnc_ratio.py --original <orig_src_root> --current <cur_src_root> \
                              [--third-party-lines N] [--extensions .java .py] [--json out.json]

Exit code 0 = aggregate <= 30% (or no original supplied, informational only);
           2 = aggregate > 30% (commercial use restricted under PNC §5).
"""

import argparse
import difflib
import json
import os
import sys

# Comment / annotation stripping rules per language family.
# A line is a "comment-only" line when, after stripping leading whitespace,
# it begins with one of these markers (block comments handled separately).
LINE_COMMENT_MARKERS = ("//", "#", "*", "/*", "<!--", "--", ";", "%", "REM ")
BLOCK_COMMENT_START = "/*"
BLOCK_COMMENT_END = "*/"


def count_valid_lines(text):
    """
    BASE LOGIC (PNC §5): count lines carrying executable source logic.

    Excludes:
      - blank lines / line breaks
      - whitespace-only / indentation-only lines
      - full-line or trailing comments (//, #, *, /* */, <!-- -->, --, ;, %)
      - annotation-only lines (e.g. Java @Annotation on its own line)
    """
    valid = 0
    in_block = False
    for raw in text.splitlines():
        line = raw.strip()
        if not line:
            continue
        if in_block:
            # still inside a /* ... */ block
            if BLOCK_COMMENT_END in line:
                in_block = False
            continue
        if line.startswith(BLOCK_COMMENT_START):
            # opening block comment; keep scanning for its close on same/diff line
            rest = line[len(BLOCK_COMMENT_START):]
            if BLOCK_COMMENT_END not in rest:
                in_block = True
            continue
        # strip trailing line comment (crude but sufficient for ratio counting)
        stripped = line
        for marker in ("//", "<!--", "--", "#", ";", "%"):
            idx = stripped.find(marker)
            if idx != -1:
                # do not treat '#' inside strings as comment in non-# langs;
                # acceptable approximation for ratio estimation
                stripped = stripped[:idx].strip()
                break
        if not stripped:
            continue
        # annotation-only line (single leading @)
        if stripped.startswith("@"):
            continue
        valid += 1
    return valid


def _read(path):
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            return f.read()
    except OSError:
        return ""


def modified_ratio(original_text, current_text):
    """
    BASE LOGIC (PNC §5): ratio of the original file's valid lines that were
    changed/removed, relative to the original file's total valid lines.

    Returns a float in [0, 1]. 1.0 means the original file was fully rewritten.
    """
    orig_lines = [l for l in original_text.splitlines() if l.strip()]
    curr_lines = [l for l in current_text.splitlines() if l.strip()]
    if not orig_lines:
        return 0.0
    matcher = difflib.SequenceMatcher(a=orig_lines, b=curr_lines)
    changed_original = 0
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag in ("delete", "replace"):
            changed_original += (i2 - i1)
        elif tag == "equal":
            pass
    return changed_original / len(orig_lines)


def classify_file(original_text, current_text, has_original):
    """
    BASE LOGIC (PNC §5): classify a single derived file.

    Returns one of: "new", "independent" (>50% modified), "retained" (<=50% modified).
    """
    if not has_original:
        return "new"
    ratio = modified_ratio(original_text, current_text)
    return "independent" if ratio > 0.5 else "retained"


def collect_files(root, extensions):
    out = {}
    for dirpath, _dirs, files in os.walk(root):
        for fn in files:
            ext = os.path.splitext(fn)[1].lower()
            if ext in extensions:
                full = os.path.join(dirpath, fn)
                rel = os.path.relpath(full, root)
                out[rel] = full
    return out


def compute_aggregate(current_files, original_files, third_party_lines):
    """
    BASE LOGIC (PNC §5): compute the aggregate ratio.

        aggregate = (valid lines of retained + unmodified original-derived files)
                    / (total valid lines of whole derivative codebase
                       + third-party library source lines)

    Returns (aggregate_ratio, detail_dict).
    """
    retained_valid = 0
    total_current_valid = 0
    detail = []
    for rel, cur_path in current_files.items():
        cur_text = _read(cur_path)
        cur_valid = count_valid_lines(cur_text)
        total_current_valid += cur_valid
        has_orig = rel in original_files
        orig_text = _read(original_files[rel]) if has_orig else ""
        cls = classify_file(orig_text, cur_text, has_orig)
        if cls in ("retained",):
            # unmodified / <=50% modified original-derived file
            retained_valid += cur_valid
        detail.append({
            "file": rel,
            "class": cls,
            "valid_lines": cur_valid,
            "modified_ratio": (round(modified_ratio(orig_text, cur_text), 4)
                               if has_orig else None),
        })
    denominator = total_current_valid + max(0, third_party_lines)
    aggregate = (retained_valid / denominator) if denominator else 0.0
    return aggregate, {
        "aggregate_ratio": round(aggregate, 4),
        "retained_valid_lines": retained_valid,
        "total_current_valid_lines": total_current_valid,
        "third_party_valid_lines": max(0, third_party_lines),
        "files": detail,
    }


def main(argv=None):
    ap = argparse.ArgumentParser(description="PNC §5 modification ratio calculator")
    ap.add_argument("--original", help="Root of the ORIGINAL unmodified bbs-mod source tree")
    ap.add_argument("--current", required=True, help="Root of the CURRENT derivative source tree")
    ap.add_argument("--third-party-lines", type=int, default=0,
                    help="Valid source lines of all incorporated third-party library source")
    ap.add_argument("--extensions", nargs="*", default=[".java", ".py", ".kt", ".js", ".ts"],
                    help="Source file extensions to scan")
    ap.add_argument("--json", help="Optional path to write the full JSON report")
    args = ap.parse_args(argv)

    current_files = collect_files(args.current, set(args.extensions))
    original_files = collect_files(args.original, set(args.extensions)) if args.original else {}

    aggregate, report = compute_aggregate(current_files, original_files, args.third_party_lines)

    # ---- EXTENSION POINT (allowed by §5‑A): add extra metrics above this line ----

    print("PNC §5 ratio report")
    print("  scanned current files : %d" % len(current_files))
    print("  original files matched: %d" % len(original_files))
    print("  third-party lines     : %d" % max(0, args.third_party_lines))
    print("  aggregate ratio       : %.4f" % aggregate)
    if aggregate > 0.30:
        print("  => AGGREGATE > 30%%: commercial use RESTRICTED under PNC §5.")
    else:
        print("  => aggregate <= 30%%: no §5 commercial restriction triggered.")

    if args.json:
        with open(args.json, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        print("  report written to %s" % args.json)

    return 2 if aggregate > 0.30 else 0


if __name__ == "__main__":
    sys.exit(main())
