#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./mvnw -pl client -am package
rm -rf client/target/package-input dist/macos
mkdir -p client/target/package-input
cp client/target/outline-client-0.1.0.jar client/target/package-input/
./mvnw -pl client dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/package-input
jpackage \
  --type app-image \
  --name "Outline Chat" \
  --app-version 1.0.0 \
  --module-path client/target/package-input \
  --module com.outline.client/com.outline.client.OutlineClientApplication \
  --dest dist/macos
