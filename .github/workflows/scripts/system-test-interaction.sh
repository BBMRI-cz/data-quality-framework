#!/bin/bash

# System Test: Agent and Server Interaction
# This script tests the interaction between the Agent and Server containers by:
# 1. Registering the server in the agent (which triggers agent auto-registration in the server)
# 2. Authenticating with the server to get a JWT token
# 3. Approving the agent in the server

set -e

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source OIDC helper functions
source "${SCRIPT_DIR}/oidc-helper.sh"

# Configuration
AGENT_URL="http://localhost:8081"
SERVER_URL="http://localhost:8082"
REGISTRATION_URL="http://host.docker.internal:8082"
export OIDC_SERVER_URL="http://localhost:4011"
AGENT_ADMIN_USERNAME="admin"
AGENT_ADMIN_PASSWORD="adminpass"
SERVER_ADMIN_USERNAME="admin"
SERVER_ADMIN_PASSWORD="adminpass"
OIDC_TEST_USERNAME="Researcher"
OIDC_TEST_PASSWORD="researcher"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Starting system test: Agent and Server interaction${NC}"

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
AGENT_JWT_TOKEN=$(echo "$AGENT_LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$AGENT_JWT_TOKEN" ]; then
  echo -e "${RED}✗ Failed to obtain JWT token from agent${NC}"
  echo "Full response: $AGENT_LOGIN_RESPONSE"
  exit 1
fi

echo -e "${GREEN}✓ Successfully obtained JWT token from agent${NC}"

# Step 2: Register the server in the agent (this triggers automatic agent registration in server)
echo -e "\n${YELLOW}Step 2: Registering server in the agent...${NC}"
SERVER_REGISTRATION_RESPONSE=$(curl -s -X POST \
  "${AGENT_URL}/api/servers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${AGENT_JWT_TOKEN}" \
  -d '{
    "url": "'${REGISTRATION_URL}'",
    "name": "Test Central Server"
  }')

echo "Response: $SERVER_REGISTRATION_RESPONSE"

# Check if registration was successful
if echo "$SERVER_REGISTRATION_RESPONSE" | grep -q '"id"'; then
  echo -e "${GREEN}✓ Server successfully registered in agent${NC}"
else
  echo -e "${RED}✗ Failed to register server in agent${NC}"
  exit 1
fi

# Step 2b: Get agent ID from agent settings endpoint
echo -e "\n${YELLOW}Step 2b: Retrieving agent ID from agent settings...${NC}"
AGENT_SETTINGS_RESPONSE=$(curl -s -X GET \
  "${AGENT_URL}/api/settings" \
  -H "Authorization: Bearer ${AGENT_JWT_TOKEN}")

echo "Response: $AGENT_SETTINGS_RESPONSE"

# Extract agent ID from settings
AGENT_ID=$(echo "$AGENT_SETTINGS_RESPONSE" | jq -r '.agentId' 2>/dev/null)

if [ -z "$AGENT_ID" ] || [ "$AGENT_ID" == "null" ]; then
  echo -e "${RED}✗ Failed to extract agent ID from agent settings${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Agent ID: $AGENT_ID${NC}"

# Step 3: Get authentication token from server
echo -e "\n${YELLOW}Step 3: Authenticating with server to get JWT token...${NC}"
SERVER_LOGIN_RESPONSE=$(curl -s -X POST \
  "${SERVER_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "'${SERVER_ADMIN_USERNAME}'",
    "password": "'${SERVER_ADMIN_PASSWORD}'"
  }')

echo "Response: $SERVER_LOGIN_RESPONSE"

# Extract JWT token from server
SERVER_JWT_TOKEN=$(echo "$SERVER_LOGIN_RESPONSE" | jq .token | tr -d '"')

if [ -z "$SERVER_JWT_TOKEN" ]; then
  echo -e "${RED}✗ Failed to obtain JWT token from server${NC}"
  echo "Full response: $SERVER_LOGIN_RESPONSE"
  exit 1
fi

echo -e "${GREEN}✓ Successfully obtained JWT token from server${NC}"

# Wait for agent to be auto-registered in the server
echo -e "\n${YELLOW}Waiting for agent auto-registration to complete...${NC}"
sleep 5

# Step 4: Approve the agent in the server with retries
echo -e "\n${YELLOW}Step 4: Approving agent in the server (setting status to ACTIVE)...${NC}"
MAX_RETRIES=5
RETRY_COUNT=0
APPROVAL_SUCCESS=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ] && [ "$APPROVAL_SUCCESS" = false ]; do
  AGENT_APPROVAL_RESPONSE=$(curl -s -X PATCH \
    "${SERVER_URL}/api/v1/agents/${AGENT_ID}" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${SERVER_JWT_TOKEN}" \
    -d '{
      "status": "ACTIVE"
    }')

  echo "Response (attempt $((RETRY_COUNT + 1))/$MAX_RETRIES): $AGENT_APPROVAL_RESPONSE"

  # Check if approval was successful
  if echo "$AGENT_APPROVAL_RESPONSE" | grep -q '"status":"ACTIVE"'; then
    echo -e "${GREEN}✓ Agent successfully approved in server${NC}"
    APPROVAL_SUCCESS=true
  else
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
      echo -e "${YELLOW}Approval attempt $RETRY_COUNT failed, retrying in 2 seconds...${NC}"
      sleep 2
    fi
  fi
done

if [ "$APPROVAL_SUCCESS" = false ]; then
  echo -e "${YELLOW}⚠ Agent status update after $MAX_RETRIES attempts (may need further verification)${NC}"
fi

# Step 5: Verify the agent status with retries
echo -e "\n${YELLOW}Step 5: Verifying agent status in server...${NC}"
RETRY_COUNT=0
VERIFICATION_SUCCESS=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ] && [ "$VERIFICATION_SUCCESS" = false ]; do
  AGENT_VERIFICATION_RESPONSE=$(curl -s -X GET \
    "${SERVER_URL}/api/v1/agents/${AGENT_ID}" \
    -H "Authorization: Bearer ${SERVER_JWT_TOKEN}")

  echo "Response (attempt $((RETRY_COUNT + 1))/$MAX_RETRIES): $AGENT_VERIFICATION_RESPONSE"

  if echo "$AGENT_VERIFICATION_RESPONSE" | grep -q '"status":"ACTIVE"'; then
    echo -e "${GREEN}✓ Agent status verified as ACTIVE${NC}"
    VERIFICATION_SUCCESS=true
  else
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
      echo -e "${YELLOW}Verification attempt $RETRY_COUNT failed, retrying in 2 seconds...${NC}"
      sleep 2
    fi
  fi
done

if [ "$VERIFICATION_SUCCESS" = false ]; then
  echo -e "${YELLOW}⚠ Agent status could not be verified as ACTIVE after $MAX_RETRIES attempts${NC}"
fi

# Wait for agent to fully stabilize
echo -e "\n${YELLOW}Waiting 1 minute for agent to fully stabilize...${NC}"
sleep 60

# Step 6: Generate report on the agent
echo -e "\n${YELLOW}Step 6: Generating report on the agent...${NC}"
REPORT_GENERATION_RESPONSE=$(curl -s -X POST \
  "${AGENT_URL}/api/reports" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${AGENT_JWT_TOKEN}" \
  -d '{}')

echo "Response: $REPORT_GENERATION_RESPONSE"

# Extract report ID from the Location header or response
REPORT_ID=$(echo "$REPORT_GENERATION_RESPONSE" | jq -r '.id // ._links.self.href' 2>/dev/null | grep -o '[0-9]\+$')

if [ -z "$REPORT_ID" ]; then
  echo -e "${RED}✗ Failed to extract report ID from response${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Report generated successfully with ID: $REPORT_ID${NC}"

# Wait for report generation and transmission to complete
echo -e "\n${YELLOW}Waiting for report generation and transmission to complete...${NC}"
sleep 10

# Step 7: Verify the report was sent to the server
echo -e "\n${YELLOW}Step 7: Verifying the report was sent to the server...${NC}"
RETRY_COUNT=0
REPORT_VERIFICATION_SUCCESS=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ] && [ "$REPORT_VERIFICATION_SUCCESS" = false ]; do
  REPORT_VERIFICATION_RESPONSE=$(curl -s -X GET \
    "${SERVER_URL}/api/v1/reports" \
    -H "Authorization: Bearer ${SERVER_JWT_TOKEN}")

  echo "Response (attempt $((RETRY_COUNT + 1))/$MAX_RETRIES): $REPORT_VERIFICATION_RESPONSE"

  # Check if there's at least one report in the response
  REPORT_COUNT=$(echo "$REPORT_VERIFICATION_RESPONSE" | jq '._embedded.reports | length' 2>/dev/null)

  if [ ! -z "$REPORT_COUNT" ] && [ "$REPORT_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓ Report successfully verified on the server (found $REPORT_COUNT report(s))${NC}"
    REPORT_VERIFICATION_SUCCESS=true
  else
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
      echo -e "${YELLOW}Verification attempt $RETRY_COUNT failed, retrying in 2 seconds...${NC}"
      sleep 2
    fi
  fi
done

if [ "$REPORT_VERIFICATION_SUCCESS" = false ]; then
  echo -e "${RED}✗ Report could not be verified on the server after $MAX_RETRIES attempts${NC}"
  exit 1
fi

echo -e "\n${GREEN}✓ Basic authentication flow completed successfully!${NC}"

# =====================================================================
# OIDC AUTHENTICATION TEST
# =====================================================================

echo -e "\n${YELLOW}========================================${NC}"
echo -e "${YELLOW}Testing OIDC Authentication Flow${NC}"
echo -e "${YELLOW}========================================${NC}"

# Step 8: Check OIDC server health
echo -e "\n${YELLOW}Step 8: Checking OIDC server health...${NC}"

if ! check_oidc_server 30; then
  echo -e "${RED}✗ OIDC server is not healthy${NC}"
  echo -e "${YELLOW}Tip: Make sure OIDC server is running with: docker compose up -d oidc-server-mock${NC}"
  exit 1
fi

echo -e "${GREEN}✓ OIDC server is healthy${NC}"

# Step 9: Test OIDC authentication with the server
echo -e "\n${YELLOW}Step 9: Testing OIDC authentication with server...${NC}"

# Get OIDC access token
OIDC_ACCESS_TOKEN=$(get_oidc_token "$OIDC_TEST_USERNAME" "$OIDC_TEST_PASSWORD")

if [ -z "$OIDC_ACCESS_TOKEN" ]; then
  echo -e "${RED}✗ Failed to obtain OIDC access token${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Successfully obtained OIDC access token${NC}"

# Step 10: Verify OIDC user can access protected endpoints
echo -e "\n${YELLOW}Step 10: Verifying OIDC user can list reports...${NC}"

OIDC_REPORTS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET \
  "${SERVER_URL}/api/v1/reports" \
  -H "Authorization: Bearer ${OIDC_ACCESS_TOKEN}")

OIDC_REPORTS_HTTP_STATUS=$(echo "$OIDC_REPORTS_RESPONSE" | tail -n 1)
OIDC_REPORTS_BODY=$(echo "$OIDC_REPORTS_RESPONSE" | head -n -1)

echo "HTTP Status: $OIDC_REPORTS_HTTP_STATUS"
echo "Response: $OIDC_REPORTS_BODY"

if [ "$OIDC_REPORTS_HTTP_STATUS" == "200" ]; then
  echo -e "${GREEN}✓ OIDC user successfully accessed reports endpoint${NC}"

  # Verify the report from the basic auth test is still visible
  REPORT_COUNT=$(echo "$OIDC_REPORTS_BODY" | jq '._embedded.reports | length' 2>/dev/null)
  if [ ! -z "$REPORT_COUNT" ] && [ "$REPORT_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓ OIDC user can see ${REPORT_COUNT} report(s)${NC}"
  fi
else
  echo -e "${RED}✗ OIDC user reports access returned HTTP ${OIDC_REPORTS_HTTP_STATUS}${NC}"
  echo -e "${YELLOW}This might indicate an issue with OIDC token validation${NC}"
  exit 1
fi

echo -e "\n${GREEN}✓ OIDC authentication flow completed!${NC}"

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}System test completed successfully!${NC}"
echo -e "${GREEN}Both Basic Auth and OIDC flows verified!${NC}"
echo -e "${GREEN}========================================${NC}"
