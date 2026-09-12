#!/usr/bin/env bash
# Keys/passwords are never printed, cached or uploaded. No unsigned publication.
set -euo pipefail
for name in LIFEMATE_KEYSTORE_BASE64 LIFEMATE_STORE_PASSWORD LIFEMATE_KEY_ALIAS LIFEMATE_KEY_PASSWORD; do
  if [[ -z "${!name:-}" ]]; then
    echo "::error::Missing private signing secret: $name. Configure the retained key before publishing. Unsigned APK downloads are disabled. See docs/RELEASE.md."
    exit 1
  fi
done
umask 077
printf '%s' "$LIFEMATE_KEYSTORE_BASE64" | base64 --decode > "$RUNNER_TEMP/lifemate-release.jks"
test -s "$RUNNER_TEMP/lifemate-release.jks"
echo "LIFEMATE_KEYSTORE_PATH=$RUNNER_TEMP/lifemate-release.jks" >> "$GITHUB_ENV"
