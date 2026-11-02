#!/usr/bin/env bash
# Tinkers Fabric daily dev commands: build, test and run (Linux, macOS, Git Bash).
# Usage: scripts/dev.sh <command>   (run "scripts/dev.sh help" for the list)
# The JDK must match java_version in gradle.properties. Set TF_JAVA_HOME, or put
# 'export JAVA_HOME=<jdk path>' in scripts/dev.local.sh (git-ignored; scripts/env/restore_env.sh writes it).
# Full machine clean/restore lives in scripts/env/.
set -euo pipefail

# Always work from the repo root, wherever the script is called from
cd "$(dirname "$0")/.."

# Required Java version comes from gradle.properties (single source of truth)
REQUIRED_JAVA="$(sed -n 's/^java_version=//p' gradle.properties | tr -d '\r' | head -n1)"

if [ -f scripts/dev.local.sh ]; then
  # shellcheck disable=SC1091
  . scripts/dev.local.sh
fi
if [ -n "${TF_JAVA_HOME:-}" ]; then
  export JAVA_HOME="$TF_JAVA_HOME"
fi

RUN_DIRS="mantle/run tconstruct/run"
SCREENSHOT_DIR="tconstruct/build/run/clientGameTest/screenshots"

usage() {
  cat <<EOF
Tinkers Fabric dev script (needs Java $REQUIRED_JAVA, from gradle.properties)

  check        Show Java, Gradle and git versions
  clean        Delete build outputs (gradlew clean)
  reset        clean + delete dev run folders (test worlds, logs, options)
  build        Compile, unit tests and server GameTests
  test         Unit tests and server GameTests only
  client-test  Client game test: boots to title, opens a world, saves screenshots
  client       Start the dev client (tconstruct + mantle)
  server       Start the dev server (needs TF_ACCEPT_EULA=1, see below)
  all          reset, build, client-test: full fresh verification

Java: set TF_JAVA_HOME or run scripts/env/restore_env.sh once (writes scripts/dev.local.sh).
Full clean incl. Loom caches: scripts/env/clean_env.sh --dry-run
Server: running a server means accepting the Minecraft EULA (https://aka.ms/MinecraftEULA).
        Set TF_ACCEPT_EULA=1 only if you accept it.
EOF
}

# Fails unless JAVA_HOME points at a JDK of the required version.
require_java() {
  if [ -z "${JAVA_HOME:-}" ]; then
    echo "JAVA_HOME is not set. Java $REQUIRED_JAVA is required." >&2
    echo "Set TF_JAVA_HOME or run scripts/env/restore_env.sh once." >&2
    exit 1
  fi
  local java_bin="$JAVA_HOME/bin/java"
  if [ ! -x "$java_bin" ] && [ -x "$java_bin.exe" ]; then
    java_bin="$java_bin.exe"
  fi
  if [ ! -x "$java_bin" ]; then
    echo "No java binary under JAVA_HOME=$JAVA_HOME" >&2
    exit 1
  fi
  # WHY: Minecraft and Loom need this exact Java; an older JDK fails late with confusing errors
  if ! "$java_bin" -version 2>&1 | grep -Eq "version .$REQUIRED_JAVA[^0-9]"; then
    echo "JAVA_HOME=$JAVA_HOME is not Java $REQUIRED_JAVA:" >&2
    "$java_bin" -version >&2
    exit 1
  fi
  export PATH="$JAVA_HOME/bin:$PATH"
}

gradle() {
  sh ./gradlew "$@"
}

remove_run_dirs() {
  for dir in $RUN_DIRS; do
    if [ -d "$dir" ]; then
      echo "Removing $dir"
      rm -rf "$dir"
    fi
  done
}

command="${1:-help}"
case "$command" in
  help|-h|--help)
    usage
    exit 0
    ;;
esac

require_java

case "$command" in
  check)
    java -version
    gradle --version
    git --version
    ;;
  clean)
    gradle clean
    ;;
  reset)
    gradle clean
    remove_run_dirs
    ;;
  build)
    gradle build
    ;;
  test)
    gradle test :mantle:runGameTest :tconstruct:runGameTest
    ;;
  client-test)
    gradle :tconstruct:runClientGameTest
    echo "Screenshots: $SCREENSHOT_DIR"
    ;;
  client)
    gradle :tconstruct:runClient
    ;;
  server)
    if [ "${TF_ACCEPT_EULA:-}" != "1" ]; then
      echo "The dev server needs the Minecraft EULA accepted: https://aka.ms/MinecraftEULA" >&2
      echo "If you accept it, run: TF_ACCEPT_EULA=1 scripts/dev.sh server" >&2
      exit 1
    fi
    mkdir -p tconstruct/run
    echo "eula=true" > tconstruct/run/eula.txt
    gradle :tconstruct:runServer
    ;;
  all)
    gradle clean
    remove_run_dirs
    gradle build
    gradle :tconstruct:runClientGameTest
    echo "Screenshots: $SCREENSHOT_DIR"
    ;;
  *)
    echo "Unknown command: $command" >&2
    usage
    exit 1
    ;;
esac
