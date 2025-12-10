# Deployment

This guide covers production deployment of the Data Quality Framework components, **primarily the Data Quality Agent**.

::: info Most Common Use Case
If you've been asked to deploy a Data Quality Framework component, you most likely need to deploy the **Data Quality Agent**. The Data Quality Server is only needed for central coordination across multiple sites and is typically deployed by administrators managing the overall network.
:::

Follow these instructions to deploy the Data Quality Agent at your site, and optionally the Data Quality Server if you're setting up central coordination.

## Prerequisites

Before deploying, ensure your system meets the following requirements:

### System Requirements

- **Docker Engine**: Version 20.10 or later ([Installation Guide](https://docs.docker.com/engine/install/))
- **Docker Compose**: Version 2.0 or later (included with Docker Desktop, or install separately)
- **Available Memory**: Minimum 2GB RAM per component
- **Storage**: At least 10GB available disk space
- **Network**: Internet access for image downloads and updates

::: warning Docker Permissions Required
To run Docker commands, you need either:
- **Sudo privileges** to run Docker commands as root, or
- **Docker group membership** to run Docker commands as a regular user

To add your user to the docker group:
```bash
sudo usermod -aG docker $USER
# Log out and log back in for changes to take effect
```

After logging back in, verify you can run Docker without sudo:
```bash
docker ps
```
:::

### Required Ports

- **Data Quality Agent**: 8081
- **Data Quality Server**: 8082 (if deploying)

### Security Tools (Optional but Recommended)

- **Cosign**: For verifying Docker image signatures
- **Watchtower**: For automatic container updates

```bash
# Install Cosign (Linux/macOS)
curl -O -L "https://github.com/sigstore/cosign/releases/latest/download/cosign-linux-amd64"
sudo mv cosign-linux-amd64 /usr/local/bin/cosign
sudo chmod +x /usr/local/bin/cosign
```

## Image Verification

::: warning Security Best Practice
Always verify Docker images before deployment to ensure authenticity and integrity.
:::

### Verify Data Quality Agent Image

```bash
cosign verify ghcr.io/bbmri-cz/data-quality-agent:latest \
  --certificate-identity=https://github.com/BBMRI-cz/data-quality-framework/.github/workflows/ci.yml@refs/heads/master \
  --certificate-oidc-issuer=https://token.actions.githubusercontent.com
```

### Verify Data Quality Server Image

```bash
cosign verify ghcr.io/bbmri-cz/data-quality-server:latest \
  --certificate-identity=https://github.com/BBMRI-cz/data-quality-framework/.github/workflows/ci.yml@refs/heads/master \
  --certificate-oidc-issuer=https://token.actions.githubusercontent.com
```

## Data Quality Agent Deployment

The Data Quality Agent runs at each data site to perform local quality assessments.

### Step 1: Create Deployment Directory

```bash
sudo mkdir -p /opt/data-quality-agent
cd /opt/data-quality-agent
```

### Step 2: Create Compose Configuration

Create a `compose.yaml` file with the following content:

```yaml
version: '3.8'

services:
  quality-agent:
    image: ghcr.io/bbmri-cz/data-quality-agent:0.1
    container_name: quality-agent
    restart: unless-stopped
    ports:
      - "8081:8081"
    volumes:
      - agent-data:/app/data
    extra_hosts:
      - "host.docker.internal:host-gateway"
    environment:
      # Remove these 2 environment variables if you do not wish to share Data Quality Reports
      - REPORTING_SERVER_URL=https://quality-dashboard.bbmri-eric.eu
      - REPORTING_SERVER_NAME=Central Data Quality Server of BBMRI

  # Remove this service if you do not want automatic updates
  watchtower:
    image: containrrr/watchtower:1.7.1
    container_name: watchtower
    restart: unless-stopped
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      - WATCHTOWER_CLEANUP=true
      - WATCHTOWER_SCHEDULE=0 0 2 * * *  # Check daily at 2 AM
      - WATCHTOWER_INCLUDE_STOPPED=true
    command: quality-agent

volumes:
  agent-data:
    driver: local
```
### Step 3: Start the Services

```bash
docker compose up -d
```

### Step 4: Verify Deployment

Check that the services are running properly:

```bash
# Check container status
docker compose ps

# View agent logs
docker compose logs quality-agent

# Test agent health (wait a minute for startup)
curl http://localhost:8081/api/health
```

### Step 5: Access the Web Interface

Open your web browser and navigate to: **http://localhost:8081**

Default login credentials:
- **Username**: `admin`
- **Password**: `adminpass`

::: tip SSH Tunnel for Remote Deployments
If you're deploying on a remote Linux VM, you can access the web interface using an SSH tunnel:

```bash
# Create SSH tunnel to forward local port 8081 to remote server
ssh -L 8081:localhost:8081 user@your-server.com

# Keep the SSH session open and access http://localhost:8081 in your browser
```

This forwards your local port 8081 to the remote server's port 8081, allowing you to access the web interface securely.
:::

### Step 6: Change Default Password

For security, change the default admin password immediately after first login:

1. After logging in, click on the **menu icon** (☰) in the upper right corner
2. Select **Change Password** from the dropdown menu
3. Enter a strong new password
4. Save the changes

### Step 7: Configure Database Connection

To connect to data sources:
1. In the web interface, navigate to **Settings > FHIR Server**
2. Configure connection until you see a positive confirmation
3. For services running on your host machine, use `host.docker.internal` as the hostname

::: tip BBMRI-ERIC Federated Search Platform
If you are deploying this agent as part of the **BBMRI-ERIC Federated Search Platform**, use the following configuration:

- **FHIR Server URL**: `https://host.docker.internal/bbmri-localdatamanagement/fhir/`
- **Username**: `bbmri`
- **Password**: Can be found in `/etc/bridgehead/bbmri.local.conf` on your server

This allows the agent to connect to the local FHIR store provided by the Bridgehead component.
:::

## Data Quality Server Deployment

The Data Quality Server runs centrally to collect and aggregate reports from multiple agents.

### Step 1: Create Deployment Directory

```bash
sudo mkdir -p /opt/data-quality-server
cd /opt/data-quality-server
```

### Step 2: Create Compose Configuration

Create a `compose.yaml` file with the following content:

```yaml
version: '3.8'

services:
  quality-server:
    image: ghcr.io/bbmri-cz/data-quality-server:latest
    container_name: quality-server
    restart: unless-stopped
    ports:
      - "8082:8082"
    volumes:
      - server-data:/app/data

  # Remove this service if you do not want automatic updates
  watchtower:
    image: containrrr/watchtower:1.7.1
    container_name: watchtower
    restart: unless-stopped
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      - WATCHTOWER_CLEANUP=true
      - WATCHTOWER_SCHEDULE=0 0 2 * * *  # Check daily at 2 AM
      - WATCHTOWER_INCLUDE_STOPPED=true
    command: quality-server

volumes:
  server-data:
    driver: local
```

### Step 3: Start the Services

```bash
docker compose up -d
```

### Step 4: Verify Deployment

Check that the services are running properly:

```bash
# Check container status
docker compose ps

# View server logs
docker compose logs quality-server

# Test server health (wait a minute for startup)
curl http://localhost:8082/actuator/health
```

### Step 5: Access the Web Interface

Open your web browser and navigate to: **http://localhost:8082**

Default login credentials:
- **Username**: `admin`
- **Password**: `adminpass`

::: tip SSH Tunnel for Remote Deployments
If you're deploying on a remote Linux VM, you can access the web interface using an SSH tunnel:

```bash
# Create SSH tunnel to forward local port 8082 to remote server
ssh -L 8082:localhost:8082 user@your-server.com

# Keep the SSH session open and access http://localhost:8082 in your browser
```

This forwards your local port 8082 to the remote server's port 8082, allowing you to access the web interface securely.
:::

### Step 6: Change Default Password

For security, change the default admin password immediately after first login:

1. After logging in, click on the **menu icon** (☰) in the upper right corner
2. Select **Change Password** from the dropdown menu
3. Enter a strong new password
4. Save the changes

## Production Considerations

### Security

- Use specific image tags instead of `latest` for production
- Configure firewall rules to restrict access to necessary ports
- Use TLS/SSL certificates for HTTPS access
- Regularly verify image signatures before updates

### Monitoring

- Monitor container health using the built-in health checks
- Set up log aggregation for centralized logging
- Configure alerting for container failures
- Monitor disk usage for persistent volumes

### Backup

```bash
# Backup persistent data
docker compose exec quality-agent tar czf /app/backup-$(date +%Y%m%d).tar.gz /app/data
docker cp quality-agent:/app/backup-$(date +%Y%m%d).tar.gz ./backups/

# Backup configuration
cp compose.yaml ./backups/compose-$(date +%Y%m%d).yaml
```

### Resource Limits

Add resource limits to prevent containers from consuming too many resources:

```yaml
services:
  quality-agent:
    # ... other configuration ...
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
        reservations:
          memory: 1G
          cpus: '0.5'
```

## Troubleshooting

### Common Issues

**Container won't start**: Check port availability and Docker daemon status
```bash
docker compose logs quality-agent
sudo netstat -tlnp | grep :8081
```

**Health check failures**: Verify application startup and configuration
```bash
docker compose exec quality-agent curl -f http://localhost:8081/actuator/health
```

**Update failures**: Check image availability and network connectivity
```bash
docker pull ghcr.io/bbmri-cz/data-quality-agent:latest
```

### Getting Help

- Check container logs: `docker compose logs <service-name>`
- Verify network connectivity: `docker network ls` and `docker network inspect <network-name>`
- Review health status: `docker compose ps`
- Consult the [getting started guide](/user/getting-started) for basic setup issues

