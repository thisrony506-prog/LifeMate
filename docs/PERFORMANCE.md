# Size and responsiveness

## Implemented optimization, not an unmeasured speed promise

- R8 code shrinking plus Android resource shrinking for release builds; preserve dynamically referenced notification/launcher resources explicitly.
- Keep ARMv7, ARM64 and x86_64 in the one downloadable APK. Do not silently drop older phones or replace actual signed-APK emulator testing with a different binary.
- Compress native libraries for direct APK delivery. Android extracts them once during installation. This lowers download size but can increase installed disk usage compared with an uncompressed mmap-in-APK build; APK bytes are not installed footprint.
- Keep encrypted-photo reads/decryption futures stable across unrelated UI rebuilds. Decode list previews at no more than 1200 pixels wide, and cap the decoded image cache at 32 MiB/40 entries.
- Move password derivation and photo encryption/decryption onto background Dart isolates. Keep PBKDF2-SHA256 at 210,000 iterations and AES-GCM unchanged.
- Cache immutable record snapshots/per-kind sorting, invalidate on data changes, return independent lists so a caller cannot corrupt indexed order.
- No Firebase sign-in, cloud sync, AI request or live location starts automatically at app launch. Offline records remain independent of backend availability.

## Measurement pipeline

The verify job builds the same code twice, differing only in native-library compression, and prints exact APK/native packed/unpacked byte counts and download reduction as a CI annotation. Both builds retain identical features and ABIs. The signed verification job records three process-cold launches on the Android 15 x86_64 hosted emulator: Android `am start -W` TotalTime and activity-to-first-Flutter-frame time. These are distinct metrics, not physical-phone FPS or installed-size benchmarks.

A Flutter test records 100 indexed reads over 1,000 local records and asserts correctness/immutability without a flaky universal time threshold. Backend unit tests use injected provider responses; emulator rules tests do not measure internet latency.

Actual numbers must come from a completed run. No percentage speedup, physical-phone result, live API latency or APK size is asserted until measured. Physical ARM devices, larger real photo libraries, frame-time traces and installed storage measurements remain required for production performance claims.
