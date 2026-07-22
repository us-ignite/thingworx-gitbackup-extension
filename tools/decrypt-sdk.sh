#!/usr/bin/env bash
set -euo pipefail

# Decrypts twx-lib/sdk-archive.tar.gz.gpg back into twx-lib/*.zip
# Usage: ./tools/decrypt-sdk.sh
# Reads GPG_PASSPHRASE env var (set in CI as a secret).

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

SDK_DIR="$PROJECT_DIR/twx-lib"
ENCRYPTED="$SDK_DIR/sdk-archive.tar.gz.gpg"
ARCHIVE="${ENCRYPTED%.gpg}"

if [ ! -f "$ENCRYPTED" ]; then
  echo "No encrypted SDK archive found at $ENCRYPTED"
  exit 1
fi

if [ -z "${GPG_PASSPHRASE:-}" ]; then
  echo "GPG_PASSPHRASE is not set."
  echo "In CI, set it as a GitHub secret. Locally, export it or use gpg directly."
  exit 1
fi

echo "Decrypting $ENCRYPTED ..."
gpg --decrypt --cipher AES256 --batch --passphrase "$GPG_PASSPHRASE" -o "$ARCHIVE" "$ENCRYPTED"

echo "Extracting SDK zips ..."
tar xzf "$ARCHIVE" -C "$SDK_DIR"
rm "$ARCHIVE"

echo "SDK zips restored to $SDK_DIR"
ls -1 "$SDK_DIR"/*.zip
