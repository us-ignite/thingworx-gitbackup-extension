#!/bin/sh
# Preserve an existing security keystore across dev stack restarts.
#
# security-init regenerates the keystore when it runs, which changes the
# licenseguid (ThingWorx device ID) and invalidates any license issued for the
# previous ID. Only initialize when the keystore is absent. Set
# FORCE_SECURITY_INIT=true to deliberately regenerate it (for example after
# changing the database password).
set -eu

KEYSTORE_DIR="${KEYSTORE_FILE_PATH:-/ThingworxStorage}"
KEYSTORE_FILE="${KEYSTORE_FILE_NAME:-keystore.pfx}"
KEYSTORE_PATH="${KEYSTORE_DIR}/${KEYSTORE_FILE}"
PASSWORD_DIR="${KEYSTORE_PASSWORD_FILE_PATH:-/ThingworxStorage}"
PASSWORD_FILE="${PASSWORD_DIR}/keystore-password"

if [ -s "${KEYSTORE_PATH}" ] && [ "${FORCE_SECURITY_INIT:-false}" != "true" ]; then
    if [ ! -s "${PASSWORD_FILE}" ] && [ -n "${KEYSTORE_PASSWORD:-}" ]; then
        /opt/security-tool/bin/security-common-cli createTokenFile "${PASSWORD_FILE}" "${KEYSTORE_PASSWORD}"
    fi
    echo "Existing keystore found at ${KEYSTORE_PATH}; preserving license and secrets."
    exit 0
fi

exec /opt/docker-entrypoint.sh
