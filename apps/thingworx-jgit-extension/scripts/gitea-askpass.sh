#!/bin/sh
case "$1" in
    *sername*) printf '%s\n' "$GITEA_USERNAME" ;;
    *) printf '%s\n' "$GITEA_PASSWORD" ;;
esac
