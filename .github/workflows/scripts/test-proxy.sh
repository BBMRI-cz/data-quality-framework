#!/bin/bash -e

# Proxy Test: verify the agent runs correctly when its outbound HTTP traffic is
# routed through an HTTP proxy (tinyproxy).
#
# Uses the documented proxy mechanism (JAVA_TOOL_OPTIONS with JVM system proxy
# properties, see docs/user/configuration.md). It asserts that outbound traffic
# actually traverses the proxy for two independent agents' HTTP stacks:
#   1. OTLP metrics push (Micrometer HttpURLConnection sender), and
#   2. FHIR (Blaze) traffic (Apache HttpClient used by the FHIR store).
# Both are verified against the tinyproxy access log.

COLLECTOR_CONTAINER="${COLLECTOR_CONTAINER:-otel-collector}"
TINYPROXY_CONTAINER="${TINYPROXY_CONTAINER:-tinyproxy}"
# Marker emitted by the collector debug exporter when metrics are received.
OTEL_LOG_PATTERN="${OTEL_LOG_PATTERN:-ResourceMetrics}"
# tinyproxy logs the target host of each proxied request.
OTEL_PROXY_PATTERN="${OTEL_PROXY_PATTERN:-otel-collector}"
FHIR_PROXY_PATTERN="${FHIR_PROXY_PATTERN:-blaze}"
AGENT_BASE="${AGENT_BASE:-http://localhost:8081}"
AGENT_URL="${AGENT_BASE}/api"
AGENT_ADMIN_USERNAME="${AGENT_ADMIN_USERNAME:-admin}"
AGENT_ADMIN_PASSWORD="${AGENT_ADMIN_PASSWORD:-adminpass}"
FHIR_URL="${FHIR_URL:-http://blaze:8080/fhir}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-120}"
SLEEP_SECONDS="${SLEEP_SECONDS:-3}"

START_EPOCH="$(date +"%s")"

elapsed() {
  EPOCH="$(date +"%s")"
  echo $((EPOCH - START_EPOCH))
}

proxy_log_contains() {
  docker logs "${TINYPROXY_CONTAINER}" 2>&1 | grep -q "$1"
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

# 4. Verify the OTLP traffic traversed the proxy.
echo "Checking tinyproxy logs for proxied OTLP request..."
if proxy_log_contains "${OTEL_PROXY_PATTERN}"; then
  echo "Proxied request to '${OTEL_PROXY_PATTERN}' found in tinyproxy logs."
else
  echo "No proxied request to '${OTEL_PROXY_PATTERN}' found in tinyproxy logs."
  echo "tinyproxy logs:"
  docker logs "${TINYPROXY_CONTAINER}" 2>&1
  exit 1
fi

# 5. Verify FHIR (Blaze) traffic traverses the proxy.
#    Configure the agent to use the FHIR data store, then trigger a FHIR health
#    check that makes an outbound call through the proxy.
echo "Configuring agent to use FHIR data store at ${FHIR_URL}..."
AGENT_LOGIN_RESPONSE=$(curl -s -X POST \
  "${AGENT_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${AGENT_ADMIN_USERNAME}\",\"password\":\"${AGENT_ADMIN_PASSWORD}\"}")

AGENT_JWT_TOKEN=$(echo "$AGENT_LOGIN_RESPONSE" | jq -r '.token // empty')

if [ -z "${AGENT_JWT_TOKEN}" ]; then
  echo "Failed to obtain agent JWT token. Login response: ${AGENT_LOGIN_RESPONSE}"
  exit 1
fi

SETTINGS_RESPONSE=$(curl -s -X PUT \
  "${AGENT_URL}/settings" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${AGENT_JWT_TOKEN}" \
  -d "{\"databaseType\":\"FHIR\",\"fhirUrl\":\"${FHIR_URL}\",\"fhirUsername\":\"\",\"fhirPassword\":\"\"}")

echo "Settings response: ${SETTINGS_RESPONSE}"

# Allow the FHIR clients to reinitialize.
sleep 5

echo "Triggering FHIR health check through the proxy..."
FHIR_HEALTH_RESPONSE=$(curl -s \
  "${AGENT_URL}/entities/health" \
  -H "Authorization: Bearer ${AGENT_JWT_TOKEN}")

echo "FHIR health response: ${FHIR_HEALTH_RESPONSE}"

# The FHIR server is directly reachable on the compose network, so a successful
# health check alone does not prove the proxy was used; the tinyproxy log does.
echo "Checking tinyproxy logs for proxied FHIR (Blaze) request..."
if proxy_log_contains "${FHIR_PROXY_PATTERN}"; then
  echo "Proxied request to '${FHIR_PROXY_PATTERN}' found in tinyproxy logs."
else
  echo "No proxied request to '${FHIR_PROXY_PATTERN}' found in tinyproxy logs."
  echo "tinyproxy logs:"
  docker logs "${TINYPROXY_CONTAINER}" 2>&1
  exit 1
fi

echo "Proxy test passed: agent OTLP and FHIR traffic is routed through the HTTP proxy."
exit 0
