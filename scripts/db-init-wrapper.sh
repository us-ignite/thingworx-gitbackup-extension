#!/bin/bash
set -e

KEEP_ALIVE="${DB_INIT_KEEP_ALIVE:-false}"

finish() {
    echo "DB_INIT_DONE"
    if [ "${KEEP_ALIVE}" = "true" ]; then
        echo "[WRAPPER] Keeping container alive (DB_INIT_KEEP_ALIVE=true)..."
        sleep infinity
    fi
    exit "$1"
}

echo "[WRAPPER] Running db-setup.sh..."
if /usr/local/bin/db-setup.sh; then
    echo "[WRAPPER] db-setup.sh completed successfully."
    finish 0
fi

EXIT_CODE=$?
echo "[WRAPPER] db-setup.sh exited with code $EXIT_CODE. Checking if database was already initialized..."

if PGPASSWORD="${DATABASE_ADMIN_PASSWORD}" psql -h "${DATABASE_HOST}" -U "${DATABASE_ADMIN_USERNAME}" -d "${TWX_DATABASE_SCHEMA}" -c "SELECT 1" > /dev/null 2>&1; then
    echo "[WRAPPER] Database is already initialized and accessible. Exiting 0."
    finish 0
fi

echo "[WRAPPER] Database is not accessible and initialization failed. Exiting 1."
exit 1
