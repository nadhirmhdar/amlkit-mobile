#!/bin/bash
# One-time setup: lets GitHub Actions publish to Google Play Console without
# any downloadable service-account key, via Workload Identity Federation
# (WIF) -- same approach the amlkit repo already uses for its Cloud Run
# deploy (see that repo's scripts/setup_github_deploy_auth.sh), reusing the
# same GCP project and the same Workload Identity Pool, just adding a
# provider scoped to this repo and a service account scoped to Play access.
#
# Run this once, from a terminal where `gcloud` is authenticated as an
# Owner/IAM Admin on the project (e.g. `gcloud auth login` as
# nadhirmhd.ar@gmail.com -- Cloud Shell already is, once you've signed in).
#
# After it finishes, it prints two values (WIF provider + service account
# email) -- give those back so the GitHub Actions workflow can be wired up,
# or run the `gh variable set` commands it prints yourself. You will still
# need to invite the printed service account email in Play Console -> Setup
# -> API access -> Users and permissions, and grant it whatever release
# permissions you intend this CLI/workflow to use.
set -euo pipefail

PROJECT_ID="gen-lang-client-0153967509"
PROJECT_NUMBER="720622408077"
REPO="nadhirmhdar/amlkit-mobile"
SA_NAME="github-play-publisher"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
# Reuses the Workload Identity Pool amlkit's own deploy setup already
# created in this project -- a pool can hold providers for multiple repos,
# each with its own attribute-condition, so there's no need for a second
# pool.
POOL_ID="github-pool"
PROVIDER_ID="github-provider-mobile"

echo "== Enabling required APIs =="
# sts/iamcredentials: needed for WIF itself (may already be enabled from
# amlkit's own setup, in which case this is a no-op). androidpublisher: the
# Google Play Android Developer API this CLI actually calls -- easy to miss
# since Play Console's own UI never prompts you to enable it.
gcloud services enable sts.googleapis.com iamcredentials.googleapis.com androidpublisher.googleapis.com \
  --project "$PROJECT_ID"

echo "== Creating Play-publisher service account (skips if it already exists) =="
gcloud iam service-accounts create "$SA_NAME" \
  --project "$PROJECT_ID" \
  --display-name "GitHub Actions Play Console Publisher (amlkit-mobile)" \
  || echo "(already exists, continuing)"

# Deliberately no `gcloud projects add-iam-policy-binding` here: unlike the
# Cloud Run deploy SA, this one needs no GCP IAM roles at all. Play Console
# release permissions are granted entirely inside Play Console itself (Setup
# -> API access -> Users and permissions), not via GCP project IAM.

echo "== Creating Workload Identity Pool (skips if it already exists) =="
gcloud iam workload-identity-pools create "$POOL_ID" \
  --project="$PROJECT_ID" \
  --location="global" \
  --display-name="GitHub Actions Pool" \
  || echo "(already exists, continuing)"

echo "== Creating OIDC provider, restricted to this repo only =="
gcloud iam workload-identity-pools providers create-oidc "$PROVIDER_ID" \
  --project="$PROJECT_ID" \
  --location="global" \
  --workload-identity-pool="$POOL_ID" \
  --display-name="GitHub provider (amlkit-mobile)" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" \
  --attribute-condition="assertion.repository=='${REPO}'" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  || echo "(already exists, continuing)"

echo "== Allowing only this repo's workflows to impersonate the service account =="
gcloud iam service-accounts add-iam-policy-binding "$SA_EMAIL" \
  --project="$PROJECT_ID" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL_ID}/attribute.repository/${REPO}" \
  --quiet

WIF_PROVIDER="projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL_ID}/providers/${PROVIDER_ID}"

echo ""
echo "========================================================================"
echo "DONE. Two values the GitHub Actions workflow needs:"
echo ""
echo "  GCP_WIF_PROVIDER        = ${WIF_PROVIDER}"
echo "  GCP_PLAY_SERVICE_ACCOUNT = ${SA_EMAIL}"
echo ""
echo "Either paste those back to Claude, or set them yourself with:"
echo ""
echo "  gh variable set GCP_WIF_PROVIDER         -b \"${WIF_PROVIDER}\" -R ${REPO}"
echo "  gh variable set GCP_PLAY_SERVICE_ACCOUNT -b \"${SA_EMAIL}\" -R ${REPO}"
echo ""
echo "STILL TO DO, in Play Console (not gcloud):"
echo "  Setup -> API access -> Users and permissions -> Invite new users"
echo "  -> ${SA_EMAIL} -> grant the release permissions you intend to use"
echo "  (e.g. 'Release apps to testing tracks'; add 'Release to production'"
echo "  only if you actually intend to publish to production this way)."
echo "========================================================================"
