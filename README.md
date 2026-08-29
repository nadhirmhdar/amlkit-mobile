# amlkit-mobile

Native Android client (Kotlin + Jetpack Compose) for
[amlkit](https://github.com/nadhirmhdar/amlkit) — sanctions/PEP screening
and AML case management for UAE DNFBPs.

Covers the same feature set as the web app: auth, dashboard, ad-hoc
screening, customer CDD (onboarding, UBOs, case file, transactions, notes,
evidence pack), alert triage, audit trail, admin, and STR/SAR reports —
all against amlkit's `/api/v1/*` JSON API (`amlkit/api/mobile.py` in the
[main repo](https://github.com/nadhirmhdar/amlkit)) over a bearer token
stored in `EncryptedSharedPreferences`.

This app connects to your firm's own amlkit deployment; it does not screen
against a shared, multi-tenant cloud. Every build is baked to point at one
server via `-PapiBaseUrl=` — see [`docs/google-play-steps.md`](docs/google-play-steps.md)
for the full path from a clean checkout to a Play Store release.

## Architecture note

This repo was split out of the main `amlkit` monorepo so the Android app's
CI/CD and Play Console pipeline have their own home. The two repos share an
API contract (`/api/v1/*`) but no longer share a CI run or a PR — a change
to `amlkit/api/mobile.py` needs a matching check here, and vice versa. See
that repo's `CLAUDE.md`/contributing notes for the cross-repo coordination
note.

## Building

```bash
./gradlew assembleDebug          # debug build, talks to http://10.0.2.2:8000/
./gradlew bundleRelease -PapiBaseUrl=https://your-deployment.example.com/
```

Debug builds need no signing config. Release builds require
`local.properties` with a signing config — see
[`docs/google-play-steps.md`](docs/google-play-steps.md) §3.

## Docs

- [`docs/google-play-steps.md`](docs/google-play-steps.md) — full publishing guide
- [`docs/play-store-listing.md`](docs/play-store-listing.md) — Play Console listing copy
- [`tools/play-console/README.md`](tools/play-console/README.md) — CLI for track/release/listing changes via the Play Developer API
