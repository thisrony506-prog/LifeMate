# LifeMate premium redesign

Brand: **LifeMate — Your Personal Life Assistant**. The supplied logo, package ID, retained signing key, native in-app updater and all organizer kinds remain.

## Interface

- White/near-white Light mode, charcoal/near-black Dark mode, neutral text and one premium pink accent. Primary actions, selected bottom/drawer navigation, completion checks and progress use pink; ordinary cards remain neutral. The primary white-label contrast is covered by a regression test.
- Bottom navigation stays **Home / Missions / Calendar / Memories / Profile**. A top-left hamburger opens direct links to Routines, Habits, Reminders, Goals, Birthdays, Notes, Facebook Posts, Statistics, Settings and About. The older All Features route remains available for compatibility.
- Home uses a compact profile/greeting, numerical daily progress, short quick actions, the next pending routine, today's tasks, missions, weekly progress and an upcoming birthday. No production demo records are inserted.
- Routines gain time-ordered timeline rows while preserving name/date/newest sorting. Habits expose check states, streaks and rolling-week/month counts. Missions retain 1/2/3/7/30/custom durations, date-based completion, reminders, remaining work and progress.
- Memories use an adaptive lazy photo grid, with search, pinned/archive filters and sorting. Videos are represented by a play affordance and opened on demand, not all decoded/autoplayed in the grid. Detail editing, photo/video/audio attachments and voice notes remain available.
- Calendar now includes memories and memoizes month indicators. Profile adds completed-task, active-goal and mission-streak metrics. Statistics retains its charts and adds today's totals and goal progress.
- Material navigation, drawer and bottom-sheet transitions plus the daily progress animation remain subtle; Compose uses Android's animator-duration scale. Large-font/OEM-device layout review is still required before universal accessibility claims.

## Facebook Posts

A dedicated **Facebook Posts** list stores actual encrypted local drafts. Create, edit, reopen, search and delete posts. Types:

Normal Post, Birthday Post, Motivation, Daily Routine, Achievement, Mission Completed, Quote, Celebration, Announcement and Personal Memory.

The composer accepts a topic, short message, optional name/date/photo and an editable caption. Four writing tones and reusable templates work entirely offline. Create/regenerate a caption, copy/share it, or save the record and open **Create image**. Facebook receives content only after the user explicitly selects a share target; no Meta SDK, account, silent posting or publishing token is included. Facebook may not accept prefilled captions from every share target—Copy remains available.

Post graphics reuse the complete local card editor: photo import, fonts, effects, rotate/zoom, text color/placement, name/date and portrait/square/landscape PNG export. Flat White, Black and Pink are the new defaults; previous creative palettes remain selectable. When a saved post's source text/photo changes, its graphic updates those fields while retaining styling. Birthday wishes retain all seven prior tones. Original gallery images remain unchanged.

## Data and migration

Room schema **2** adds only `social_posts`, its profile foreign key and indices. `MIGRATION_1_2` does not recreate old tables or change the SQLCipher/Keystore key. A real version-one schema migration test checks retained profile data plus post create/read/update/delete, reopen persistence and cascading deletion. KSP regenerates the current schema during builds; the checked-in v1 schema is retained as the migration-test fixture.

Portable backup format **2** includes saved posts and their private normalized photos. Restoring older **format 1** backups is supported. New-format backups intentionally require a newer app rather than silently dropping posts in an old reader. Duplicate IDs, invalid post types/content/date and unsafe/missing media remain rejected before replacement. Existing routines, notes, media, completions and profile backup behavior remains.

Separate card-layout drafts/previews are not part of record ZIP backups; save finished PNGs separately. Settings → Delete all data clears posts, organizer records, private media and studio drafts. Exported files and original gallery photos are not deleted. Backup ZIPs are **unencrypted** and retain the explicit warning.

## Optional AI: backend contract

No AI service is enabled by default, and no provider API key is stored in the APK, DataStore or source. Configure **Settings → Optional post AI** only with a trusted HTTPS server endpoint that keeps its provider keys server-side. Endpoints containing credentials, query parameters, fragments, nonstandard ports or ordinary local/IP URLs are rejected. No key field or provider SDK is bundled.

After the user taps AI, a consent dialog identifies the backend host and the fields to be sent. The client sends one bounded JSON POST, without redirects:

```json
{
  "type": "Achievement",
  "topic": "Reading",
  "message": "30 days completed",
  "name": "",
  "date": "",
  "tone": "Short"
}
```

The backend returns HTTP 200 with:

```json
{"caption":"A meaningful milestone: 30 days of reading."}
```

Only these explicitly reviewed composer fields are sent. Photos, file paths, profile/database records, voice recordings and the existing saved caption are not included. The response is capped at 32 KB and a nonblank 4,000-character caption; connection/read timeouts and cancellation apply. If the user edits the draft while generating, the response does not overwrite those new edits. Failures leave the draft intact and offline templates available.

Backend hosting, provider configuration, authentication/abuse controls and rate limits belong to the server owner. A backend requiring user authentication needs a proper server/user-auth integration, **not** a provider key pasted into the endpoint. No live AI backend was supplied or provisioned in this task; live AI output has not been certified. The text-only contract and configuration policy are tested independently.

## Optional feedback and notifications

**Sound effects** is off by default. Small bundled PCM chimes provide task, mission and post-save/success feedback through a single-stream SoundPool. They are not played on every tap, are rate-limited, respect foreground state/silent mode/DND and are stopped when leaving/locking the app. Resources are released with the ViewModel. Scheduled notification sound/vibration and the prior opt-in offline TTS voice reminders remain separate controls.

Default notification text is shorter; user-authored notification text remains untouched. AlarmManager scheduling, persisted occurrences, WorkManager reconciliation, permission checks, notification channels, process-exit and reboot recovery are preserved.

## Performance and verification scope

Completion/date/media lookups are indexed once per state snapshot, with preparation on `Dispatchers.Default`, rather than repeatedly scanning full completion/attachment lists per row. Attachment deletion uses an indexed per-item query. Post lists use a separate Room flow and only collect when viewed. Calendar indicators and major Home collections are memoized. Lazy lists/grids and bounded image import remain; no new media or UI library was introduced. Home does not wait for AI or a remote backend.

These are concrete implementation improvements, not a fabricated benchmark. Physical-device startup timing, very large libraries, large fonts, audio quality, Bluetooth/OEM restrictions and installed Facebook/gallery providers remain manual checks. See [VERIFICATION.md](VERIFICATION.md) for executed CI, migration, navigation, reminder, signing and release evidence.
