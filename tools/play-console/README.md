# Play Console CLI

`playcli.py` drives the Google Play Developer API (`androidpublisher v3`) to
do the track/release/listing work that
[`docs/google-play-steps.md`](../../docs/google-play-steps.md) otherwise
walks through by hand in the Play Console UI. It does **not** replace that
doc's first-time setup (app creation, content questionnaire, data safety
form, privacy policy, initial testers) — those are one-time judgment calls
that stay manual. This CLI is for the repeat work once the app exists:
uploading a new bundle, promoting a release between tracks, adjusting a
staged rollout, and editing store listing text.

## Setup

1. In Play Console: **Setup → API access** → link or create a Google Cloud
   project.
2. In that Google Cloud project: **IAM & Admin → Service Accounts** → create
   a service account, then create a JSON key for it and download it.
3. Back in Play Console: **Users and permissions** → invite the service
   account's email → grant only the permissions you intend to use here
   (e.g. "Release apps to testing tracks"; grant "Release to production"
   only if you actually intend to run production commands from this CLI —
   it is a real trust decision, not a default).
4. Install dependencies:
   ```bash
   pip install -r tools/play-console/requirements.txt
   ```
5. Point the CLI at the key, either per-command or once via env var:
   ```bash
   export PLAY_CONSOLE_CREDENTIALS_FILE=/absolute/path/to/play-console-key.json
   ```

**Never commit the JSON key.** It is bearer-token equivalent access to your
Play Console listing. Keep it in a secrets manager or password manager, the
same way `docs/google-play-steps.md` §3 treats the upload keystore. The
repo's `.gitignore` blocks common key filenames under this directory as a
backstop, not as a substitute for keeping it outside the repo entirely.

The CLI defaults `--package-name` to `com.grovisoramlkit.myapp` (this repo's
locked-in `applicationId` — see `app/build.gradle.kts`), so it needs no flag
for normal use.

## Usage

Every mutating command supports `--dry-run`, which validates the change with
Play's API and discards it without publishing anything — always try a change
dry-run first.

```bash
# See every track and its current release(s)
python3 tools/play-console/playcli.py tracks list

# Upload a freshly built bundle to internal testing
python3 tools/play-console/playcli.py upload \
  app/build/outputs/bundle/release/app-release.aab \
  --track internal \
  --release-notes "Fixes the alert triage crash on empty queues."

# Promote whatever's active on internal to a 10% production rollout
python3 tools/play-console/playcli.py promote \
  --from-track internal --to-track production \
  --status inProgress --user-fraction 0.1

# Increase an in-progress production rollout
python3 tools/play-console/playcli.py rollout --track production --user-fraction 0.5

# Finish a staged rollout
python3 tools/play-console/playcli.py rollout --track production --complete

# Halt a bad rollout immediately
python3 tools/play-console/playcli.py rollout --track production --halt

# Update the store listing text (see docs/play-store-listing.md for copy)
python3 tools/play-console/playcli.py listing update \
  --short-description "Sanctions/PEP screening and AML case management for UAE DNFBPs." \
  --full-description-file /path/to/full-description.txt
```

Any command that would touch the `production` track asks for interactive
confirmation unless you pass `--yes` (required in non-interactive/CI use) or
`--dry-run`.

Run `python3 tools/play-console/playcli.py --help`, or `<command> --help`,
for the full flag list — `tracks`, `upload`, `promote`, `rollout`,
`listing show`/`update`, `details show`/`update`.
