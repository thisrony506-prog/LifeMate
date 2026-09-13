# Release maintenance

## Identity must remain unchanged

- Package `com.lifemate`, minimum Android 8.0.
- `versionCode = 100000 + GITHUB_RUN_NUMBER`, version `1.2.N`, tag `v1.2.N`, asset `LifeMate-<code>.apk`.
- Retain the existing production signing key and four repository secrets: `LIFEMATE_KEYSTORE_BASE64`, `LIFEMATE_STORE_PASSWORD`, `LIFEMATE_KEY_ALIAS`, `LIFEMATE_KEY_PASSWORD`.
- Never print/request credentials in chat, check in a keystore, regenerate an identity or fall back to debug/disposable signing. The existing signing setup is complete. `scripts/setup-release-signing.sh` is an initial-setup reference only; do not rerun it against the existing identity.

## New first-release behavior

This is an owner-approved clean replacement, **not a data-preserving migration from the old native organizer**. Release notes must prominently disclose one-time deletion of old app-owned private local data. The marker preserves new data across later starts/upgrades. Existing cloud/export/gallery copies are outside the reset. See [fresh-start contract](FRESH-START.md).

The release job is held until clean-replacement verification passes. After green Flutter/Android jobs, it must verify the actual previous published APK and new APK with Build Tools 35.0.0, compare retained signer, perform real in-place installation and the correct reset/persistence assertions, then publish a draft and verify uploaded bytes before making it public. The sole Actions artifact/asset is the signed APK.

Metadata stays `lifemate-update-v1`: code, version, canonical official URL, byte size, SHA-256, signer fingerprint and RSA/SHA-256 signature. The canonical signed payload starts `LifeMate-Update-V1\n`. Do not weaken it to support the UI rewrite.

The new Flutter gate uses the same native metadata/download/archive verification. Known newer versions remain required offline. Foreground checks are throttled to six hours; manual checks bypass cadence. Private download and native installation still require Android source approval and final confirmation. Backup export and Close remain available, not ordinary feature navigation. API/network failures must not display a false “up to date”.

No signing/upload occurs in untrusted PR checks. No debug/report/screenshot/checksum/logo/document artifacts are published. Keep generated SDK hosts, caches, APKs and keys out of Git.
