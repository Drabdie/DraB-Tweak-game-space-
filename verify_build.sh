#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/opt/android-sdk}"
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
[[ -f app/src/main/AndroidManifest.xml ]]
[[ -f app/src/main/java/com/drabdie/tweak/MainActivity.java ]]
[[ -f app/src/main/aidl/com/drabdie/tweak/IShellService.aidl ]]

APK_DEBUG="app/build/outputs/apk/debug/app-debug.apk"
APK_RELEASE="HELLBOOST-Universal-Game-Booster.apk"

[[ -f "$APK_DEBUG" ]] || { echo "APK not built: $APK_DEBUG"; exit 1; }
[[ -f "$APK_RELEASE" ]] || { echo "APK not copied: $APK_RELEASE"; exit 1; }

"$ANDROID_SDK_ROOT/build-tools/35.0.0/aapt" dump badging "$APK_RELEASE" | grep -q "application-label:'HELLBOOST'"
"$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" verify "$APK_RELEASE"
printf 'static checks & signature verification: PASS\n'
