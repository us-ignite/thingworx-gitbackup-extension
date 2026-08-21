#!/usr/bin/env sh
# Install or upgrade a ThingWorx extension package via the ExtensionPackageUploader
# REST API. Shared by docker-compose (extension-init service) and the Testcontainers
# installer (JGitExtensionInstaller) so both flows use identical install logic.
#
# Environment:
#   TWX_URL        ThingWorx base URL (default: http://thingworx:8080)
#   TWX_USERNAME   Admin user (default: Administrator)
#   TWX_PASSWORD   Admin password (required)
#   EXTENSION_ZIP  Path to the extension ZIP, or a directory containing exactly
#                  one *.zip to auto-select (default: /tmp/extension.zip)

set -uo pipefail

TWX_URL="${TWX_URL:-http://thingworx:8080}"
TWX_USERNAME="${TWX_USERNAME:-Administrator}"
TWX_PASSWORD="${TWX_PASSWORD:?TWX_PASSWORD must be set}"
EXTENSION_ZIP="${EXTENSION_ZIP:-/tmp/extension.zip}"
UPLOADER="${TWX_URL}/Thingworx/ExtensionPackageUploader"

echo "=== Starting extension install ==="
echo "Target:  ${TWX_URL}"
echo "Package: ${EXTENSION_ZIP}"

if [ -d "${EXTENSION_ZIP}" ]; then
    set -- "${EXTENSION_ZIP}"/*.zip
    if [ "$#" -ne 1 ] || [ ! -f "$1" ]; then
        echo "ERROR: EXTENSION_ZIP directory does not contain exactly one *.zip" >&2
        exit 1
    fi
    EXTENSION_ZIP="$1"
    echo "Selected: ${EXTENSION_ZIP}"
fi

if [ ! -f "${EXTENSION_ZIP}" ]; then
    echo "ERROR: extension ZIP not found at ${EXTENSION_ZIP}" >&2
    exit 1
fi

echo "Waiting for ThingWorx health endpoint..."
HEALTHY=false
for i in $(seq 1 60); do
    if curl -sf --connect-timeout 5 --max-time 10 "${TWX_URL}/Thingworx/health" > /dev/null 2>&1; then
        HEALTHY=true
        echo "ThingWorx is healthy!"
        break
    fi
    echo "  Waiting... (${i}/60)"
    sleep 15
done
if [ "${HEALTHY}" != "true" ]; then
    echo "ERROR: ThingWorx did not become healthy at ${TWX_URL}" >&2
    exit 1
fi

UPLOAD_OK=false
for purpose in import upgrade; do
    for attempt in 1 2 3; do
        RESP_FILE=$(mktemp)
        UPLOAD_STATUS=$(curl -s -o "${RESP_FILE}" -w '%{http_code}' -X POST \
            -H 'X-XSRF-TOKEN: TWX-XSRF-TOKEN-VALUE' \
            -H 'X-Requested-By: ThingWorx' \
            -H 'Accept: application/json' \
            -u "${TWX_USERNAME}:${TWX_PASSWORD}" \
            -F "file=@${EXTENSION_ZIP};filename=$(basename "${EXTENSION_ZIP}");type=application/octet-stream" \
            --connect-timeout 30 --max-time 300 \
            "${UPLOADER}?purpose=${purpose}") || true
        RESP_BODY=$(cat "${RESP_FILE}")
        rm -f "${RESP_FILE}"
        echo "Attempt ${attempt} (${purpose}): upload status=${UPLOAD_STATUS}"
        echo "Response body: ${RESP_BODY}"

        if [ "${UPLOAD_STATUS}" = "406" ] && printf '%s' "${RESP_BODY}" | grep -q "queued for installation"; then
            echo "Upload queued for installation; restart required"
            UPLOAD_OK=true
            break 2
        fi
        if [ "${purpose}" = "import" ] && { [ "${UPLOAD_STATUS}" = "406" ] || printf '%s' "${RESP_BODY}" | grep -q "already installed"; }; then
            echo "Extension already installed, trying upgrade..."
            break
        fi
        if [ "${UPLOAD_STATUS}" = "200" ] || [ "${UPLOAD_STATUS}" = "201" ]; then
            echo "Upload succeeded"
            UPLOAD_OK=true
            break 2
        fi
        echo "Retrying..."
        sleep 10
    done
done

if [ "${UPLOAD_OK}" != "true" ]; then
    echo "UPLOAD_FAILED"
    exit 1
fi

echo "Waiting for GIT.Utility.Thing to become available..."
THING_AVAILABLE=false
for i in $(seq 1 60); do
    THING_STATUS=$(curl -s -o /dev/null -w '%{http_code}' \
        -H 'Accept: application/json' \
        -H 'X-XSRF-TOKEN: TWX-XSRF-TOKEN-VALUE' \
        -H 'X-Requested-By: ThingWorx' \
        -u "${TWX_USERNAME}:${TWX_PASSWORD}" \
        --connect-timeout 10 --max-time 30 \
        "${TWX_URL}/Thingworx/Things/GIT.Utility.Thing") || true
    case "${THING_STATUS}" in
        2??)
            THING_AVAILABLE=true
            echo "GIT.Utility.Thing is available"
            break
            ;;
    esac
    echo "  Waiting for GIT.Utility.Thing... (${i}/60, status=${THING_STATUS})"
    sleep 2
done
if [ "${THING_AVAILABLE}" != "true" ]; then
    echo "ERROR: GIT.Utility.Thing did not become available after upload" >&2
    echo "UPLOAD_FAILED"
    exit 1
fi

echo "UPLOAD_DONE"
echo "=== Extension install complete ==="
