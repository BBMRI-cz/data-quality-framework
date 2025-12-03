#!/bin/bash

# OIDC Helper Script
# This script provides helper functions for OIDC authentication flow

# Configuration
OIDC_SERVER_URL="${OIDC_SERVER_URL:-http://localhost:4011}"
CLIENT_ID="${CLIENT_ID:-auth-code-client}"
MAX_RETRIES="${OIDC_MAX_RETRIES:-3}"
RETRY_DELAY="${OIDC_RETRY_DELAY:-2}"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Function to get OIDC tokens using Resource Owner Password Credentials flow
# Returns both access_token and id_token in a single request
# Usage: get_oidc_tokens <username> <password> [token_type]
#   token_type: "access" (default), "id", or "both"
get_oidc_tokens() {
  local username=$1
  local password=$2
  local token_type="${3:-access}"

  if [ -z "$username" ] || [ -z "$password" ]; then
    echo -e "${RED}✗ Username and password are required${NC}" >&2
    return 1
  fi

  local discovery_url="${OIDC_SERVER_URL}/.well-known/openid-configuration"
  local token_endpoint=""

  # Get token endpoint
  for attempt in $(seq 1 $MAX_RETRIES); do
    if [ $attempt -gt 1 ]; then
      echo -e "${YELLOW}Retry attempt $attempt/$MAX_RETRIES...${NC}" >&2
      sleep $RETRY_DELAY
    fi

    # Check if OIDC server is reachable
    if ! curl -sf --connect-timeout 5 --max-time 10 "$discovery_url" > /dev/null 2>&1; then
      echo -e "${YELLOW}⚠ OIDC server not reachable at ${OIDC_SERVER_URL} (attempt $attempt/$MAX_RETRIES)${NC}" >&2
      continue
    fi

    token_endpoint=$(curl -sf --connect-timeout 5 --max-time 10 "$discovery_url" | jq -r '.token_endpoint // empty')

    if [ -n "$token_endpoint" ]; then
      echo -e "${GREEN}✓ Discovery endpoint: ${discovery_url}${NC}" >&2
      echo -e "${GREEN}✓ Token endpoint: ${token_endpoint}${NC}" >&2
      break
    fi
  done

  if [ -z "$token_endpoint" ]; then
    echo -e "${RED}✗ Failed to get token endpoint after $MAX_RETRIES attempts${NC}" >&2
    echo -e "${BLUE}Debug: Discovery URL = ${discovery_url}${NC}" >&2
    return 1
  fi

  # Prepare token request
  local token_data="grant_type=password"
  token_data+="&client_id=${CLIENT_ID}"
  token_data+="&username=${username}"
  token_data+="&password=${password}"
  token_data+="&scope=openid profile email"

  echo -e "${YELLOW}Requesting OIDC tokens for user: ${username}${NC}" >&2

  # Request tokens
  local token_response=""
  local curl_exit_code=0

  for attempt in $(seq 1 $MAX_RETRIES); do
    if [ $attempt -gt 1 ]; then
      echo -e "${YELLOW}Retry attempt $attempt/$MAX_RETRIES...${NC}" >&2
      sleep $RETRY_DELAY
    fi

    token_response=$(curl -sf --connect-timeout 5 --max-time 10 -X POST "$token_endpoint" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "$token_data" 2>&1)

    curl_exit_code=$?

    if [ $curl_exit_code -eq 0 ] && [ -n "$token_response" ]; then
      # Check if response contains an access_token
      local test_token=$(echo "$token_response" | jq -r '.access_token // empty' 2>/dev/null)
      if [ -n "$test_token" ]; then
        break
      fi
    fi
  done

  if [ $curl_exit_code -ne 0 ]; then
    echo -e "${RED}✗ Failed to connect to token endpoint after $MAX_RETRIES attempts${NC}" >&2
    echo -e "${BLUE}Debug: curl exit code = $curl_exit_code${NC}" >&2
    return 1
  fi

  # Parse tokens based on requested type
  case "$token_type" in
    access)
      local access_token=$(echo "$token_response" | jq -r '.access_token // empty')
      if [ -z "$access_token" ]; then
        echo -e "${RED}✗ Failed to obtain access token${NC}" >&2
        local error=$(echo "$token_response" | jq -r '.error // "unknown"')
        local error_desc=$(echo "$token_response" | jq -r '.error_description // "No description"')
        echo -e "${RED}Error: ${error} - ${error_desc}${NC}" >&2
        return 1
      fi
      echo -e "${GREEN}✓ Successfully obtained access token${NC}" >&2
      echo "$access_token"
      ;;

    id)
      local id_token=$(echo "$token_response" | jq -r '.id_token // empty')
      if [ -z "$id_token" ]; then
        echo -e "${RED}✗ Failed to obtain ID token${NC}" >&2
        local error=$(echo "$token_response" | jq -r '.error // "unknown"')
        local error_desc=$(echo "$token_response" | jq -r '.error_description // "No description"')
        echo -e "${RED}Error: ${error} - ${error_desc}${NC}" >&2
        return 1
      fi
      echo -e "${GREEN}✓ Successfully obtained ID token${NC}" >&2
      echo "$id_token"
      ;;

    both)
      local access_token=$(echo "$token_response" | jq -r '.access_token // empty')
      local id_token=$(echo "$token_response" | jq -r '.id_token // empty')
      if [ -z "$access_token" ] || [ -z "$id_token" ]; then
        echo -e "${RED}✗ Failed to obtain tokens${NC}" >&2
        local error=$(echo "$token_response" | jq -r '.error // "unknown"')
        local error_desc=$(echo "$token_response" | jq -r '.error_description // "No description"')
        echo -e "${RED}Error: ${error} - ${error_desc}${NC}" >&2
        return 1
      fi
      echo -e "${GREEN}✓ Successfully obtained both tokens${NC}" >&2
      echo "${access_token}|${id_token}"
      ;;

    *)
      echo -e "${RED}✗ Invalid token type: $token_type${NC}" >&2
      return 1
      ;;
  esac

  return 0
}

# Convenience function: Get access token only
get_oidc_token() {
  get_oidc_tokens "$1" "$2" "access"
}

# Convenience function: Get ID token only
get_oidc_id_token() {
  get_oidc_tokens "$1" "$2" "id"
}

# Function to verify OIDC server is running and healthy
check_oidc_server() {
  local max_wait="${1:-30}"  # Default: wait up to 30 seconds
  local check_interval=2

  echo -e "${YELLOW}Checking OIDC server health at ${OIDC_SERVER_URL}...${NC}" >&2

  local discovery_url="${OIDC_SERVER_URL}/.well-known/openid-configuration"
  local elapsed=0

  while [ $elapsed -lt $max_wait ]; do
    local response=$(curl -sf --connect-timeout 2 --max-time 5 -w "%{http_code}" -o /dev/null "$discovery_url" 2>/dev/null)

    if [ "$response" = "200" ]; then
      echo -e "${GREEN}✓ OIDC server is healthy${NC}" >&2

      # Optionally display server info
      if [ "${OIDC_DEBUG:-false}" = "true" ]; then
        local issuer=$(curl -sf "$discovery_url" | jq -r '.issuer // "unknown"')
        echo -e "${BLUE}Debug: Issuer = ${issuer}${NC}" >&2
      fi

      return 0
    fi

    if [ $elapsed -eq 0 ]; then
      echo -e "${YELLOW}Waiting for OIDC server to become ready...${NC}" >&2
    fi

    sleep $check_interval
    elapsed=$((elapsed + check_interval))
  done

  echo -e "${RED}✗ OIDC server is not responding after ${max_wait}s${NC}" >&2
  echo -e "${BLUE}Debug: Discovery URL = ${discovery_url}${NC}" >&2
  return 1
}

if [ "${BASH_SOURCE[0]}" != "${0}" ]; then
  export -f get_oidc_tokens
  export -f get_oidc_token
  export -f get_oidc_id_token
  export -f check_oidc_server
fi

