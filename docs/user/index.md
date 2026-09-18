# Overview

The **Federated Data Quality Framework (FDQF)** is open-source tooling for assessing data quality in federated
environments — networks where data remains distributed across independent locations for legal, organizational, or
technical reasons.

Instead of moving data to a central location, the FDQF runs quality checks locally at each site and reports only
aggregated, privacy-preserving results.

## Components

The FDQF consists of two main components:

![Architecture diagram](/diagram.svg)

### Data Quality Agent (DQA)

Runs locally at each data-holding site. It:

- Executes data quality checks against the site's data source
- Supports two data source types:
  - **FHIR** — a FHIR R4 server queried with HL7 CQL (Clinical Quality Language)
  - **SQL** — a relational database or directory of CSV files queried with SQL (PostgreSQL, MySQL, SQLite,
    Apache Calcite/CSV)
- Aggregates results and applies differential privacy before anything leaves the site
- Pushes quality reports to the central server (one-way only; the server cannot pull data)

See [Data Sources](/user/data-sources) for configuration details and [Privacy and Security](/user/privacy) for how the
reported results are anonymized.

### Data Quality Server (DQS)

Runs centrally to collect and present results. It:

- Receives quality reports from multiple agents
- Aggregates results across the network
- Provides dashboards for end users
- Exposes a REST API for integrating reports into external dataset catalogues

This lets researchers and study investigators compare data quality across sites without accessing raw data.

## Why Federated Data Quality?

Traditional data quality validation requires central access to raw datasets, which is often impossible in healthcare
and other regulated environments. The FDQF enables:

- Local processing at each data site
- Privacy-preserving quality metrics
- Cross-site comparison without data sharing
- Standardized and reproducible quality evaluation

## Who Is It For?

The FDQF is designed for:

- Research networks and data consortia
- Healthcare institutions
- Biobanking and cohort infrastructures
- Privacy-sensitive or regulated environments

Typical roles include data stewards, study principal investigators, clinical researchers, and data engineers or IT
operators.

## Summary

| Concept            | Description                                                       |
|--------------------|-------------------------------------------------------------------|
| Federated approach | Data stays at each site; checks run locally                       |
| Local agent        | Executes CQL or SQL checks and reports privacy-preserving metrics |
| Central server     | Collects, aggregates, and displays results                        |
| Goal               | Assess data quality across sites without sharing raw data         |

::: tip See it in action
Follow the [Hands-On Guide](/user/hands‑on_guide) to deploy an agent and run your first quality checks.
:::
