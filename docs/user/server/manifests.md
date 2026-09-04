# Manifests

Manifests are named collections of quality check versions maintained by the Data Quality Server. A manifest defines
exactly which quality checks (and which versions of them) belong together. Publishing a manifest creates an
**immutable, cryptographically signed snapshot** — a *manifest version* — that agents and other clients can download
and verify before executing the checks it lists.

## What Publishing Does

Each manifest starts as simple metadata. When you **publish** a manifest, the server:

1. Resolves the requested SHA-256 hashes to published quality check versions stored in its database.
2. Builds a JSON **body** containing the manifest ID, a generation timestamp, and the list of checks
   (check ID, check version, and hash for each entry).
3. **Signs** the body with the server's private key (ECDSA; `SHA256withECDSA` for the recommended `secp256r1` key).
4. Stores the body together with its Base64-encoded signature and the ID of the signing key.

The resulting manifest version looks like this:

```json
{
  "id": 1,
  "version": 1,
  "generatedAt": "2026-08-13T10:00:00Z",
  "body": {
    "manifest_id": 1,
    "generated_at": "2026-08-13T10:00:00Z",
    "checks": [
      {"id": "3", "version": 2, "hash": "5f3c9a..."}
    ]
  },
  "signature": "MEUCIBd...",
  "keyId": "central-signing"
}
```

Key properties of manifest versions:

- **Immutable** — once published, a version cannot be changed. Updates are published as a new version.
- **Monotonically numbered** — if no explicit version number is given, the server assigns the next number
  (latest version + 1, starting at 1). A version number can only be used once per manifest.
- **Independently verifiable** — the signature is computed over the JSON body only. Clients can fetch the server's
  public key from the unauthenticated `GET /api/v1/public-key` endpoint and verify authenticity and integrity of the
  body offline.

## Prerequisites: Signing Key Pair

Publishing requires a private/public key pair on the server. If no keystore is configured, the server still starts,
but publishing a manifest version fails because the body cannot be signed.

Generate the key pair with `keytool` as described in the
[Cryptography section of the Server Configuration](./configuration.md#generating-the-key-pair), then point the server
at the keystore via environment variables (see [Server Configuration](./configuration.md#cryptography) for the full
reference). The keystore alias becomes the `keyId` embedded in every published manifest version.

## See Also

- [Server Configuration](./configuration.md)
- [Deployment Guide](../deployment.md)
