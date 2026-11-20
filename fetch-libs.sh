#!/usr/bin/env bash
# Fetch the CC:Tweaked 1.18.2 production jar used for compile/dev runs.
# Build wiring: flatDir 'libs' + fg.deobf('cct:cct:1.101.3').
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p libs
URL="https://cdn.modrinth.com/data/gu7yAYhd/versions/fmRxKFx2/cc-tweaked-1.18.2-1.101.3.jar"
curl -fsSL --retry 3 -o "libs/cct-1.101.3.jar" "$URL"
echo "Fetched libs/cct-1.101.3.jar"
