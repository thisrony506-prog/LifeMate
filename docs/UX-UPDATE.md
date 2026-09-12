# Feature design, studio, weekly progress and required updates

## Navigation and feature identity

The supplied LifeMate logo, package ID, offline records, scheduling, PIN/biometric security and existing organizer functions are retained. Home keeps today's tasks, a real weekly summary, upcoming birthdays and direct Birthday/Photo studio entry points. Updates no longer occupy the Home header; Settings has a dedicated update section near the top.

The latest premium redesign replaces the former eight-purpose-color palette with one **white/black/pink** system, including pink selected navigation. Bottom destinations are **Home, Missions, Calendar, Memories and Profile**; the hamburger opens secondary features directly. The original logo and all record types remain. [PREMIUM.md](PREMIUM.md) documents the compact Home, lazy Memories grid, sorting, habit/profile/statistics changes, persistent Facebook Posts, additive Room migration, optional sounds and consent-gated AI backend contract.

## Private photo and birthday studio

Open **All Features → Photo & card studio**, a birthday's wishes action, or **Facebook Posts → Create image**. Birthday creation uses the pink feature theme and editable offline wishes in the existing seven tones.

- Import an image with Android's system document picker; no broad gallery access and no social account are required.
- Inputs are capped at 25 MB, decoded at a bounded size, oriented using EXIF, and normalized into a private app copy. Originals are unchanged. Exported PNGs do not contain the source location metadata.
- Edit up to 600 characters, with Modern, Serif, Handwritten, Bold or Mono system fonts; five text colors; size, alignment and top/center/bottom placement.
- Choose flat White, Black or Pink, or the retained Rose, Lavender, Ocean, Jade and Midnight creative palettes, or a photo background with crop zoom and 90-degree rotation. Original, Warm, Mono and Dreamy effects apply to photos. Overlay darkness helps text contrast.
- Export portrait **1080×1350**, square **1080×1080**, or landscape **1200×630** PNGs. The live preview is the same generated file that is saved/shared. Export is disabled while a changed design is still rendering. Text shrinks and may ellipsize when it cannot fit: review the preview.
- Tap Share and select Facebook if available, or save the PNG and attach it to a Facebook post yourself. LifeMate never posts or uploads automatically.
- One draft per birthday, per saved Facebook post, and a separate generic studio draft are stored with Android-Keystore AES-GCM encryption. Private normalized photos are device-local, not separately encrypted by LifeMate. Drafts restore after activity recreation/restart; previous private copies are removed when replaced in a saved draft. Temporary previews are bounded and are not a permanent gallery.
- **Studio-only drafts/photos are not included in record ZIP backups. Saved Facebook post records and their source photos are included in format-2 backups. Save finished images separately.** Settings → Delete all data clears studio drafts/private copies and previews too; it does not delete exported files or gallery originals.

## Weekly insights

Home and Statistics use the same Monday–Sunday calculation. Totals count actual scheduled routine, mission, habit and reminder occurrences, and stored completion records; they do not invent scores. Archived records are excluded, duplicate completions do not inflate totals, and future check-ins are not counted early.

The summary distinguishes full-week completed/remaining work, tasks due through today, and unfinished tasks before today. Future dates are never called missed. The percentage-point comparison uses the same weekday cutoff in the previous week, not an incomplete week against a complete week. Statistics also breaks down each mission's weekly work, daily focus and total mission progress. Editing/deleting a schedule changes these current-record summaries; they are not an immutable historical audit.

## Required signed-update policy

This intentionally replaces the older optional Home-banner/disable-check preference in the new app version:

1. Foreground release checks remain rate-limited to at most once per six hours; Settings also has manual checking. Debug/instrumentation builds do not automatically fetch.
2. Only a numerically newer release whose metadata validates against the **installed signing certificate**, official canonical asset URL and matching GitHub digest can trigger the required dialog.
3. The dialog cannot be dismissed with Back or an outside tap. It offers **Download update**, **Export a private backup** and **Close app**, but not a continue/skip action.
4. Download happens **inside the popup**, with percentage/MB progress and a verification stage. After checking the signed size/SHA-256, package, version and matching installed signer, **Install update** opens Android's native installer through a read-only content URI. Android may require permission to install from **LifeMate**; grant it in Android settings, return and tap Install update. The app declares `REQUEST_INSTALL_PACKAGES` for this user-requested flow, but cannot install silently. Downloading or cancelling installation never unlocks the old version. Do not uninstall to update.
5. An offline/API error alone never invents an update lock. Once a verified newer release is known, its validated cached metadata keeps the requirement in force even while offline; a stale older response or 404 cannot erase it. Installing the required or a newer version removes the gate through numeric comparison.
6. App PIN protection remains ahead of private content and backup access. Export from the update dialog warns that record ZIP backups are **unencrypted**.

Older installed builds must install the newer APK once to acquire its updater: 1.2.52 and earlier still have their old browser-based download code. No old release binary is rewritten.

### Native download reliability and trust

Only the canonical signed GitHub asset is requested. Redirects are limited to HTTPS on `github.com`, `release-assets.githubusercontent.com` and `objects.githubusercontent.com`, with no user credentials, fragment, nonstandard port or arbitrary host; at most five redirects. There are 15-second network timeouts, a ten-minute transfer limit, a 200 MB maximum signed size, a free-space preflight and bounded streaming (not a whole-APK memory buffer). No account, token, analytics or personal record is sent.

Partial files remain private and are removed on failure/cancellation/retry. Transfer stops when the activity stops; this is not an always-running download service. Retry starts from the beginning for interrupted transfers. Completed APKs can be reused after revalidating their hash/identity; cache eviction requires downloading again. The installed version—not a writable “download finished” preference—controls the mandatory gate.

Test coverage includes safe/unsafe redirects, exact size/hash checks, truncated/oversized/tampered input, non-APK rejection, cancellation cleanup, cached-file tampering/reuse, progress/disabled actions, native content-URI intent construction and the gate remaining after Install is tapped. Network transfer tests use controlled fixtures. Permission refusal, cancelled system installer, actual device/CDN connectivity and OEM installer presentation remain manual-device checks.

## Opt-in spoken reminders

Settings → Notifications → **Voice reminders** is off by default. It reads a short reminder using an installed offline English/Bangla Android TTS voice. Test playback and Android voice settings are available. No microphone is used for speech playback, no voice model is bundled, and LifeMate sends no reminder text to a network speech API.

Speech requires notifications to be allowed, nonzero notification volume, normal ringer mode, unrestricted DND, an unlocked screen, and no LifeMate PIN lock. Per-reminder silent settings and Android channel mute/importance are respected. Nearby people can hear enabled speech; this is explained before enabling it.

Playback uses a non-exported media-playback foreground service, visible Stop notification, audio focus and a hard **30-second** lifetime. It stops when quiet/privacy conditions change, when disabled, on focus loss or when speech completes. It does not restart itself, record, run continuously, or start at boot. Concurrent reminders retain individual normal notifications rather than stacking overlapping voices.

Android can reject background speech, particularly for approximate alarms; missing/offline-incompatible TTS voices also fall back to the normal text notification. Install any needed offline voice through Android settings. Exact timing, audible English/Bangla quality, DND/channel behavior, Bluetooth routing and OEM restrictions still require physical-device checks.

## Test coverage and device limits

Added JVM coverage for weekly boundaries, future/missed work, duplicates, archives, current schedules, required-update rollback retention and the quiet/privacy voice policy. Android tests exercise the required dialog and Back handling, Settings navigation, encrypted studio drafts, actual photo import/effects, output dimensions and long Unicode card text. Existing encrypted persistence, backup, PIN, navigation, authenticated metadata and real process-exit/reboot notification tests remain.

See [VERIFICATION.md](VERIFICATION.md) for the final hosted run, signed APK, real previous-release installation upgrade and artifact evidence. Automated success does not certify every phone: verify large fonts, Android 8+ ARM devices, Facebook's installed share targets, actual offline voices, OEM background restrictions and real photo-picker providers on the user's device.
