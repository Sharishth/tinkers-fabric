#!/usr/bin/env bash
# Removes what this project's build and dev runs put on the machine, after saving the built jars to dist/.
#
# Version-aware: versions come from gradle.properties and gradle/wrapper/gradle-wrapper.properties.
# Default scope (safe for your other Fabric projects):
#   project : build/ .gradle/ mantle/build/ tconstruct/build/ mantle/run/ tconstruct/run/
#   global  : Loom's Minecraft <minecraft_version> files only:
#             <gradle home>/caches/fabric-loom/<minecraft_version>
#             <gradle home>/caches/fabric-loom/minecraftMaven/net/minecraft/*/<minecraft_version>
# --deep also removes caches shared with every Gradle/Fabric project on this machine:
#             fabric-loom/assets, wrapper/dists/gradle-<gradle version>-*, caches/modules-2
# Never touches: sources, .git, dist/, scripts/dev.local.*, your Minecraft launcher, JDKs.
#
# Usage: scripts/env/clean_env.sh [--dry-run] [--yes] [--deep] [--no-keep-jar]   (Linux, macOS, Git Bash)
set -euo pipefail

DRY_RUN=0; YES=0; DEEP=0; KEEP_JAR=1
for arg in "$@"; do
	case "$arg" in
		--dry-run) DRY_RUN=1 ;;
		--yes|-y) YES=1 ;;
		--deep) DEEP=1 ;;
		--no-keep-jar) KEEP_JAR=0 ;;
		-h|--help) sed -n '2,15p' "$0"; exit 0 ;;
		*) echo "Unknown option: $arg" >&2; exit 64 ;;
	esac
done

PROJECT="$(cd "$(dirname "$0")/../.." && pwd)"
GRADLE_HOME_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}"

# Prints the value of a key in a .properties file.
read_prop() { sed -n "s/^$1=//p" "$2" | tr -d '\r' | head -n1; }

MC_VERSION="$(read_prop minecraft_version "$PROJECT/gradle.properties")"
GRADLE_VERSION="$(read_prop distributionUrl "$PROJECT/gradle/wrapper/gradle-wrapper.properties" | sed -n 's#.*gradle-\([0-9][0-9.]*\)-bin\.zip#\1#p')"
[ -n "$MC_VERSION" ] && [ -n "$GRADLE_VERSION" ] || { echo "Could not read minecraft_version or the Gradle version" >&2; exit 1; }

LOOM="$GRADLE_HOME_DIR/caches/fabric-loom"
LABELS=(); PATHS=()
add_target() { LABELS+=("$1"); PATHS+=("$2"); }
add_target "root build output"                 "$PROJECT/build"
add_target "project Gradle/Loom cache"         "$PROJECT/.gradle"
add_target "mantle build output"               "$PROJECT/mantle/build"
add_target "tconstruct build output (incl. test runs)" "$PROJECT/tconstruct/build"
add_target "mantle dev runs and worlds"        "$PROJECT/mantle/run"
add_target "tconstruct dev runs and worlds"    "$PROJECT/tconstruct/run"
add_target "Loom Minecraft $MC_VERSION files"  "$LOOM/$MC_VERSION"
for dir in "$LOOM"/minecraftMaven/net/minecraft/*; do
	[ -d "$dir" ] && add_target "Loom Minecraft $MC_VERSION jars ($(basename "$dir"))" "$dir/$MC_VERSION"
done
if [ "$DEEP" -eq 1 ]; then
	add_target "Loom assets (shared by all MC versions)" "$LOOM/assets"
	for dist in "$GRADLE_HOME_DIR"/wrapper/dists/gradle-"$GRADLE_VERSION"-*; do
		[ -d "$dist" ] && add_target "Gradle $GRADLE_VERSION distribution (shared)" "$dist"
	done
	add_target "Gradle dependency cache (shared)" "$GRADLE_HOME_DIR/caches/modules-2"
fi

size_mb() { du -sk "$1" 2>/dev/null | cut -f1 | awk '{printf "%.1f", $1/1024}'; }

echo "Project   : $PROJECT"
echo "Gradle    : $GRADLE_HOME_DIR (Gradle $GRADLE_VERSION)"
echo "Minecraft : $MC_VERSION"
echo

PRESENT=()
for i in "${!PATHS[@]}"; do
	if [ -e "${PATHS[$i]}" ]; then
		PRESENT+=("$i")
		printf '  %9s MB  %s  [%s]\n' "$(size_mb "${PATHS[$i]}")" "${PATHS[$i]}" "${LABELS[$i]}"
	fi
done
if [ "${#PRESENT[@]}" -eq 0 ]; then
	echo "Nothing to clean: environment is already clean."
	exit 0
fi
echo

if [ "$DRY_RUN" -eq 1 ]; then
	echo "Dry run: nothing was deleted."
	exit 0
fi
if [ "$YES" -ne 1 ]; then
	read -r -p "Delete the items above? Type YES to continue: " answer
	[ "$answer" = "YES" ] || { echo "Aborted."; exit 1; }
fi

# 1. Keep the built jars (build folders are about to disappear).
if [ "$KEEP_JAR" -eq 1 ]; then
	saved=0
	for mod in mantle tconstruct; do
		for jar in "$PROJECT/$mod"/build/libs/"$mod"-*.jar; do
			[ -e "$jar" ] || continue
			case "$jar" in *-sources.jar) continue ;; esac
			mkdir -p "$PROJECT/dist"
			cp -f "$jar" "$PROJECT/dist/"
			echo "Saved jar: $PROJECT/dist/$(basename "$jar")"
			saved=1
		done
	done
	[ "$saved" -eq 1 ] || echo "No built jars found (nothing saved to dist/)."
fi

# 2. Stop what holds files open: this project's Gradle daemons and dev game processes.
# WHY: 'gradlew --stop' needs a JDK; reuse the one the dev scripts use when JAVA_HOME is unset
if [ -n "${TF_JAVA_HOME:-}" ]; then export JAVA_HOME="$TF_JAVA_HOME"; fi
if [ -z "${JAVA_HOME:-}" ] && [ -f "$PROJECT/scripts/dev.local.sh" ]; then
	# shellcheck disable=SC1091
	. "$PROJECT/scripts/dev.local.sh"
fi
if [ -n "${JAVA_HOME:-}" ] && [ -f "$PROJECT/gradlew" ]; then
	echo "Stopping Gradle daemons"
	(cd "$PROJECT" && sh ./gradlew --stop >/dev/null 2>&1) || true
fi
# WHY: match the project path too, so dev games of other projects keep running
pkill -f "Knot(Client|Server).*$(basename "$PROJECT")|$(basename "$PROJECT").*Knot(Client|Server)" 2>/dev/null || true
sleep 2

# 3. Delete.
failed=0
for i in "${PRESENT[@]}"; do
	if rm -rf "${PATHS[$i]}"; then
		echo "Deleted ${PATHS[$i]}"
	else
		echo "WARNING: could not delete ${PATHS[$i]}" >&2
		failed=1
	fi
done
echo
if [ "$failed" -eq 1 ]; then
	echo "Some items could not be deleted. Close Minecraft, your IDE and Gradle, then run again."
	exit 2
fi
echo "Environment cleaned. To build again run: scripts/env/restore_env.sh"
