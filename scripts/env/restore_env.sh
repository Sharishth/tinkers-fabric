#!/usr/bin/env bash
# Rebuilds the dev environment from nothing: finds the right JDK, fetches Gradle, builds and tests the mods.
#
# Version-aware: every requirement is read from the project, nothing is hard-coded here.
#   Java      : java_version in gradle.properties
#   Minecraft : minecraft_version, loader_version, fabric_api_version, loom_version in gradle.properties
#   Gradle    : distributionUrl in gradle/wrapper/gradle-wrapper.properties (checksum-pinned, verified by the wrapper)
# Steps:
#   1. Find a JDK of the required version: --java-home, TF_JAVA_HOME, scripts/dev.local.sh, JAVA_HOME,
#      macOS java_home, /usr/lib/jvm, /Library/Java/JavaVirtualMachines, ~/.sdkman.
#   2. Save it to scripts/dev.local.sh (git-ignored) if missing, so scripts/dev.sh works.
#   3. 'gradlew --version' then 'gradlew build' (unit tests + server GameTests; first run downloads Minecraft).
#   4. With --client-test, also run the client game test (opens a game window, saves screenshots).
#
# Usage: scripts/env/restore_env.sh [--java-home PATH] [--no-build] [--client-test]   (Linux, macOS, Git Bash)
set -euo pipefail

JAVA_HOME_ARG=""; NO_BUILD=0; CLIENT_TEST=0
while [ $# -gt 0 ]; do
	case "$1" in
		--java-home) JAVA_HOME_ARG="${2:?--java-home needs a path}"; shift 2 ;;
		--no-build) NO_BUILD=1; shift ;;
		--client-test) CLIENT_TEST=1; shift ;;
		-h|--help) sed -n '2,15p' "$0"; exit 0 ;;
		*) echo "Unknown option: $1" >&2; exit 64 ;;
	esac
done

PROJECT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$PROJECT"

# Prints the value of a key in a .properties file.
read_prop() { sed -n "s/^$1=//p" "$2" | tr -d '\r' | head -n1; }

# 0. Requirements, read from the project
REQUIRED_JAVA="$(read_prop java_version gradle.properties)"
GRADLE_VERSION="$(read_prop distributionUrl gradle/wrapper/gradle-wrapper.properties | sed -n 's#.*gradle-\([0-9][0-9.]*\)-bin\.zip#\1#p')"
[ -n "$REQUIRED_JAVA" ] || { echo "java_version is missing from gradle.properties" >&2; exit 1; }
# WHY: the daemon JDK (gradle-daemon-jvm.properties) and compile target must not drift apart
DAEMON_JAVA="$(read_prop toolchainVersion gradle/gradle-daemon-jvm.properties)"
if [ "$DAEMON_JAVA" != "$REQUIRED_JAVA" ]; then
	echo "gradle/gradle-daemon-jvm.properties toolchainVersion=$DAEMON_JAVA but gradle.properties java_version=$REQUIRED_JAVA; make them equal." >&2
	exit 1
fi
echo "Requirements (from gradle.properties and the Gradle wrapper):"
echo "  Java $REQUIRED_JAVA, Gradle $GRADLE_VERSION, Loom $(read_prop loom_version gradle.properties)"
echo "  Minecraft $(read_prop minecraft_version gradle.properties), Fabric Loader $(read_prop loader_version gradle.properties), Fabric API $(read_prop fabric_api_version gradle.properties)"
echo

# Prints the java binary for a JDK home (java or java.exe), or nothing.
java_bin() {
	if [ -x "$1/bin/java" ]; then echo "$1/bin/java"; elif [ -x "$1/bin/java.exe" ]; then echo "$1/bin/java.exe"; fi
}

# 1. JDK of the required version
LOCAL_FILE="scripts/dev.local.sh"
LOCAL_JAVA=""
if [ -f "$LOCAL_FILE" ]; then
	LOCAL_JAVA="$(sed -n 's/^export JAVA_HOME=//p' "$LOCAL_FILE" | tr -d '"'"'"'\r' | head -n1)"
fi
CANDIDATES=("$JAVA_HOME_ARG" "${TF_JAVA_HOME:-}" "$LOCAL_JAVA" "${JAVA_HOME:-}")
if [ -x /usr/libexec/java_home ]; then
	CANDIDATES+=("$(/usr/libexec/java_home -v "$REQUIRED_JAVA" 2>/dev/null || true)")
fi
for d in /usr/lib/jvm/*"$REQUIRED_JAVA"* /Library/Java/JavaVirtualMachines/*"$REQUIRED_JAVA"*/Contents/Home "$HOME"/.sdkman/candidates/java/"$REQUIRED_JAVA"*; do
	CANDIDATES+=("$d")
done
JDK=""
for c in "${CANDIDATES[@]}"; do
	[ -n "$c" ] || continue
	bin="$(java_bin "$c")"
	[ -n "$bin" ] || continue
	if "$bin" -version 2>&1 | head -n1 | grep -Eq "\"$REQUIRED_JAVA(\.|\")"; then JDK="$c"; break; fi
	echo "Skipping $c (not Java $REQUIRED_JAVA)" >&2
done
[ -n "$JDK" ] || { echo "No JDK $REQUIRED_JAVA found. Install one and pass --java-home PATH." >&2; exit 1; }
export JAVA_HOME="$JDK"
export PATH="$JDK/bin:$PATH"
echo "JDK $REQUIRED_JAVA : $JDK"

# 2. Remember the JDK for scripts/dev.sh
if [ ! -f "$LOCAL_FILE" ]; then
	printf '# Local machine settings (git-ignored).\nexport JAVA_HOME="%s"\n' "$JDK" > "$LOCAL_FILE"
	echo "Saved    : $LOCAL_FILE"
fi
command -v git >/dev/null 2>&1 || echo "WARNING: git not found on PATH (only needed to commit)." >&2

# 3. Gradle, build, tests
echo "Gradle   : fetching $GRADLE_VERSION through the wrapper (checksum verified)"
sh ./gradlew --version --console=plain
if [ "$NO_BUILD" -eq 0 ]; then
	echo "Building: compile, unit tests, server GameTests (first run downloads Minecraft)..."
	sh ./gradlew build --console=plain
fi
if [ "$CLIENT_TEST" -eq 1 ]; then
	echo "Client game test: a game window opens, boots to title, loads a world..."
	sh ./gradlew :tconstruct:runClientGameTest --console=plain
	echo "Screenshots: $PROJECT/tconstruct/build/run/clientGameTest/screenshots"
fi

# 4. Summary
echo
echo "Environment restored."
for mod in mantle tconstruct; do
	for jar in "$PROJECT/$mod"/build/libs/"$mod"-*.jar; do
		[ -e "$jar" ] || continue
		case "$jar" in *-sources.jar) continue ;; esac
		echo "Jar      : $jar"
	done
done
echo
echo "Next: scripts/dev.sh help   (build, test, client-test, client, server)"
