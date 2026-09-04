#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Patch BBS preview world so it is internal-only: hide from all user-facing
world lists and reserve the name. No sed, no CRLF corruption (byte-safe)."""
import io, re

ROOT = "D:/DEV/bbs_clean"
EDITOR = ROOT + "/src/client/java/lingfeng/bbsnext/mcef/EditorBridge.java"
DASHBOARD = ROOT + "/src/client/java/lingfeng/bbsnext/mcef/DashboardBridge.java"

IND = "    "  # 4-space indentation (confirmed for these files)

def read(p):
    with io.open(p, "r", encoding="utf-8", newline="") as f:
        return f.read()

def write(p, s):
    with io.open(p, "w", encoding="utf-8", newline="") as f:
        f.write(s)

def replace_once(text, old, new, label):
    n = text.count(old)
    if n != 1:
        raise SystemExit("FAIL [%s] expected 1 occurrence, found %d" % (label, n))
    return text.replace(old, new, 1)

def patch(p):
    t = read(p)
    NL = "\r\n" if "\r\n" in t else "\n"
    J = NL  # joiner for multi-line blocks

    # ---- R1: reserved-name constant before enteringWorldUntil field (level 1) ----
    old1 = IND + "private static volatile long enteringWorldUntil = 0L;"
    new1 = (
        IND + "/** Reserved internal preview world folder name. Must never appear in any" + NL
        + IND + " *  user-facing world list and must not be created by users; see" + NL
        + IND + " *  LevelStorageSourceMixin (hide) and WorldOpenFlowsMixin (reserve name). */" + NL
        + IND + "public static final String BBS_PREVIEW_WORLD = \"bbs_preview\";" + NL + NL
        + old1
    )
    t = replace_once(t, old1, new1, "R1-constant")

    # ---- R2: primary world picker filter (findLevelCandidates path) - level 4/5 ----
    old2 = IND*4 + "if (!name.isEmpty())" + NL + IND*4 + "{" + NL + IND*5 + "worldArr.add(name);"
    new2 = IND*4 + "if (!name.isEmpty() && !BBS_PREVIEW_WORLD.equals(name))" + NL + IND*4 + "{" + NL + IND*5 + "worldArr.add(name);"
    t = replace_once(t, old2, new2, "R2-primary-filter")

    # ---- R3: fallback world picker filter (manual saves scan) - level 6/7 ----
    old3 = IND*6 + "if (new File(dir, \"level.dat\").exists())" + NL + IND*6 + "{" + NL + IND*7 + "worldArr.add(dir.getName());"
    new3 = IND*6 + "if (new File(dir, \"level.dat\").exists() && !BBS_PREVIEW_WORLD.equals(dir.getName()))" + NL + IND*6 + "{" + NL + IND*7 + "worldArr.add(dir.getName());"
    t = replace_once(t, old3, new3, "R3-fallback-filter")

    # ---- R4: remove createFreshLevel("bbs_preview") fallback (name reserved now) - level 4/5 ----
    pat4 = re.compile(
        re.escape(IND*4 + "else" + NL + IND*4 + "{" + NL + IND*5 + "/* Last resort: ask MC to create a fresh level.")
        + r".*?BBSMod\.LOGGER\.error\(\"\[EditorBridge\] createFreshLevel failed\", t\);\s*\}\s*\}",
        re.DOTALL,
    )
    if not pat4.search(t):
        raise SystemExit("FAIL [R4] createFreshLevel fallback block not found")
    new4 = (
        IND*4 + "else" + NL
        + IND*4 + "{" + NL
        + IND*5 + "/* No template world available to clone bbs_preview from." + NL
        + IND*5 + " * We deliberately do NOT call createFreshLevel(\"bbs_preview\")" + NL
        + IND*5 + " * here: that name is reserved/internal and must not be" + NL
        + IND*5 + " * created by this path (creation under it is redirected in" + NL
        + IND*5 + " * WorldOpenFlowsMixin). The preview world therefore depends" + NL
        + IND*5 + " * on at least one existing singleplayer save to clone from. */" + NL
        + IND*5 + "BBSMod.LOGGER.error(\"[EditorBridge] enterPreviewWorld: no template world to clone bbs_preview from\");" + NL
        + IND*4 + "}" + NL
    )
    t = pat4.sub(new4, t, count=1)

    # ---- R5: dstDir resolve literal -> constant (level 3) ----
    old5 = IND*3 + "Path dstDir = savesDir.resolve(\"bbs_preview\");"
    new5 = IND*3 + "Path dstDir = savesDir.resolve(BBS_PREVIEW_WORLD);"
    t = replace_once(t, old5, new5, "R5-dstDir")

    # ---- R6: openWorld literal -> constant (level 4) ----
    old6 = IND*4 + "mc.createWorldOpenFlows().openWorld(\"bbs_preview\", () ->"
    new6 = IND*4 + "mc.createWorldOpenFlows().openWorld(BBS_PREVIEW_WORLD, () ->"
    t = replace_once(t, old6, new6, "R6-openWorld")

    write(p, t)
    return NL

NL = patch(EDITOR)
print("EditorBridge patched OK (NL=%r)" % NL)

# ---- DashboardBridge ----
d = read(DASHBOARD)
NL2 = "\r\n" if "\r\n" in d else "\n"
old7 = IND*4 + "stream.filter(Files::isDirectory)" + NL2 + IND*5 + ".map(p -> p.getFileName().toString())" + NL2 + IND*5 + ".sorted()" + NL2 + IND*5 + ".forEach(worlds::add);"
new7 = IND*4 + "stream.filter(Files::isDirectory)" + NL2 + IND*5 + ".map(p -> p.getFileName().toString())" + NL2 + IND*5 + ".filter(name -> !EditorBridge.BBS_PREVIEW_WORLD.equals(name))" + NL2 + IND*5 + ".sorted()" + NL2 + IND*5 + ".forEach(worlds::add);"
d = replace_once(d, old7, new7, "R7-dashboard-filter")
write(DASHBOARD, d)
print("DashboardBridge patched OK")
