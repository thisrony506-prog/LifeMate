# Download LifeMate Release APK

## One download only

1. Open **Actions → LifeMate Release APK**.
2. Select a successful run for `arena/01a090c4-lifemate` (or use **Run workflow** on that branch when available).
3. Both **Verify app features** and **Build Release APK** must pass.
4. Under **Artifacts**, download **LifeMate-Release-APK** and extract GitHub's ZIP.

The artifact contains **exactly one APK**, with no debug APK, test report, screenshot, checksum or readme file. Build reports remain internal to the runner; diagnostics appear only in ordinary Actions logs/annotations. Older runs are historical and may still have multiple artifacts.

The APK is the optimized, R8-minified, non-debuggable **release** variant—not a renamed debug APK. Version 1.1.1 has versionCode 3 and the same application ID/database schema as before.

## Optional automatic signing with your retained key

In GitHub **Settings → Secrets and variables → Actions**, configure these four **repository secrets** privately:

| Secret | Value |
| --- | --- |
| `LIFEMATE_KEYSTORE_BASE64` | Base64 encoding of your retained production keystore |
| `LIFEMATE_STORE_PASSWORD` | Keystore password |
| `LIFEMATE_KEY_ALIAS` | Existing signing key alias |
| `LIFEMATE_KEY_PASSWORD` | Signing key password |

Encode the keystore locally, without printing its content into chat or public logs. Do not commit the keystore, its base64 encoding or passwords. Keep a secure backup of the original key.

When all four secrets are present, the job decodes the keystore into the private runner's temporary directory, signs with it, and removes the temporary file in an always-run cleanup step. The only downloadable file is `LifeMate-release.apk`. Partial signing configuration fails explicitly rather than silently releasing unsigned. Signing material is never uploaded as an artifact.

Without any signing secrets, the only file is `LifeMate-release-unsigned.apk`. **Android will not install it until it is signed.** CI does not create a disposable key or use a debug key. Builds from fork pull requests normally do not receive repository secrets and therefore remain unsigned.

## Sign locally instead

Use Android Studio **Build → Generate Signed App Bundle / APK → APK**, selecting your own retained key or creating one securely. Do not send the key or its passwords in chat.

Alternatively, use Android SDK Build Tools:

```sh
zipalign -P 16 -f -v 4 LifeMate-release-unsigned.apk LifeMate-aligned.apk
apksigner sign --ks /private/path/lifemate.jks --out LifeMate-release.apk LifeMate-aligned.apk
apksigner verify --verbose LifeMate-release.apk
```

`apksigner` prompts locally for credentials. Updates must keep the application ID and signing key, and use an appropriate versionCode. For Google Play, use Play App Signing and retain the appropriate upload key.

A successful unsigned build does not test secret-based signing, real signed upgrades or every physical device. See `VERIFICATION.md` for acceptance boundaries.
