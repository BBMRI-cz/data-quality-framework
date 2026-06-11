[![CI/CD Pipeline](https://github.com/BBMRI-cz/data-quality-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/BBMRI-cz/data-quality-framework/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/BBMRI-cz/data-quality-framework/branch/master/graph/badge.svg)](https://codecov.io/gh/BBMRI-cz/data-quality-framework)
[![Static Badge](https://img.shields.io/badge/TRL-7-yellow)](https://ec.europa.eu/research/participants/data/ref/h2020/wp/2014_2015/annexes/h2020-wp1415-annex-g-trl_en.pdf)
![GitHub release](https://img.shields.io/github/v/release/BBMRI-cz/data-quality-framework)
![License](https://img.shields.io/github/license/BBMRI-ERIC/negotiator)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/BBMRI-cz/data-quality-framework/badge)](https://scorecard.dev/viewer/?uri=github.com/BBMRI-cz/data-quality-framework)

# Data Quality Framework

A federated, privacy-preserving framework for assessing and monitoring data quality in distributed biomedical
repositories. The framework enables quality assessment at source while sharing only aggregated, differentially private
metrics with a central coordination server.

## Key Features

- **Privacy-First**: Raw data never leaves the site — only aggregated quality metrics are shared
- **Open Source**: Fully transparent implementation with community oversight
- **Privacy Preserving**: Differential privacy mechanisms protect individual records
- **Easy to Deploy**: Containerized deployment with automatic updates
- **Extensible**: Modular architecture supports multiple data sources and quality checks

## Project Status

**Technology Readiness Level 7** - Stable enough for experimentation and pilot deployments against HL7 FHIR stores or OMOP CDM databases.


## Quick Start

```bash
# Start with Docker Compose
git clone https://github.com/BBMRI-cz/data-quality-framework.git
cd data-quality-framework
docker compose up -d

# Access the agent at http://localhost:8081
# Default credentials: admin / adminpass
```

## Documentation

📖 **[Complete Documentation](https://fdqf.bbmri-eric.eu/)**

## Support

- **Issues**: [GitHub Issues](https://github.com/BBMRI-cz/data-quality-framework/issues) for bug reports and feature
  requests
- **Security**: Report security vulnerabilities
  via [GitHub Security Advisories](https://github.com/BBMRI-cz/data-quality-framework/security/advisories)

## License

Licensed under the [GNU General Public License v3.0](LICENSE)

---

Developed by BBMRI-ERIC & Masaryk Memorial Cancer Institute
