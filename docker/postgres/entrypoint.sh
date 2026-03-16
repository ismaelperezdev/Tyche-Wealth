#!/bin/sh
set -eu

POSTGRES_PASSWORD="$(sed -n 's/^POSTGRES_PASSWORD=//p' /run/config/application-local.properties | tail -n 1 | tr -d '\r')"

if [ -z "$POSTGRES_PASSWORD" ]; then
  echo "POSTGRES_PASSWORD not found in application-local.properties" >&2
  exit 1
fi

export POSTGRES_PASSWORD
exec docker-entrypoint.sh postgres
