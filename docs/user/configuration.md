# Configuration

The Data Quality Agent can be configured through its web interface or using environment variables. This page provides details on how to use environment variables for configuration.

::: tip
Configuration is typically done during deployment. See the [Deployment Guide](./deployment.md) for basic setup instructions.
:::

## Environment Variables

Environment variables are the primary way to configure the Data Quality Agent, especially in containerized deployments (Docker). They allow you to modify the application's behavior without changing the code or rebuilding the image.

### General Guidance

When deploying with Docker or Docker Compose, you can set environment variables in your `compose.yaml` file:

```yaml
services:
  quality-agent:
    image: ghcr.io/bbmri-cz/data-quality-agent:latest
    environment:
      - ENV_VAR_NAME=value
      - ANOTHER_VAR=another_value
```

### Configuration Overrides

The Data Quality Agent supports a flexible mechanism to override any application setting using environment variables. This is particularly useful for automated deployments or when you need to change settings that are not exposed through specific environment variables.

#### How It Works

Any setting available in the application configuration can be overridden by setting an environment variable with the prefix `APP_SETTING_`. The override system works as follows:

1.  **Prefix**: Use `APP_SETTING_` at the beginning of the environment variable name.
2.  **Normalization**: The system normalizes both the environment variable name (after removing the prefix) and the internal setting names. Normalization involves:
    *   Converting to lowercase.
    *   Removing underscores (`_`), dots (`.`), and dashes (`-`).
3.  **Matching**: If a normalized environment variable matches a normalized setting name, the setting's value is updated with the value from the environment variable.

#### Examples

Suppose you want to override the `fhirUrl` setting.

*   Internal Setting Name: `fhirUrl`
*   Allowed Environment Variable Formats:
    *   `APP_SETTING_FHIRURL=http://new-url.com`
    *   `APP_SETTING_FHIR_URL=http://new-url.com`
    *   `APP_SETTING_fhir_url=http://new-url.com`

All of the above will successfully override the `fhirUrl` setting because they all normalize to `fhirurl`, which matches the normalized setting name.

The same logic applies to other settings like `fhirUsername`.

*   Override `fhirUsername`: `APP_SETTING_FHIR_USERNAME=admin`

::: warning Secure Your Config
Be careful when using environment variables for sensitive information like passwords. Ensure your `compose.yaml` file is secure or use Docker secrets/env files where appropriate.
:::

### Specific Agent Configuration

The following are standard environment variables recognized by the Data Quality Agent:

| Variable                              | Description                                                                                         | Default                                         |
|:--------------------------------------|:----------------------------------------------------------------------------------------------------|:------------------------------------------------|
| `REPORTING_SERVER_URL`                | URL of the central reporting server to send data to.                                                | (Optional)                                      |
| `REPORTING_SERVER_NAME`               | Name of the reporting server for identification.                                                    | `Central Data Quality Server of BBMRI`          |
| `OTEL_METRICS_EXPORT_ENABLED`         | Enables OTLP metrics export (`true`/`false`).                                                       | `false`                                         |
| `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT` | OTLP HTTP metrics endpoint URL.                                                                     | `https://grafana.bbmri-eric.eu/otel/v1/metrics` |
| `OTEL_EXPORTER_OTLP_AUTHORIZATION`    | Optional Authorization header value (for example `Bearer <token>`) sent with OTLP metrics requests. | (Not set)                                       |
You can also define additional labels with `MANAGEMENT_METRICS_TAGS_<LABEL_NAME>`.

Additionally, as described above, you can override any internal application setting using the `APP_SETTING_` prefix. The complete set of application settings supported by the Data Quality Agent is:

| Setting          | Description                                                                                       | Default                     |
|:-----------------|:--------------------------------------------------------------------------------------------------|:----------------------------|
| `fhirUrl`        | The URL of the FHIR server to connect to.                                                         | `http://localhost:8080/fhir` |
| `fhirUsername`   | Username for FHIR server authentication.                                                          | `fhiruser`                  |
| `fhirPassword`   | Password for FHIR server authentication (Base64 encoded).                                         | (Base64 of `fhirpass`)      |
| `epsilon`        | Privacy budget (ε). Must be positive. When using `GAUSSIAN` noise it must be `<= 1.0`.           | `3.0`                       |
| `delta`          | Delta parameter (δ) - probability of privacy failure. Must be positive.                           | `1e-8`                      |
| `minThreshold`   | Minimum threshold for low count suppression.                                                      | `50`                        |
| `noiseMechanism` | Noise mechanism used for differential privacy. One of `LAPLACE` or `GAUSSIAN`.                    | `LAPLACE`                   |
| `databaseType`   | Type of source database the agent connects to. One of `FHIR` or `SQL`.                            | `FHIR`                      |
| `sqlUrl`         | SQL database JDBC URL (used when `databaseType` is `SQL`).                                        | (Empty)                     |
| `sqlUsername`    | SQL database username.                                                                            | (Empty)                     |
| `sqlPassword`    | SQL database password (Base64 encoded).                                                           | (Empty)                     |
| `agentId`        | Agent identifier (read-only, auto-generated at initial setup).                                    | (Auto-generated)            |

::: info Environment Variable Examples
*   Override `fhirUrl`: `APP_SETTING_FHIR_URL=http://fhir.example.com`
*   Override `noiseMechanism`: `APP_SETTING_NOISE_MECHANISM=GAUSSIAN`
*   Override `minThreshold`: `APP_SETTING_MIN_THRESHOLD=100`
*   Override `databaseType`: `APP_SETTING_DATABASE_TYPE=SQL`
:::

By using environment variables, you can fully automate the configuration of your Data Quality Agent during deployment.
