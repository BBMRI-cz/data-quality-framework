# Data Quality Agent

The Data Quality Agent helps you assess, monitor, and securely share the quality of data held in distributed
repositories. It computes transparent quality metrics and (optionally) publishes aggregated, privacy‑preserving reports
to a central Data Quality Server.

Key capabilities (design goals):

- Data‑model agnostic core (current implementation ships with an HL7 FHIR connector; more sources to follow)
- Extensible metric & rules engine (CQL / declarative checks today, pluggable strategies tomorrow)
- Differential privacy safeguards for outbound / shared statistics
- Local dashboard for exploration & validation
- Secure, configurable remote publishing workflow (opt‑in)

> [!NOTE]  
> While the architecture is data‑agnostic, the first production connector targets clinical data exposed via HL7
> FHIR using the BBMRI.de profiles.
> Additional connectors (e.g. OMOP, relational SQL schemas, delimited files, other research / biobank formats) will be
> added based on emerging use cases.

## Status

Current focus: Early stage ("alpha").
Stable enough for experimentation against HL7 FHIR endpoints implementing
the [BBMRI.de FHIR profiles](https://simplifier.net/BBMRI.de).

What works today:

- Connect to an HL7 FHIR server (tested primarily with Blaze)
- Run bundled quality checks (CQL-based) against BBMRI.de profile data
- Generate local quality reports & view them in the dashboard

Planned / roadmap (subject to change):

- (Optional) Share aggregated metrics with a central server (differential privacy layer in progress / iterative)
- Additional data source connectors (OMOP, tabular/CSV, SQL, imaging metadata)
- Custom rule authoring & packaging
- Scheduling & historical trend comparison
- Hardening of privacy / anonymization guarantees
- Deployment recipes (Kubernetes / Helm, Docker Compose)

If you rely on a future feature, please open an issue to help prioritize.

## Quick Start

Spin up a local Blaze FHIR store and the Data Quality Agent on a shared Docker network:

```shell
docker network create quality
docker run -d --name fhir-store --network quality -p 8080:8080 ghcr.io/samply/blaze:latest
docker run -d --name quality-agent --network quality -p 8081:8081 \
  -e EU_BBMRI_ERIC_QUALITY_AGENT_FHIR_URL=http://fhir-store:8080/fhir \
  ghcr.io/bbmri-cz/data-quality-agent:latest
```

Open the dashboard: http://localhost:8081

> [!NOTE]  
> Default dashboard credentials: `admin / adminpass` (change in production).

Optional: Load bundled synthetic test data (requires `blazectl`). See: https://github.com/samply/blazectl

## Deployment

Follow the instructions below for deployment in production or shared test environments.

### Prerequisites

Minimal requirements with the current (FHIR) connector:

- **Docker** (runtime environment)
- **HL7 FHIR Store** containing BBMRI.de‑compliant resources (e.g. Blaze)
- **Network access** from the agent container to the data source (Docker network or reachable host/port)

### Setup

Example (Bridgehead / BBMRI-ERIC Locator integration pointing to a host-exposed FHIR endpoint):

```shell
docker run -d --name quality-agent -p 8081:8081 \
  -e EU_BBMRI_ERIC_QUALITY_AGENT_FHIR_URL=https://host.docker.internal/bbmri-localdatamanagement/fhir \
  -e EU_BBMRI_ERIC_QUALITY_AGENT_FHIR_PASSWORD=<PASSWORD> \
  --add-host=host.docker.internal:host-gateway \
  ghcr.io/bbmri-cz/data-quality-agent:latest
```

### Accessing the dashboard

Remote server access:

```
http://<server-ip>:8081
```

Via SSH tunnel (if ports are firewalled):

```shell
ssh -L 8081:127.0.0.1:8081 [USER@]SERVER_IP
```

Then browse to: http://localhost:8081

### Configuration

Available environment variables and defaults:

| Environment Variable | Description                                      | Default Value                |
|----------------------|--------------------------------------------------|------------------------------|
| `FHIR_URL`           | Base URL of the target FHIR server               | `http://localhost:8080/fhir` |
| `FHIR_USERNAME`      | Username for authenticating with the FHIR server | `bbmri`                      |
| `FHIR_PASSWORD`      | Password for authenticating with the FHIR server | `fhirpass`                   |

### Security / Hardening Notes

- Always override default credentials in non-local environments.
- Prefer passing secrets via a Docker secret / orchestrator secret store instead of plain env vars when possible.
- Restrict network exposure (bind to internal interfaces or use a reverse proxy with TLS in production).
- Validate that only aggregated, privacy-approved metrics leave your environment (feature still evolving).

### Troubleshooting

| Symptom                 | Check                                                              |
|-------------------------|--------------------------------------------------------------------|
| Dashboard not reachable | Container running? (`docker ps`) Port free? Firewall rules?        |
| FHIR connection errors  | URL correct? Container network access? Blaze healthy on port 8080? |
| Empty reports           | Confirm test data loaded; inspect logs for failed CQL execution.   |
| Auth failures           | Verify username/password env vars; check FHIR server auth method.  |

Show logs:

```shell
docker logs -f quality-agent
```

Health probe (example):

```shell
curl -s http://localhost:8081/actuator/health | jq
```

---

Feel free to open issues for: feature requests, connector ideas, unclear docs, or false-positive / false-negative
quality checks.
