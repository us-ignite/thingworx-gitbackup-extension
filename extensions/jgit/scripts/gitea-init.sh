#!/usr/bin/env sh
# Initialize Gitea with an admin user and a test repository. Shared by docker-compose
# (gitea-init service) and the Testcontainers one-shot GiteaInit container so both
# flows use identical init logic.
#
# Environment:
#   GITEA_URL             Gitea base URL (default: http://gitea:3000)
#   GITEA_USERNAME        Admin username (required)
#   GITEA_PASSWORD        Admin password (required)
#   GITEA_ADMIN_EMAIL     Admin email (default: test@example.com)
#   GITEA_REPO_NAME       Repository to create (required)
#   GITEA_REPO_AUTO_INIT  Whether to auto-initialize the repository (default: true)
#   GITEA_DEFAULT_BRANCH  Default branch for the repository (default: main)

set -e

GITEA_URL="${GITEA_URL:-http://gitea:3000}"
GITEA_USERNAME="${GITEA_USERNAME:?GITEA_USERNAME must be set}"
GITEA_PASSWORD="${GITEA_PASSWORD:?GITEA_PASSWORD must be set}"
GITEA_ADMIN_EMAIL="${GITEA_ADMIN_EMAIL:-test@example.com}"
GITEA_REPO_NAME="${GITEA_REPO_NAME:?GITEA_REPO_NAME must be set}"
GITEA_REPO_AUTO_INIT="${GITEA_REPO_AUTO_INIT:-true}"
GITEA_DEFAULT_BRANCH="${GITEA_DEFAULT_BRANCH:-main}"
GITEA_BIN="$(command -v gitea || echo /usr/local/bin/gitea)"

echo "[gitea-init] Waiting for Gitea API at ${GITEA_URL}..."
READY=false
for i in $(seq 1 30); do
    if curl -sf --connect-timeout 5 --max-time 10 "${GITEA_URL}/api/v1/version" > /dev/null 2>&1; then
        READY=true
        echo "[gitea-init] Gitea API ready."
        break
    fi
    echo "[gitea-init]  Waiting... (${i}/30)"
    sleep 3
done
if [ "${READY}" != "true" ]; then
    echo "[gitea-init] ERROR: Gitea API did not become ready at ${GITEA_URL}" >&2
    exit 1
fi

# Run the gitea CLI as the 'git' user when running as root so it can write to /data.
run_gitea() {
    if [ "$(id -u)" = "0" ] && command -v su > /dev/null 2>&1; then
        su git -c "$*"
    else
        sh -c "$*"
    fi
}

echo "[gitea-init] Creating admin user '${GITEA_USERNAME}'..."
if curl -sf --connect-timeout 5 --max-time 10 -u "${GITEA_USERNAME}:${GITEA_PASSWORD}" "${GITEA_URL}/api/v1/user" > /dev/null 2>&1; then
    echo "[gitea-init] Admin user already exists."
else
    for i in $(seq 1 10); do
        if run_gitea "${GITEA_BIN} admin user create --username '${GITEA_USERNAME}' --password '${GITEA_PASSWORD}' --email '${GITEA_ADMIN_EMAIL}' --admin --must-change-password=false"; then
            echo "[gitea-init] Admin user created."
            break
        fi
        echo "[gitea-init]  User creation attempt ${i} failed, retrying..."
        sleep 2
    done
    if ! curl -sf --connect-timeout 5 --max-time 10 -u "${GITEA_USERNAME}:${GITEA_PASSWORD}" "${GITEA_URL}/api/v1/user" > /dev/null 2>&1; then
        echo "[gitea-init] ERROR: Failed to create admin user '${GITEA_USERNAME}'" >&2
        exit 1
    fi
fi

echo "[gitea-init] Creating repository '${GITEA_REPO_NAME}'..."
if [ "${GITEA_REPO_AUTO_INIT}" = "true" ]; then
    BODY="{\"name\":\"${GITEA_REPO_NAME}\",\"private\":false,\"auto_init\":true,\"default_branch\":\"${GITEA_DEFAULT_BRANCH}\"}"
else
    BODY="{\"name\":\"${GITEA_REPO_NAME}\",\"private\":false,\"auto_init\":false,\"default_branch\":\"${GITEA_DEFAULT_BRANCH}\"}"
fi
HTTP_CODE=$(curl -s -o /tmp/gitea-repo-resp.json -w '%{http_code}' -X POST \
    "${GITEA_URL}/api/v1/user/repos" \
    -H "Content-Type: application/json" \
    -u "${GITEA_USERNAME}:${GITEA_PASSWORD}" \
    -d "${BODY}") || true
echo "[gitea-init] Repo creation HTTP status: ${HTTP_CODE}"
if [ "${HTTP_CODE}" != "200" ] && [ "${HTTP_CODE}" != "201" ] && [ "${HTTP_CODE}" != "409" ]; then
    cat /tmp/gitea-repo-resp.json 2> /dev/null || true
    echo "[gitea-init] ERROR: Failed to create repository '${GITEA_REPO_NAME}' (HTTP ${HTTP_CODE})" >&2
    exit 1
fi
rm -f /tmp/gitea-repo-resp.json

echo "GITEA_INIT_DONE"
echo "[gitea-init] Setup complete."
