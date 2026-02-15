#!/bin/bash

# Test: Generate Report on Agent
# This script tests report generation by:
# 1. Authenticating with the agent to get a JWT token
# 2. Triggering report generation
# 3. Polling until the report status is GENERATED

set -e

# Configuration
AGENT_URL="${AGENT_URL:-http://localhost:8081}"
AGENT_ADMIN_USERNAME="${AGENT_ADMIN_USERNAME:-admin}"
AGENT_ADMIN_PASSWORD="${AGENT_ADMIN_PASSWORD:-adminpass}"
MAX_POLL_ATTEMPTS="${MAX_POLL_ATTEMPTS:-30}"
POLL_INTERVAL="${POLL_INTERVAL:-5}"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Starting report generation test${NC}"

# Step 1: Get authentication token from agent
echo -e "\n${YELLOW}Step 1: Authenticating with agent to get JWT token...${NC}"
AGENT_LOGIN_RESPONSE=$(curl -s -X POST \
  "${AGENT_URL}/api/auth/login" \
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

# Step 2: Generate report on the agent
echo -e "\n${YELLOW}Step 2: Triggering report generation on the agent...${NC}"
REPORT_GENERATION_RESPONSE=$(curl -s -X POST \
  "${AGENT_URL}/api/reports" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${AGENT_JWT_TOKEN}" \
  -d '{}')

echo "Response: $REPORT_GENERATION_RESPONSE"

# Extract report ID from the response
REPORT_ID=$(echo "$REPORT_GENERATION_RESPONSE" | jq -r '.id // ._links.self.href' 2>/dev/null | grep -o '[0-9]\+$')

if [ -z "$REPORT_ID" ]; then
  echo -e "${RED}✗ Failed to extract report ID from response${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Report generation triggered with ID: $REPORT_ID${NC}"

# Step 3: Poll until the report status is GENERATED
echo -e "\n${YELLOW}Step 3: Polling for report completion...${NC}"
POLL_COUNT=0
REPORT_STATUS=""

while [ $POLL_COUNT -lt $MAX_POLL_ATTEMPTS ]; do
  POLL_COUNT=$((POLL_COUNT + 1))

  REPORT_STATUS_RESPONSE=$(curl -s -X GET \
    "${AGENT_URL}/api/reports/${REPORT_ID}" \
    -H "Authorization: Bearer ${AGENT_JWT_TOKEN}")

  REPORT_STATUS=$(echo "$REPORT_STATUS_RESPONSE" | jq -r '.status // empty')

  echo "Poll attempt ${POLL_COUNT}/${MAX_POLL_ATTEMPTS}: Status = ${REPORT_STATUS}"

  if [ "$REPORT_STATUS" == "GENERATED" ]; then
    echo -e "${GREEN}✓ Report generated successfully!${NC}"
    echo "Final report response: $REPORT_STATUS_RESPONSE"
    exit 0
  elif [ "$REPORT_STATUS" == "GENERATING" ]; then
    echo -e "${YELLOW}Report still generating, waiting ${POLL_INTERVAL} seconds...${NC}"
    sleep $POLL_INTERVAL
  else
    echo -e "${RED}✗ Unexpected report status: ${REPORT_STATUS}${NC}"
    echo "Full response: $REPORT_STATUS_RESPONSE"
    exit 1
  fi
done

echo -e "${RED}✗ Report generation did not complete within $((MAX_POLL_ATTEMPTS * POLL_INTERVAL)) seconds${NC}"
exit 1

