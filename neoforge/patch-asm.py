import re, sys
from pathlib import Path
root = Path(sys.argv[1])
for cp in root.glob('.gradle/configuration/neoForm/**/writeMinecraftClasspath*/classpath.txt'):
    s = cp.read_text()
    for name in ['asm', 'asm-tree', 'asm-util', 'asm-analysis']:
        m95 = re.search(r'/org\.ow2\.asm/' + re.escape(name) + r'/9\.5/[^:]+/' + re.escape(name) + r'-9\.5\.jar', s)
        m98 = re.search(r'/org\.ow2\.asm/' + re.escape(name) + r'/9\.8/[^:]+/' + re.escape(name) + r'-9\.8\.jar', s)
        if m95 and m98:
            s = s.replace(m95.group(0), m98.group(0))
    cp.write_text(s)
    print(f"patched {cp.name}: stale={s.count('/9.5/')}")
