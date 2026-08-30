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

There are two ways to authenticate as the service account this CLI uses.
Either way, start the same:

1. In Play Console: **Setup → API access** → link or create a Google Cloud
   project.
2. In that Google Cloud project, create a service account (name it whatever
   you like — `github-play-publisher` if you're setting up the CI path
   below).
3. Back in Play Console: **Users and permissions** → invite the service
   account's email → grant only the permissions you intend to use here
   (e.g. "Release apps to testing tracks"; grant "Release to production"
   only if you actually intend to run production commands from this CLI —
   it is a real trust decision, not a default).
4. Install dependencies:
   ```bash
   pip install -r tools/play-console/requirements.txt
   ```

Then pick one:

### Option A — Workload Identity Federation (recommended for CI)

No downloadable key ever exists. GitHub's own OIDC token is exchanged for
short-lived GCP credentials at workflow run time — the same mechanism the
[`amlkit`](https://github.com/nadhirmhdar/amlkit) repo already uses for its
Cloud Run deploy (see that repo's
`.github/workflows/source-canary.yml` and `scripts/setup_github_deploy_auth.sh`).

Run [`scripts/setup_github_play_auth.sh`](../../scripts/setup_github_play_auth.sh)
once, from a shell authenticated as an Owner/IAM Admin on the linked Google
Cloud project (Cloud Shell, already signed in, works well for this). It
creates the service account, reuses that same repo's Workload Identity Pool
with a new provider scoped to `nadhirmhdar/amlkit-mobile`, and prints two
values to set as repository variables (**Settings → Secrets and variables →
Actions → Variables**):

```
GCP_WIF_PROVIDER
GCP_PLAY_SERVICE_ACCOUNT
```

`.github/workflows/play-console-publish.yml` uses these to authenticate
automatically — no `--credentials` flag, no `PLAY_CONSOLE_CREDENTIALS_FILE`,
nothing to rotate or leak. This is what that workflow uses; there's no local
setup step beyond running the script and inviting the service account in
Play Console.

### Option B — a downloadable JSON key (for local/manual use)

In the same Cloud project: **IAM & Admin → Service Accounts** → your service
account → **Keys** → **Add Key → JSON**, then point the CLI at it, either
per-command or once via env var:
```bash
export PLAY_CONSOLE_CREDENTIALS_FILE=/absolute/path/to/play-console-key.json
```

**Never commit the JSON key.** It is bearer-token equivalent access to your
Play Console listing. Keep it in a secrets manager or password manager, the
same way `docs/google-play-steps.md` §3 treats the upload keystore. The
repo's `.gitignore` blocks common key filenames under this directory as a
backstop, not as a substitute for keeping it outside the repo entirely.

If you omit both `--credentials` and `PLAY_CONSOLE_CREDENTIALS_FILE`, the
CLI falls back to Application Default Credentials — which is what makes
Option A work in CI, and also lets you run the CLI locally after
`gcloud auth application-default login` instead of downloading a key.

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
