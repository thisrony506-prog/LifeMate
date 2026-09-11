# Verification scope

## Automated checks included

- Gradle: debug/release compilation, release R8, Android lint, JVM tests.
- 18 recurrence/streak unit tests: daily/once/far-future/weekday/weekend/specific/custom/monthly, leap birthdays and lead times, mission boundaries, DST gap/overlap, timezone behavior, streaks.
- Encrypted Room device tests: close/reopen persistence, completion uniqueness, foreign-key cascade, delivery receipt uniqueness.
- Compose device tests: real note creation and persisted record after Activity recreation; bottom navigation; persisted light/dark preference; mission check-in/undo.
- Scheduling device tests: actual PendingIntent registration, persisted schedule reconciliation after creating a new scheduler, birthday first lead-time occurrence.

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
- [ ] Share image/text/video, external media-player availability, PNG preview/save.
- [ ] Light/dark/system appearance, rotation, keyboard, large text, TalkBack, contrast, landscape/tablet.
- [ ] PIN attempts/cooldown, rotation/process death, >30-second background lock, strong biometric success/cancel/unenrolled.
- [ ] ZIP round trip with media, corrupt/oversized/traversal/duplicate-path backup rejection, low storage, cancel document picker.
- [ ] Upgrade an installed signed release without data loss; verify explicit migrations for any future schema version.

See the repository's Android verification workflow for execution results. The checked-in tests do not prove OEM-specific scheduling or full production readiness.
