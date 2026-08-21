#!/usr/bin/env bash
set -euo pipefail

# Report the version update that the commit-msg hook would make.
# Usage: ./extensions/jgit/scripts/bump-version.sh 'fix: describe the change'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ $# -ne 1 ]; then
  echo "Usage: $0 '<Conventional Commit message>'"
  exit 1
fi

ROOT_DIR="$(cd "$PROJECT_DIR/../.." && pwd)"
cd "$ROOT_DIR"
MESSAGE_FILE=$(mktemp)
trap 'rm -f "$MESSAGE_FILE"' EXIT
printf '%s\n' "$1" > "$MESSAGE_FILE"
.githooks/versioning-commit-msg.sh --dry-run "$MESSAGE_FILE"
