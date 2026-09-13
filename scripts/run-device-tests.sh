#!/usr/bin/env bash
set -euo pipefail
# Only the new Flutter host/reset and retained update-security tests. No legacy
# fixture receiver or obsolete native feature smoke tests are shipped or invoked.
./gradlew :app:connectedDebugAndroidTest --stacktrace 2>&1 | tee device.log
