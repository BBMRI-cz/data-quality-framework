# Privacy & Security

The Data Quality Framework is designed with privacy and security as core principles. This page outlines the technical measures, security practices, and privacy-preserving mechanisms that protect your data and ensure the integrity of the system.

## Security-First Development

### Open Source Transparency

The entire Data Quality Framework is **open source**, providing complete transparency into its operations:

- **Source Code**: All code is publicly available on [GitHub](https://github.com/BBMRI-cz/data-quality-framework)
- **Code Review**: All changes undergo peer review through pull requests
- **Issue Tracking**: Security vulnerabilities and bugs are tracked publicly
- **Community Oversight**: The open nature allows security researchers and the community to audit the code

### Automated Security Testing

Our CI/CD pipeline includes comprehensive security measures:

- **Automated Testing**: Every code change triggers automated test suites that validate functionality and security
- **Code Coverage**: We maintain [test coverage reports](https://codecov.io) to ensure critical paths are tested
- **Security Scorecard**: We use [OpenSSF Scorecard](https://securityscorecards.dev/) to continuously assess and improve our security posture
- **Dependency Updates**: Automated dependency scanning and updates via Dependabot to address known vulnerabilities

### Supply Chain Security

We implement multiple layers of supply chain protection:

- **Signed Docker Images**: All published Docker images are cryptographically signed using [Cosign](https://docs.sigstore.dev/cosign/overview/)
- **Reproducible Builds**: Our build process is deterministic and reproducible
- **Pinned Dependencies**: All GitHub Actions and base images use SHA-pinned versions to prevent supply chain attacks
- **Container Security**: Images are built from minimal, security-hardened base images

### Image Verification

You can verify the authenticity and integrity of our Docker images using Cosign:

```bash
# Verify the Data Quality Agent image
cosign verify ghcr.io/bbmri-cz/data-quality-agent:latest \
  --certificate-identity=https://github.com/BBMRI-cz/data-quality-framework/.github/workflows/ci.yml@refs/heads/master \
  --certificate-oidc-issuer=https://token.actions.githubusercontent.com

# Verify the Data Quality Server image
cosign verify ghcr.io/bbmri-cz/data-quality-server:latest \
  --certificate-identity=https://github.com/BBMRI-cz/data-quality-framework/.github/workflows/ci.yml@refs/heads/master \
  --certificate-oidc-issuer=https://token.actions.githubusercontent.com
```

## Privacy-Preserving Data Processing

### Core Privacy Principle

> **Raw data never leaves the site — only aggregated quality metrics are shared.**

The Data Quality Framework implements a federated architecture where sensitive data remains at the source while still enabling quality assessment across multiple sites.

### Differential Privacy Implementation

The framework employs **differential privacy** to provide mathematically rigorous privacy guarantees when sharing quality metrics.

#### What is Differential Privacy?

Differential privacy is a mathematical framework that provides provable privacy guarantees by adding carefully calibrated noise to query results. It ensures that the presence or absence of any individual record in a dataset cannot be determined from the output, even by an adversary with auxiliary information.

The key properties of differential privacy are:

- **Quantifiable Privacy**: Privacy protection is measured by a parameter ε (epsilon) - smaller values provide stronger privacy
- **Composition**: Multiple queries can be combined while maintaining privacy guarantees
- **Robustness**: Protection holds even against powerful adversaries with background knowledge

For a comprehensive understanding of differential privacy, see the foundational paper: [*"The Algorithmic Foundations of Differential Privacy"*](https://www.cis.upenn.edu/~aaroth/Papers/privacybook.pdf) by Dwork and Roth.

#### Technical Implementation

Our differential privacy implementation uses the **Laplace mechanism** with additional safeguards:

**Implementation Location:**
- **Class**: `eu.bbmri_eric.quality.agent.dataquality.DifferentialPrivacyUtil`
- **Source**: `agent/backend/src/main/java/eu/bbmri_eric/quality/agent/report/DifferentialPrivacyUtil.java`
- **Tests**: `agent/backend/src/test/java/eu/bbmri_eric/quality/agent/report/DifferentialPrivacyUtilTest.java`

The implementation includes comprehensive test coverage to ensure correctness of the differential privacy mechanisms.

#### Example: Obfuscated Percentage Calculation

To illustrate how differential privacy is applied in practice, consider the following scenario:

1. A local site runs a quality check and finds **67 patients with a faulty record** (e.g., invalid gender value).
2. Let the total number of patients locally be **10,000**.
3. Before transmitting anything, the agent applies the Laplace mechanism to the raw count:
   - `trueCount = 67`
   - `epsilon = 1.0` (example privacy budget; actual value may differ per check)
   - `noise ~ Laplace(scale = 1 / epsilon)`
   - Suppose the sampled `noise = -1.73`
   - `noisyCount = max(0, trueCount + noise) = max(0, 67 - 1.73) ≈ 65.27`
4. The agent converts this to an obfuscated percentage:
   - `percentage = (noisyCount / totalPatients) * 100 ≈ (65.27 / 10,000) * 100 ≈ 0.65%`
5. Only this **privacy-preserved percentage (≈ 0.65%)** is sent to the Data Quality Server – never the raw count (67).
6. The central server stores and displays the obfuscated metric. Individual patient presence or absence cannot be inferred due to the added calibrated noise.

Key points:
- Small datasets receive proportionally more effective protection because the noise impact is relatively larger.
- Counts are clipped to zero to avoid negative artifacts after noise addition.
- Each quality check consumes part of the privacy budget (ε); budgeting strategy can be tuned.

> This approach ensures that even if an attacker knows most of the dataset, they cannot reliably determine whether any one patient contributed to the reported metric.

## Data Flow Architecture

1. **Local Processing**: Quality checks execute entirely within the local environment
2. **Aggregation**: Results are aggregated locally before any privacy mechanisms are applied
3. **Privacy Application**: Differential privacy is applied to aggregated counts and metrics
4. **Secure Transmission**: Only privacy-protected aggregated metrics are transmitted to the central server
5. **Central Collection**: The server receives only anonymized, aggregated quality metrics

## Communication Security

- **HTTPS/TLS**: All communication between agents and servers uses encrypted channels
- **Authentication**: Secure authentication mechanisms protect against unauthorized access
- **Authorization**: Role-based access controls limit what data can be accessed by whom
- **Audit Logging**: All data access and sharing activities are logged for accountability

## Getting Help

If you have security concerns or questions about our privacy implementation:

- **Security Issues**: Report security vulnerabilities through [GitHub Security Advisories](https://github.com/BBMRI-cz/data-quality-framework/security/advisories)
- **Privacy Questions**: Contact the development team for clarification on privacy practices
- **Implementation Support**: Consult the [getting started guide](/user/getting-started) for secure deployment practices

::: warning Security Best Practices
When deploying the Data Quality Framework:
- Use the latest versions of all components
- Verify Docker image signatures before deployment
- Configure network isolation appropriately
- Follow secure configuration guidelines
- Monitor logs for suspicious activity
:::
