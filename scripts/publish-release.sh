#!/usr/bin/env bash
set -euo pipefail
repo=thisrony506-prog/LifeMate
tag="v$LIFEMATE_VERSION_NAME"
apk="release-download/LifeMate-$LIFEMATE_VERSION_CODE.apk"
test -f "$apk"
# Releases are immutable: reruns must not silently swap an APK under an installed version.
if gh release view "$tag" --repo "$repo" >/dev/null 2>&1; then
  echo '::error::This version already exists. Start a NEW workflow run for a new version; existing APKs are not overwritten.'
  exit 1
fi
# Refuse to move Latest backwards (for example when an older run is rerun later).
latest=$(gh api "repos/$repo/releases?per_page=100")
LATEST_RELEASES="$latest" python3 - <<'PY'
import json, os, re
current = int(os.environ['LIFEMATE_VERSION_CODE'])
for release in json.loads(os.environ['LATEST_RELEASES']):
    match = re.fullmatch(r'v1\.2\.([0-9]+)', release['tag_name'])
    if match and not release['draft'] and not release['prerelease']:
        if 100_000 + int(match[1]) >= current:
            raise SystemExit('A newer/equal release is already published. Start a new workflow run.')
PY
gh release create "$tag" "$apk" --repo "$repo" --target "$GITHUB_SHA" \
  --title "LifeMate $LIFEMATE_VERSION_NAME" --latest \
  --notes "Signed LifeMate APK for Android 8.0+. Version code: $LIFEMATE_VERSION_CODE. Install over the existing official app to retain local data. Android asks you to confirm the update."
