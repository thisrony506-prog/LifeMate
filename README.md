> **Current owner instruction:** keep the same app/user/name/logo/version series; replace only the inside UI/features. Automatic reset is removed. See [account continuity](docs/ACCOUNT-CONTINUITY.md).

# Life Mate — Personal Life OS

**Your Life, Organized in One App**
**তোমার জীবন, এক অ্যাপে গোছানো**

A Flutter Android/iOS personal life companion: white `#FFFFFF`, sage `#A8C3B9`, ink `#1A1A1A`, dark mode and switchable বাংলা / English. The supplied `20260912_111618.png` logo is preserved.

## Same app, new design — important

The owner now requires **retaining the existing user, name, logo and version series** while replacing the inside UI/features with Personal Life OS. This supersedes fresh reset.

- No old feature screens or matrimony features are restored.
- No automatic deletion of original databases, private media, preferences, PIN or local encryption keys.
- A read-only compatibility reader retains the original profile and supported private profile photo in encrypted Hive. Other original records stay in their original storage; conversion into the new modules is not yet implemented.
- `com.lifemate`, Android 8+, the supplied logo and existing release key are unchanged. `1.2.N` / `100000 + run number` remains the update version scheme.
- **Release is held for revised account-continuity verification.** Last inspected public APK is native 1.2.64, not this UI. No new signed APK is claimed.

## Current product scope

Bottom navigation: **Home / My Life / Health / Money / Profile**.

| Area | MVP implementation |
| --- | --- |
| My Day | Routine times, top-three focus tasks, completion/streak, unified reminders |
| Mind Mate | Emoji moods/weekly graph, encrypted authenticated journal, breathing exercise, foreground sleep sound, optional Sathi client |
| Health | Water/sleep/workout logs, optional period records, private prescription photo and medicine reminders |
| Money | Income/expense CRUD, reviewable voice/text expense parsing, summaries/chart, savings targets, bill alerts |
| People | Important contacts, birthday/anniversary reminders, system dialer, opt-in foreground encrypted location-sharing client |
| Emergency | Up to three configured contacts; GPS snapshot in the system SMS composer and separate dialer actions; no silent sending/calling |
| Memory & goals | Encrypted photo/note timeline and image-backed vision/savings goals |
| Profile/privacy | Local name, language, full dark/system/light themes, device-authentication app lock, encrypted portable backup/import, explicit local deletion |

Demo mode is read-only and in-memory, with examples for every entry kind. It never seeds a real vault or uploads sample data. There are **no dating/matrimony/matchmaking features**.

## Architecture and boundaries

- `life_mate_flutter/lib`: all product UI, typed entry models, encrypted Hive vault, private encrypted photos, notifications and optional service clients.
- `app/`: a `FlutterFragmentActivity` Android host, read-only existing-account compatibility and verified native APK download/install plumbing. SQLCipher is used only to retain the prior profile; no Room/Compose feature UI or old routes.
- `backend/`: optional Firebase Auth/Firestore/Storage rules and authenticated server-side Gemini callable. No provider key is embedded in the app. Services remain unavailable unless the owner configures/deploys them.
- Offline CRUD works without an account. Cloud sync is manual, encrypted before upload and restricted to its owning account. AI and location require explicit choices. Live backend success is not claimed.
- Both journal entry routes and editors require device PIN/passcode/biometrics outside demo. App-wide locking is optional; Android blocks screenshots. iOS needs independent device validation.
- Notification bodies are generic, not personal titles. Android uses inexact scheduling; batteries/permissions can delay delivery. Re-save schedules after changing timezone. Feb-29 yearly reminders use the next valid Feb-29; monthly day 31 skips shorter months. Do not use this as a sole medical/emergency alarm.

## Build and verification

Flutter **3.35.7**, JDK **17**, Android compile SDK **36**, Build Tools **35.0.0**, minimum Android **26**. Run `bash scripts/setup-flutter.sh` before Gradle; generated module hosts/caches are not source artifacts. See [verification status](docs/VERIFICATION.md), [account-continuity contract](docs/ACCOUNT-CONTINUITY.md), and [release maintenance](docs/RELEASE.md).

Only a verified signed release APK may be uploaded to Actions. No debug APK, screenshot, report, logo or signing file is a downloadable artifact. iOS project generation is not an IPA build/distribution claim.

## Performance and API readiness

See [measured-size/startup methodology](docs/PERFORMANCE.md) and [backend setup and keyless deployment](backend/README.md). The owner chose to provide the Firebase Project ID later: live deployment remains deferred, not silently represented as working. Offline app builds are supported without client config.
