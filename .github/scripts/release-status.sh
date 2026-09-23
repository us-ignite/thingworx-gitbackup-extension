#!/usr/bin/env bash
# Read-only release preflight. A missing tag is distinct from a Git/network error.
set -euo pipefail
version=$(tr -d '[:space:]' < "$2")
tag="$1-v${version}"
if git ls-remote --exit-code --refs origin "refs/tags/$tag" >/dev/null; then
  echo 'published=true' >> "$GITHUB_OUTPUT"
  echo "Version already published: $tag"
  echo "Version already published: $tag; publication skipped." >> "$GITHUB_STEP_SUMMARY"
else
  status=$?
  if [ "$status" -ne 2 ]; then
    echo "Unable to check remote release tag $tag (git exit $status)" >&2
    exit "$status"
  fi
  echo 'published=false' >> "$GITHUB_OUTPUT"
fi
