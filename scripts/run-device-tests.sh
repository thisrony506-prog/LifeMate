#!/usr/bin/env bash
set -o pipefail
./gradlew connectedDebugAndroidTest --stacktrace 2>&1 | tee device.log
status=${PIPESTATUS[0]}
if [[ -f app/build/outputs/apk/debug/app-debug.apk && -f app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk ]]; then
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
  adb shell am instrument -w -e class com.lifemate.ScreenshotTest com.lifemate.test/androidx.test.runner.AndroidJUnitRunner | tee screenshot-run.log
  adb pull /sdcard/Android/data/com.lifemate/files/screenshots screenshots || true
fi
if [ "$status" -eq 0 ]; then
  bash scripts/device-smoke.sh 2>&1 | tee -a device.log
  status=${PIPESTATUS[0]}
fi
exit "$status"
