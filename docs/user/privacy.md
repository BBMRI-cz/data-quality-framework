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

Our differential privacy implementation uses the **Laplace mechanism** (or optionally the **Gaussian mechanism** for (ε, δ)-differential privacy) with additional safeguards:

**Implementation Location:**
- **Class**: `eu.bbmri_eric.quality.agent.dataquality.DifferentialPrivacyUtil`
- **Source**: `agent/backend/src/main/java/eu/bbmri_eric/quality/agent/dataquality/DifferentialPrivacyUtil.java`
- **Tests**: `agent/backend/src/test/java/eu/bbmri_eric/quality/agent/dataquality/DifferentialPrivacyUtilTest.java`

The implementation includes comprehensive test coverage to ensure correctness of the differential privacy mechanisms.

#### Protection Against Repeated-Report Averaging

Reports are generated repeatedly (by default daily). If every report sampled fresh noise for a count that barely moved, an observer could average the noisy values over many reports and cancel the noise out — and a perfectly unchanging reported value would even reveal *exactly when* a count moved, i.e. which checks a known patient affected. To prevent both, the agent reuses the previous noisy value until the count has drifted far enough from the value the noise was drawn for:

- Noise is only re-sampled once a raw count has moved **at least 5** away from the value its current noise was drawn for (the **anchor**). Smaller drifts reuse the previous noisy value verbatim.
- The anchor is **derived from the report history**, not stored separately: because reused noise propagates bit-for-bit, the anchor is simply the raw count of the oldest report in the backwards run of reports sharing the current noisy value.
- This applies identically to the entity totals and to individual check results (matched by check and stratum).
- Only one report can be generated at a time, so the report history used for the derivation is always unambiguous.

**What still leaks, and you should be aware of:**

- **A change event, but only for large drifts and only in terms of timing.** If a count drifts at least 5 away from its anchor between two reports, the reported value visibly changes once. Patient-level changes (adding one or a few records) stay below the threshold and are not observable at all.
- **Larger drifts also bound the error, not add to it.** Because the anchor moves each time fresh noise is drawn, slow day-to-day drift cannot accumulate into an unbounded mismatch between the reported value and the true count; at the same time it cannot be used to average many fresh samples of the same value either.
- Settings changes are not a trigger. If privacy settings (ε, mechanism, threshold) change, an unchanged count keeps its old noisy value rather than being re-sampled under the new parameters — the old protection level simply carries over until the count itself drifts past the threshold.

#### Protection of Entity Totals

The total patient and sample counts that accompany each report are **also protected with noise** — they are never shared as exact or merely rounded values:

- Each report reserves a share of its privacy budget for the two totals: **20% of ε in total (10% per total)**; the remaining **80% of ε** is split among the quality check results as described below. These shares are defined in `DifferentialPrivacyUtil` (`ENTITY_TOTALS_EPSILON_FRACTION` / `CHECK_RESULTS_EPSILON_FRACTION`), so releasing a full report never exceeds its epsilon budget.
- Noise is applied to the raw totals once during report generation (in the `EntityCountStep` of the report pipeline) and the noisy values are persisted, following the reuse rule described above. Re-sending or re-exporting the same report reuses the stored noisy totals.
- The same **low-count suppression** applies to totals as to check counts: noisy counts below the configured minimum threshold are reported as 0.
- Check results are ratios computed as `noisy check count / noisy patient count`, so no exact count — numerator or denominator — is ever used in a shared metric.

Reports generated before this protection existed (or lacking a noisy total for any other reason) do not fall back to weaker rounding: their totals are simply withheld.

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
4. The agent converts this to an obfuscated percentage, using the **noisy total number of patients** (≈ 10,000 after noise addition) as the denominator:
   - `percentage = (noisyCount / noisyTotalPatients) * 100 ≈ (65.27 / 10,000) * 100 ≈ 0.65%`
5. Only this **privacy-preserved percentage (≈ 0.65%)**, together with the noisy totals, is sent to the Data Quality Server – never the raw counts.
6. The central server stores and displays the obfuscated metric. Individual patient presence or absence cannot be inferred due to the added calibrated noise.

Key points:
- Small datasets receive proportionally more effective protection because the noise impact is relatively larger.
- Counts (including the entity totals) are clipped to zero to avoid negative artifacts after noise addition.
- Each quality check and each entity total consumes part of the report's privacy budget (ε); budgeting strategy can be tuned.

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
- **Implementation Support**: Consult the [getting started guide](/user/hands‑on_guide) for secure deployment practices

::: warning Security Best Practices
When deploying the Data Quality Framework:
- Use the latest versions of all components
- Verify Docker image signatures before deployment
- Configure network isolation appropriately
- Follow secure configuration guidelines
- Monitor logs for suspicious activity
:::
