#!/usr/bin/env bash
set -euo pipefail

# Decrypts vendor/sdk-archive.tar.gz.gpg back into vendor/*.zip
# Usage: ./scripts/decrypt-sdk.sh
# Reads GPG_PASSPHRASE env var (set in CI as a secret).

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

RESOURCE_DIR="$PROJECT_DIR/vendor"
ENCRYPTED="$RESOURCE_DIR/twx-resources.tar.gz.gpg"
ARCHIVE="${ENCRYPTED%.gpg}"

if [ ! -f "$ENCRYPTED" ]; then
  echo "No encrypted resources archive found at $ENCRYPTED"
  exit 1
fi

if [ -z "${GPG_PASSPHRASE:-}" ]; then
  echo "GPG_PASSPHRASE is not set."
  echo "In CI, set it as a GitHub secret. Locally, export it or use gpg directly."
  exit 1
fi

echo "Decrypting $ENCRYPTED ..."
gpg --decrypt --cipher AES256 --batch --passphrase "$GPG_PASSPHRASE" -o "$ARCHIVE" "$ENCRYPTED"

echo "Extracting resources ..."
tar xzf "$ARCHIVE" -C "$RESOURCE_DIR"
rm "$ARCHIVE"

echo "Resources restored to $RESOURCE_DIR"
ls -1 "$RESOURCE_DIR"/*.zip "$RESOURCE_DIR"/license.bin
