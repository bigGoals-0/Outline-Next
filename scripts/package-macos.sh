#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./mvnw -pl client -am package
jpackage \
  --type app-image \
  --name "Outline Next" \
  --input client/target \
  --main-jar outline-client-0.1.0.jar \
  --main-class com.outline.client.Launcher \
  --dest dist/macos
