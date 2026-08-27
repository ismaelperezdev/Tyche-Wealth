#!/bin/sh
set -eu

CONFIG_FILE="/run/config/application-local.properties"

extract_db_name() {
  key="$1"
  value="$(sed -n "s/^${key}=//p" "$CONFIG_FILE" | tail -n 1 | tr -d '\r')"
  if [ -z "$value" ]; then
    value="$(printenv "$key" 2>/dev/null || true)"
  fi
  printf '%s' "$value" | sed -E 's#^.*/([^/?]+)(\?.*)?$#\1#'
}

create_db_if_missing() {
  db_name="$1"

  if [ -z "$db_name" ]; then
    return 0
  fi

  exists="$(
    psql --username "$POSTGRES_USER" --dbname postgres -tAc \
      "SELECT 1 FROM pg_database WHERE datname = '${db_name}'"
  )"

  if [ "$exists" != "1" ]; then
    createdb --username "$POSTGRES_USER" --owner "$POSTGRES_USER" "$db_name"
  fi
}

create_db_if_missing "$(extract_db_name DB_URL_USER)"
create_db_if_missing "$(extract_db_name DB_URL_PORTFOLIO)"
create_db_if_missing "$(extract_db_name DB_URL_MARKET_DATA)"
