# LifeMate

**Your Personal Life Assistant.** A native, offline-first Android app built with Kotlin, Jetpack Compose, and Material 3. Calm jade-and-cream light styling, a coordinated dark theme, and real local data—no static screens or seeded production demo records.

## Installable APKs and safe updates

**[Download signed LifeMate 1.2.55 (Android 8+)](https://github.com/thisrony506-prog/LifeMate/releases/download/v1.2.55/LifeMate-100055.apk)** — versionCode `100055`, approximately 22.8 MB. Old unsigned 1.1.x downloads cannot be installed; use this signed release instead.

Signing is configured and verified. **Do not generate a replacement key or repeat initial setup.** Keep an encrypted offline backup of the original JKS and its password separately. [Release maintenance and one-time setup reference](docs/RELEASE.md) explains recovery; never send private signing material into chat.

The release workflow retains `com.lifemate`, assigns `100000 + GITHUB_RUN_NUMBER` / `1.2.<run number>`, verifies `apksigner` continuity, and checks a signed data-preserving upgrade before publication. Successful runs expose one signed `LifeMate-<versionCode>.apk`, not debug/report/signing downloads.

Update controls are in **Settings**. Foreground checks run at most once every six hours, and a manual check is available. A **verified newer signed version** displays a non-dismissible update dialog: download and install it to continue. **The APK downloads inside the popup with percentage/MB progress.** Signed size/SHA-256 and APK identity checks must pass before **Install update** opens Android's installer—no browser. If asked, allow LifeMate in Android's installation-source settings, return and tap Install update. Android still requires confirmation; LifeMate cannot install silently. Backup export and closing the app remain available. No connection/API error alone triggers a lock, but an already-verified required update remains required offline. Same-certificate metadata, official asset URL and GitHub digest checks are retained.

**Verified:** [run 34707758773](https://github.com/thisrony506-prog/LifeMate/actions/runs/34707758773), source `64526e2`, passed all three jobs: compilation/lint, **49 JVM tests**, **17 script tests**, Android feature/UI/card tests and real notification checks. The signed emulator test upgraded the actual published **1.2.52 → 1.2.55**, retaining its encrypted profile. Publication completed automatically; Actions contains exactly one **LifeMate-Release-APK** artifact. See [verification evidence](docs/VERIFICATION.md). Older installed builds (1.2.52 and earlier) need this APK installed once before they gain the new browser-free updater.

## A cleaner, more personal LifeMate

- Feature-colored collections, forms and details, grouped by planning, growth and memories/celebrations. Eight readable primary accents and coordinated dark mode.
- Pink birthday wishes and a **private photo/card studio**: local photo import, editable text, five font styles, color palettes, photo effects, rotate/crop zoom, text placement and portrait/square/landscape PNG output. Save or share to Facebook yourself; nothing is posted automatically. Draft text is encrypted on-device.
- **Weekly insights** on Home and Statistics: scheduled/completed/remaining work, missed past tasks, same-weekday comparison and each mission's weekly/full progress. No invented scores or future tasks counted as missed.
- **Opt-in voice reminders** through an installed offline English/Bangla Android TTS voice, with quiet/privacy checks, a Stop notification and a 30-second limit. Text notifications remain the fallback. Phone/OEM/voice compatibility must be checked on a real device.

See [feature behavior, privacy, backup limits and manual-device checks](docs/UX-UPDATE.md). Studio drafts/photos are not in record ZIP backups: save completed images separately. Existing organizer features and the supplied logo are retained.

## Design and branding

The user-supplied blue person, green leaves and yellow rays logo now appears on the adaptive launcher icon, Android 13 themed icon, welcome/loading screens, dashboard, inner-screen header and About card. Matching single-color icons identify notifications. The original upload is retained as `20260912_111618.png`; `bash scripts/update-brand-assets.sh` reproducibly sizes it for Android without redrawing it. Clear sans-serif headings, coordinated light/dark colors, distinct feature badges, descriptive feature cards and an animated live progress ring make the existing offline tools easier to explore. Profile editing remains accessible through the dashboard shortcut and Profile tab. No sample records are inserted into the shipping app.

## Run it

1. Open this repository in Android Studio Ladybug or newer.
2. Install Android SDK 35 and use JDK 17.
3. Sync Gradle, choose an Android 8.0+ device, and run `app`.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
./gradlew connectedDebugAndroidTest  # requires a running emulator/device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The checked-in Gradle wrapper downloads Gradle 8.10.2. Dependencies come from Google and Maven Central. No API key, backend, account, or internet connection is needed for core offline features. Optional update checks and APK downloads require a connection.

**Build outputs:** `app/build/outputs/apk/debug/app-debug.apk` is installable for testing. `app/build/outputs/apk/release/app-release-unsigned.apk` is an optimized **unsigned** release; sign it with your own retained production key before distribution. Never commit signing keys. Updates must retain the application ID and signing key.

GitHub Actions runs compilation, lint, JVM tests and Android 15 emulator tests internally, then publishes only the release APK. See [verification notes](docs/VERIFICATION.md) for the exact tested scope and remaining device checks.

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

- No analytics, ad SDK, account requirement, automatic media upload or automatic cloud backup. INTERNET is limited to public update metadata and user-requested official APK downloads. Foreground checks are rate-limited; verified newer versions require installation. No personal records are sent.
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
