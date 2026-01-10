# Getting Started

::: tip Production Deployment
This page is dedicated to local deployments and experimentation. If you would like to deploy the tooling to production,
see [Deployment](/user/deployment).
:::

## Overview

This guide will help you set up and run the Data Quality Framework locally for testing and development purposes. You'll
learn how to deploy the Data Quality Agent and connect it to a FHIR data store to begin assessing data quality.

## Prerequisites

Before you begin, ensure you have the following installed on your system:

- **Docker Engine** - For running containerized applications
- **Available ports** - 8080, 8081, and optionally 8082 (for the Data Quality Server)
- **Git** - For cloning the repository
- **Blazectl** - Command line utility for loading test data (installation instructions below)

## Quick Start

### Step 1: Set Up the Environment

First, create a Docker network and run the required containers:

```shell
# Create a dedicated Docker network
docker network create quality

# Start the FHIR Store (Blaze)
docker run -d --name fhir-store --network quality -p 8080:8080 ghcr.io/samply/blaze:latest

# Start the Data Quality Agent
docker run -d --name quality-agent --network quality -p 8081:8081 ghcr.io/bbmri-cz/data-quality-agent:latest
```

::: info About the Components

- **Blaze FHIR Store**: A high-performance FHIR server that supports HL7 CQL queries
- **Data Quality Agent**: The main application for assessing data quality at the source
  :::

### Step 2: Install Blazectl and Clone Repository

Install the Blazectl command line utility by following
the [installation instructions](https://github.com/samply/blazectl/blob/main/README.md).

Then clone this repository:

```shell
git clone https://github.com/BBMRI-cz/data-quality-framework.git
cd data-quality-framework
```

### Step 3: Load Test Data

Load the provided test data into the FHIR store:

```shell
blazectl --server http://localhost:8080/fhir upload test_data
```

### Step 4: Configure the Data Quality Agent

1. Open your web browser and navigate to the Data Quality Agent UI at: **http://localhost:8081**

2. Log in using the default credentials:
    - **Username**: `admin`
    - **Password**: `adminpass`

3. Configure the FHIR server connection:
    - Navigate to **Settings** > **FHIR Server** in the left sidebar
    - Set the FHIR Server URL to: `http://fhir-store:8080/fhir`
    - Save the configuration

## Optional: Data Quality Server Setup

The following steps are optional and allow you to set up a central Data Quality Server to collect and aggregate reports
from multiple agents.

### Step 5 (Optional): Start the Data Quality Server

Add the Data Quality Server to your setup:

```shell
# Start the Data Quality Server
docker run -d --name quality-server --network quality -p 8082:8082 ghcr.io/bbmri-cz/data-quality-server:latest
```

::: info About the Data Quality Server
The Data Quality Server runs centrally to collect and present aggregated results from multiple Data Quality Agents. It
provides dashboards and views for multi-site data quality evaluation without accessing raw data.
:::

### Step 6 (Optional): Register the Server in the Agent UI

1. In the Data Quality Agent UI (http://localhost:8081), navigate to **Settings** > **Reporting**

2. Configure the server connection:
    - Set the Server URL to: `http://quality-server:8082`
    - Provide a name

### Step 7 (Optional): Approve the Agent in the Server UI

1. Open your web browser and navigate to the Data Quality Server UI at: **http://localhost:8082**

2. Log in using the default credentials:
    - **Username**: `admin`
    - **Password**: `adminpass`

3. Navigate to the **Agent Management** or **Pending Registrations** section

4. Find your agent registration request and approve it

5. Once approved, the agent can start sending quality reports to the central server

::: tip Multi-Site Setup
In a real-world scenario, you would have multiple Data Quality Agents running at different sites, all connecting to the
same central Data Quality Server. This enables federated data quality assessment across your network.
:::

## Next Steps

Once you have completed the setup:

1. **Explore the Dashboard**: View data quality metrics and reports
2. **Run Quality Checks**: Execute predefined quality assessments on your test data
3. **Review Results**: Analyze the quality metrics and identify potential issues
4. **Customize Checks**: Create custom quality rules specific to your use case

## Troubleshooting

### Common Issues

**Containers won't start**: Ensure ports 8080, 8081, and 8082 (if using the server) are not in use by other
applications.

**Cannot connect to FHIR server**: Verify that both containers are running and on the same Docker network.

**Test data upload fails**: Check that the Blaze FHIR server is accessible at http://localhost:8080/fhir.

**Agent cannot register with server**: Ensure the Data Quality Server is running and accessible at the configured URL.

**Agent registration not appearing**: Check the server logs and verify the network connectivity between containers.

### Getting Help

If you encounter issues not covered here, please:

- Check the [troubleshooting section](/user/deployment#troubleshooting) in the deployment guide
- Review the container logs: `docker logs fhir-store`, `docker logs quality-agent`, or `docker logs quality-server`
- Open an issue on the [GitHub repository](https://github.com/BBMRI-cz/data-quality-framework)
