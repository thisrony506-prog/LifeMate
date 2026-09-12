#!/usr/bin/env bash
set +x
set +v
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
    match = re.fullmatch(r'v1\.2\.([1-9][0-9]{0,6})', release['tag_name'])
    if match and not release['draft'] and not release['prerelease']:
        if 100_000 + int(match[1]) >= current:
            raise SystemExit('A newer/equal release is already published. Start a new workflow run.')
PY
# Metadata is public release-body text, not a companion downloadable file.
# Keep the release draft until the uploaded bytes' GitHub digest matches our signed proof.
gh release create "$tag" "$apk" --repo "$repo" --target "$GITHUB_SHA" --draft \
  --title "LifeMate $LIFEMATE_VERSION_NAME" --notes-file "$RUNNER_TEMP/release-notes.md"
gh api "repos/$repo/releases?per_page=100" --jq ".[] | select(.tag_name == \"$tag\") | .id" > "$RUNNER_TEMP/release-id"
grep -Eq '^[0-9]+$' "$RUNNER_TEMP/release-id"
gh api "repos/$repo/releases/$(cat "$RUNNER_TEMP/release-id")" > "$RUNNER_TEMP/uploaded-release.json"
python3 - <<'PYVERIFY'
import hashlib, json, os, pathlib
root = pathlib.Path(os.environ['RUNNER_TEMP'])
r = json.loads((root / 'uploaded-release.json').read_text())
assets = r['assets']
assert len(assets) == 1, 'Release must contain exactly one APK asset'
a = assets[0]
p = pathlib.Path('release-download') / f"LifeMate-{os.environ['LIFEMATE_VERSION_CODE']}.apk"
sha = hashlib.sha256(p.read_bytes()).hexdigest()
assert a['name'] == p.name and a['size'] == p.stat().st_size and a['state'] == 'uploaded'
assert a.get('digest') == 'sha256:' + sha, 'Uploaded APK digest mismatch or unavailable'
assert a['browser_download_url'] == f"https://github.com/thisrony506-prog/LifeMate/releases/download/v{os.environ['LIFEMATE_VERSION_NAME']}/{p.name}"
PYVERIFY
gh release edit "$tag" --repo "$repo" --draft=false --latest
