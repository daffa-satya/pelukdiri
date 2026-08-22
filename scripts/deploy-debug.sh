#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB_BIN="${PELUKDIRI_ADB:-adb}"
PACKAGE="com.makhp.pelukdiri"
MAIN_ACTIVITY="$PACKAGE/.MainActivity"
APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
RECORD="$PROJECT_DIR/app/build/outputs/apk/debug/deployment-record.txt"

"$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" assembleDebug
test -f "$APK"

APK_SHA256="$(sha256sum "$APK" | awk '{print $1}')"
"$ADB_BIN" wait-for-device
"$ADB_BIN" shell am force-stop "$PACKAGE"
"$ADB_BIN" install -r -t --no-incremental "$APK"
"$ADB_BIN" shell am force-stop "$PACKAGE"
"$ADB_BIN" shell am start -S -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$MAIN_ACTIVITY"

{
    printf 'apk=%s\n' "$APK"
    printf 'sha256=%s\n' "$APK_SHA256"
    printf 'deployed_utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    "$ADB_BIN" shell pm path "$PACKAGE"
    "$ADB_BIN" shell dumpsys package "$PACKAGE" | grep -E 'versionCode=|versionName=|firstInstallTime=|lastUpdateTime='
} > "$RECORD"

printf 'Installed %s\n' "$APK"
printf 'SHA-256 %s\n' "$APK_SHA256"
printf 'Record %s\n' "$RECORD"
