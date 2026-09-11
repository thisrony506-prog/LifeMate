# LifeMate verification

## Latest successful automated run

[Android verification — run 34618393752](https://github.com/thisrony506-prog/LifeMate/actions/runs/34618393752)

Verified application/test commit: `35bddd2` (11 September 2026). Both **build** and **device-tests** completed successfully. Android tests ran on an Android 15 (API 35), x86_64 emulator. These are real native-app tests, not browser or static mockup checks.

| Area | Verified result |
| --- | --- |
| Kotlin / Compose | Debug and release compilation passed |
| Release | `assembleRelease` and R8 minification passed; unsigned APK produced |
| Debug | Installable debug APK produced |
| Lint | Android lint completed without blocking errors |
| JVM rules | 22 tests: 19 recurrence/streak cases and 3 historical/archived-progress regressions |
| Navigation | Home/Missions/Calendar/Memories/Profile, settings, and light/dark appearance controls |
| CRUD / persistence | Note creation through real UI, Activity recreation, encrypted database close/reopen, completion uniqueness, cascading deletion |
| Missions | Daily completion and undo through the dashboard, persisted in Room |
| Scheduling | Durable alarm plan and PendingIntent registration; reconciliation after scheduler recreation |
| Birthdays | First lead-time alarm on device; 7/3/1/day-of, leap birthdays, and New Year boundaries in JVM tests |
| Backup | Full ZIP round trip with metadata and media; unsafe archive rejected without replacing current data |
| PIN | Keystore-backed key stability, PIN persistence, wrong-PIN rejection, cooldown, and correct unlock |
| App lock UI | PIN required after recreation; open private dialog removed after 31 seconds in the background; unlock restores navigation |
| Themes | Live settings changes and screenshots captured from the real Compose app in light/dark modes |
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

The release APK is **unsigned**. A retained production signing key is necessary for installation/distribution and safe future updates. Do not substitute an ephemeral CI signing key for a production identity.

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
