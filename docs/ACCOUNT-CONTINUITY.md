# Same app, new Personal Life OS

## Current owner decision (supersedes fresh reset)

Retain the installed app identity, existing user, supplied logo and Life Mate name. Redesign the inside using only the requested calm Personal Life OS modules. Do not restore old feature screens or matrimony features. Do not automatically delete existing personal storage.

- Android package remains `com.lifemate`; retained production signing secrets are unchanged.
- Keep the `1.2.N` version series and numeric-newer `100000 + GITHUB_RUN_NUMBER` updater contract. Freezing the exact version would prevent the in-app updater recognizing a newer release.
- `LifeMateApp` no longer runs any reset. The destructive reset implementation has been removed, not merely bypassed with a marker.
- Original SQLCipher database, private media, preferences, app PIN and Android Keystore data key remain. The compatibility reader opens only the profile table **read-only**, never generates a replacement key, and installs a non-destructive corruption handler.
- Full name, nickname, preferred name, birthday, introduction, personal information and a supported private profile photo are copied into the new encrypted Hive namespace. The prior display-name preference is respected. Existing Personal Life OS identities are not overwritten. The completion marker is written after the copied data is flushed.
- The original app PIN still verifies against the same encrypted secret and keeps the original persisted retry cooldown. A PIN gate hides new routes/backup content before unlocking. Original biometric-only locking is carried into the device-authentication setting.
- Portable backups include the retained profile/photo; additive restore places that identity in a Memory Box archive without overwriting the current account. Original non-migrated storage is not in this new-format backup.
- New-vault deletion is explicit, authenticated and does not erase original storage. It prevents automatic re-import on the next launch.

## Honest migration boundary

The compatibility bridge is **not** a return of the old native UI. Other original records/media are retained in their original storage but are **not yet converted** into the new modules. Keeping original bytes is not a claim that every old record/reminder works in the new UI. Publication stays held until the required continuity behavior is verified and any remaining migration scope is resolved.

## MVP UI/data

Flutter has Home / My Life / Health / Money / Profile; My Day, Mind Mate, Health, Money, Family, SOS, Memory and Goal Board; sage/white/soft-black styling, dark mode, Bangla/English. Hive holds offline records. Dummy data is only in explicitly labelled Demo mode and never silently seeds real user balances, contacts or tasks.

Firebase/Gemini deployment remains deferred by owner choice. SOS uses permission/confirmation-safe system SMS/dialer; it cannot promise silent messaging or three completed calls. Location sharing is opt-in, foreground-only and expires. These limitations must remain visible.

## Verification

Added SQLCipher/profile/key/media/PIN read-only fixture checks, missing-key non-destruction, encrypted Hive adoption/idempotence and PIN visibility/lifecycle tests. Signed verification now expects native→Flutter user retention, followed by another Flutter update retaining that user, never fresh onboarding that replaces them. These new Flutter/Android checks must run before a release claim. Earlier reset tests and pre-bridge size results do not verify this revision.
