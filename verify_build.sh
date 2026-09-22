#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/tmp/android-sdk}"
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
[[ -f app/src/main/AndroidManifest.xml ]]
[[ -f app/src/main/java/com/drabdie/tweak/MainActivity.java ]]
[[ -f app/src/main/aidl/com/drabdie/tweak/IShellService.aidl ]]
APK="app/build/outputs/apk/debug/app-debug.apk"
[[ -f "$APK" ]] || { echo "APK not built: $APK"; exit 1; }
"$ANDROID_SDK_ROOT/build-tools/35.0.0/aapt" dump badging "$APK" | grep -q "package: name='com.drabdie.tweak'"
"$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" verify "$APK"
printf 'static checks: PASS\n'
