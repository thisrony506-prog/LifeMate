#!/usr/bin/env bash
# Keys/passwords are never printed, cached or uploaded. No unsigned publication.
set +x
set +v
set -euo pipefail
for name in LIFEMATE_KEYSTORE_BASE64 LIFEMATE_STORE_PASSWORD LIFEMATE_KEY_ALIAS LIFEMATE_KEY_PASSWORD; do
  if [[ -z "${!name:-}" ]]; then
    echo "::error::Missing private signing secret: $name. Configure the retained key before publishing. Unsigned APK downloads are disabled. See docs/RELEASE.md."
    exit 1
  fi
done
umask 077
# Accept line wrapping/CRLF introduced by private copy-paste, but never ignore invalid characters.
if ! printf '%s' "$LIFEMATE_KEYSTORE_BASE64" | tr -d '[:space:]' | base64 --decode > "$RUNNER_TEMP/lifemate-release.jks" 2>/dev/null; then
  rm -f "$RUNNER_TEMP/lifemate-release.jks"
  echo '::error::The keystore secret is not valid Base64. Use the complete encoding of your retained JKS file; do not use an APK, path or password.'
  exit 1
fi
if [[ ! -s "$RUNNER_TEMP/lifemate-release.jks" ]]; then
  rm -f "$RUNNER_TEMP/lifemate-release.jks"
  echo '::error::The decoded keystore is empty. Restore the encoding of your retained JKS file.'
  exit 1
fi
echo "LIFEMATE_KEYSTORE_PATH=$RUNNER_TEMP/lifemate-release.jks" >> "$GITHUB_ENV"
