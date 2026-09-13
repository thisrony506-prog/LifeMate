# Life Mate verification — clean replacement

The latest instruction supersedes the retention-based migration: **remove old features and old private local data, keep only Personal Life OS**.

## Executed in this revision

- Removed legacy native feature UI, models, repositories, Room/SQLCipher/Compose feature dependencies, old alarm/voice services and their obsolete feature fixtures.
- Added one-time fresh-reset implementation, file-boundary tests, Android reset/Flutter-host tests, new Flutter-only privacy/update controls and cross-platform plugin notifications.
- Local Python regression suite: **19 passed** after these changes. This includes source/transport/release-policy checks; it is not a Flutter or Android runtime result.
- GitHub authentication works again. New clean-replacement hosted verification is pending.

## Not yet verified

- Flutter analysis/tests after the clean rewrite, dependency lock refresh including `flutter_timezone`, Android compilation/lint/R8, new engine/reset/device tests.
- Actual signed old-to-new reset, future new-data persistence, APK publication and canonical byte checks.
- Real permission/notification/reboot/biometric/gallery/audio/installer flows on physical Android; iOS compilation/signing/device testing.
- Firebase/Gemini deployment, rules emulator and live cloud/location/AI requests. Backend availability must remain honestly labelled.

The most recently inspected pre-rewrite run, `34716431559` (`14e245c`), failed both Android verification jobs; signed publication was skipped. It is not evidence for this new architecture. Last public release known to this work is native **1.2.64**, not the new Flutter product. Its historic tests must not be reused as proof for replaced functionality.

The workflow contains an explicit publication hold. No new APK is claimed until that hold can safely be removed following observed successful verification. Only one signed release APK may be uploaded as an Actions artifact.
