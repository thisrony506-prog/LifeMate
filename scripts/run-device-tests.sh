#!/usr/bin/env bash
set -o pipefail
./gradlew connectedDebugAndroidTest --stacktrace 2>&1 | tee device.log
status=${PIPESTATUS[0]}
if [ "$status" -eq 0 ]; then
  # AGP may clean up the target/test packages after connected tests.
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
  bash scripts/device-smoke.sh 2>&1 | tee -a device.log
  status=${PIPESTATUS[0]}
fi
adb pull /sdcard/Android/data/com.lifemate/files/screenshots screenshots || true
exit "$status"
