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

> Raw data never leaves the site. The agent sends only aggregated, noisy quality metrics to the central server.

### What the server receives

For every report, the agent sends:

- for each quality check, the share of patients it affects (for example 0.07 for 7 %), rounded to two decimals;
- the number of patients and the number of samples at the site.

All of these are computed from counts to which the agent has added random noise. Patient identifiers, records and
exact counts stay in the agent.

### How differential privacy protects patients

Before anything is sent, the agent adds a random amount to each count, so the published number is slightly off in an
unpredictable direction. The noise is sized so that whether one particular patient is in the data makes only a
limited, measurable difference to what the server sees. This approach is called *differential privacy*. Its strength
is set by the privacy budget ε (epsilon): a smaller ε means more noise and stronger protection, a larger ε means more
accurate results and weaker protection.

Two more safeguards work on top of the noise:

- **Small counts are hidden.** A noisy count below the minimum threshold (default 50) is reported as 0.
- **Unchanged numbers are not re-randomised.** Reports run every day. If each report drew new noise, someone could
  average many reports and cancel the noise out. So the agent resends its previous noisy value until the real count
  has moved by at least 5 patients (100 for the sample total), and only then draws new noise.

### Example

A site has 10 000 patients, and a check finds 67 with an invalid gender value.

1. The agent adds noise to 67. The difference is typically around 7, so it might use 61.4.
2. It also adds noise to the patient total, for example 10 003.
3. It sends 61.4 / 10 003 = 0.006, rounded to **0.01**, together with the noisy totals.

The dashboard shows "about 1 % of patients at this site have an invalid gender value". It does not show 67, and it
does not show who. As long as the count stays within 5 of 67, the following reports send exactly the same value.

### Limits you should know about

Differential privacy gives a mathematical guarantee for one report. A system that reports daily for years on real
biobank data needs some practical compromises to stay useful. These are the ones we made:

- **The guarantee is per report, not lifelong.** Each time a count moves enough to get new noise, a little more privacy
  is spent, and there is no yearly limit. A site whose data changes every day accumulates more privacy loss than a
  site whose data is stable.
- **Changes are visible.** Because unchanged values repeat exactly, a change of 5 or more patients (100 or more
  samples) shows up in the next report. Smaller changes usually stay hidden, but roughly one time in 25 a single
  patient's change is the one that crosses the line. Someone who knows that a specific person was added on a given
  day could then see which checks changed.
- **Some counts can move by more than one per patient.** The protection assumes that one patient changes each count by
  at most one. That is not true for the duplicate-identifier and invalid-ICD-10 checks, where one patient with many
  records can move the count by 20 or more, or for the sample total, which assumes at most 20 samples per patient.
  Patients outside these assumptions are less protected. If you write your own checks, make them count patients,
  each at most once.
- **The checks themselves must be trustworthy.** Noise protects the result of a check, not its intent. A check written
  to single out one person (for example "count all patients if patient X has diagnosis Y") can defeat the protection.
  Install checks only from sources you trust, and review your own.
- **Small sites get less useful results.** The noise has the same size everywhere, so it matters more when counts are
  small. If the noisy patient total itself is below the threshold, every check in that report is shown as 0. Read
  that as "too small to report", not as "no problems found".
- **Settings changes apply gradually.** A count that does not move keeps its old noisy value, and with it the old
  settings, until it moves. Values that a raised threshold would hide are re-randomised straight away.
- **Rounding hides more than the guarantee promises.** Shares are rounded to whole percent, which at large sites hides
  most single-patient changes. That is a side effect, not something the guarantee relies on.

For stronger protection, lower ε, raise the threshold, and keep only the checks you need. Each of these costs
accuracy, and every site has to find its own balance. [Privacy Configuration](./privacy-configuration.md) shows what
the settings mean in practice.

The theory behind differential privacy is described in Dwork and Roth,
[*The Algorithmic Foundations of Differential Privacy*](https://www.cis.upenn.edu/~aaroth/Papers/privacybook.pdf).
The implementation is part of the agent's open source code and is covered by automated tests.

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
