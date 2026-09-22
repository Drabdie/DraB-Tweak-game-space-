#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME="${JAVA_HOME:-/opt/jdk21}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/tmp/android-sdk}"
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
[[ -f app/src/main/java/com/drabdie/tweak/MainActivity.java ]]
[[ -f app/src/main/java/com/drabdie/tweak/ShellService.java ]]
[[ -f app/src/main/aidl/com/drabdie/tweak/IShellService.aidl ]]
# The old build had a launch crash: ShizukuProvider class was missing from the APK.
"$ANDROID_SDK_ROOT/build-tools/35.0.0/dexdump" dist/DraB-Tweak-1.1.0.apk > /tmp/dexdump.txt 2>/dev/null
grep -q "rikka/shizuku/ShizukuProvider;" /tmp/dexdump.txt
"$ANDROID_SDK_ROOT/build-tools/35.0.0/aapt" dump badging dist/DraB-Tweak-1.1.0.apk | grep -q "package: name='com.drabdie.tweak'"
"$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" verify dist/DraB-Tweak-1.1.0.apk
printf 'static checks: PASS\n'
