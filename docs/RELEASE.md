# Build and download LifeMate

## GitHub Actions

1. Open **Actions → Android verification**.
2. Select a run for branch `arena/01a090c4-lifemate` (or use **Run workflow** on that branch when available).
3. Wait for **build** to finish. The independent **device-tests** job reports verification separately.
4. Download **LifeMate-Release-APK** from **Artifacts** and extract the ZIP.

The download contains `LifeMate-release-unsigned.apk` and its SHA-256 checksum. It is the optimized, R8-minified **release** build, not a renamed debug APK. It must be signed before installation. **LifeMate-Debug-APK** is separately available for immediate device testing.

Do not install an unsigned APK expecting Android to accept it. Keep a stable private signing key for production updates; replacing that key prevents updates to existing installations.

## Signing your release

Use Android Studio **Build → Generate Signed App Bundle / APK → APK**, selecting your own existing key or creating one securely. Do not send the key or its passwords in chat, commit it, or upload it as an artifact.

Alternatively, use Android SDK Build Tools:

```sh
zipalign -P 16 -f -v 4 LifeMate-release-unsigned.apk LifeMate-aligned.apk
apksigner sign --ks /private/path/lifemate.jks --out LifeMate-release.apk LifeMate-aligned.apk
apksigner verify --verbose LifeMate-release.apk
```

`apksigner` will prompt locally for the keystore credentials. Retain a secure backup of the signing key. For Google Play, use Play App Signing and retain the appropriate upload key.

No production signing material is included in this repository. A successful APK build does not replace the device acceptance checklist in `VERIFICATION.md`.
