#!/bin/bash

# Local System Test Script
# This script runs the complete system test locally without requiring act
# It builds the Docker images, starts all services, and runs the system test

set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Local System Test${NC}"
echo -e "${YELLOW}========================================${NC}"

# Step 1: Build images
echo -e "\n${YELLOW}Step 1: Building Docker images...${NC}"
cd "$REPO_ROOT"

echo -e "${YELLOW}Building agent image...${NC}"
docker build -t ghcr.io/bbmri-cz/data-quality-agent:latest ./agent

echo -e "${YELLOW}Building server image...${NC}"
docker build -t ghcr.io/bbmri-cz/data-quality-server:latest ./server

echo -e "${GREEN}✓ Docker images built successfully${NC}"

# Step 2: Start Blaze FHIR server
echo -e "\n${YELLOW}Step 2: Starting Blaze FHIR server...${NC}"
docker run -d --name blaze -p 8080:8080 samply/blaze:latest

# Wait for Blaze to be ready
echo -e "${YELLOW}Waiting for Blaze to be ready...${NC}"
"$SCRIPT_DIR/wait-for-url.sh" http://localhost:8080/health
echo -e "${GREEN}✓ Blaze FHIR server is ready${NC}"

# Step 3: Load test data into Blaze
echo -e "\n${YELLOW}Step 3: Loading test data into Blaze...${NC}"

# Check if blazectl is installed
if ! command -v blazectl &> /dev/null; then
    echo -e "${YELLOW}blazectl not found, installing...${NC}"
    "$SCRIPT_DIR/install-blazectl.sh"
fi

blazectl --no-progress --server http://localhost:8080/fhir upload "$REPO_ROOT/test_data"
echo -e "${GREEN}✓ Test data loaded successfully${NC}"

# Step 4: Start application services
echo -e "\n${YELLOW}Step 4: Starting application services...${NC}"
cd "$REPO_ROOT"
docker compose up -d

# Step 5: Wait for services to be healthy
echo -e "\n${YELLOW}Step 5: Waiting for services to be healthy...${NC}"

# Check agent health
echo -e "${YELLOW}Checking agent health...${NC}"
for i in {1..20}; do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' quality-agent 2>/dev/null || echo "starting")
  echo "Agent health status: $STATUS"
  if [ "$STATUS" == "healthy" ]; then
    echo -e "${GREEN}✓ Agent is healthy${NC}"
    break
  fi
  if [ $i -eq 20 ]; then
    echo -e "${RED}✗ Agent failed to become healthy${NC}"
    docker logs quality-agent
    exit 1
  fi
  sleep 3
done

# Check server health
echo -e "${YELLOW}Checking server health...${NC}"
for i in {1..20}; do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' quality-server 2>/dev/null || echo "starting")
  echo "Server health status: $STATUS"
  if [ "$STATUS" == "healthy" ]; then
    echo -e "${GREEN}✓ Server is healthy${NC}"
    break
  fi
  if [ $i -eq 20 ]; then
    echo -e "${RED}✗ Server failed to become healthy${NC}"
    docker logs quality-server
    exit 1
  fi
  sleep 3
done

# Check OIDC server health
echo -e "${YELLOW}Checking OIDC server health...${NC}"
for i in {1..30}; do
  if curl -s -f http://localhost:4011/.well-known/openid-configuration > /dev/null 2>&1; then
    echo -e "${GREEN}✓ OIDC server is ready${NC}"
    break
  fi
  echo "Waiting for OIDC server... attempt $i/30"
  if [ $i -eq 30 ]; then
    echo -e "${RED}✗ OIDC server failed to become ready${NC}"
    docker logs oidc-server-mock
    exit 1
  fi
  sleep 2
done

# Step 6: Run system test
echo -e "\n${YELLOW}Step 6: Running system test...${NC}"
"$SCRIPT_DIR/system-test-interaction.sh"

# Step 7: Cleanup
echo -e "\n${YELLOW}Step 7: Cleaning up...${NC}"
cd "$REPO_ROOT"
docker compose down
docker stop blaze || true
docker rm blaze || true

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}Local system test completed successfully!${NC}"
echo -e "${GREEN}========================================${NC}"

