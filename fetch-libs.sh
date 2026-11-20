#!/usr/bin/env bash
# Fetch the CC:Tweaked 1.18.1 production jar used for compile/dev runs.
# Build wiring: flatDir 'libs' + fg.deobf('cct:cct:1.99.1').
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p libs
URL="https://cdn.modrinth.com/data/gu7yAYhd/versions/1.18.1-1.99.1/cc-tweaked-1.18.1-1.99.1.jar"
curl -fsSL --retry 3 -o "libs/cct-1.99.1.jar" "$URL"
echo "Fetched libs/cct-1.99.1.jar"
