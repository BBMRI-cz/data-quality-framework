#!/bin/bash -e

# Proxy Test: verify the agent runs correctly when its outbound HTTP traffic is
# routed through an HTTP proxy (tinyproxy).
#
# Uses the documented proxy mechanism (JAVA_TOOL_OPTIONS with JVM system proxy
# properties, see docs/user/configuration.md). The agent pushes OTLP metrics and
# this script asserts that:
#   1. the metrics actually reach the collector (outbound works), and
#   2. tinyproxy logged the request (proving traffic traversed the proxy).

COLLECTOR_CONTAINER="${COLLECTOR_CONTAINER:-otel-collector}"
TINYPROXY_CONTAINER="${TINYPROXY_CONTAINER:-tinyproxy}"
# Marker emitted by the collector debug exporter when metrics are received.
OTEL_LOG_PATTERN="${OTEL_LOG_PATTERN:-ResourceMetrics}"
# tinyproxy logs the target host of each proxied request.
PROXY_LOG_PATTERN="${PROXY_LOG_PATTERN:-otel-collector}"
AGENT_URL="${AGENT_URL:-http://localhost:8081/api}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-120}"
SLEEP_SECONDS="${SLEEP_SECONDS:-3}"

START_EPOCH="$(date +"%s")"

elapsed() {
  EPOCH="$(date +"%s")"
  echo $((EPOCH - START_EPOCH))
}

# 1. Wait for the agent to come up.
echo "Waiting for agent health..."
until curl -fsS "${AGENT_URL}/health" >/dev/null 2>&1; do
  if [ "$(elapsed)" -ge "${MAX_WAIT_SECONDS}" ]; then
    echo "Agent did not become healthy."
    echo "Agent logs:"
    docker logs quality-agent 2>&1 || true
    exit 1
  fi
  sleep "${SLEEP_SECONDS}"
done
echo "Agent is healthy."

# 2. Trigger a few requests so application metrics are generated.
for i in {1..5}; do
  curl -fsS "${AGENT_URL}/health" >/dev/null
  sleep 1
done

# 3. Wait for the OTLP metrics to reach the collector.
echo "Waiting for OTLP metrics push to reach the collector..."
METRICS_OK=0
while [ "$(elapsed)" -lt "${MAX_WAIT_SECONDS}" ]; do
  if docker logs "${COLLECTOR_CONTAINER}" 2>&1 | grep -q "${OTEL_LOG_PATTERN}"; then
    echo "OTEL metrics push detected in collector logs."
    METRICS_OK=1
    break
  fi
  sleep "${SLEEP_SECONDS}"
done

if [ "${METRICS_OK}" -ne 1 ]; then
  echo "Timed out waiting for OTEL metrics push (${MAX_WAIT_SECONDS}s)."
  echo "Collector logs:"
  docker logs "${COLLECTOR_CONTAINER}" 2>&1
  exit 1
fi

# 4. Verify the traffic actually traversed the proxy by inspecting tinyproxy logs.
echo "Checking tinyproxy logs for proxied requests..."
if docker logs "${TINYPROXY_CONTAINER}" 2>&1 | grep -q "${PROXY_LOG_PATTERN}"; then
  echo "Proxied request to '${PROXY_LOG_PATTERN}' found in tinyproxy logs."
else
  echo "No proxied request to '${PROXY_LOG_PATTERN}' found in tinyproxy logs."
  echo "tinyproxy logs:"
  docker logs "${TINYPROXY_CONTAINER}" 2>&1
  exit 1
fi

echo "Proxy test passed: agent traffic is routed through the HTTP proxy."
exit 0
