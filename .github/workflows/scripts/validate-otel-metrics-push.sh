#!/bin/bash -e

COLLECTOR_CONTAINER="${COLLECTOR_CONTAINER:-otel-collector}"
# Default marker emitted by collector debug exporter when metrics are received.
OTEL_LOG_PATTERN="${OTEL_LOG_PATTERN:-ResourceMetrics}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-120}"
SLEEP_SECONDS="${SLEEP_SECONDS:-3}"

START_EPOCH="$(date +"%s")"

elapsed() {
  EPOCH="$(date +"%s")"
  echo $((EPOCH - START_EPOCH))
}

echo "Waiting for OTEL metrics in collector logs..."
while [ "$(elapsed)" -lt "${MAX_WAIT_SECONDS}" ]; do
  if docker logs "${COLLECTOR_CONTAINER}" 2>&1 | grep -q "${OTEL_LOG_PATTERN}"; then
    echo "OTEL metrics push detected."
    exit 0
  fi
  sleep "${SLEEP_SECONDS}"
done

echo "Timed out waiting for OTEL metrics push (${MAX_WAIT_SECONDS}s)."
echo "Collector logs:"
docker logs "${COLLECTOR_CONTAINER}" 2>&1
exit 1
