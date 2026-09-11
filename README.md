# LifeMate

**Your Personal Life Assistant.** A native, offline-first Android app built with Kotlin, Jetpack Compose, and Material 3. Calm jade-and-cream light styling, a coordinated dark theme, and real local data—no static screens or seeded production demo records.

## APK downloads & verification

[**Download the last verified build artifacts**](https://github.com/thisrony506-prog/LifeMate/actions/runs/34612828020) → **Artifacts → LifeMate-build-and-reports**.

- `app-debug.apk`: installable testing build.
- `app-release-unsigned.apk`: optimized release; requires your own signing key before installation/distribution.
- That build (source commit `43c2f04`) passed Kotlin compilation, Android lint, all 18 JVM tests, and release/debug assembly. Its workflow is marked failed because two UI test selectors failed, not because APK assembly failed.
- The current source also includes subsequent historical-completion filtering corrections, backup tests, and locally corrected UI test selectors. The final rerun and actual process-death/reboot notification smoke checks remain **unverified** after GitHub authentication expired. The user chose to hand off the current project rather than reconnect in this session.
- APK binaries are hosted as GitHub Actions artifacts, not committed to source. Artifacts are subject to GitHub retention; rebuild with the commands below if they expire.

## Run it

1. Open this repository in Android Studio Ladybug or newer.
2. Install Android SDK 35 and use JDK 17.
3. Sync Gradle, choose an Android 8.0+ device, and run `app`.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
./gradlew connectedDebugAndroidTest  # requires a running emulator/device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The checked-in Gradle wrapper downloads Gradle 8.10.2. Dependencies come from Google and Maven Central. No API key, backend, account, or internet connection is needed at runtime.

**Build outputs:** `app/build/outputs/apk/debug/app-debug.apk` is installable for testing. `app/build/outputs/apk/release/app-release-unsigned.apk` is an optimized **unsigned** release; sign it with your own retained production key before distribution. Never commit signing keys. Updates must retain the application ID and signing key.

GitHub Actions runs compilation, lint, JVM tests, release/debug assembly, and Android 15 emulator tests. APKs and reports are attached to each workflow run. See [verification notes](docs/VERIFICATION.md) for the exact tested scope and remaining device checks.

## Working features

- **Profile & onboarding:** required full name; optional preferred name, nickname, birthday, photo, introduction, and personal information; later editing and personalized greetings.
- **Home:** live task list, completion ring, upcoming birthday, missions, quick creation, global search, and floating add menu. Empty states invite creation rather than displaying fictional records.
- **Routines & reminders:** date/time, custom messages, sound picker, vibration, importance, notes, attachments, once/daily/weekdays/weekends/specific weekdays/weekly/monthly/yearly/every-N-days schedules.
- **Missions:** 1/2/3/7/30/custom-day duration, daily targets, date-specific check-ins, calendar history, current/best streak, percentage, elapsed days, remaining calendar days, and unfinished targets.
- **Habits:** daily check-ins, editable history, weekly/monthly counts and rates, calendar heatmap, current/best streak.
- **Birthdays:** names/nicknames, relationship, date, notes, optional photo attachment, upcoming ordering, configurable 7/3/1/day-of reminders.
- **Wishes & cards:** seven offline tones, editable personalized messages, copy/share, three card backgrounds, serif/modern type, real rendered PNG preview, save/share image. Messages are never automatically sent.
- **Notes:** text/checklists, important flag, tags, search, sorting, pin/unpin, archive/restore, edit/delete, and media/audio attachments.
- **Memories:** system photo/video picker, private local copies, captions/dates/notes, image preview, in-app system VideoView playback, explicit sharing.
- **Voice:** microphone permission at point of use, record/pause/resume/save/play/pause/rename/delete, local AAC/M4A files. Recording stops and saves when backgrounded; there is no background recording service.
- **Goals:** descriptions, target date/time reminder, short-/long-term category, manually adjustable progress, and checklist milestones.
- **Calendar:** date navigation, type-colored indicators, details navigation, selected-day event creation and check-ins.
- **Statistics:** daily chart, last-seven-days/current-month rates, totals, routine consistency, mission counts, upcoming birthdays, habit streaks.
- **Settings:** notification access, exact-alarm access, global sound/vibration, appearance, profile, app lock/strong biometrics, export/import/backup/restore, typed-confirmation delete-all, privacy, terms, support.

## Architecture

```
ui/             Compose screens, theme, state, ViewModel
navigation/     Bottom destinations; NavHost lives in LifeRoot
domain/        Recurrence/streak rules and future assistant-provider boundary
 database/      Room entities, indexed DAO, encrypted database
 data/          Repository, Preferences DataStore, validated ZIP backups
 notifications/ AlarmManager, broadcast receivers, WorkManager reconciliation
 utils/         Keystore/PIN, voice recording, share intents, card rasterization
```

A singleton profile owns typed `LifeItem` records through a Room foreign key. Completions use `(itemId, date)` as their primary key. Attachments, delivery receipts, and scheduled alarms reference their owning item with cascading deletion. The encrypted database is the source of truth; Compose observes Room flows through a ViewModel. Writes and file operations run off the UI thread. No destructive-migration fallback is used. Schema version 1 is the initial release; future database changes must add explicit tested migrations.

## Reminder behavior

- One durable alarm plan per record, using distinct immutable, URI-addressed PendingIntents.
- Exact `setExactAndAllowWhileIdle` only when permitted; graceful `setAndAllowWhileIdle` fallback otherwise.
- Per-sound/vibration/importance Android notification channels; notification deep links open the correct detail screen, behind app lock when enabled.
- Duplicate receipts enforce at-most-once posting for a record/occurrence. There is a narrow crash window between persisting a receipt and posting to Android; exactly-once delivery across process death is not promised.
- Restart, package update, clock/timezone change, and exact-alarm grant receivers enqueue reconciliation. A 12-hour WorkManager safety net repairs interrupted scheduling. Persisted pending alarms up to 12 hours late are recovered; older occurrences are skipped.
- Local wall-clock semantics; DST gaps shift forward, overlaps use the earlier offset. Monthly dates clamp to shorter months; February 29 birthdays use February 28 in non-leap years.
- Notifications require permission and enabled channels. Do Not Disturb, manufacturer battery restrictions, force-stop, powered-off devices, and Android idle quotas can prevent/delay alerts. Reopen after force-stop. **Not suitable as the sole medical, emergency, or safety-critical reminder.**
- Important reminders request high-importance notifications, not full-screen intents. LifeMate is not an alarm-clock or calling app and does not request restricted full-screen access.

## Privacy & security

- No INTERNET permission, analytics, ad SDK, account requirement, automatic media upload, or automatic cloud backup.
- SQLCipher encrypts structured data. A random database key is AES-GCM wrapped with Android Keystore.
- PINs use random salts and PBKDF2-HMAC-SHA256 (120,000 iterations), with encrypted storage and a 60-second lockout after five failures. Optional strong biometrics use Android's BiometricPrompt. PIN protection enables screenshot blocking and relocks after 30 seconds away.
- Media/audio live in private internal storage, relying on Android sandbox/device encryption rather than separate file encryption. Exported/shared files leave that protection only after an explicit user action.
- ZIP backups are **unencrypted**, clearly warned before export, include profile/records/completions/media, and exclude PIN/device settings. Restore validates version, paths, archive expansion limits, dates, IDs, and relationships before an atomic database replacement. Invalid restore leaves current data intact.
- Import limit: 2 GB total, 100,000 archive files, 32 MB JSON. Media imports: 250 MB/file. Voice: 10 minutes. No artificial normal record-count quota; device storage is the practical limit.
- Original gallery files and previously exported backups are never deleted when removing LifeMate's data.

## AI extension

`AssistantProvider` provides an optional future integration seam. Shipping wishes/cards are deterministic and offline. To add AI, use an explicitly consented backend, keep provider keys on that backend, and preserve the offline path. No external AI integration is shipped or required.

## Release checklist

Run the tests, complete the physical-device checklist, retain a production signing key, verify native-library compatibility on your deployment targets (SQLCipher 4.9.0 is built with flexible page-size support), add explicit Room migrations for subsequent schema versions, and complete your store privacy disclosures. A successful compile alone is not a production certification.
