#!/usr/bin/env bash
set -euo pipefail
PACKAGE=com.lifemate
RUNNER=com.lifemate.test/androidx.test.runner.AndroidJUnitRunner
adb shell pm grant "$PACKAGE" android.permission.POST_NOTIFICATIONS
adb shell appops set "$PACKAGE" SCHEDULE_EXACT_ALARM allow
seed() {
  adb shell am instrument -w -e class com.lifemate.ExternalAlarmTest -e externalAlarmTest true -e alarmTitle "$1" "$RUNNER"
}
assert_notification() {
  adb shell dumpsys notification --noredact > "$1-notifications.txt"
  if grep -q "$2" "$1-notifications.txt"; then
    echo "PASS: $1 notification delivered by Android after UI/process exit"
  else
    adb logcat -d -s AndroidRuntime AlarmManager LifeMate | tail -100
    echo "FAILED: $1 notification was not observed"
    exit 1
  fi
}
seed LifeMate-process-death-check
adb shell am kill "$PACKAGE"
sleep 40
assert_notification process-death LifeMate-process-death-check
seed LifeMate-reboot-check
adb reboot
adb wait-for-device
timeout 150 bash -c 'until [[ "$(adb shell getprop sys.boot_completed | tr -d "\r")" == "1" ]]; do sleep 2; done'
adb shell input keyevent 82
sleep 40
assert_notification reboot LifeMate-reboot-check
