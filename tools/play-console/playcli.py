#!/usr/bin/env python3
"""CLI for the amlkit-mobile Google Play Console listing.

Wraps the Google Play Developer API (androidpublisher v3) to do from the
command line what docs/google-play-steps.md otherwise walks through by hand
in the Play Console UI: upload a bundle to a track, promote a release
between tracks, adjust a staged rollout, and edit the store listing text.

Every mutating command works through the API's edit-transaction model: open
an edit, make one change, then either commit it or (with --dry-run) validate
and discard it. An edit is always discarded on error so a failed run never
leaves a half-applied edit sitting in Play Console.

Setup:
  1. In Play Console, go to Setup -> API access and link (or create) a
     Google Cloud project.
  2. In that project, create a service account -- see
     tools/play-console/README.md for two ways to authenticate as it:
     a Workload Identity Federation setup with no downloadable key at all
     (recommended, used by .github/workflows/play-console-publish.yml), or
     a JSON key file for local use.
  3. Back in Play Console -> Users and permissions, invite the service
     account's email and grant it exactly the permissions this tool needs
     (e.g. "Release to testing tracks" and/or "Release to production" --
     grant production access only if you actually intend to use it here).
  4. Either point this CLI at a downloaded key with --credentials /
     PLAY_CONSOLE_CREDENTIALS_FILE (never commit that file -- see
     tools/play-console/README.md), or omit both and let it fall back to
     Application Default Credentials -- what a Workload Identity Federation
     login (`google-github-actions/auth@v2` in CI) or a local
     `gcloud auth application-default login` both set up.

Every subcommand is scoped to one package; it defaults to this repo's
locked-in applicationId (com.grovisoramlkit.myapp -- see
app/build.gradle.kts) but --package-name/PLAY_CONSOLE_PACKAGE_NAME can
override it for testing against another app.
"""
from __future__ import annotations

import argparse
import contextlib
import os
import sys
from typing import Iterator, Optional

DEFAULT_PACKAGE_NAME = "com.grovisoramlkit.myapp"
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]
ACTIVE_RELEASE_STATUSES = ("completed", "inProgress")


def _die(message: str) -> "None":
    print(f"error: {message}", file=sys.stderr)
    raise SystemExit(1)


def build_service(credentials_path: Optional[str]):
    try:
        import google.auth
        from google.oauth2 import service_account
        from googleapiclient.discovery import build
    except ImportError:
        _die(
            "missing dependencies -- run: pip install -r "
            "tools/play-console/requirements.txt"
        )
    if credentials_path:
        if not os.path.isfile(credentials_path):
            _die(f"credentials file not found: {credentials_path}")
        creds = service_account.Credentials.from_service_account_file(
            credentials_path, scopes=SCOPES
        )
    else:
        # Application Default Credentials: what a Workload Identity
        # Federation login (google-github-actions/auth@v2) or a local
        # `gcloud auth application-default login` both set up -- no
        # downloadable key involved either way.
        try:
            creds, _ = google.auth.default(scopes=SCOPES)
        except google.auth.exceptions.DefaultCredentialsError as exc:
            _die(
                f"no credentials found ({exc}) -- pass --credentials PATH, "
                "set PLAY_CONSOLE_CREDENTIALS_FILE, or run "
                "`gcloud auth application-default login`"
            )
    return build("androidpublisher", "v3", credentials=creds, cache_discovery=False)


def _safe_delete_edit(service, package_name: str, edit_id: str) -> None:
    from googleapiclient.errors import HttpError

    try:
        service.edits().delete(packageName=package_name, editId=edit_id).execute()
    except HttpError:
        pass


@contextlib.contextmanager
def edit_session(service, package_name: str) -> Iterator[str]:
    """Opens a Play Console edit, discarding it if the body raises."""
    edit = service.edits().insert(packageName=package_name, body={}).execute()
    edit_id = edit["id"]
    try:
        yield edit_id
    except BaseException:
        _safe_delete_edit(service, package_name, edit_id)
        raise


def finish_edit(service, package_name: str, edit_id: str, dry_run: bool) -> None:
    """Validates-and-discards on a dry run, otherwise commits for real."""
    if dry_run:
        service.edits().validate(packageName=package_name, editId=edit_id).execute()
        _safe_delete_edit(service, package_name, edit_id)
    else:
        service.edits().commit(packageName=package_name, editId=edit_id).execute()


def confirm_production(args: argparse.Namespace, track: Optional[str]) -> None:
    """Requires an explicit --yes (or an interactive yes) before touching production."""
    if track != "production" or getattr(args, "dry_run", False):
        return
    if args.yes:
        return
    if not sys.stdin.isatty():
        _die(
            "refusing to modify the production track non-interactively "
            "without --yes"
        )
    reply = input(
        f"This will change the PRODUCTION track for {args.package_name}. "
        "Type 'yes' to continue: "
    )
    if reply.strip().lower() != "yes":
        _die("aborted")


def _read_text_arg(value: Optional[str], value_file: Optional[str]) -> Optional[str]:
    if value_file:
        with open(value_file, "r", encoding="utf-8") as f:
            return f.read().strip()
    return value


def _max_version_code(version_codes: list) -> int:
    return max(int(v) for v in version_codes)


# --- tracks -----------------------------------------------------------------


def cmd_tracks_list(args, service) -> None:
    with edit_session(service, args.package_name) as edit_id:
        result = service.edits().tracks().list(
            packageName=args.package_name, editId=edit_id
        ).execute()
        _safe_delete_edit(service, args.package_name, edit_id)
    for track in result.get("tracks", []):
        print(f"track: {track['track']}")
        for release in track.get("releases", []):
            codes = ",".join(release.get("versionCodes", []))
            fraction = release.get("userFraction")
            fraction_str = f" userFraction={fraction}" if fraction is not None else ""
            print(
                f"  status={release.get('status')} versionCodes=[{codes}]"
                f"{fraction_str} name={release.get('name', '')}"
            )
        if not track.get("releases"):
            print("  (no releases)")


def cmd_tracks_get(args, service) -> None:
    with edit_session(service, args.package_name) as edit_id:
        track = service.edits().tracks().get(
            packageName=args.package_name, editId=edit_id, track=args.track
        ).execute()
        _safe_delete_edit(service, args.package_name, edit_id)
    import json

    print(json.dumps(track, indent=2))


# --- upload -------------------------------------------------------------


def cmd_upload(args, service) -> None:
    confirm_production(args, args.track)
    release_notes_text = _read_text_arg(args.release_notes, args.release_notes_file)
    if args.status == "inProgress" and args.user_fraction is None:
        _die("--user-fraction is required when --status inProgress")

    from googleapiclient.http import MediaFileUpload

    with edit_session(service, args.package_name) as edit_id:
        bundle = (
            service.edits()
            .bundles()
            .upload(
                packageName=args.package_name,
                editId=edit_id,
                # Explicit mimetype: MediaFileUpload/mimetypes.guess_type
                # doesn't know the .aab extension and raises UnknownFileType
                # if left to auto-detect from the path string.
                media_body=MediaFileUpload(
                    args.bundle_path, mimetype="application/octet-stream", resumable=True
                ),
            )
            .execute()
        )
        version_code = bundle["versionCode"]

        release = {"versionCodes": [str(version_code)], "status": args.status}
        if release_notes_text:
            release["releaseNotes"] = [
                {"language": args.language, "text": release_notes_text}
            ]
        if args.user_fraction is not None:
            release["userFraction"] = args.user_fraction

        service.edits().tracks().update(
            packageName=args.package_name,
            editId=edit_id,
            track=args.track,
            body={"releases": [release]},
        ).execute()

        finish_edit(service, args.package_name, edit_id, args.dry_run)

    verb = "Validated (dry run)" if args.dry_run else "Uploaded"
    print(
        f"{verb}: versionCode {version_code} -> track '{args.track}' "
        f"(status={args.status})"
    )


# --- promote --------------------------------------------------------------


def cmd_promote(args, service) -> None:
    confirm_production(args, args.to_track)

    with edit_session(service, args.package_name) as edit_id:
        source = (
            service.edits()
            .tracks()
            .get(packageName=args.package_name, editId=edit_id, track=args.from_track)
            .execute()
        )
        candidates = [
            r
            for r in source.get("releases", [])
            if r.get("status") in ACTIVE_RELEASE_STATUSES and r.get("versionCodes")
        ]
        if not candidates:
            _safe_delete_edit(service, args.package_name, edit_id)
            _die(f"no active release found on track '{args.from_track}'")
        release = max(candidates, key=lambda r: _max_version_code(r["versionCodes"]))

        new_release = {
            "versionCodes": release["versionCodes"],
            "status": args.status,
        }
        if "releaseNotes" in release:
            new_release["releaseNotes"] = release["releaseNotes"]
        if args.user_fraction is not None:
            new_release["userFraction"] = args.user_fraction
        elif args.status == "inProgress":
            new_release["userFraction"] = 0.1

        service.edits().tracks().update(
            packageName=args.package_name,
            editId=edit_id,
            track=args.to_track,
            body={"releases": [new_release]},
        ).execute()

        finish_edit(service, args.package_name, edit_id, args.dry_run)

    verb = "Validated (dry run)" if args.dry_run else "Promoted"
    codes = ",".join(release["versionCodes"])
    print(
        f"{verb}: versionCode(s) [{codes}] {args.from_track} -> {args.to_track} "
        f"(status={args.status})"
    )


# --- rollout ----------------------------------------------------------------


def cmd_rollout(args, service) -> None:
    confirm_production(args, args.track)

    with edit_session(service, args.package_name) as edit_id:
        track = (
            service.edits()
            .tracks()
            .get(packageName=args.package_name, editId=edit_id, track=args.track)
            .execute()
        )
        candidates = [
            r
            for r in track.get("releases", [])
            if r.get("status") in ACTIVE_RELEASE_STATUSES and r.get("versionCodes")
        ]
        if not candidates:
            _safe_delete_edit(service, args.package_name, edit_id)
            _die(f"no active release found on track '{args.track}'")
        release = max(candidates, key=lambda r: _max_version_code(r["versionCodes"]))

        new_release = {"versionCodes": release["versionCodes"]}
        if "releaseNotes" in release:
            new_release["releaseNotes"] = release["releaseNotes"]

        if args.halt:
            new_release["status"] = "halted"
        elif args.complete:
            new_release["status"] = "completed"
        else:
            new_release["status"] = "inProgress"
            new_release["userFraction"] = args.user_fraction

        service.edits().tracks().update(
            packageName=args.package_name,
            editId=edit_id,
            track=args.track,
            body={"releases": [new_release]},
        ).execute()

        finish_edit(service, args.package_name, edit_id, args.dry_run)

    verb = "Validated (dry run)" if args.dry_run else "Updated"
    print(f"{verb}: track '{args.track}' -> status={new_release['status']}"
          + (f" userFraction={new_release['userFraction']}" if "userFraction" in new_release else ""))


# --- listing ------------------------------------------------------------


def cmd_listing_show(args, service) -> None:
    with edit_session(service, args.package_name) as edit_id:
        listing = (
            service.edits()
            .listings()
            .get(packageName=args.package_name, editId=edit_id, language=args.language)
            .execute()
        )
        _safe_delete_edit(service, args.package_name, edit_id)
    import json

    print(json.dumps(listing, indent=2))


def cmd_listing_update(args, service) -> None:
    body = {}
    title = _read_text_arg(args.title, None)
    short_description = _read_text_arg(args.short_description, args.short_description_file)
    full_description = _read_text_arg(args.full_description, args.full_description_file)
    if title:
        body["title"] = title
    if short_description:
        body["shortDescription"] = short_description
    if full_description:
        body["fullDescription"] = full_description
    if args.video:
        body["video"] = args.video
    if not body:
        _die("nothing to update -- pass at least one of --title/--short-description/"
             "--full-description/--video (or their *-file variants)")

    with edit_session(service, args.package_name) as edit_id:
        service.edits().listings().update(
            packageName=args.package_name,
            editId=edit_id,
            language=args.language,
            body=body,
        ).execute()
        finish_edit(service, args.package_name, edit_id, args.dry_run)

    verb = "Validated (dry run)" if args.dry_run else "Updated"
    print(f"{verb}: listing [{args.language}] fields: {', '.join(body.keys())}")


# --- details ------------------------------------------------------------


def cmd_details_show(args, service) -> None:
    with edit_session(service, args.package_name) as edit_id:
        details = service.edits().details().get(
            packageName=args.package_name, editId=edit_id
        ).execute()
        _safe_delete_edit(service, args.package_name, edit_id)
    import json

    print(json.dumps(details, indent=2))


def cmd_details_update(args, service) -> None:
    body = {}
    if args.contact_email:
        body["contactEmail"] = args.contact_email
    if args.contact_phone:
        body["contactPhone"] = args.contact_phone
    if args.contact_website:
        body["contactWebsite"] = args.contact_website
    if args.default_language:
        body["defaultLanguage"] = args.default_language
    if not body:
        _die(
            "nothing to update -- pass at least one of --contact-email/"
            "--contact-phone/--contact-website/--default-language"
        )

    with edit_session(service, args.package_name) as edit_id:
        service.edits().details().update(
            packageName=args.package_name, editId=edit_id, body=body
        ).execute()
        finish_edit(service, args.package_name, edit_id, args.dry_run)

    verb = "Validated (dry run)" if args.dry_run else "Updated"
    print(f"{verb}: app details: {', '.join(body.keys())}")


# --- argument parsing ---------------------------------------------------


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="playcli",
        description="Manage the amlkit-mobile Google Play Console listing from the CLI.",
    )
    parser.add_argument(
        "--credentials",
        default=os.environ.get("PLAY_CONSOLE_CREDENTIALS_FILE"),
        help="Path to a Play Console service-account JSON key "
        "(default: $PLAY_CONSOLE_CREDENTIALS_FILE). Omit to use Application "
        "Default Credentials instead -- what a Workload Identity Federation "
        "login or `gcloud auth application-default login` both set up.",
    )
    parser.add_argument(
        "--package-name",
        default=os.environ.get("PLAY_CONSOLE_PACKAGE_NAME", DEFAULT_PACKAGE_NAME),
        help=f"Play Console application ID (default: {DEFAULT_PACKAGE_NAME})",
    )

    subparsers = parser.add_subparsers(dest="command", required=True)

    # tracks
    tracks = subparsers.add_parser("tracks", help="Inspect release tracks")
    tracks_sub = tracks.add_subparsers(dest="tracks_command", required=True)
    tracks_list = tracks_sub.add_parser("list", help="List all tracks and their releases")
    tracks_list.set_defaults(func=cmd_tracks_list)
    tracks_get = tracks_sub.add_parser("get", help="Show one track in full")
    tracks_get.add_argument("track", help="Track name, e.g. internal, beta, production")
    tracks_get.set_defaults(func=cmd_tracks_get)

    # upload
    upload = subparsers.add_parser("upload", help="Upload an .aab and assign it to a track")
    upload.add_argument("bundle_path", help="Path to app-release.aab")
    upload.add_argument("--track", default="internal", help="Target track (default: internal)")
    upload.add_argument(
        "--status",
        default="completed",
        choices=["draft", "inProgress", "halted", "completed"],
        help="Release status (default: completed)",
    )
    upload.add_argument("--release-notes", help="Release notes text")
    upload.add_argument("--release-notes-file", help="Read release notes from this file")
    upload.add_argument("--language", default="en-US", help="Release notes language (default: en-US)")
    upload.add_argument(
        "--user-fraction",
        type=float,
        help="Staged rollout fraction 0-1; required if --status inProgress",
    )
    upload.add_argument("--dry-run", action="store_true", help="Validate only, do not commit")
    upload.add_argument("--yes", action="store_true", help="Skip the production confirmation prompt")
    upload.set_defaults(func=cmd_upload)

    # promote
    promote = subparsers.add_parser(
        "promote", help="Move the active release from one track to another"
    )
    promote.add_argument("--from-track", required=True, dest="from_track")
    promote.add_argument("--to-track", required=True, dest="to_track")
    promote.add_argument(
        "--status",
        default="completed",
        choices=["draft", "inProgress", "halted", "completed"],
        help="Status to set on the destination track (default: completed)",
    )
    promote.add_argument(
        "--user-fraction",
        type=float,
        help="Staged rollout fraction 0-1 (only meaningful with --status inProgress; "
        "defaults to 0.1 if omitted)",
    )
    promote.add_argument("--dry-run", action="store_true", help="Validate only, do not commit")
    promote.add_argument("--yes", action="store_true", help="Skip the production confirmation prompt")
    promote.set_defaults(func=cmd_promote)

    # rollout
    rollout = subparsers.add_parser(
        "rollout", help="Adjust the staged rollout of a track's active release"
    )
    rollout.add_argument("--track", required=True)
    rollout_group = rollout.add_mutually_exclusive_group(required=True)
    rollout_group.add_argument(
        "--user-fraction", type=float, help="Set the staged rollout fraction (0-1)"
    )
    rollout_group.add_argument(
        "--complete", action="store_true", help="Roll out to 100%% (status=completed)"
    )
    rollout_group.add_argument(
        "--halt", action="store_true", help="Halt the release (status=halted)"
    )
    rollout.add_argument("--dry-run", action="store_true", help="Validate only, do not commit")
    rollout.add_argument("--yes", action="store_true", help="Skip the production confirmation prompt")
    rollout.set_defaults(func=cmd_rollout)

    # listing
    listing = subparsers.add_parser("listing", help="Read or edit the main store listing")
    listing_sub = listing.add_subparsers(dest="listing_command", required=True)

    listing_show = listing_sub.add_parser("show", help="Show the store listing for one language")
    listing_show.add_argument("--language", default="en-US")
    listing_show.set_defaults(func=cmd_listing_show)

    listing_update = listing_sub.add_parser("update", help="Update store listing text fields")
    listing_update.add_argument("--language", default="en-US")
    listing_update.add_argument("--title", help="App title (<=30 chars)")
    listing_update.add_argument("--short-description")
    listing_update.add_argument("--short-description-file")
    listing_update.add_argument("--full-description")
    listing_update.add_argument("--full-description-file")
    listing_update.add_argument("--video", help="YouTube URL for the promo video")
    listing_update.add_argument("--dry-run", action="store_true", help="Validate only, do not commit")
    listing_update.set_defaults(func=cmd_listing_update)

    # details
    details = subparsers.add_parser("details", help="Read or edit app-wide contact details")
    details_sub = details.add_subparsers(dest="details_command", required=True)

    details_show = details_sub.add_parser("show", help="Show app details")
    details_show.set_defaults(func=cmd_details_show)

    details_update = details_sub.add_parser("update", help="Update app contact details")
    details_update.add_argument("--contact-email")
    details_update.add_argument("--contact-phone")
    details_update.add_argument("--contact-website")
    details_update.add_argument("--default-language")
    details_update.add_argument("--dry-run", action="store_true", help="Validate only, do not commit")
    details_update.set_defaults(func=cmd_details_update)

    return parser


def main(argv=None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    service = build_service(args.credentials)

    from googleapiclient.errors import HttpError

    try:
        args.func(args, service)
    except HttpError as e:
        _die(f"Play Developer API error: {e}")
    except FileNotFoundError as e:
        _die(str(e))
    return 0


if __name__ == "__main__":
    sys.exit(main())
