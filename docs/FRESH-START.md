# Personal Life OS clean replacement

## Approved scope

The owner explicitly selected removal of the old feature set **and old personal data**. No migration or hidden “retained tools” entry point is intended. New Flutter routines, birthdays, memories and goals implement the latest brief, not the removed native screens.

## One-time reset contract

`FreshStartReset.run` executes in `Application.onCreate`, before Flutter opens Hive. It cancels old jobs/posted notifications, deletes old app databases and private files/preferences/media, removes app-scoped Android Keystore data keys, then synchronously writes `no_backup/personal-os-fresh-start-v1`. It does not touch the APK signing key, which is not stored in that app namespace. Only `release_updates.xml` and its backup are exempt from preference deletion; the updater still verifies those bytes against the installed certificate.

The marker is checked before any destructive operation on every later launch. A failed reset is not marked complete; retry must finish before Flutter starts. Symbolic links are deleted, not traversed. Android 34+ supports cancelling all pending alarms; older alarm intents target removed receivers and cannot execute. New notification receivers have distinct plugin components.

Scope is app-owned accessible local storage. This is logical deletion/key removal, not a forensic secure-wipe guarantee. Gallery originals, separately exported files, cloud records and unavailable removable storage are not erased by this app update. Public old GitHub releases/history are not deleted.

## Required tests before publication

1. Pure reset-file tests: delete personal files; keep only updater exceptions; never follow a link into a gallery fixture.
2. Instrumentation: create old database/private sentinel and updater fixture; reset; prove sentinels absent and updater cache retained. Create a new sentinel; run again; prove no repeated deletion.
3. Flutter engine: start new onboarding, save a new name, reach Money, recreate activity and confirm the name still exists.
4. Signed device test: install actual previous official APK, create old profile, install new signed APK with `adb install -r` (no uninstall). Old native baseline must lead to new onboarding without importing its profile. Save a new profile; process restart must retain it. A subsequent Personal Life OS-to-Personal Life OS update must not reset it.
5. Verify new encrypted Hive/media reopen; backup wrong-passphrase/tamper handling; journal editor and list background relock; all five tabs, language/dark mode and demo isolation.
6. Verify notification delivery/reboot, cancellation/completion and permission denial using the new plugin. Old native-alarm test results do not certify this scheduler.
7. Recheck required-update gating, same-key proof/cache, private download, cancellation and Android installer confirmation with the new Flutter controls.

No physical-device or iOS results are implied by this plan. Publication remains disabled until the appropriate build/device/signing checks are observed passing.
