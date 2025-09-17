# Data Quality Agent

The Data Quality Agent helps you assess, monitor, and securely share the quality of data held in distributed repositories. It computes transparent quality metrics and (optionally) publishes aggregated, privacy‑preserving reports to a central Data Quality Server.

Key capabilities (design goals):
- Data‑model agnostic core (current implementation ships with an HL7 FHIR connector; more sources to follow)
- Extensible metric & rules engine (CQL / declarative checks today, pluggable strategies tomorrow)
- Differential privacy safeguards for outbound / shared statistics
- Local dashboard for exploration & validation
- Secure, configurable remote publishing workflow (opt‑in)

> NOTE: While the architecture is data‑agnostic, the first production connector targets clinical data exposed via HL7 FHIR using the BBMRI.de profiles. Additional connectors (e.g. OMOP, relational SQL schemas, delimited files, other research / biobank formats) will be added based on emerging use cases.

## Status
Current focus: Early stage ("alpha").
Stable enough for experimentation against HL7 FHIR endpoints implementing the [BBMRI.de FHIR profiles](https://simplifier.net/BBMRI.de).

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

## Prerequisites
Minimal requirements to get started with the current (FHIR) connector:
- **Docker** (runtime environment)
- **HL7 FHIR Store** containing BBMRI.de‑compliant resources (e.g. Blaze)
- **Network Access** from the agent container to the data source (Docker network or reachable host/port)

