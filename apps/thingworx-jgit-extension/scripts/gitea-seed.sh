#!/bin/sh
# Seed the local Gitea test repository from the ThingWorx example export.
# Safe to rerun: a commit is pushed only when the export contents change.

set -eu

GITEA_URL="${GITEA_URL:-http://gitea:3000}"
GITEA_USERNAME="${GITEA_USERNAME:?GITEA_USERNAME must be set}"
GITEA_PASSWORD="${GITEA_PASSWORD:?GITEA_PASSWORD must be set}"
GITEA_REPO_NAME="${GITEA_REPO_NAME:?GITEA_REPO_NAME must be set}"
GITEA_DEFAULT_BRANCH="${GITEA_DEFAULT_BRANCH:-main}"
GITEA_SEED_PATH="${GITEA_SEED_PATH:-examples/thingworx-jgit-extension-export}"
GITEA_COMMIT_NAME="${GITEA_COMMIT_NAME:-Gitea Test User}"
GITEA_COMMIT_EMAIL="${GITEA_COMMIT_EMAIL:-test@example.com}"

case "$GITEA_SEED_PATH" in
    /*|*..*)
        echo "[gitea-seed] ERROR: GITEA_SEED_PATH must be a safe repository-relative path." >&2
        exit 1
        ;;
esac

export GITEA_USERNAME GITEA_PASSWORD
export GIT_ASKPASS=/scripts/gitea-askpass.sh
export GIT_TERMINAL_PROMPT=0

work_dir="$(mktemp -d /tmp/gitea-seed.XXXXXX)"
trap 'rm -rf "$work_dir"' EXIT HUP INT TERM

repository_url="${GITEA_URL}/${GITEA_USERNAME}/${GITEA_REPO_NAME}.git"
echo "[gitea-seed] Cloning ${GITEA_USERNAME}/${GITEA_REPO_NAME}..."
git clone --branch "$GITEA_DEFAULT_BRANCH" "$repository_url" "$work_dir/repo"

# The export folder is the complete working-tree content for this test repo.
find "$work_dir/repo" -mindepth 1 -maxdepth 1 ! -name .git -exec rm -rf {} +
mkdir -p "$work_dir/repo/$GITEA_SEED_PATH"
cp -a /seed/. "$work_dir/repo/$GITEA_SEED_PATH/"

git -C "$work_dir/repo" config user.name "$GITEA_COMMIT_NAME"
git -C "$work_dir/repo" config user.email "$GITEA_COMMIT_EMAIL"
git -C "$work_dir/repo" add --all

if git -C "$work_dir/repo" diff --cached --quiet; then
    echo "[gitea-seed] Repository already matches the example export."
    exit 0
fi

git -C "$work_dir/repo" commit -m "Seed ThingWorx example export"
git -C "$work_dir/repo" push origin "$GITEA_DEFAULT_BRANCH"
echo "[gitea-seed] Test repository seeded."
