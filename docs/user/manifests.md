# Manifests

A manifest is a curated set of quality checks published by a central Data Quality Server. Instead of creating
quality checks by hand, you can download a manifest version from a registered server and install all of its checks
in one step. Every manifest version is cryptographically signed by the server, and your agent verifies that
signature before anything is installed.

## Before You Start

1. **Register the server** — add the central server under **Servers** (see
   [Deployment](./deployment.md) for the full setup).
2. **Add the server's public key** — open the server's detail page and paste its public key into the **Public Key**
   section (**Add Key**). The agent uses this key to verify manifests; without it, downloads are refused. See
   [Trusted Central Servers](#trusted-central-servers) below for where to get the key.

## Browsing Manifests

1. Open **Servers** and pick the server.
2. Click **Manifests** to see everything the server has published, including how many versions each manifest has and
   when the latest one was published.
3. Click a manifest to open its detail page.

Nothing is stored on your agent while browsing — lists and details are fetched live from the server.

## Inspecting a Manifest Version

The detail page always opens on the **latest version**. Use the **Version** picker to switch to any previously
published version. For the selected version you can see:

- **Version details** — when it was published, the ID of the signing key, and its signature. Click
  **View Signed Body** to inspect the exact signed payload.
- **Quality checks** — the checks pinned by this version, with their category, type, and thresholds. Click any check
  to open its full details, including the complete query.

## Downloading a Version

Click **Download v\<n\>** on the manifest detail page to install the selected version:

1. The agent verifies the version's signature against the public key you configured.
2. The manifest and its quality checks are stored locally.
3. The installed checks appear in **Quality Checks** and are picked up by your report runs like any locally created
  check.

Downloading again — the same version or a newer one — **replaces the previous installation** of that manifest
instead of adding duplicates. You always end up with exactly the checks of the version you downloaded last.

### When a Download Is Refused

The agent refuses to install a version (and stores nothing) when:

- **No public key is configured** for the server — add it on the server's detail page and try again.
- **The signature is missing or invalid** — the version may be unsigned or was altered in transit. Contact your
  server operator.
- **A check type is not supported by the agent** — the manifest contains a check the agent cannot execute (for
  example, a legacy check). The error message names the affected checks; ask your server operator to publish a
  version without them.

## Trusted Central Servers

This section lists public keys of central servers operated by trusted partners. Copy the key of your server into the
**Public Key** section of its detail page as described above.

If your server is not listed yet, ask your server operator for the key — every server also exposes it directly at
`GET /api/v1/public-key`.

_No trusted servers published yet. Entries will appear here as servers are onboarded, using the following format:_

**Server name** (server URL) — operated by organization

```text
-----BEGIN PUBLIC KEY-----
...
-----END PUBLIC KEY-----
```

## See Also

- [Publishing Manifests on the Server](./server/manifests.md)
- [Data Quality Agent Configuration](./configuration.md)
