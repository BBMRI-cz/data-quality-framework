#!/bin/bash
set -e

# Configuration
AGENT_URL="${AGENT_URL:-http://localhost:8081}"
AGENT_ADMIN_USERNAME="${AGENT_ADMIN_USERNAME:-admin}"
AGENT_ADMIN_PASSWORD="${AGENT_ADMIN_PASSWORD:-adminpass}"

# API Endpoints
API_LOGIN="${AGENT_URL}/api/auth/login"
API_REPORTS="${AGENT_URL}/api/reports"
API_QUALITY_CHECKS="${AGENT_URL}/api/quality-checks"
API_SETTINGS="${AGENT_URL}/api/settings"
API_HEALTH="${AGENT_URL}/api/health"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Starting basic API calls test${NC}"

# Step 1: Get authentication token from agent
echo -e "\n${YELLOW}Step 1: Authenticating with agent to get JWT token...${NC}"
AGENT_LOGIN_RESPONSE=$(curl -s -X POST \
  "${API_LOGIN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "'${AGENT_ADMIN_USERNAME}'",
    "password": "'${AGENT_ADMIN_PASSWORD}'"
  }')

echo "Response: $AGENT_LOGIN_RESPONSE"

# Extract JWT token from agent
AGENT_JWT_TOKEN=$(echo "$AGENT_LOGIN_RESPONSE" | jq -r '.token // empty')

if [ -z "$AGENT_JWT_TOKEN" ]; then
  echo -e "${RED}✗ Failed to obtain JWT token from agent${NC}"
  echo "Full response: $AGENT_LOGIN_RESPONSE"
  exit 1
fi

echo -e "${GREEN}✓ Successfully obtained JWT token from agent${NC}"

# Step 2: Get reports
echo -e "\n${YELLOW}Step 2: Getting list of reports...${NC}"
REPORTS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET \
  "${API_REPORTS}" \
  -H "Authorization: Bearer ${AGENT_JWT_TOKEN}")

REPORTS_HTTP_STATUS=$(echo "$REPORTS_RESPONSE" | tail -n 1)
REPORTS_BODY=$(echo "$REPORTS_RESPONSE" | head -n -1)

echo "HTTP Status: $REPORTS_HTTP_STATUS"
echo "Response: $REPORTS_BODY"

if [ "$REPORTS_HTTP_STATUS" != "200" ]; then
  echo -e "${RED}✗ Failed to get reports (HTTP $REPORTS_HTTP_STATUS)${NC}"
  exit 1
fi

REPORT_COUNT=$(echo "$REPORTS_BODY" | jq '._embedded.reports | length' 2>/dev/null || echo "0")
echo -e "${GREEN}✓ Successfully retrieved reports (found $REPORT_COUNT report(s))${NC}"

# Step 3: Get quality checks
echo -e "\n${YELLOW}Step 3: Getting list of quality checks...${NC}"
CHECKS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET \
  "${API_QUALITY_CHECKS}" \
  -H "Authorization: Bearer ${AGENT_JWT_TOKEN}")

CHECKS_HTTP_STATUS=$(echo "$CHECKS_RESPONSE" | tail -n 1)
CHECKS_BODY=$(echo "$CHECKS_RESPONSE" | head -n -1)

echo "HTTP Status: $CHECKS_HTTP_STATUS"
echo "Response: $CHECKS_BODY"

if [ "$CHECKS_HTTP_STATUS" != "200" ]; then
  echo -e "${RED}✗ Failed to get quality checks (HTTP $CHECKS_HTTP_STATUS)${NC}"
  exit 1
fi

CHECK_COUNT=$(echo "$CHECKS_BODY" | jq '._embedded.qualityChecks | length' 2>/dev/null || echo "0")
echo -e "${GREEN}✓ Successfully retrieved quality checks (found $CHECK_COUNT check(s))${NC}"

# Step 4: Get settings
echo -e "\n${YELLOW}Step 4: Getting agent settings...${NC}"
SETTINGS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET \
  "${API_SETTINGS}" \
  -H "Authorization: Bearer ${AGENT_JWT_TOKEN}")

SETTINGS_HTTP_STATUS=$(echo "$SETTINGS_RESPONSE" | tail -n 1)
SETTINGS_BODY=$(echo "$SETTINGS_RESPONSE" | head -n -1)

echo "HTTP Status: $SETTINGS_HTTP_STATUS"
echo "Response: $SETTINGS_BODY"

if [ "$SETTINGS_HTTP_STATUS" != "200" ]; then
  echo -e "${RED}✗ Failed to get settings (HTTP $SETTINGS_HTTP_STATUS)${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Successfully retrieved agent settings${NC}"

# Step 5: Get health endpoint (no auth required)
echo -e "\n${YELLOW}Step 5: Checking health endpoint...${NC}"
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET \
  "${API_HEALTH}")

HEALTH_HTTP_STATUS=$(echo "$HEALTH_RESPONSE" | tail -n 1)
HEALTH_BODY=$(echo "$HEALTH_RESPONSE" | head -n -1)

echo "HTTP Status: $HEALTH_HTTP_STATUS"
echo "Response: $HEALTH_BODY"

if [ "$HEALTH_HTTP_STATUS" != "200" ]; then
  echo -e "${RED}✗ Health endpoint check failed (HTTP $HEALTH_HTTP_STATUS)${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Health endpoint is responding${NC}"

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}All basic API calls completed successfully!${NC}"
echo -e "${GREEN}========================================${NC}"

