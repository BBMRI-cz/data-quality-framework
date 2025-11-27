#!/bin/bash

# OIDC Helper Script
# This script provides helper functions for OIDC authentication flow

# Configuration
OIDC_SERVER_URL="http://localhost:4011"
CLIENT_ID="auth-code-client"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to get OIDC token using Resource Owner Password Credentials flow
# This is a simplified approach for testing purposes
# Parameters:
#   $1 - username
#   $2 - password
get_oidc_token() {
  local username=$1
  local password=$2

  echo -e "${YELLOW}Attempting to get OIDC token for user: ${username}${NC}" >&2

  # Get the token endpoint from the discovery document
  local discovery_url="${OIDC_SERVER_URL}/.well-known/openid-configuration"
  local token_endpoint=$(curl -s "$discovery_url" | jq -r '.token_endpoint')

  if [ -z "$token_endpoint" ] || [ "$token_endpoint" == "null" ]; then
    echo -e "${RED}✗ Failed to get token endpoint from discovery document${NC}" >&2
    return 1
  fi

  echo -e "${GREEN}✓ Token endpoint: ${token_endpoint}${NC}" >&2

  # Try to get token using password grant (if supported)
  # Note: OIDC server mock might need specific configuration for this
  local token_response=$(curl -s -X POST "$token_endpoint" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password&client_id=${CLIENT_ID}&username=${username}&password=${password}&scope=openid profile email")

  local access_token=$(echo "$token_response" | jq -r '.access_token')

  if [ -z "$access_token" ] || [ "$access_token" == "null" ]; then
    echo -e "${RED}✗ Failed to obtain OIDC access token${NC}" >&2
    echo "Response: $token_response" >&2
    return 1
  fi

  echo "$access_token"
  return 0
}

# Function to get OIDC ID token (for authentication with the server)
# Parameters:
#   $1 - username
#   $2 - password
get_oidc_id_token() {
  local username=$1
  local password=$2

  echo -e "${YELLOW}Attempting to get OIDC ID token for user: ${username}${NC}" >&2

  # Get the token endpoint from the discovery document
  local discovery_url="${OIDC_SERVER_URL}/.well-known/openid-configuration"
  local token_endpoint=$(curl -s "$discovery_url" | jq -r '.token_endpoint')

  if [ -z "$token_endpoint" ] || [ "$token_endpoint" == "null" ]; then
    echo -e "${RED}✗ Failed to get token endpoint from discovery document${NC}" >&2
    return 1
  fi

  # Try to get token using password grant
  local token_response=$(curl -s -X POST "$token_endpoint" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password&client_id=${CLIENT_ID}&username=${username}&password=${password}&scope=openid profile email")

  local id_token=$(echo "$token_response" | jq -r '.id_token')

  if [ -z "$id_token" ] || [ "$id_token" == "null" ]; then
    echo -e "${RED}✗ Failed to obtain OIDC ID token${NC}" >&2
    echo "Response: $token_response" >&2
    return 1
  fi

  echo "$id_token"
  return 0
}

# Function to verify OIDC server is running
check_oidc_server() {
  echo -e "${YELLOW}Checking OIDC server health...${NC}" >&2

  local discovery_url="${OIDC_SERVER_URL}/.well-known/openid-configuration"
  local response=$(curl -s -w "%{http_code}" -o /dev/null "$discovery_url")

  if [ "$response" != "200" ]; then
    echo -e "${RED}✗ OIDC server is not responding (HTTP ${response})${NC}" >&2
    return 1
  fi

  echo -e "${GREEN}✓ OIDC server is running${NC}" >&2
  return 0
}

# Export functions if script is sourced
if [ "${BASH_SOURCE[0]}" != "${0}" ]; then
  export -f get_oidc_token
  export -f get_oidc_id_token
  export -f check_oidc_server
fi

