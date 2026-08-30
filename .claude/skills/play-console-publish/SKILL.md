---
name: play-console-publish
description: Drive a Google Play Console release for the amlkit-mobile Android app (package com.grovisoramlkit.myapp) — build+upload a new version, promote a release between tracks, adjust a staged rollout, or edit the store listing, all without a downloadable service-account key. Use this whenever the user asks to publish, release, ship, upload, or push the app to Play Console/Play Store/testers, asks about internal/closed/open/production testing tracks, mentions versionCode/versionName for this app, or wants to check what's currently live on a track. This only works from within the amlkit-mobile repo, where the CLI, workflow, and Workload Identity Federation auth already live — do not try to recreate this setup from scratch or in another repo.
---

# Publishing amlkit-mobile to Google Play Console

This repo already has a complete, keyless publishing pipeline. Nothing here needs
a JSON service-account key: authentication goes through Workload Identity
Federation (WIF) — GitHub's own OIDC token is exchanged for short-lived GCP
credentials, mirroring how the sibling `amlkit` repo deploys to Cloud Run. If a
conversation starts drifting toward "let's create a service account key" or
"let's set up WIF from scratch," stop — it's already provisioned. See
`scripts/setup_github_play_auth.sh` and `tools/play-console/README.md` if you
need to confirm exactly what exists, but you should not need to re-run that
setup.

Two ways to act, depending on what's being asked:

## Path A — Build and publish a new version (most requests)

**`.github/workflows/play-console-publish.yml`**, triggered via `workflow_dispatch`,
does the whole thing in one job: builds the signed AAB, authenticates via WIF,
and uploads it to a chosen track. This is what "publish a new version" or
"ship this to internal testing" means in practice.

Trigger it with the GitHub Actions API/tool available in this session
(`workflow_id: play-console-publish.yml`, `ref: master`), passing:

- `apiBaseUrl` — the amlkit server this build should point at. Get it fresh
  rather than assuming it hasn't changed:
  `gcloud run services describe amlkit --platform managed --region me-central1 --project gen-lang-client-0153967509 --format "value(status.url)"`
  (add the trailing slash — it's a Retrofit base URL). If that comes back
  empty, check `gcloud run services list --project gen-lang-client-0153967509`
  for the actual region.
- `versionCode` — **must strictly increase over whatever's already been
  uploaded to this app, on any track.** This has bitten every single release
  so far: Play returns a plain 403 ("Version code N has already been used")
  with no hint about what the right number actually is. Don't guess by
  incrementing the last number you personally used — someone may have
  uploaded a version manually or from a different session since. Either ask
  the user to check Play Console → Release → Overview for the true current
  highest version code, or note that `app/build.gradle.kts`'s checked-in
  default is kept in sync with the last known-good release as one signal
  (not a guarantee — CI callers always pass an explicit `-PversionCode`
  override, so the checked-in default can lag).
- `versionName` — just a label, e.g. `1.0.3`. No uniqueness constraint.
- `track` — `internal`, `alpha`, `beta`, or `production`. Default to
  `internal` unless told otherwise; that's this repo's "test first" norm
  (see `docs/google-play-steps.md`).
- `releaseNotes` — optional.

**Publishing to `production` needs the user's explicit go-ahead in the
conversation before you trigger the workflow** — the workflow itself passes
`playcli.py`'s `--yes` flag unconditionally once dispatched (since dispatching
it *is* the human decision), so there's no second confirmation prompt inside
CI. Don't supply that decision on the user's behalf.

After dispatching, poll the run (`list_workflow_runs` /
`get_workflow_run` for `play-console-publish.yml`) rather than assuming
success — every real run so far has needed at least one retry (see
Troubleshooting below), and reporting "done" before confirming the run
actually succeeded has been wrong more than once in this repo's history.

## Path B — Track/listing operations only (no new build)

For promoting an existing release between tracks, adjusting a staged
rollout, or editing store listing text — nothing that needs a fresh AAB —
use `tools/play-console/playcli.py` directly. Full command reference:
`python3 tools/play-console/playcli.py --help`, or read
`tools/play-console/README.md`.

This only runs somewhere with real Google credentials: either a local
service-account JSON key (`--credentials` / `PLAY_CONSOLE_CREDENTIALS_FILE`)
or Application Default Credentials. A sandboxed agent session with no GCP
auth of its own **cannot** run this directly — don't attempt it and report
success; either hand the exact command to the user to run themselves, or (if
the operation truly needs no new build) consider whether a tiny one-off
workflow_dispatch job would be more honest than pretending to have run it
locally.

Every mutating `playcli.py` command supports `--dry-run` (validates via the
API, changes nothing) — use it to sanity-check a command before treating it
as done, especially for anything touching `production`.

## Facts worth not re-deriving

- Package name / `applicationId`: `com.grovisoramlkit.myapp` (locked in
  permanently since Play accepted the first release under it — see
  `app/build.gradle.kts`'s comment).
- GCP project: `gen-lang-client-0153967509`, region `me-central1`.
- WIF service account: `github-play-publisher@gen-lang-client-0153967509.iam.gserviceaccount.com`,
  already invited in Play Console → Setup → API access → Users and
  permissions with testing-track release permissions.
- Repo variables already set: `GCP_WIF_PROVIDER`, `GCP_PLAY_SERVICE_ACCOUNT`
  (Settings → Secrets and variables → Actions → Variables).
- The four keystore secrets (`ANDROID_KEYSTORE_BASE64` etc.) that sign the
  build are unrelated to Play Console auth — don't confuse the two credential
  types, they solve different problems (signing vs. publishing API access).

## Troubleshooting patterns already seen

- **`UnknownFileType: .../app-release.aab`** — already fixed in
  `playcli.py`'s `cmd_upload` (wraps the path in an explicit
  `MediaFileUpload(..., mimetype="application/octet-stream")`). If this
  regresses, that's the fix to reapply.
- **`403: Version code N has already been used`** — not a code bug, a real
  Play Console state conflict. Get the actual current highest version code
  (see Path A above) rather than incrementing blindly a second time.
- A failed `play-console-publish.yml` run almost always fails on the last
  step (`Publish to Play Console`) after the build/auth steps already
  succeeded — pull that step's log specifically
  (`get_job_logs` with `failed_only: true`, a generous `tail_lines` since the
  real error is often buried above post-job Gradle cleanup noise) rather than
  assuming a build or auth problem.
