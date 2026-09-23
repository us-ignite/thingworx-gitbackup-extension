#!/bin/sh
# Materialize vendor/license.bin from the TWX_LICENSE_B64 environment
# variable (base64, same value as the TWX_LICENSE_B64 GitHub secret).
# No-op when vendor/license.bin already exists. Run before
# `docker compose up` on machines without the license file:
#   ./apps/thingworx-jgit-extension/scripts/materialize-license.sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
DEST="$ROOT/vendor/license.bin"

if [ -s "$DEST" ]; then
  echo "license present: $DEST"
  exit 0
fi
if [ -z "${TWX_LICENSE_B64:-}" ]; then
  echo "error: no vendor/license.bin and TWX_LICENSE_B64 is unset" >&2
  exit 1
fi
printf '%s' "$TWX_LICENSE_B64" | base64 -d > "$DEST"
echo "wrote $DEST from TWX_LICENSE_B64"
