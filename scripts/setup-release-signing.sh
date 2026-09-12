#!/usr/bin/env bash
# Interactive OWNER setup only. Never run with secret input in Actions or shared logs.
set +x
set +v
set -euo pipefail
umask 077
repo=thisrony506-prog/LifeMate
[[ $# -eq 0 ]] || { echo 'Usage: bash scripts/setup-release-signing.sh (no arguments)'; exit 1; }
[[ "${CI:-false}" != true && "${GITHUB_ACTIONS:-false}" != true ]] || { echo 'Run locally in a private terminal, not CI.'; exit 1; }
for tool in java javac keytool openssl gh git base64 realpath; do
  command -v "$tool" >/dev/null || { echo "Required tool missing: $tool"; exit 1; }
done
java -version 2>&1 | grep -Eq 'version "17([."]|$)' || { echo 'Select JDK 17 (JAVA_HOME and PATH) first.'; exit 1; }
javac -version 2>&1 | grep -Eq '^javac 17([.]|$)' || { echo 'JDK 17 javac is required.'; exit 1; }
keytool -J-version 2>&1 | grep -Eq 'version "17([."]|$)' || { echo 'Use the keytool belonging to JDK 17.'; exit 1; }
cd "$(dirname "$0")/.."
# Check authentication, repository access, Secrets permission and existing identity BEFORE generation.
gh auth status --hostname github.com >/dev/null 2>&1 || { echo 'Authenticate gh privately with GitHub first.'; exit 1; }
gh api "repos/$repo" --jq .full_name | grep -Fxq "$repo" || { echo 'Repository access is required.'; exit 1; }
gh api "repos/$repo/actions/secrets/public-key" --jq .key_id >/dev/null
existing=$(gh secret list --repo "$repo" --json name --jq '.[].name')
if grep -Eq '^LIFEMATE_(KEYSTORE_BASE64|STORE_PASSWORD|KEY_ALIAS|KEY_PASSWORD)$' <<< "$existing"; then
  echo 'Signing secrets already exist (possibly partial setup). Restore/finish the SAME identity; do not replace it.'; exit 1
fi
published_apks=$(gh api "repos/$repo/releases?per_page=100" --paginate --jq '.[] | select(.draft == false and .prerelease == false) | .assets[] | select(.name | endswith(".apk")) | .name')
[[ -z "$published_apks" ]] || { echo 'An official APK already exists. Recover its original signing identity instead of generating a new one.'; exit 1; }
folder="$HOME/lifemate-private-signing-backup"
[[ ! -e "$folder" && ! -L "$folder" ]] || { echo 'Private backup folder already exists. It will NOT be overwritten.'; exit 1; }
root=$(git rev-parse --show-toplevel)
physical_folder="$(realpath "$HOME")/lifemate-private-signing-backup"
case "$physical_folder/" in "$root/"*) echo 'The private backup folder must be outside the repository.'; exit 1;; esac
# /dev/tty keeps password input out of pipelines, argv, shell history and CI logs.
exec 3<>/dev/tty || { echo 'An interactive private terminal is required.'; exit 1; }
created=false
cleanup() {
  status=$?
  unset LIFEMATE_STORE_PASSWORD LIFEMATE_KEY_PASSWORD password confirmation
  if [[ $status -ne 0 && "$created" == true ]]; then
    echo 'Setup did not finish. KEEP the private backup folder and the same key; never regenerate to recover a partial upload.' >&2
  fi
}
trap cleanup EXIT
printf 'Choose a strong password for BOTH the keystore and key (at least 12 characters): ' >&3
IFS= read -r -s password <&3; printf '\n' >&3
printf 'Confirm password: ' >&3
IFS= read -r -s confirmation <&3; printf '\n' >&3
[[ ${#password} -ge 12 && "$password" == "$confirmation" ]] || { echo 'Password too short or confirmation did not match. Nothing was created.'; exit 1; }
unset confirmation
export LIFEMATE_STORE_PASSWORD="$password" LIFEMATE_KEY_PASSWORD="$password"
unset password
# Atomic creation: never follow a pre-existing folder/symlink, even after a race.
mkdir -m 700 "$folder"
created=true
keytool -genkeypair -keystore "$folder/lifemate-release.jks" -storetype JKS \
  -alias lifemate -keyalg RSA -keysize 3072 -validity 36500 \
  -storepass:env LIFEMATE_STORE_PASSWORD -keypass:env LIFEMATE_KEY_PASSWORD \
  -dname 'CN=LifeMate Release, O=LifeMate' >/dev/null 2>&1
chmod 600 "$folder/lifemate-release.jks"
# Verify the generated public certificate/key using OpenSSL. No private key is exported.
keytool -exportcert -rfc -alias lifemate -keystore "$folder/lifemate-release.jks" \
  -storepass:env LIFEMATE_STORE_PASSWORD 2>/dev/null | \
  openssl x509 -pubkey -noout | openssl pkey -pubin -text -noout | grep -Fq 'Public-Key: (3072 bit)'
# Passwords go over stdin; the keystore/base64 never enters Git or a temporary file.
base64 < "$folder/lifemate-release.jks" | tr -d '\n' | gh secret set LIFEMATE_KEYSTORE_BASE64 --repo "$repo" >/dev/null 2>&1
printf '%s' "$LIFEMATE_STORE_PASSWORD" | gh secret set LIFEMATE_STORE_PASSWORD --repo "$repo" >/dev/null 2>&1
printf '%s' "$LIFEMATE_KEY_PASSWORD" | gh secret set LIFEMATE_KEY_PASSWORD --repo "$repo" >/dev/null 2>&1
printf 'lifemate' | gh secret set LIFEMATE_KEY_ALIAS --repo "$repo" >/dev/null 2>&1
printf '%s\n' 'Signing secrets configured. No password file was written.' \
  'Keep an encrypted offline backup of $HOME/lifemate-private-signing-backup/lifemate-release.jks.' \
  'Save the password separately in your password manager before deleting this Codespace.'
