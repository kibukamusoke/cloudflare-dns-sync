#!/bin/bash
set -euo pipefail

# Build the CloudDNSync image for linux/amd64 and push it to Docker Hub.
# The jar is built inside the multi-stage Dockerfile, so there is no separate
# build step (unlike the Node "npm run build && ..." flow).
#
# Optional (with defaults):
#   NAMESPACE      Docker Hub namespace/user   (default: kibukamusoke)
#   IMAGE          repository name             (default: clouddnsync)
#   VERSION        image tag                   (default: project version from pom.xml)
#   PLATFORM       target platform(s)          (default: linux/amd64)
#   PUSH_LATEST    also push ":latest"         (default: true)
#
# Login: either run `docker login` beforehand, or set
#   DOCKERHUB_USERNAME and DOCKERHUB_TOKEN  (a Docker Hub access token)
# and the script logs in for you.
#
# Usage:
#   ./deploy.sh
#   VERSION=1.2.0 ./deploy.sh
#   DOCKERHUB_USERNAME=kibukamusoke DOCKERHUB_TOKEN=dckr_pat_xxx ./deploy.sh

cd "$(dirname "$0")"

NAMESPACE="${NAMESPACE:-kibukamusoke}"
IMAGE="${IMAGE:-clouddnsync}"
VERSION="${VERSION:-$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -1)}"
PLATFORM="${PLATFORM:-linux/amd64}"
PUSH_LATEST="${PUSH_LATEST:-true}"

REPO="$NAMESPACE/$IMAGE"

# Log in if a token is provided; otherwise assume `docker login` was already run.
if [ -n "${DOCKERHUB_TOKEN:-}" ]; then
    echo "Logging in to Docker Hub as ${DOCKERHUB_USERNAME:-$NAMESPACE}..."
    echo "$DOCKERHUB_TOKEN" | docker login --username "${DOCKERHUB_USERNAME:-$NAMESPACE}" --password-stdin
fi

# One buildx invocation builds for the target platform and pushes every tag.
TAG_ARGS=(--tag "$REPO:$VERSION")
if [ "$PUSH_LATEST" = "true" ]; then
    TAG_ARGS+=(--tag "$REPO:latest")
fi

echo "Building and pushing $REPO:$VERSION ($PLATFORM)..."
docker buildx build --platform "$PLATFORM" "${TAG_ARGS[@]}" --push .

echo "Done: docker pull $REPO:$VERSION"
