#!/usr/bin/env bash
# CC:T 1.122.0's Fabric jar was built with Loom 1.19.3, which is newer than
# any published architectury-loom. Loom refuses to remap such a jar. This
# script downloads the pinned jar, strips only the Fabric-Loom-Version manifest
# marker (no code/resources change) and installs it into a local maven.
set -euo pipefail
cd "$(dirname "$0")"

MC=1.20.1
CCV=1.122.0
ART="cc-tweaked-${MC}-fabric"
DEST="local-maven/cc/tweaked/${ART}/${CCV}-patched"
mkdir -p "$DEST"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

curl -fLsS --retry 3 -o "$TMP/mod.jar" \
  "https://maven.squiddev.cc/cc/tweaked/${ART}/${CCV}/${ART}-${CCV}.jar"

python3 - "$TMP/mod.jar" "$DEST/${ART}-${CCV}-patched.jar" <<'PY'
import sys, zipfile
src, dst = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, 'w') as zout:
    for info in zin.infolist():
        data = zin.read(info.filename)
        if info.filename == 'META-INF/MANIFEST.MF':
            lines = data.decode('utf-8').splitlines()
            out, skip = [], False
            for line in lines:
                if line.startswith('Fabric-Loom-Version:'):
                    skip = True
                    continue
                if skip and line.startswith(' '):
                    continue
                skip = False
                out.append(line)
            data = ('\n'.join(out) + '\n').encode('utf-8')
        zout.writestr(info, data)
PY

cat > "$DEST/${ART}-${CCV}-patched.pom" <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>cc.tweaked</groupId>
  <artifactId>${ART}</artifactId>
  <version>${CCV}-patched</version>
</project>
EOF
echo "Installed ${ART}-${CCV}-patched.jar into local-maven/"

# Extract CC:T's embedded libraries (NightConfig core/toml, Cobalt, ...) so
# the Fabric dev runtime can load them; production jars carry them as JiJ.
JIJ=libs-jij
mkdir -p "$JIJ"
unzip -o -j "$TMP/mod.jar" 'META-INF/jars/*.jar' -d "$JIJ" > /dev/null
echo "Extracted CC:T embedded jars into $JIJ/"
