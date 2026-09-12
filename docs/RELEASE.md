# Installable LifeMate releases and updates

## Why the old APK would not install

The earlier 1.1.x downloads were unsigned. Android cannot install unsigned APKs. They have not been repaired retroactively; do not use them. The new workflow **refuses publication without retained signing secrets**. Debug or disposable keys are never substituted.

Supported phones require Android 8.0 (API 26) or newer. A different already-installed signing identity, insufficient storage, or a damaged download can also prevent installation. Do not uninstall an app holding important data just to bypass a conflict; export a private backup first.

## One-time private signing setup (owner action currently required)

The Arena GitHub integration currently returns HTTP 403 for repository Secrets management. No key was created in the shared workspace, because it could not be stored safely as repository secrets. Reconnect/configure the GitHub integration with the required repository Secrets permission, or use your own authenticated GitHub CLI on a trusted computer/private Codespace.

With JDK 17, OpenSSL and GitHub CLI available, and your own GitHub session authorized to manage this repository's Actions secrets:

```sh
bash scripts/setup-release-signing.sh "$HOME/lifemate-private-signing-backup"
```

The script checks permission and existing secrets FIRST, refuses to overwrite an identity, creates one 3072-bit RSA JKS key, and privately uploads four repository secrets:

- `LIFEMATE_KEYSTORE_BASE64`
- `LIFEMATE_STORE_PASSWORD`
- `LIFEMATE_KEY_ALIAS`
- `LIFEMATE_KEY_PASSWORD`

The original keystore and password remain in that **private folder outside the repository**. Make an encrypted offline backup before deleting a Codespace or computer copy. GitHub cannot reveal secret values later. If uploading one secret fails, retain the same folder/key and finish configuring the remaining secrets—do not generate a replacement identity.

Never send keys, passwords, tokens or backup files in chat; never commit them or publish them as Actions artifacts. An existing signed installation must use its existing key instead of this new-key setup.

## Publish and install

After secrets are configured, start a **new** run of **LifeMate Release APK** on `arena/01a090c4-lifemate` using Run workflow, or push a source change.

1. Emulator feature/security tests, compilation, lint and JVM tests run.
2. The release is signed with the retained key, and verified with `apksigner`.
3. If a prior official release exists, CI downloads it and verifies signer continuity. For the first release, a lower-version signed test fixture is generated internally.
4. An emulator installs the earlier APK, creates a real encrypted profile through the UI, then performs `adb install -r` with the new non-debuggable APK and checks the profile survives.
5. Only after these checks, a versioned APK is published to GitHub Releases and one **LifeMate-Release-APK** Actions artifact. No reports, debug APKs, companion JSON, checksums, or readme downloads are generated. GitHub's normal artifact ZIP and automatic release source archives are platform packaging.

Open the APK from your browser/Downloads and confirm Android's install prompt. Android may ask you to allow installs from that browser. Future official versions should show **Update**, not require uninstalling.

## Versions and in-app update notice

This single workflow assigns `versionCode = 100000 + GITHUB_RUN_NUMBER` and `versionName = 1.2.<run number>`. Every new run advances the version. A rerun retains its original identity: published versions cannot be overwritten, and an older run cannot move Latest backwards. Starting a new workflow run is the way to publish another update. Do not replace/delete the workflow's version history or reduce this version offset without a migration plan.

Releases use immutable tags `v1.2.<run number>` and assets `LifeMate-<versionCode>.apk`, at the fixed official repository. App updates compare numeric versionCode, reject foreign/malformed URLs and old unsigned assets, and only offer a strictly newer release.

The Home header opens App updates and shows a notice when a newer release is found. Automatic metadata checks run in the foreground at most every six hours and can be disabled. Manual checks remain available. GitHub receives the public app version and ordinary connection data, not personal records. Core functions remain offline. Debug/instrumentation builds do not check automatically.

Download opens the official HTTPS APK in the browser; Android owns the final installation prompt and verifies the app signature. There is no silent installer, root command, or permission to install packages in LifeMate. An offline/API error is reported, not mistaken for "up to date".

## Verification boundary

Until private signing secrets are actually configured, only code/emulator checks can complete: signed installation, upgrade continuity and release publication remain blocked. Do not describe this as a successful installable release until the final signing job passes. Physical-device/OEM and signed-update acceptance still matters; see VERIFICATION.md.
