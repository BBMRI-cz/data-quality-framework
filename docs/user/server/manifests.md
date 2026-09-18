# Manifests

Manifests let you distribute a curated set of quality checks from the Data Quality Server to connected agents.
Each manifest is a named collection of quality checks. When you **publish** a manifest, the server creates an
immutable, cryptographically signed snapshot — a **manifest version** — pinning down exactly which checks (and which
versions of them) belong together.

Agents can browse published manifests, inspect their versions, and download a version to install its checks locally.
Because every version is signed, agents can verify that what they install genuinely comes from your server and has
not been altered.

## Before You Start

Publishing requires a signing key pair on the server. If no keystore is configured, the server starts normally but
publishing a manifest version fails.

Set up the key pair as described in
[Server Configuration — Cryptography](./configuration.md#cryptography), then publish at least one version of the
quality checks you want to distribute (only checks with published versions can be added to a manifest).

## Creating a Manifest

1. Open **Manifests** in the server navigation.
2. Click **Create Manifest** and give it a name (for example, `Core Checks 2026`).
3. Confirm with **Create**.

The new manifest is created without any versions yet; you publish versions from its detail page.

## Publishing a Version

1. Open the manifest from the **Manifests** list.
2. Click **Publish New Version**. If the signing key is missing or invalid, the page shows a warning and publishing
   stays unavailable.
3. Select the quality checks to include. Expand a check's row to pick the exact version to pin.
4. Click **Publish** and confirm.

Things to know about manifest versions:

- **Immutable** — a published version cannot be edited; changes are released as a new version.
- **Sequential numbering** — versions are numbered automatically (1, 2, 3, …) and each number is used only once per
  manifest.
- **Signed** — every version carries a signature and the ID of the key that signed it. Agents verify this signature
  against your public key before installing anything.

Your server's public key is available at the unauthenticated endpoint `GET /api/v1/public-key`. Agent operators need
this key to verify your manifests — share it with them when you onboard a new site. Public keys of well-known
servers can also be listed under [Trusted Central Servers](../manifests.md#trusted-central-servers) in the agent
guide.

## See Also

- [Server Configuration](./configuration.md)
- [Using Manifests on the Agent](../manifests.md)
- [Deployment Guide](../deployment.md)
