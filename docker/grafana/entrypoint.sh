#!/bin/sh
set -eu

GRAFANA_ADMIN_USER="$(sed -n 's/^GRAFANA_ADMIN_USER=//p' /run/config/application-local.properties | tail -n 1 | tr -d '\r')"
GRAFANA_ADMIN_PASSWORD="$(sed -n 's/^GRAFANA_ADMIN_PASSWORD=//p' /run/config/application-local.properties | tail -n 1 | tr -d '\r')"

if [ -z "$GRAFANA_ADMIN_USER" ] || [ -z "$GRAFANA_ADMIN_PASSWORD" ]; then
  echo "GRAFANA_ADMIN_USER or GRAFANA_ADMIN_PASSWORD not found in application-local.properties" >&2
  exit 1
fi

export GF_SECURITY_ADMIN_USER="$GRAFANA_ADMIN_USER"
export GF_SECURITY_ADMIN_PASSWORD="$GRAFANA_ADMIN_PASSWORD"

exec /run.sh
