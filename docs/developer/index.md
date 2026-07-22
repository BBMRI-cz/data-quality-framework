# Developer Guide

Welcome to the Data Quality Framework developer documentation. This guide provides technical information for developers
who want to understand, modify, or contribute to the framework.

## Overview

The Data Quality Framework is a federated system designed to assess data quality at source while preserving privacy. It
consists of two main components:

- **Data Quality Agent**: Deployed at data sites to perform local quality assessments
- **Data Quality Server**: Central coordinator that aggregates quality reports from multiple agents

The system implements a privacy-first architecture where raw data never leaves the source site. Only aggregated,
differentially private quality metrics are shared with the central server.

### Key Design Principles

- **Privacy by Design**: Raw data remains at source; only privacy-protected aggregated metrics are shared
- **Federated Architecture**: Distributed processing with central coordination
- **Extensible**: Modular design supports multiple data sources and quality checks
- **Secure**: Comprehensive security measures including signed container images and encrypted communication
- **Data Model Agnostic**: Core architecture supports various data formats (currently focused on HL7 FHIR)

## Technology Stack

### Backend Technologies

- Java 25
- Spring Boot 3.5
- SQLite
- Maven

### Frontend Technologies

- Vue.js 3
- Vue Router 4
- Bootstrap 5
- Bootstrap Icons
- Axios
- Vite

### Infrastructure & DevOps

- Docker
- Docker Compose
- GitHub Actions
- Cosign
- Codecov
- OpenSSF Scorecard

### Documentation

- VitePress
- Markdown

### Security & Privacy

- Differential Privacy
- TLS/HTTPS
- Spring Security
- Signed Containers

## Architecture Overview

### System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Data Site A   │    │   Data Site B   │    │   Data Site C   │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │    Agent    │ │    │ │    Agent    │ │    │ │    Agent    │ │
│ │             │ │    │ │             │ │    │ │             │ │
│ │  ┌───────┐  │ │    │ │  ┌───────┐  │ │    │ │  ┌───────┐  │ │
│ │  │ FHIR  │  │ │    │ │  │ FHIR  │  │ │    │ │  │ FHIR  │  │ │
│ │  │ Store │  │ │    │ │  │ Store │  │ │    │ │  │ Store │  │ │
│ │  └───────┘  │ │    │ │  └───────┘  │ │    │ │  └───────┘  │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │ Privacy-protected     │ Privacy-protected     │ Privacy-protected
         │ quality metrics       │ quality metrics       │ quality metrics
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────────────┐
                    │  Data Quality Server    │
                    │                         │
                    │  ┌─────────────────┐    │
                    │  │   Aggregation   │    │
                    │  │   & Analysis    │    │
                    │  └─────────────────┘    │
                    │                         │
                    │  ┌─────────────────┐    │
                    │  │   Dashboard     │    │
                    │  │   & Reports     │    │
                    │  └─────────────────┘    │
                    └─────────────────────────┘
```

### Agent Architecture

The Data Quality Agent follows a modular Spring Boot architecture:

- **Web Layer**: Vue.js frontend + REST API controllers
- **Service Layer**: Business logic and orchestration
- **Data Access Layer**: JPA repositories and data persistence
- **External Integration**: FHIR client and CQL query execution
- **Security Layer**: Authentication, authorization, and differential privacy

### Server Architecture

The Data Quality Server provides central coordination:

- **Agent Management**: Registration and authentication of data quality agents
- **Report Aggregation**: Collection and processing of quality reports
- **Dashboard**: Multi-site quality visualization
- **API**: RESTful endpoints for external integration

## API Documentation

Both agent and server expose OpenAPI/Swagger documentation:

- **Agent API**: http://localhost:8081/swagger-ui.html
- **Server API**: http://localhost:8082/swagger-ui.html

## Documentation

The project documentation is built using [VitePress](https://vitepress.dev/), a modern static site generator that
provides:

- **Hot Reload**: Instant preview of documentation changes during development
- **Vue.js Integration**: Custom components and interactive elements
- **Markdown-based**: Easy-to-write documentation using standard Markdown
- **Built-in Features**: Tips, warnings, code highlighting, and cross-references

For detailed information on contributing to documentation, see the [Contributing Guide](/developer/CONTRIBUTING).
