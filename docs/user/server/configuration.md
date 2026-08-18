# Configuration

The Data Quality Server is configured primarily through environment variables, especially in containerized deployments
(Docker). This page describes the available configuration options for the central Data Quality Server.

::: tip Configuration is typically done during deployment. See the [Deployment Guide](./deployment.md) for basic setup
instructions.
:::

## Environment Variables

Environment variables are the primary way to configure the Data Quality Server. They allow you to modify the
application's behavior without changing the code or rebuilding the image.

When deploying with Docker or Docker Compose, you can set environment variables in your `compose.yaml` file:

```yaml
services:
  quality-server:
    image: ghcr.io/bbmri-cz/data-quality-server:latest
    environment:
      - ENV_VAR_NAME=value
      - ANOTHER_VAR=another_value
```

### Database Configuration

The server persists its data in a PostgreSQL database. Configure the connection using Spring Datasource properties:

| Variable                              | Description                          | Default                                           |
|:--------------------------------------|:-------------------------------------|:--------------------------------------------------|
| `SPRING_DATASOURCE_URL`               | JDBC URL of the PostgreSQL database. | `jdbc:postgresql://localhost:5432/quality_server` |
| `SPRING_DATASOURCE_USERNAME`          | Database username.                   | `quality`                                         |
| `SPRING_DATASOURCE_PASSWORD`          | Database password.                   | `quality`                                         |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | JDBC driver class name.              | `org.postgresql.Driver`                           |

## Cryptography

The server uses a private/public key pair (from a PKCS#12 keystore). The
keys are loaded from a keystore on the filesystem.

| Variable                      | Description                                              | Default                     |
|:------------------------------|:---------------------------------------------------------|:----------------------------|
| `CENTRAL_SERVER_KEY_PATH`     | Path to the PKCS#12 keystore on the server's filesystem. | (Empty – crypto not set up) |
| `CENTRAL_SERVER_KEY_PASSWORD` | Password of the keystore / key entry.                    | (Empty)                     |
| `CENTRAL_SERVER_KEY_ALIAS`    | Alias of the key within the keystore.                    | `central-server-key`        |

::: warning Crypto Not Configured
If `CENTRAL_SERVER_KEY_PATH` is not set (or blank), the server starts successfully but cryptographic functionality
(e.g., key signing) is not enabled. A warning is logged at startup. Configure a valid keystore path if you need
signature functionality, for example the public key endpoint `/api/v1/public-key`.
:::

The key path and password map to the underlying Spring properties `app.crypto.key-path`, `app.crypto.key-password`, and
`app.crypto.key-alias`, which can also be set directly.

### Generating the Key Pair

Before enabling crypto, generate a key pair on the host using the `keytool` utility (bundled with the JDK). The command
below creates a PKCS#12 keystore with an EC key and writes it to `./secrets/signing.p12`:

```bash
keytool -genkeypair \
  -alias central-signing \
  -keyalg EC -groupname secp256r1 \
  -storetype PKCS12 \
  -keystore ./secrets/signing.p12 \
  -storepass "$KEYSTORE_PASSWORD" \
  -validity 730
```

Replace `$KEYSTORE_PASSWORD` with a strong password and keep it secret. Note that `keytool` will prompt for a
distinguished name (DN) for the key; you can supply a minimal value such as `CN=Data Quality Server`.

### Loading the Key Pair in Docker

To make the keystore available to the server container, mount the `secrets` directory as a read-only volume and provide
the keystore path and password as environment variables:

```yaml
services:
  quality-server:
    image: ghcr.io/bbmri-cz/data-quality-server:latest
    volumes:
      - ./secrets:/run/secrets:ro
    environment:
      - CENTRAL_SERVER_KEY_PATH=/run/secrets/signing.p12
      - CENTRAL_SERVER_KEY_PASSWORD=${KEYSTORE_PASSWORD}
      - CENTRAL_SERVER_KEY_ALIAS=central-signing
```

::: warning Keep the Password Secret
The keystore password is sensitive. Do not commit it to the repository or place it directly in `compose.yaml`. Use a
secret reference, an env file, or a secrets manager to inject it at runtime.
:::

The alias in `CENTRAL_SERVER_KEY_ALIAS` must match the alias used when generating the keystore (`central-signing` in the
example above). The keystore file should match the key type expected by the server (an EC key on curve `secp256r1`).

## See Also

- [Deployment Guide](../deployment.md)
- [OIDC Configuration](../oidc-configuration.md)
