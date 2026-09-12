#!/usr/bin/env bash
# Run locally, or in a private Codespace with JDK 17 and authenticated GitHub CLI.
# Do NOT run in a public CI job. No credential values are printed.
set -euo pipefail
umask 077
repo=thisrony506-prog/LifeMate
folder=${1:?Usage: bash scripts/setup-release-signing.sh /private/persistent/backup-folder}
for tool in gh keytool openssl; do command -v "$tool" >/dev/null || { echo "Install $tool first."; exit 1; }; done
# Check permissions before creating any signing identity. Never overwrite existing secrets.
gh api "repos/$repo/actions/secrets/public-key" --jq .key_id >/dev/null
existing=$(gh secret list --repo "$repo" --json name --jq '.[].name')
if grep -q '^LIFEMATE_\(KEYSTORE_BASE64\|STORE_PASSWORD\|KEY_ALIAS\|KEY_PASSWORD\)$' <<< "$existing"; then
  echo 'Signing secrets already exist. Do not replace an existing app signing identity.'; exit 1
fi
if [[ -e "$folder" ]]; then echo 'Choose a NEW private backup folder; existing files are never overwritten.'; exit 1; fi
mkdir -m 700 -p "$folder"
folder=$(cd "$folder" && pwd)
# Credentials must stay outside the checkout.
root=$(git rev-parse --show-toplevel)
case "$folder/" in "$root/"*) echo 'Use a folder outside the repository.'; rmdir "$folder"; exit 1;; esac
openssl rand -base64 36 > "$folder/store-password.txt"
export LIFEMATE_STORE_PASSWORD
LIFEMATE_STORE_PASSWORD=$(cat "$folder/store-password.txt")
keytool -genkeypair -keystore "$folder/lifemate-release.jks" -storetype JKS \
  -alias lifemate -keyalg RSA -keysize 3072 -validity 36500 \
  -storepass:env LIFEMATE_STORE_PASSWORD -keypass:env LIFEMATE_STORE_PASSWORD \
  -dname 'CN=LifeMate Release, O=LifeMate' >/dev/null 2>&1
# If an upload fails, KEEP this folder/key and finish configuration with the same key.
base64 < "$folder/lifemate-release.jks" | tr -d '\n' | gh secret set LIFEMATE_KEYSTORE_BASE64 --repo "$repo"
printf '%s' "$LIFEMATE_STORE_PASSWORD" | gh secret set LIFEMATE_STORE_PASSWORD --repo "$repo"
printf '%s' "$LIFEMATE_STORE_PASSWORD" | gh secret set LIFEMATE_KEY_PASSWORD --repo "$repo"
printf 'lifemate' | gh secret set LIFEMATE_KEY_ALIAS --repo "$repo"
unset LIFEMATE_STORE_PASSWORD
printf '%s\n' 'Signing identity configured. Keep an encrypted, offline backup of the private folder.'
printf '%s\n' 'Never upload that folder, share its contents in chat, or recreate the key for an update.'
