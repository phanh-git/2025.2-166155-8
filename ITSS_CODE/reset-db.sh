#!/usr/bin/env bash
set -euo pipefail

DB_NAME="${DB_NAME:-importorder_db}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
SCHEMA_FILE="${SCHEMA_FILE:-src/main/resources/database/schema.sql}"

if [[ ! -f "$SCHEMA_FILE" ]]; then
  echo "Khong tim thay file schema: $SCHEMA_FILE" >&2
  exit 1
fi

export PGPASSWORD="$DB_PASSWORD"

echo "1) Drop database neu da ton tai: $DB_NAME"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 <<SQL
DROP DATABASE IF EXISTS $DB_NAME WITH (FORCE);
SQL

echo "2) Tao lai database: $DB_NAME"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 <<SQL
CREATE DATABASE $DB_NAME OWNER $DB_USER;
SQL

echo "3) Nap lai schema va seed data tu: $SCHEMA_FILE"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -f "$SCHEMA_FILE"

echo "OK: da reset xong database $DB_NAME"
