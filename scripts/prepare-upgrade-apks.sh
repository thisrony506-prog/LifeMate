#!/usr/bin/env bash
set -euo pipefail
repo=thisrony506-prog/LifeMate
mkdir -p release-download "$RUNNER_TEMP/previous-release"
# Use the real previous published APK whenever available, to test signing continuity.
gh api "repos/$repo/releases?per_page=100" > "$RUNNER_TEMP/releases.json"
previous_tag=$(python3 - "$RUNNER_TEMP/releases.json" <<'PY'
import json, re, sys
releases=[]
for release in json.load(open(sys.argv[1])):
    m=re.fullmatch(r'v1\.2\.([0-9]+)', release['tag_name'])
    if m and not release['draft'] and not release['prerelease']:
        releases.append((int(m[1]),release['tag_name']))
print(max(releases)[1] if releases else '')
PY
)
if [[ -n "$previous_tag" ]]; then
  previous_run=${previous_tag##*.}
  previous_code=$((100000 + previous_run))
  if (( previous_code >= LIFEMATE_VERSION_CODE )); then
    echo '::error::A newer/equal version already exists. Start a new workflow run.'; exit 1
  fi
  gh release download "$previous_tag" --repo "$repo" --pattern "LifeMate-$previous_code.apk" --dir "$RUNNER_TEMP/previous-release"
  cp "$RUNNER_TEMP/previous-release/LifeMate-$previous_code.apk" "$RUNNER_TEMP/previous.apk"
else
  # First-ever signed publication: make a lower-version fixture with this retained key.
  # It stays on the runner and is never published or uploaded.
  LIFEMATE_VERSION_CODE=$((LIFEMATE_VERSION_CODE - 1)) LIFEMATE_VERSION_NAME=1.2.0-upgrade-test ./gradlew assembleRelease --stacktrace
  cp app/build/outputs/apk/release/app-release.apk "$RUNNER_TEMP/previous.apk"
fi
./gradlew assembleRelease --stacktrace
cp app/build/outputs/apk/release/app-release.apk "release-download/LifeMate-$LIFEMATE_VERSION_CODE.apk"
apksigner=$(find "$ANDROID_HOME/build-tools" -name apksigner | sort -V | tail -1)
aapt=$(find "$ANDROID_HOME/build-tools" -name aapt | sort -V | tail -1)
for apk in "$RUNNER_TEMP/previous.apk" release-download/*.apk; do
  "$apksigner" verify --verbose --print-certs "$apk" > "$apk.certificate.txt"
done
old=$(grep 'Signer #1 certificate SHA-256 digest:' "$RUNNER_TEMP/previous.apk.certificate.txt")
new=$(grep 'Signer #1 certificate SHA-256 digest:' release-download/*.certificate.txt)
[[ "$old" == "$new" ]] || { echo '::error::Signing key changed. Refusing to publish an incompatible upgrade.'; exit 1; }
# Only the APK may remain in the download folder.
rm release-download/*.certificate.txt
"$aapt" dump badging release-download/*.apk > "$RUNNER_TEMP/badging.txt"
grep -q "package: name='com.lifemate' versionCode='$LIFEMATE_VERSION_CODE'" "$RUNNER_TEMP/badging.txt"
if grep -q application-debuggable "$RUNNER_TEMP/badging.txt"; then
  echo '::error::Release must not be debuggable.'; exit 1
fi
test "$(find release-download -type f | wc -l)" -eq 1
