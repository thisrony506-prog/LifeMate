# Life Mate backend API

This is a deployable Firebase backend, **not a claim of live deployment**. The owner has an existing project but chose to supply its public Project ID later. No live project, provider secret or deployment identity is available to this work yet.

## API surface

- `apiHealth`: GET; reports only the function process/API version, not Gemini/database health.
- `sathi`: authenticated Firebase callable in `asia-south1`. Input `{message, language: "en" | "bn"}`, output `{reply}`. Only the approved message is sent to Gemini. Provider errors, secrets and request/reply text are not logged or echoed.
- Firestore `users/<uid>/vault/<id>` and `users/<uid>/meta/vault`: owner-private encrypted records/recovery marker. Record revisions must start at 1 and increment by exactly 1; no raw delete (encrypted tombstones carry deletions).
- Storage `users/<uid>/encrypted/<id>`: owner-private binary objects, below 21 MiB. Encryption is performed by the client, not inferred from content type.
- Firestore `live/<random invite ID>`: authenticated direct gets only, no listing; owner-only changes; short expiry cannot be extended. The invite contains the decryption secret separately.

Sathi is limited to 20 requests per user and 200 globally per UTC day, with bounded output, a 25-second provider timeout, 64 KiB response limit, max two instances and zero minimum instances. These reduce exposure, but do not replace billing alerts or production abuse monitoring. Crisis-keyword fallback is not a complete clinical risk detector. Do not use the app as an emergency or medical service.

## Tests and safe deployment

`functions/` has TypeScript compilation and provider-boundary tests with inert, injected responses. `rules-tests/` uses the local Firestore/Storage emulators and the `demo-life-mate` project; it must never use production records.

The separate **Deploy approved Life Mate backend** workflow is manual-only and restricted to the session branch. It runs tests, then uses Google Workload Identity Federation—no downloaded service-account private key. Configure an approval-protected `firebase-production` GitHub environment and its `GCP_WORKLOAD_IDENTITY_PROVIDER` / `FIREBASE_DEPLOY_SERVICE_ACCOUNT` variables. The Google trust policy must restrict this repository, branch/environment and intended deployment principal; grant only the Firebase deployment/runtime permissions required for the approved project.

The owner/project admin must first:
1. Confirm the existing Firebase project ID, billing approval and chosen Firestore/Storage location.
2. Enable email/password and anonymous Firebase Auth (anonymous is used for optional Sathi/location sessions).
3. Create Firestore and the Storage bucket; enable required Functions/Cloud Run/build APIs.
4. Store `GEMINI_API_KEY` in that project's **Secret Manager**, never in Git, chat, Flutter config or APK. Grant its runtime principal access. Review budgets and consider App Check before a wider public launch.
5. Establish the keyless GitHub deployment identity and protected environment; dispatch the workflow for the approved project. No workflow has been dispatched against a live project in this work.
6. Configure a TTL policy for `internalLimits.expiresAt` if desired; TTL is cleanup, not authorization. Location expiry is enforced by rules without TTL.
7. Smoke-test the deployed health/callable endpoints and ownership rules with test accounts before claiming service availability.

## Connecting the APK

The **public** Firebase app settings go into GitHub Actions variable `LIFEMATE_FIREBASE_CONFIG` as JSON with `FIREBASE_PROJECT_ID`, `FIREBASE_API_KEY`, `FIREBASE_ANDROID_APP_ID`, `FIREBASE_SENDER_ID`, `FIREBASE_STORAGE_BUCKET`, optional `FIREBASE_IOS_APP_ID`, and `FIREBASE_FUNCTIONS_REGION: "asia-south1"`. Firebase's client API key is not the Gemini provider secret; restrict it to the intended Firebase APIs/app where supported. The validator rejects unknown/provider/private-key fields and incomplete config. The app must be rebuilt to embed this public configuration.

Without that configuration the APK remains a working offline MVP and honestly disables unavailable cloud/AI/location-sharing services. It does not invent a server or pretend a deployment succeeded. Never share passwords, service-account keys or Gemini keys in chat.
