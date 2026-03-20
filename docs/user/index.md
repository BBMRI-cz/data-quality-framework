# Overview

This guide introduces the **Federated Data Quality Framework (FDQF)** and its tooling.  
It is intended for users who want to **deploy, operate, or understand** the system at a high level.

For deeper technical information, configuration guides, and deployment steps, please refer to the additional sections in
the documentation menu.

---

# What is the FDQF?

The **Federated Data Quality Framework (FDQF)** provides a structured approach and supporting software to assess and
improve data quality in **federated environments** — situations where data remains distributed across independent
locations due to **legal, organizational, or technical constraints**.

Instead of moving data to a central location, the FDQF enables **local data quality analysis** and **privacy-preserving
reporting** across multiple sites.

## Key Components

The FDQF consists of two main software components:

![](/diagram.svg)

### Data Quality Agent (DQA)

Runs **locally at each data-holding site**.

Responsibilities:

- Executes predefined, domain-specific **data quality checks**
- Converts human-readable rules into machine-readable queries  
  *(currently supported: HL7 CQL — Clinical Quality Language)*
- Queries the connected database locally
- Generates **aggregated and privacy-preserving results**
- **Pushes** data quality reports to the central server (one-way; no data pull)

::: warning Privacy-First Approach
If you would like to learn how do we guarantee privacy preservation/anonymization of the shared results see
the [Privacy page](/user/privacy).
:::

### Data Quality Server (DQS)

Runs **centrally** to collect and present results.

Responsibilities:

- Receives quality reports from multiple sites
- Aggregates results across the network
- Provides dashboards and views for end-users
- Provides a REST API for including the reports in external Dataset catalogues
- Enables researchers or study investigators to evaluate **multi-site data quality** without accessing raw data

## Why Federated Data Quality?

Traditional data quality validation typically requires central access to raw datasets.  
In many real-world environments — such as healthcare or regulated research settings — this is not possible.

The FDQF enables:

- <i class="bi bi-check-circle"></i> **Local processing** at each data site
- <i class="bi bi-check-circle"></i> **Privacy-preserving quality metrics**
- <i class="bi bi-check-circle"></i> **Cross-site comparison without data sharing**
- <i class="bi bi-check-circle"></i> **Standardized and reproducible quality evaluation**
- <i class="bi bi-check-circle"></i> Support for federated biomedical and research infrastructures

## Who Is This For?

The FDQF is designed for:

- Research networks and data consortia
- Healthcare institutions
- Federated biobanking and cohort infrastructures
- Privacy-sensitive or regulated environments
- Projects requiring **data quality transparency without data transfer**

Roles that may use the system include:

- Data Stewards
- Study Principal Investigators
- Clinical Researchers
- Data Engineers and IT Operators

## In Summary

| Concept            | Description                                                        |
|--------------------|--------------------------------------------------------------------|
| Federated approach | Data stays at each site — processing happens locally               |
| Local Agent        | Executes checks and generates privacy-preserving metrics           |
| Central Server     | Collects, aggregates, and displays results                         |
| Goal               | Enable cross-site data quality assessment without raw data sharing |

The FDQF ensures **trust, privacy, and transparency** in multi-institution data ecosystems by combining **local
computation** with **centralized insight**.

::: tip See for yourself
If you would like to experiment with the tooling please go to the [Getting Started page](/user/hands‑on_guide.md).
:::
