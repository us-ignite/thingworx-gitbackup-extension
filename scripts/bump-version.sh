#!/usr/bin/env bash
set -euo pipefail

# Bump the extension version (canonical source: .version).
# Other files (gradle.properties, metadata.xml) use git smudge/clean
# filters that substitute @PACKAGE_VERSION@ from .version automatically.
#
# Usage: ./scripts/bump-version.sh <X.Y.Z>

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ $# -ne 1 ]; then
  echo "Usage: $0 <X.Y.Z>"
  exit 1
fi

NEW_VER="$1"
if ! echo "$NEW_VER" | grep -qxE '[0-9]+\.[0-9]+\.[0-9]+'; then
  echo "Error: version must be semver X.Y.Z, got: $NEW_VER"
  exit 1
fi

cd "$PROJECT_DIR"

CUR_VER="$(cat .version)"
echo "Bumping version from $CUR_VER to $NEW_VER"

echo "$NEW_VER" > .version

echo ""
echo "Updated: .version"
echo ""
echo "Re-smudge tracked files to propagate: git checkout -- gradle.properties configfiles/metadata.xml"
echo ""
echo "Verify with: ./gradlew packageExtension -PtwxVersion=9.6 --no-daemon"
echo ""
echo "Commit with:"
echo "  git add .version"
echo "  git commit -m 'Bump version to $NEW_VER'"
echo "  git tag v$NEW_VER"
