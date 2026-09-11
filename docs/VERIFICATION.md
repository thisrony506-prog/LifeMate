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

## Session results (11 September 2026)

- **Passed**: debug and optimized unsigned release assembly, Kotlin compilation, Android lint, all 18 JVM recurrence/streak tests (GitHub Actions run 34612530827; subsequent build 34612828020 also passed).
- **Passed on Android 15 emulator**: two encrypted Room persistence/uniqueness/cascade tests, two alarm registration/reconciliation/birthday tests, mission check-in/undo, real home capture in light and dark themes.
- **UI test harness fixes pending rerun**: exact text selectors failed on duplicate subtitle labels and the leading spaces in icon-button labels; selectors have been corrected locally. These failures did not report app crashes.
- **Pending**: newest backup round-trip/rejection tests, end-to-end process-death and reboot notification smoke script, final suite rerun after selector fixes, physical-device acceptance list above.
- GitHub authentication expired during artifact retrieval. Reconnect GitHub in Arena to push the last selector fixes and complete verification. Existing APKs remain in the successful build job's artifacts.

The user selected **hand off the current build** after the connection expired. No final rerun is claimed. Source changes are saved on the tracked Arena branch; the last local test-selector changes have not been pushed.
