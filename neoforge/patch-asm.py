"""Build-level fix for NeoGradle 7.0.x run classpaths.

SpongeMixin pulls ASM 9.5 while NeoForge pulls a newer ASM. BootstrapLauncher
rejects two different jars declaring the same module name. Rewrite every older
ASM path to the newest ASM path already present in the generated classpath so
the jars are recognised as already loaded on the module path.
"""
import re
import sys
from pathlib import Path


def version_key(v):
    return tuple(int(x) for x in re.findall(r"\d+", v))


for cp in Path(sys.argv[1]).glob(".gradle/configuration/neoForm/**/writeMinecraftClasspath*/classpath.txt"):
    text = cp.read_text()
    for name in ("asm", "asm-tree", "asm-util", "asm-analysis"):
        pattern = re.compile(
            r"/org\.ow2\.asm/" + re.escape(name) + r"/([0-9.]+)/[^:]+/" + re.escape(name) + r"-[0-9.]+\.jar"
        )
        found = pattern.findall(text)
        versions = sorted(set(found), key=version_key)
        if len(versions) < 2:
            continue
        newest = versions[-1]
        for old in versions[:-1]:
            old_path = re.search(pattern.pattern.replace("([0-9.]+)", re.escape(old)), text)
            new_path = re.search(pattern.pattern.replace("([0-9.]+)", re.escape(newest)), text)
            if old_path and new_path:
                text = text.replace(old_path.group(0), new_path.group(0))
    cp.write_text(text)
    print(f"patched {cp.name}: stale={sum(text.count('/org.ow2.asm/' + n + '/9.5/') for n in ('asm', 'asm-tree', 'asm-util', 'asm-analysis'))}")
