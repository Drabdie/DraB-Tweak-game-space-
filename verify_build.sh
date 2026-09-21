#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/tmp/android-sdk}"
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
[[ -f app/src/main/AndroidManifest.xml ]]
[[ -f app/src/main/java/com/drabdie/tweak/MainActivity.java ]]
! grep -q 'content.addView(l,p)' app/src/main/java/com/drabdie/tweak/MainActivity.java
"$ANDROID_SDK_ROOT/build-tools/35.0.0/aapt" dump badging dist/DraB-Tweak-fixed-1.0.1.apk | grep -q "package: name='com.drabdie.tweak'"
"$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" verify dist/DraB-Tweak-fixed-1.0.1.apk
printf 'static checks: PASS\n'
