#!/usr/bin/env bash
# GitHub Actions only. Keys/passwords are never printed, cached or uploaded.
set -euo pipefail
names=(LIFEMATE_KEYSTORE_BASE64 LIFEMATE_STORE_PASSWORD LIFEMATE_KEY_ALIAS LIFEMATE_KEY_PASSWORD)
provided=0
for name in "${names[@]}"; do
  if [[ -n "${!name:-}" ]]; then provided=$((provided + 1)); fi
done
if [[ "$provided" -eq 0 ]]; then
  echo 'LIFEMATE_SIGNED_RELEASE=false' >> "$GITHUB_ENV"
  echo '::notice::No release signing secrets configured. The only download will be an UNSIGNED release APK; it must be signed before installation.'
elif [[ "$provided" -ne 4 ]]; then
  echo '::error::Release signing is only partially configured. Set all four LIFEMATE signing secrets, or remove all four to build unsigned.'
  exit 1
else
  umask 077
  printf '%s' "$LIFEMATE_KEYSTORE_BASE64" | base64 --decode > "$RUNNER_TEMP/lifemate-release.jks"
  test -s "$RUNNER_TEMP/lifemate-release.jks"
  echo "LIFEMATE_KEYSTORE_PATH=$RUNNER_TEMP/lifemate-release.jks" >> "$GITHUB_ENV"
  echo 'LIFEMATE_SIGNED_RELEASE=true' >> "$GITHUB_ENV"
fi
