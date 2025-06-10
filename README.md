# Data Quality Agent

A tool for generating data quality reports with differential privacy for HL7 FHIR data, tailored to BBMRI.de FHIR
profiles.

## Status

The application generates quality reports for data stored in an HL7 FHIR store, provided the data adheres to
the [BBMRI.de FHIR profiles](https://simplifier.net/BBMRI.de).

## Prerequisites

- **Docker**: Ensure Docker is installed and running.
- **FHIR Store**: Access to an HL7 FHIR server with BBMRI.de-compliant data.
- **Network Configuration**: If running the FHIR store locally, configure Docker networking (
  see [Networking](#networking)).

## Getting Started

Pull the latest image from GitHub Container Registry:
```shell
docker pull ghcr.io/bbmri-cz/data-quality-agent:latest
```

Run the application, specifying the FHIR server URL:

```shell
docker run -d --name quality-agent -p 8081:8081 -e EU_BBMRI_ERIC_QUALITY_AGENT_FHIR_URL=<fhir_server_url> ghcr.io/bbmri-cz/data-quality-agent:latest
```

Access the dashboard at http://localhost:8081.
Setup a FHIR store.
We recommend using the [Blaze store](https://github.com/samply/blaze) along with its suite of support tools such as [Blazectl](https://github.com/samply/blazectl).
> [!WARNING]  
> Warning: If running the FHIR store locally (e.g., in another Docker container), ensure both containers are on the same
> Docker network to communicate. Create a network and attach both containers:
>
> ```shell
> docker network create quality
> docker run -d --name fhir-store --network quality <fhir_image>
> docker run -d --name quality-agent --network quality -p 8081:8081 -e EU_BBMRI_ERIC_QUALITY_AGENT_FHIR_URL=http://fhir-store:<fhir_port> ghcr.io/bbmri-cz/data-quality-agent:latest
> ```

Replace <fhir_image> and <fhir_port> with your FHIR store's image and port.
### Test Data
Compatible test data can be found in the _test_data_ directory. This synthetic test data was generated using this [Generator](https://github.com/samply/bbmri-fhir-gen).

## Configuration

## Environment Variables:

| EU_BBMRI_ERIC_QUALITY_AGENT_FHIR_URL | URL of the FHIR server (e.g., http://fhir.example.com or http://fhir-store:8080 for a local container). |
|--------------------------------------|---------------------------------------------------------------------------------------------------------|

## Troubleshooting

Dashboard Inaccessible: Verify the container is running (docker ps) and port 8081 is not blocked.
FHIR Connection Issues: Ensure the FHIR server URL is correct and reachable from the container. Check Docker network
settings if running locally.
Logs: View logs with docker logs quality-agent.

## Contributing

Contributions are welcome! Please submit issues or pull requests to the repository.
