#!/usr/bin/env bash
# GHCR-backed cache for the integration-test images (platform-postgres,
# postgres-init, security-tool) of one ThingWorx version.
#
# Cache tags are keyed by a hash of the git blob/tree IDs of everything that
# feeds the image build, so the key is computable before vendor decryption and
# changes whenever an input changes. Cache images live under a separate
# ci-cache/ namespace so they can be pruned without touching release tags.
#
# Usage:
#   ci-images.sh key  <twx>   print the cache key
#   ci-images.sh has  <twx>   exit 0 if the cache images exist in GHCR
#   ci-images.sh pull <twx>   pull cache images and retag to the names tests use
#   ci-images.sh push <twx>   tag locally built images as cache images and push
set -euo pipefail

# Bump to invalidate every cache entry (e.g. to pick up new "latest" JDKs,
# which are downloaded at build time and are not part of the key).
CACHE_SCHEMA=1
REGISTRY=ghcr.io/us-ignite/thingworx
CACHE_REPO="${REGISTRY}/ci-cache"

cmd="${1:?usage: ci-images.sh key|has|pull|push <twx>}"
twx="${2:?missing ThingWorx version (e.g. 9.6)}"

# Keep in sync with images/platform/build.gradle (platformVersion) and
# images/security-tool/build.gradle (security-tool version tag).
case "$twx" in
  9.6)  platform=9.6.9;  security=9.6.9 ;;
  9.7)  platform=9.7.6;  security=9.7.9 ;;
  10.0) platform=10.0.4; security=10.0.9 ;;
  10.1) platform=10.1.2; security=10.1.2 ;;
  10.2) platform=10.2.1; security=10.2.1 ;;
  *) echo "Unsupported ThingWorx version: $twx" >&2; exit 2 ;;
esac

# Image names the tests expect (see thingworx/TwxVersions.groovy and
# TestImages.securityImageForPlatform).
local_images=(
  "${REGISTRY}/platform-postgres:${platform}-openjdk"
  "${REGISTRY}/postgres-init:${platform}-openjdk"
  "${REGISTRY}/security-tool:${security}"
)

cache_key() {
  local major="${twx%%.*}" minor="${twx#*.}"
  local code
  code=$(printf '%02d%d' "$major" "$minor")
  {
    echo "schema=${CACHE_SCHEMA} twx=${twx}"
    git ls-tree -r HEAD -- \
      images/platform \
      images/security-tool \
      libraries/build-logic/src/main/groovy/thingworx.base.gradle \
      libraries/build-logic/src/main/groovy/thingworx.docker.gradle
    git ls-tree HEAD -- vendor/ | grep -E "/MED-(61268|61111|61282)-CD-${code}_"
  } | sha256sum | cut -c1-16
}

cache_images() {
  local key="$1"
  echo "${CACHE_REPO}/platform-postgres:${twx}-${key}"
  echo "${CACHE_REPO}/postgres-init:${twx}-${key}"
  echo "${CACHE_REPO}/security-tool:${twx}-${key}"
}

key=$(cache_key)
mapfile -t remote_images < <(cache_images "$key")

case "$cmd" in
  key)
    echo "$key"
    ;;
  has)
    for img in "${remote_images[@]}"; do
      if ! docker manifest inspect "$img" >/dev/null 2>&1; then
        echo "Cache miss: $img"
        exit 1
      fi
    done
    echo "Cache hit for TWX ${twx} (key ${key})"
    ;;
  pull)
    for i in "${!remote_images[@]}"; do
      docker pull --quiet "${remote_images[$i]}"
      docker tag "${remote_images[$i]}" "${local_images[$i]}"
    done
    echo "Pulled cached images for TWX ${twx} (key ${key})"
    ;;
  push)
    # Licences are mounted at runtime and must never be baked into an image
    # we publish. Scan each image's filesystem before anything is pushed.
    for img in "${local_images[@]}"; do
      cid=$(docker create "$img")
      found=$(docker export "$cid" | tar -t | grep -E '(^|/)(license\.bin|trial\.bin)$' || true)
      docker rm "$cid" >/dev/null
      if [ -n "$found" ]; then
        echo "Refusing to push ${img}: it contains licence files:" >&2
        echo "$found" >&2
        exit 1
      fi
    done
    for i in "${!remote_images[@]}"; do
      docker tag "${local_images[$i]}" "${remote_images[$i]}"
      docker push --quiet "${remote_images[$i]}"
    done
    echo "Pushed cached images for TWX ${twx} (key ${key})"
    ;;
  *)
    echo "Unknown command: $cmd" >&2
    exit 2
    ;;
esac
