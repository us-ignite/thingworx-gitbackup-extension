#!/usr/bin/env bash
set -euo pipefail

# Bump each changed component's version.
# Usage: ./extensions/jgit/scripts/bump-version.sh <major|minor|patch>

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ $# -ne 1 ] || ! [[ "$1" =~ ^(major|minor|patch)$ ]]; then
  echo "Usage: $0 <major|minor|patch>"
  exit 1
fi

ROOT_DIR="$(cd "$PROJECT_DIR/../.." && pwd)"
cd "$ROOT_DIR"
exec ./gradlew bumpVersion "-P$1"
