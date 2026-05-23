#!/usr/bin/env sh
set -eu

BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
MVN_VERSION="3.9.11"
MVN_HOME="$BASE_DIR/.mvn/apache-maven-$MVN_VERSION"
MVN_BIN="$MVN_HOME/bin/mvn"

if [ ! -x "$MVN_BIN" ]; then
  mkdir -p "$BASE_DIR/.mvn"
  ARCHIVE="$BASE_DIR/.mvn/apache-maven-$MVN_VERSION-bin.tar.gz"
  if [ ! -f "$ARCHIVE" ]; then
    curl -L "https://archive.apache.org/dist/maven/maven-3/$MVN_VERSION/binaries/apache-maven-$MVN_VERSION-bin.tar.gz" -o "$ARCHIVE"
  fi
  tar -xzf "$ARCHIVE" -C "$BASE_DIR/.mvn"
fi

exec "$MVN_BIN" "$@"
