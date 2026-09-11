#!/usr/bin/env bash
set -o pipefail
./gradlew connectedDebugAndroidTest --stacktrace 2>&1 | tee device.log
status=${PIPESTATUS[0]}
if [ "$status" -eq 0 ]; then
  bash scripts/device-smoke.sh 2>&1 | tee -a device.log
  status=${PIPESTATUS[0]}
fi
adb pull /sdcard/Android/data/com.lifemate/files/screenshots screenshots || true
exit "$status"
