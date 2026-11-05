#!/bin/sh
# Configure the local demo repository Thing and Administrator's Gitea credentials.

set -eu

TWX_URL="${TWX_URL:-http://thingworx:8080}"
TWX_USERNAME="${TWX_USERNAME:-Administrator}"
TWX_PASSWORD="${TWX_PASSWORD:?TWX_PASSWORD must be set}"
GITEA_URL="${GITEA_URL:-http://gitea:3000}"
GITEA_USERNAME="${GITEA_USERNAME:?GITEA_USERNAME must be set}"
GITEA_PASSWORD="${GITEA_PASSWORD:?GITEA_PASSWORD must be set}"
GITEA_REPO_NAME="${GITEA_REPO_NAME:?GITEA_REPO_NAME must be set}"
DEMO_REPO_NAME="${DEMO_REPO_NAME:-demo}"
DEMO_REPO_PATH="${DEMO_REPO_PATH:-examples/thingworx-jgit-extension-export}"
DEMO_PROJECT_NAME="${DEMO_PROJECT_NAME:-Example}"

if [ ! -f /example/Projects/Example.xml ]; then
    echo "[demo-init] ERROR: The mounted ThingWorx example export is missing." >&2
    exit 1
fi

echo "[demo-init] Waiting for the JGit credential service..."
UTILITY_READY=false
for i in $(seq 1 120); do
    readiness_response=$(curl -sS -u "${TWX_USERNAME}:${TWX_PASSWORD}" \
        -H 'X-XSRF-TOKEN: TWX-XSRF-TOKEN-VALUE' \
        -H 'X-Requested-By: ThingWorx' \
        -H 'Accept: application/json' \
        -H 'Content-Type: application/json' \
        -X POST "${TWX_URL}/Thingworx/Things/GIT.Utility.Thing/Services/GitCredentialList" \
        -d '{}' 2>/dev/null) || true
    if printf '%s' "$readiness_response" | grep -Eq '"Error"[[:space:]]*:[[:space:]]*false'; then
        UTILITY_READY=true
        break
    fi
    sleep 5
done
if [ "$UTILITY_READY" != "true" ]; then
    echo "[demo-init] ERROR: JGit credential services did not become available." >&2
    exit 1
fi

call_service() {
    service_name=$1
    body=$2
    curl -fsS -u "${TWX_USERNAME}:${TWX_PASSWORD}" \
        -H 'X-XSRF-TOKEN: TWX-XSRF-TOKEN-VALUE' \
        -H 'X-Requested-By: ThingWorx' \
        -H 'Accept: application/json' \
        -H 'Content-Type: application/json' \
        -X POST "${TWX_URL}/Thingworx/Things/GIT.Utility.Thing/Services/${service_name}" \
        -d "$body"
}

thing_status=$(curl -sS -o /dev/null -w '%{http_code}' -u "${TWX_USERNAME}:${TWX_PASSWORD}" \
    -H 'Accept: application/json' "${TWX_URL}/Thingworx/Things/${DEMO_REPO_NAME}") || true
if [ "$thing_status" = "404" ]; then
    create_payload="{\"RepoName\":\"${DEMO_REPO_NAME}\",\"GitRepoURL\":\"${GITEA_URL}/${GITEA_USERNAME}/${GITEA_REPO_NAME}.git\",\"RepoPath\":\"${DEMO_REPO_PATH}\",\"BranchName\":\"main\",\"ProjectName\":\"${DEMO_PROJECT_NAME}\",\"UseProxy\":false,\"ProxyURL\":\"\",\"ProxyPort\":0,\"LocalizationTokensPrefix\":\"\",\"GitCommitterUser\":\"${GITEA_USERNAME}\",\"GitCommitterPassword\":\"${GITEA_PASSWORD}\",\"GitCommitterEmail\":\"test@example.com\",\"GitCommitterFullName\":\"Gitea Test User\"}"
    create_response=$(call_service RepositoryCreate "$create_payload") || true
    if ! printf '%s' "$create_response" | grep -Eq '"Error"[[:space:]]*:[[:space:]]*false'; then
        echo "[demo-init] RepositoryCreate did not report success; checking whether the Thing was created." >&2
    fi
elif [ "$thing_status" != "200" ]; then
    echo "[demo-init] ERROR: Could not inspect Thing '${DEMO_REPO_NAME}' (HTTP ${thing_status})." >&2
    exit 1
fi

thing_status=$(curl -sS -o /dev/null -w '%{http_code}' -u "${TWX_USERNAME}:${TWX_PASSWORD}" \
    -H 'Accept: application/json' "${TWX_URL}/Thingworx/Things/${DEMO_REPO_NAME}") || true
if [ "$thing_status" != "200" ]; then
    echo "[demo-init] ERROR: Thing '${DEMO_REPO_NAME}' is unavailable (HTTP ${thing_status})." >&2
    exit 1
fi

set_property() {
    property_name=$1
    property_value=$2
    status=$(curl -sS -o /dev/null -w '%{http_code}' -u "${TWX_USERNAME}:${TWX_PASSWORD}" \
        -H 'Content-Type: application/json' \
        -H 'X-XSRF-TOKEN: TWX-XSRF-TOKEN-VALUE' \
        -H 'X-Requested-By: ThingWorx' \
        -X PUT "${TWX_URL}/Thingworx/Things/${DEMO_REPO_NAME}/Properties/${property_name}" \
        -d "{\"${property_name}\":\"${property_value}\"}") || true
    if [ "$status" != "200" ]; then
        echo "[demo-init] ERROR: Failed to set ${property_name} (HTTP ${status})." >&2
        exit 1
    fi
}

set_property GitRepoURL "${GITEA_URL}/${GITEA_USERNAME}/${GITEA_REPO_NAME}.git"
set_property RepoPath "$DEMO_REPO_PATH"
set_property BranchName main
set_property ProjectName "$DEMO_PROJECT_NAME"

credential_payload="{\"GitCommitterUser\":\"${GITEA_USERNAME}\",\"GitCommitterPassword\":\"${GITEA_PASSWORD}\",\"GitCommitterEmail\":\"test@example.com\",\"GitCommitterFullName\":\"Gitea Test User\",\"GitThing\":\"${DEMO_REPO_NAME}\",\"GpgKeyFingerprint\":\"\"}"
credential_response=$(call_service GitCredentialUpdate "$credential_payload") || true
if ! printf '%s' "$credential_response" | grep -Eq '"Error"[[:space:]]*:[[:space:]]*false'; then
    credential_response=$(call_service GitCredentialCreate "$credential_payload") || true
    if ! printf '%s' "$credential_response" | grep -Eq '"Error"[[:space:]]*:[[:space:]]*false'; then
        echo "[demo-init] ERROR: Could not create or update Administrator's demo Git credentials." >&2
        exit 1
    fi
fi

echo "[demo-init] Demo repository and Administrator Git credentials are configured."
