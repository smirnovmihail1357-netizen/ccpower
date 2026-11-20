"""Build-level fix for NeoGradle 7.0.x run classpaths.

SpongeMixin pulls ASM 9.5 while NeoForge pulls a newer ASM (9.8). BootstrapLauncher
rejects two different jars declaring the same module name. Rewrite every older ASM
entry to the newest one already present so the jars are recognised as already loaded.
Handles both newline-separated (NeoGradle 7.0.97 / MC 1.20.4) and colon-separated
(NeoGradle 7.0.142 / MC 1.20.5+) classpath files.
"""
import re
import sys
from pathlib import Path

NAMES = ("asm", "asm-tree", "asm-util", "asm-analysis")


def vkey(v):
    return tuple(int(x) for x in re.findall(r"\d+", v))


root = Path(sys.argv[1])
paths = list(root.glob(".gradle/configuration/neoForm/**/writeMinecraftClasspath*/classpath.txt"))
paths += list(root.glob("build/neoForm/**/writeMinecraftClasspath*/classpath.txt"))

for cp in paths:
    text = cp.read_text()
    sep = "\n" if ("\n" in text and ":" not in text) else ":"
    entries = [e for e in re.split(r"[\n:]", text) if e]
    for name in NAMES:
        pattern = re.compile(r"/org\.ow2\.asm/" + re.escape(name) + r"/([0-9.]+)/[^/:]+/")
        by_version = {}
        for entry in entries:
            m = pattern.search(entry)
            if m:
                by_version.setdefault(m.group(1), entry)
        if len(by_version) < 2:
            continue
        newest = max(by_version, key=vkey)
        keep = by_version[newest]
        entries = [keep if (e in by_version.values() and e != keep and pattern.search(e).group(1) != newest) else e for e in entries]
    cp.write_text(sep.join(entries) + ("\n" if sep == "\n" else ""))
    leftovers = sum(1 for e in entries if any(f"/org.ow2.asm/{n}/9.5/" in e for n in NAMES))
    print(f"patched {cp.parent.parent.name}/{cp.name}: sep={sep!r} stale_9.5={leftovers}")
