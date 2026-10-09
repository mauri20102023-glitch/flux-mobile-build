#!/usr/bin/env bash
set -uo pipefail
mkdir -p android-evidence
apps/android/gradlew -p apps/android :app:connectedDebugAndroidTest --stacktrace
flux_test_status=$?
adb pull /sdcard/Android/data/ai.flux.mobile.preview.pro/files/evidence android-evidence/ || true
adb logcat -d -s AndroidRuntime:E > android-evidence/android-crashes.txt
exit "$flux_test_status"
