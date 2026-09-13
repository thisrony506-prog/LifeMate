# Life Mate verification — clean replacement

The latest instruction supersedes the retention-based migration: **remove old features and old private local data, keep only Personal Life OS**.

## Executed in this revision

- Removed legacy native feature UI, models, repositories, Room/SQLCipher/Compose feature dependencies, old alarm/voice services and their obsolete feature fixtures.
- Added one-time fresh-reset implementation, file-boundary tests, Android reset/Flutter-host tests, new Flutter-only privacy/update controls and cross-platform plugin notifications.
- Local Python regression suite: **19 passed** after these changes. This includes source/transport/release-policy checks; it is not a Flutter or Android runtime result.
- GitHub authentication works again. In clean-replacement run `34734351337` (`5c89d06`), Flutter analysis and the Flutter test step passed. The Android device runner completed 14 tests with one failure: the host test looked for a Home greeting after recreation while the selected Money tab could be retained. It now explicitly enters Home before checking the persisted name, with visible-label diagnostics if it still fails.
- The compile job failed on the third-party geolocator module's standalone lint, not a disabled application check. Gradle commands are now explicitly scoped to `:app:` so the application is compiled, linted and tested rather than invoking every plugin's upstream development suite. App lint is not disabled and no lint baseline was added.
- All formatter groups and the current dependency lock were recovered from CI. Two further Flutter widget regression checks cover hidden locked content and absence of old feature links. The follow-up run is pending.

## Latest hosted result

Run `34734917030` (`f91035a`) completed the compile/lint/release-assembly and Flutter test job successfully. The device suite again completed 14 tests with only the Flutter host test failing. Its new diagnostics showed a functioning restored Home with the default name **Friend**, not missing data or a blank engine. The test had used accessibility `setText` and could match the outgoing input field before Home appeared. It now types through the real focused input connection and waits for Home before asserting the saved name. That stronger test has not yet been observed passing. The one-time reset and retained update-security tests were not among the reported failures.

Run `34735374318` (`5d61168`) subsequently completed the compile/lint/Flutter test job successfully, and its Android emulator test step also succeeded after the real text-input correction. The full device job also completed successfully. Signed fresh-install/update verification and publication remain held; there is no new release APK yet.

## Not yet verified

- Physical-device regression coverage beyond the hosted Android emulator.
- Actual signed old-to-new reset, future new-data persistence, APK publication and canonical byte checks.
- Real permission/notification/reboot/biometric/gallery/audio/installer flows on physical Android; iOS compilation/signing/device testing.
- Firebase/Gemini deployment, rules emulator and live cloud/location/AI requests. Backend availability must remain honestly labelled.

The most recently inspected pre-rewrite run, `34716431559` (`14e245c`), failed both Android verification jobs; signed publication was skipped. It is not evidence for this new architecture. Last public release known to this work is native **1.2.64**, not the new Flutter product. Its historic tests must not be reused as proof for replaced functionality.

The workflow contains an explicit publication hold. No new APK is claimed until that hold can safely be removed following observed successful verification. Only one signed release APK may be uploaded as an Actions artifact.

## Current optimization/API work

Provider-boundary TypeScript build and **6 backend unit tests passed locally**. **21 Python regression checks passed locally**. Added emulator ownership/revision/expiry/storage tests, isolated crypto work, indexed local reads, bounded photo decoding, release shrinking/compression, same-code APK size comparison and signed startup measurement. These new Flutter/Android/rules checks await hosted CI. The signed release hold is replaced by dependency gates on app, device and backend jobs plus signed fresh-start/publication verification. Live backend deployment remains explicitly deferred until the owner supplies the approved project/access; no live API claim is made.
