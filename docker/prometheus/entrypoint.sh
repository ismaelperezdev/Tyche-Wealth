#!/bin/sh
set -eu

PROMETHEUS_PASSWORD="$(sed -n 's/^PROMETHEUS_PASSWORD=//p' /run/config/application-local.properties | tail -n 1 | tr -d '\r')"

if [ -z "$PROMETHEUS_PASSWORD" ]; then
  echo "PROMETHEUS_PASSWORD not found in application-local.properties" >&2
  exit 1
fi

awk -v password="$PROMETHEUS_PASSWORD" '{gsub(/__PROMETHEUS_PASSWORD__/, password)}1' \
  /etc/prometheus/prometheus.yml.template > /tmp/prometheus.yml

exec /bin/prometheus --config.file=/tmp/prometheus.yml --storage.tsdb.path=/prometheus
