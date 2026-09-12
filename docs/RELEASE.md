# LifeMate: private signing setup and signed updates

Application ID remains **`com.lifemate`**. Core organizer features remain offline-capable. The old 1.1.x unsigned APKs are not installable; no unsigned APK is published by the current workflow.

## Current verified release

**[Download signed LifeMate 1.2.64](https://github.com/thisrony506-prog/LifeMate/releases/download/v1.2.64/LifeMate-100064.apk)**. [Run 34711233177](https://github.com/thisrony506-prog/LifeMate/actions/runs/34711233177) passed all three jobs and automatically published the release and one APK-only Actions artifact. A real in-place upgrade from the public 1.2.55 to 1.2.64 retained the encrypted profile. See [VERIFICATION.md](VERIFICATION.md) for evidence and manual-device limits.

**All four signing secrets are configured and working. Do not repeat setup or generate a new key.** Keep an encrypted offline backup of the original JKS and its password separately in a password manager. Future signed versions reuse this identity. If an earlier signed LifeMate is already installed, install the newer APK over it and choose Android's **Update**; do not uninstall first.

The latest release adds a unified white/black/pink interface, five bottom destinations, a direct drawer, persistent Facebook Posts, reusable flat graphics, an additive Room 1→2 migration, backward-compatible backup import and optional completion sounds. The existing mandatory browser-free updater, organizer tools, studio, weekly insights and opt-in TTS remain. [PREMIUM.md](PREMIUM.md) explains posts, privacy and the optional consent-gated backend contract; [UX-UPDATE.md](UX-UPDATE.md) covers installer/speech/device limits and studio-only backup exclusions. No live AI backend was deployed.

## Exact commands in your private Codespace — one-time reference only

Open a private Codespace for **`arena/01a090c4-lifemate`** in this repository. Do not share its terminal or record password entry. The commands below assume the standard Ubuntu/Debian Codespaces image and repository path.

```bash
cd /workspaces/LifeMate
# Stop if this is not the session branch; open a Codespace on that branch instead.
test "$(git branch --show-current)" = 'arena/01a090c4-lifemate' || exit 1
git pull --ff-only origin arena/01a090c4-lifemate

sudo apt-get update
sudo apt-get install -y openjdk-17-jdk openssl gh
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-$(dpkg --print-architecture)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
javac -version

# Codespaces' default token may not have permission to manage Actions secrets.
# Authenticate your own owner account via GitHub's browser flow, not chat.
unset GH_TOKEN GITHUB_TOKEN
gh auth login --hostname github.com --git-protocol https --web --scopes repo,workflow
gh auth status --hostname github.com

bash scripts/setup-release-signing.sh
```

The script accepts **no path/password arguments**. It requires JDK 17 (`java`, `javac`, `keytool`), OpenSSL and authenticated GitHub CLI. It checks repository/Secrets access, existing signing secrets and previously published APKs before requesting a password or creating files. It refuses CI, existing backup folders/symlinks, existing secrets (including partial setup), and any attempt to replace a previously released identity.

Enter and confirm your strong password **only at the local hidden terminal prompts** (minimum 12 characters). The same password protects the keystore and its `lifemate` key; it is not your GitHub password. The script creates one RSA **3072-bit JKS** keystore:

```text
$HOME/lifemate-private-signing-backup/lifemate-release.jks
```

Directory permissions are `700`; file permissions are `600`. No plaintext password file, private-key export or Base64 file is written. The script disables tracing, keeps passwords off command arguments, uploads values through stdin, and removes password variables on exit. It uploads exactly these repository secrets:

- `LIFEMATE_KEYSTORE_BASE64`
- `LIFEMATE_STORE_PASSWORD`
- `LIFEMATE_KEY_ALIAS` (`lifemate`)
- `LIFEMATE_KEY_PASSWORD`

**Never send a password, token, keystore, Base64 value, or secret-bearing screenshot into chat.** GitHub login/device authorization happens only in your private terminal/browser.

### Backup is mandatory

Before deleting the Codespace, store an **encrypted offline backup of the JKS** and retain its password separately in a password manager. Codespaces storage is not an independent backup; GitHub cannot reveal secret values later. Never commit these files or upload them as Actions artifacts. Retain the same key for every future update.

If generation/upload fails after the backup folder is created, the script preserves it and refuses regeneration. Recover/finish setup with that exact key. Do not delete the folder or replace existing secrets just to make a rerun succeed. For an already-published application, recover the original identity; a new unrelated key cannot update existing installs.

If permission checks return **403**, no key is generated. Your GitHub account/token must have repository Actions Secrets read/write permission. Reconnecting an integration without adding that permission does not resolve the restriction.

## Start a signed release

After the setup script succeeds and you have backed up the key:

```bash
gh workflow run android.yml \
  --repo thisrony506-prog/LifeMate \
  --ref arena/01a090c4-lifemate

gh run list --repo thisrony506-prog/LifeMate \
  --workflow android.yml --branch arena/01a090c4-lifemate --limit 5
```

Watch the new run in Actions. It must pass **all three jobs**, including **Signed APK and safe upgrade**. A successful compile-only job is not proof of an installable release.

## Build and publication gates

1. Run Android feature/security/update tests, script safety tests, Java helper compilation, Kotlin/JVM tests, lint, and R8 release compilation. Gradle 8.10.2's distribution is SHA-256 pinned; JDK 17, AGP 8.7.3, Kotlin 2.0.21, SDK 35 and Android Build Tools **35.0.0** are configured.
2. Require all four retained-key secrets. There is no debug-key/unsigned fallback. Secrets are not printed or cached; Gradle configuration caching is disabled during signed builds, and the temporary CI keystore is removed in an always-run cleanup.
3. Verify both previous/current APK signatures with Android `apksigner` for API 26+, extract the verified certificate using the official `ApkVerifier` Java API (not CLI-label parsing), require one signer, compare signer SHA-256 fingerprints, validate the application ID and release version, and reject debuggable APKs.
4. Use the previous official signed APK when available. For the first signed release only, build an internal lower-version fixture with the same retained key. Never publish that fixture.
5. In an emulator, install the earlier signed APK, create a real encrypted profile through visible UI, then use `adb install -r` on the new non-debuggable APK without uninstalling. Confirm the higher version and retained profile.
6. Sign public update metadata using the same RSA private key. Metadata includes version, URL, size, SHA-256 and signer identity. It is release-body text, not an additional downloadable file.
7. Create a **draft** GitHub Release; require its sole APK asset to have the expected name, size, uploaded state, stable official API asset ID/URL and GitHub SHA-256 digest. Draft browser URLs may legitimately contain `/untagged-.../`; they are never offered for downloading. Only then publish and mark Latest, fetch the published metadata again, and require the canonical official browser URL and unchanged digest. Never overwrite an existing version or move Latest backwards.
8. Upload one **LifeMate-Release-APK** Actions artifact containing one APK. No debug APK, reports, certificates, keystores, metadata files or passwords are uploaded. GitHub itself wraps Actions artifacts in ZIPs and adds standard source archives to Release pages.

## Version contract

```text
versionCode = 100000 + GITHUB_RUN_NUMBER
versionName = 1.2.<run number>
tag         = v1.2.<run number>
APK         = LifeMate-<versionCode>.apk
```

A **new workflow run** increases the version. A rerun retains its identity, so already-published versions are never overwritten. If publication leaves a draft on failure, inspect it privately; do not turn an unverified draft into a published release. Start a new run after correcting the failure. Do not reset workflow history or reduce the version offset; retain this application ID, key, and Room migration discipline.

## Update trust and installation

The required-update dialog / Settings update screen compares numeric version codes and only offers strictly newer, canonical official assets. It rejects malformed/foreign/non-HTTPS URLs, leading-zero/malformed versions, draft/prerelease releases, duplicate assets/proofs, missing digests, invalid asset types/states/sizes, and missing/forged signatures. The metadata signature must verify against the **installed app's certificate**, not an arbitrary key supplied by the server. The signed APK digest must match GitHub's asset digest.

The app retains the authenticated byte size, SHA-256 and signer from the proof. A user-requested download streams into private cache storage with percentage/MB progress; only HTTPS redirects to GitHub's exact supported asset hosts are allowed. Wrong size/hash, unsafe redirects, partial files, non-APKs, wrong package/version/signers and debuggable APKs cannot become installable updates. The complete file is checked again before installer handoff. Android performs final cryptographic APK verification and installation.

Automatic checks start only in the foreground, at most once per six hours. Clock rollback does not trigger a request loop. Manual checks intentionally bypass that automatic interval. Offline, timeout, rate-limit, API and invalid-proof errors are not reported as "up to date". The redesigned app always performs these rate-limited foreground checks. A verified newer version requires installation; missing connectivity alone does not invent a lock, and a known verified requirement survives offline/stale responses. Only public version/connection information reaches GitHub, not profile, notes or media. Debug/instrumentation builds do not auto-check production releases.

Download stays inside LifeMate. After verification, **Install update** opens the native Android package installer using a narrowly scoped, read-only FileProvider content URI. `REQUEST_INSTALL_PACKAGES` is declared for this explicit user action; Android may first ask you to allow LifeMate as an installation source. Return and tap Install update, then confirm Android's prompt. There is no browser, root operation, silent install, or permission bypass. Do not uninstall to update—uninstalling deletes local data. Back up before important updates and stop on a signature conflict rather than bypassing it.

## Acceptance limits

A signed APK is not automatically store-approved or physically tested on every phone. Android 8.0+ is required. Verify ARM hardware, 16KB page-size devices, OEM restrictions, biometrics, media and real signed upgrades using the checklist in VERIFICATION.md. Until signing secrets exist and the signed job passes, no production-ready install/upgrade claim is made.


Unfinished foreground downloads stop when leaving the app and restart from the beginning on retry; partial files are discarded. Completed cached APKs are revalidated and reused when the download button is pressed again, including after process recreation. Cancelling download/permission/installation never unlocks an outdated app. A verified required update remains required until a new installed version satisfies it; private backup export and Close app remain available. Builds 1.2.52 and earlier must install the newer APK once to acquire this native updater.
