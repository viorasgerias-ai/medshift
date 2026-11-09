#!/usr/bin/env bash
# ==============================================================================
# MedShift / Médicos a Postos — Automated Cloud Run & Firebase Deployment Script
# ==============================================================================
# Containerizes the Kotlin backend logic (LGPD Auto-Redactor + TCRIA 5-Gate
# Governance Engine + Firebase Firestore & Auth Integration) and deploys it to
# Google Cloud Run alongside Firebase Firestore Rules and Firebase Hosting.
#
# Usage:
#   ./deploy-cloudrun.sh [--project PROJECT_ID] [--region REGION] [--with-android-apk]
# ==============================================================================

set -euo pipefail

PROJECT_ID="${FIREBASE_PROJECT_ID:-gen-lang-client-0265308632}"
REGION="${CLOUD_RUN_REGION:-southamerica-east1}"
SERVICE_NAME="${CLOUD_RUN_SERVICE:-medshift-tcria-api}"
FIRESTORE_DB="${FIRESTORE_DATABASE_ID:-ai-studio-android-medshift-eabb9a2d-89b1-44ad-943c-843ef86c682b}"
BUILD_ANDROID_APK=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --project)
      PROJECT_ID="$2"
      shift 2
      ;;
    --region)
      REGION="$2"
      shift 2
      ;;
    --service)
      SERVICE_NAME="$2"
      shift 2
      ;;
    --with-android-apk)
      BUILD_ANDROID_APK=true
      shift
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

echo "=================================================================="
echo " MedShift / Médicos a Postos — Cloud Run & Firebase Deployment"
echo "=================================================================="
echo " • GCP / Firebase Project : ${PROJECT_ID}"
echo " • Cloud Run Service      : ${SERVICE_NAME} (${REGION})"
echo " • Firestore Database ID  : ${FIRESTORE_DB}"
echo "=================================================================="

if ! command -v gcloud >/dev/null 2>&1; then
  echo "[ERROR] gcloud CLI is required to deploy to Google Cloud Run."
  exit 1
fi

echo "[1/6] Configuring Google Cloud project (${PROJECT_ID})..."
gcloud config set project "${PROJECT_ID}"

echo "[2/6] Enabling required Google Cloud Serverless & Firebase APIs..."
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  firestore.googleapis.com \
  firebase.googleapis.com

if [[ "${BUILD_ANDROID_APK}" == "true" ]]; then
  echo "[3/6] Running Android unit tests & assembling Release APK..."
  gradle :app:testDebugUnitTest --tests "com.example.ExampleUnitTest"
  gradle :app:assembleRelease
else
  echo "[3/6] Skipping local Android APK build (pass --with-android-apk to include)."
fi

echo "[4/6] Building and deploying Kotlin container to Google Cloud Run (${SERVICE_NAME})..."
gcloud run deploy "${SERVICE_NAME}" \
  --source . \
  --region "${REGION}" \
  --platform managed \
  --allow-unauthenticated \
  --min-instances 0 \
  --max-instances 10 \
  --memory 512Mi \
  --cpu 1 \
  --set-env-vars "FIREBASE_PROJECT_ID=${PROJECT_ID},FIRESTORE_DATABASE_ID=${FIRESTORE_DB},K_REGION=${REGION},LGPD_REDACTION_MODE=STRICT,TCRIA_RULESET_VERSION=1.0-CORE"

SERVICE_URL="$(gcloud run services describe "${SERVICE_NAME}" --region "${REGION}" --format 'value(status.url)')"
echo " ✓ Cloud Run Service URL: ${SERVICE_URL}"

if command -v firebase >/dev/null 2>&1; then
  echo "[5/6] Deploying Firestore Security Rules & Firebase Hosting..."
  firebase deploy --project "${PROJECT_ID}" --only firestore:rules,hosting
else
  echo "[5/6] Firebase CLI not found in PATH; skipping firebase hosting deploy."
fi

echo "[6/6] Running post-deployment health probe against ${SERVICE_URL}/api/health..."
curl -fsSL "${SERVICE_URL}/api/health" && echo ""

echo "=================================================================="
echo " Deployment Complete!"
echo " • Cloud Run Endpoint : ${SERVICE_URL}"
echo " • Health Check       : ${SERVICE_URL}/api/health"
echo " • TCRIA Evaluation   : ${SERVICE_URL}/api/governance/evaluate"
echo " • LGPD Auto-Redactor : ${SERVICE_URL}/api/lgpd/redact"
echo "=================================================================="
