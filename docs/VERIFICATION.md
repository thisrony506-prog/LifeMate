# LifeMate verification

## Current signing hardening audit

This revision repairs the interactive signing setup (JDK 17, owner authentication/access checks, locally entered password, fixed private backup path, no plaintext password file, no overwrite) and authenticates update metadata with the installed app's retained signing certificate. It also pins the Gradle distribution SHA-256 and makes APK signing schemes explicit. No unrelated profile, database, reminder or media behavior was changed.

- Local: 11 Python/PTY safety checks passed using fake tools and inert fixtures; Bash syntax and whitespace checks passed. These tests never create a production signing identity.
- Hosted: [run 34680342469](https://github.com/thisrony506-prog/LifeMate/actions/runs/34680342469), source `c2ee94f`, passed both **Compile, lint and unit tests** and **Verify app features**. Gradle/R8, lint, all **34 JVM tests**, **11 Python safety tests**, and JDK 17 compilation of `SignReleaseMetadata.java` passed.
- Android 15 emulator: zero test failures, including authentic/forged release metadata, asset digest/type/URL/state validation, duplicate-proof rejection, HTTP/offline error behavior, updater UI, navigation, persistence, backup and PIN checks. The opt-in alarm helper was deliberately skipped in the normal suite, then three host-invoked checks passed for actual process-exit/reboot delivery and next recurring alarm persistence.
- Overall run **failed at the signing gate** because `LIFEMATE_KEYSTORE_BASE64` is missing. The Actions API confirms **0 downloadable artifacts**. This is not a signed-build success.
- Requires owner secrets: real JKS signing, `apksigner` continuity, signed installation/upgrade smoke, signed metadata generation, digest-checked draft publication, and final download. No production signing key/password was generated here.

### Signing setup and backups

Run the exact private Codespace commands in [RELEASE.md](RELEASE.md). The interactive script writes only `$HOME/lifemate-private-signing-backup/lifemate-release.jks`; retain an encrypted offline JKS backup and the locally chosen password separately before deleting the Codespace. It uploads four Actions secrets through stdin and refuses existing identities. Never share credentials in chat. Partial upload preserves the original key and must be recovered without regeneration.

### Release and signed update verification

CI publishes only after tests/lint, `apksigner` verification, same-package/same-signer checks, higher version code, and a real `adb install -r` test retaining an encrypted profile. Update metadata is RSA-signed by that same key; the app verifies it against its installed certificate, canonical URL/version and the GitHub asset digest. Only the APK is attached to a draft release, whose uploaded digest is checked before publication. Android—not LifeMate—verifies/installs the downloaded APK. The release tag/file/version formulas and failure recovery are documented in RELEASE.md.

### Physical-device acceptance

In addition to the full checklist below: install the first signed APK on an Android 8+ ARM phone, create records/media, update to a higher version with the same key without uninstalling, and verify records, PIN/Keystore access and reminders survive. Test denied unknown-source permission, failed/cancelled downloads, offline/403/429/malformed update responses, forged metadata, older/equal release suppression, disabled automatic checks, six-hour throttling and explicit Android confirmation. Test real 16KB page-size hardware/emulation and OEM background restrictions. Signed pipeline success alone does not certify all of these.


## Earlier updater verification (historical 1.2 baseline)

Current work adds strictly increasing CI version codes, fixed-repository HTTPS release checks, Home update notices, a manual update page, an automatic-check preference, and fail-closed signed publication. Pure version/URL tests and UI navigation/banner tests were added. The signed-release job checks signer continuity, actual installation and profile retention across `adb install -r` before publishing.

[Updater verification run 34678452031](https://github.com/thisrony506-prog/LifeMate/actions/runs/34678452031), application commit `cf65f56`: compilation, R8 assembly, lint and all **26 JVM tests** passed. Android emulator tests passed with zero failures, including the update banner, equal-version suppression, updates-page navigation and persisted automatic-check preference. Real notification delivery after process exit/reboot and next-recurrence persistence passed again.

**Overall run: failed at the required signing gate**, specifically missing `LIFEMATE_KEYSTORE_BASE64`. The Actions API confirms **zero downloadable artifacts**: no unsigned APK was published. Signed APK installation, actual data-preserving upgrades and release publication remain **unverified/blocked**. A subsequent static check also verified the host smoke script can match non-clickable profile text and locate the input ancestor; that is not a substitute for running it on a signed app.

Rechecking after the user's reconnect choice still returned HTTP 403 for repository Secrets access. No new private key has been created. The owner must complete the secure setup in RELEASE.md (or grant the integration the required permission). Historical unsigned build results below do not prove the new signing path works.


## User-supplied logo update — LifeMate 1.1.1

The original uploaded `20260912_111618.png` replaces the previous mark throughout the app and on the launcher. Only the connected outer black border is removed for the in-app asset. The adaptive icon preserves the colored emblem on a pale background; Android 13 themed icons and notification icons use a matching alpha silhouette. Asset generation is reproducible with `scripts/update-brand-assets.sh` and does not use generative imagery.

Local checks: resource XML parses, the foreground/themed alpha masks match, and the emblem's 154.5px radius fits inside the 156.4px adaptive safe circle at 512px resolution. The app version is 1.1.1 / code 3; features, application ID, database schema and release-only workflow are unchanged. Android build/device verification passed in the run below.

## Historical unsigned build — LifeMate 1.1.1

[LifeMate Release APK — run 34677197968](https://github.com/thisrony506-prog/LifeMate/actions/runs/34677197968)

Verified application/test/workflow commit: `117709b` (12 September 2026). Both **Verify app features** and **Build Release APK** completed successfully. Android tests ran on an Android 15 (API 35), x86_64 emulator. These are real native-app tests, not browser or static mockup checks.

The supplied logo is bundled in all branded surfaces and Android icon variants. Existing professional styling and offline features are preserved. The navigation regression test opens the profile shortcut and all eight feature cards. The PIN-dialog test now waits for the actual settings UI after asynchronous storage completes, rather than racing the dialog dismissal; its security assertions remain intact. No production fixture data was added.

The Actions API confirms **exactly one artifact**, `LifeMate-Release-APK` (ID `10292543851`). The successful packaging step enforces one APK file and rejects debuggable APKs. There are no debug/report/screenshot/checksum/readme downloads in this run. No private signing secrets were configured: this verified release is **unsigned**, and must be signed before installation. Actual secret-based signing and signed upgrades remain untested.

| Area | Verified result |
| --- | --- |
| Kotlin / Compose | Debug and release compilation passed |
| Release | `assembleRelease` and R8 minification passed; unsigned APK produced |
| Internal test build | Debug/test APKs built for emulator verification only; not published |
| Lint | Android lint completed without blocking errors |
| JVM rules | 22 tests: 19 recurrence/streak cases and 3 historical/archived-progress regressions |
| Navigation / branding | Visible LifeMate logo, dashboard profile shortcut, all eight feature cards, Home/Missions/Calendar/Memories/Profile, settings, and light/dark appearance controls |
| CRUD / persistence | Note creation through real UI, Activity recreation, encrypted database close/reopen, completion uniqueness, cascading deletion |
| Missions | Daily completion and undo through the dashboard, persisted in Room |
| Scheduling | Durable alarm plan and PendingIntent registration; reconciliation after scheduler recreation |
| Birthdays | First lead-time alarm on device; 7/3/1/day-of, leap birthdays, and New Year boundaries in JVM tests |
| Backup | Full ZIP round trip with metadata and media; unsafe archive rejected without replacing current data |
| PIN | Keystore-backed key stability, PIN persistence, wrong-PIN rejection, cooldown, and correct unlock |
| App lock UI | PIN required after recreation; open private dialog removed after 31 seconds in the background; unlock restores navigation |
| Themes | Live light/dark settings and real Compose render/capture test passed; screenshots are not published |
| Permission fallback | Scheduling without notification access/exact-alarm access; actual delivery with permissions granted in the system smoke stage |
| Process exit | Actual Android notification observed after instrumentation ends and `am kill` terminates the app process |
| Device restart | Actual Android notification observed after `adb reboot`, boot completion, and unlock |
| Recurrence after delivery | The next-day alarm plan persisted after both real system deliveries |
| Duplicate receipts | Room rejected a second receipt for the same record and occurrence |

The external alarm seed test intentionally skips the ordinary suite. The host script explicitly runs it three times: seed a process-death check, seed a reboot check while validating the previous recurrence, and validate the final recurrence. All three completed with `OK (1 test)`.

The host logged:

```text
PASS: process-death notification delivered by Android after UI/process exit
PASS: reboot notification delivered by Android after UI/process exit
PASS: recurring next-day alarm persisted after both real deliveries
```

Test fixture names and records exist only in `androidTest`; fresh installations show onboarding and empty states.

## Release boundaries

Without configured private signing secrets, the release APK is **unsigned**. A retained production signing key is necessary for installation/distribution and safe future updates. Do not substitute an ephemeral CI signing key for a production identity.

Passing an emulator suite does not certify all OEMs, hardware features, accessibility settings, or store requirements. Camera/media picker, microphone interruptions, strong biometric hardware, custom audio, manufacturer battery restrictions, and real signed upgrades still need physical-device acceptance. Full-screen intents, cloud sync, and network AI are intentionally not shipped; important reminders use high-priority Android notifications.

## Physical-device acceptance (not replaced by unit tests)

- [ ] Android 8, 12, 13, 15 and a 16 KB page-size device; installation and native SQLCipher load.
- [ ] Deny/allow/revoke POST_NOTIFICATIONS; inspect silent/normal/custom-sound channels and DND behavior.
- [ ] Grant/revoke exact alarms; confirm fallback timing and foreground status text.
- [ ] Schedule a reminder, close UI, verify a notification and its deep link.
- [ ] Reboot before a due alarm, unlock device, confirm schedule recovery and no duplicates.
- [ ] Check a real recurring delivery, completion suppression, late pending recovery, timezone and DST changes.
- [ ] Birthday notifications 7/3/1/0 days before; leap birthday and New Year offsets.
- [ ] Force-stop app, document expected non-delivery, reopen to recover.
- [ ] Mission day boundaries, history corrections, habit monthly calendar, goal milestones.
- [ ] System picker on Android 8 fallback and modern Android; media deletion leaves gallery originals.
- [ ] Microphone denial, pause/resume, interruption/background save, playback, rename, deletion.
- [ ] Share image/text/video, in-app video playback, PNG preview/save.
- [ ] Light/dark/system appearance, rotation, keyboard, large text, TalkBack, contrast, landscape/tablet.
- [ ] PIN attempts/cooldown, rotation/process death, >30-second background lock, strong biometric success/cancel/unenrolled.
- [ ] ZIP round trip with media, corrupt/oversized/traversal/duplicate-path backup rejection, low storage, cancel document picker.
- [ ] Upgrade an installed signed release without data loss; verify explicit migrations for any future schema version.

## Exact project files changed in this request

Relative to `67b4e57`; `A` = created, `M` = modified. No unrelated app functionality files were changed.

```text
M	.github/workflows/android.yml
M	README.md
M	app/build.gradle.kts
A	app/src/androidTest/java/com/lifemate/UpdateMetadataTest.kt
M	app/src/main/java/com/lifemate/updates/ReleaseInfo.kt
A	app/src/main/java/com/lifemate/updates/ReleaseMetadata.kt
A	app/src/main/java/com/lifemate/updates/ReleaseProof.kt
M	app/src/main/java/com/lifemate/updates/UpdateRepository.kt
M	app/src/test/java/com/lifemate/updates/ReleaseInfoTest.kt
A	app/src/test/java/com/lifemate/updates/ReleaseProofTest.kt
M	docs/RELEASE.md
M	docs/VERIFICATION.md
M	gradle/wrapper/gradle-wrapper.properties
A	scripts/SignReleaseMetadata.java
M	scripts/prepare-signing.sh
M	scripts/prepare-upgrade-apks.sh
M	scripts/publish-release.sh
M	scripts/setup-release-signing.sh
A	scripts/tests/test_release_pipeline.py
A	scripts/tests/test_signing_setup.py
```
