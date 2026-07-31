#!/usr/bin/env bash
set -euo pipefail

# Encrypts vendor/*.zip into vendor/sdk-archive.tar.gz.gpg
# Usage: ./scripts/encrypt-sdk.sh
# Prompts for a passphrase or reads GPG_PASSPHRASE env var.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

SDK_DIR="$PROJECT_DIR/vendor"
ARCHIVE="$SDK_DIR/sdk-archive.tar.gz"
ZIPS=(
  MED-61098-CD-096_9-6-0_ThingWorx-Extension-SDK-9-6-0.zip
  MED-61098-CD-097_9-7-0_ThingWorx-Extension-SDK-9-7-0.zip
  MED-61098-CD-101_10-1-0_ThingWorx-Extension-SDK-10-1-0.zip
)

for f in "${ZIPS[@]}"; do
  if [ ! -f "$SDK_DIR/$f" ]; then
    echo "Missing: $SDK_DIR/$f"
    exit 1
  fi
done

echo "Packing SDK zips into $ARCHIVE ..."
tar czf "$ARCHIVE" -C "$SDK_DIR" "${ZIPS[@]}"

echo "Encrypting ..."
if [ -n "${GPG_PASSPHRASE:-}" ]; then
  gpg --symmetric --cipher AES256 --batch --passphrase "$GPG_PASSPHRASE" -o "$SDK_DIR/sdk-archive.tar.gz.gpg" "$ARCHIVE"
else
  gpg --symmetric --cipher AES256 -o "$SDK_DIR/sdk-archive.tar.gz.gpg" "$ARCHIVE"
fi

rm "$ARCHIVE"
echo "Created: $SDK_DIR/sdk-archive.tar.gz.gpg"
echo ""
echo "Now commit the archive:"
echo "  git add vendor/sdk-archive.tar.gz.gpg"
echo "  git commit -m 'Add encrypted SDK archive'"
echo ""
echo "Then set the GPG_PASSPHRASE secret in GitHub."
