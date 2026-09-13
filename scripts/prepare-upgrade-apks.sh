#!/usr/bin/env bash
set +x
set +v
set -euo pipefail
trap 'echo "::error::Signed APK verification failed at script line $LINENO. See the named check above."' ERR
repo=thisrony506-prog/LifeMate
mkdir -p release-download "$RUNNER_TEMP/previous-release"
# Use the real previous published APK whenever available, to test signing continuity.
gh api "repos/$repo/releases?per_page=100" > "$RUNNER_TEMP/releases.json"
previous_tag=$(python3 - "$RUNNER_TEMP/releases.json" <<'PY'
import json, re, sys
releases=[]
for release in json.load(open(sys.argv[1])):
    m=re.fullmatch(r'v1\.2\.([1-9][0-9]{0,6})', release['tag_name'])
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
  LIFEMATE_VERSION_CODE=$((LIFEMATE_VERSION_CODE - 1)) LIFEMATE_VERSION_NAME=1.2.0-upgrade-test ./gradlew :app:assembleRelease --no-configuration-cache --stacktrace
  cp app/build/outputs/apk/release/app-release.apk "$RUNNER_TEMP/previous.apk"
fi
# An unpublished same-source lower-version Flutter APK proves native account
# retention followed by another non-destructive Flutter update.
LIFEMATE_VERSION_CODE=$((LIFEMATE_VERSION_CODE - 1)) LIFEMATE_VERSION_NAME=1.2.0-retention-test ./gradlew :app:assembleRelease --no-configuration-cache --stacktrace
cp app/build/outputs/apk/release/app-release.apk "$RUNNER_TEMP/flutter-retention.apk"
./gradlew :app:assembleRelease --no-configuration-cache --stacktrace
cp app/build/outputs/apk/release/app-release.apk "release-download/LifeMate-$LIFEMATE_VERSION_CODE.apk"
# Match the pinned AGP toolchain, not whichever newer/preview tool happens to be on the runner.
apksigner="$ANDROID_HOME/build-tools/35.0.0/apksigner"
aapt="$ANDROID_HOME/build-tools/35.0.0/aapt"
test -x "$apksigner" && test -x "$aapt"
apksig_jar="$ANDROID_HOME/build-tools/35.0.0/lib/apksigner.jar"
test -f "$apksig_jar"
echo 'Verifying with Android Build Tools 35.0.0.'
echo 'Checking APK signatures, signer count and package identity.'
for apk in "$RUNNER_TEMP/previous.apk" "$RUNNER_TEMP/flutter-retention.apk" release-download/*.apk; do
  if ! "$apksigner" verify --min-sdk-version 26 --verbose --print-certs "$apk" > "$apk.certificate.txt"; then
    grep -E '^(DOES NOT VERIFY|ERROR|WARNING|Verified using|Number of signers:)' "$apk.certificate.txt" || true
    echo '::error::Android apksigner rejected the APK.'
    exit 1
  fi
  # Only public verification summaries/fingerprints, never keys/passwords or certificate subjects.
  grep -E '^(Verified using|Number of signers:)|^Signer .* certificate SHA-256 digest:' "$apk.certificate.txt" || true
  # Repeat verification through the official library and obtain the exact verified certificate.
  java -cp "$apksig_jar" scripts/VerifiedApkSigner.java "$apk" > "$apk.signer.txt"
  "$aapt" dump badging "$apk" > "$RUNNER_TEMP/candidate-badging.txt"
  grep '^package:' "$RUNNER_TEMP/candidate-badging.txt"
  grep -q "package: name='com.lifemate' " "$RUNNER_TEMP/candidate-badging.txt"
done
old=$(cat "$RUNNER_TEMP/previous.apk.signer.txt")
new=$(cat release-download/*.signer.txt)
[[ "$old" == "$new" ]] || { echo '::error::Signing key changed. Refusing to publish an incompatible upgrade.'; exit 1; }
[[ "$(cat "$RUNNER_TEMP/flutter-retention.apk.signer.txt")" == "$new" ]] || { echo '::error::Retention fixture signer differs'; exit 1; }
echo 'PASS: verified APK signer continuity and package identity'
# Only the APK may remain in the download folder.
rm release-download/*.certificate.txt release-download/*.signer.txt
"$aapt" dump badging release-download/*.apk > "$RUNNER_TEMP/badging.txt"
echo "Checking current APK versionCode=$LIFEMATE_VERSION_CODE"
grep -q "package: name='com.lifemate' versionCode='$LIFEMATE_VERSION_CODE'" "$RUNNER_TEMP/badging.txt"
if grep -q application-debuggable "$RUNNER_TEMP/badging.txt"; then
  echo '::error::Release must not be debuggable.'; exit 1
fi
test "$(find release-download -type f | wc -l)" -eq 1
python3 scripts/apk-metrics.py release-download/*.apk

echo 'Signing public update metadata with the verified retained key.'
java scripts/SignReleaseMetadata.java "release-download/LifeMate-$LIFEMATE_VERSION_CODE.apk" \
  "$LIFEMATE_VERSION_CODE" "$LIFEMATE_VERSION_NAME" \
  "https://github.com/$repo/releases/download/v$LIFEMATE_VERSION_NAME/LifeMate-$LIFEMATE_VERSION_CODE.apk" \
  "$RUNNER_TEMP/release-notes.md" "${new##* }"
