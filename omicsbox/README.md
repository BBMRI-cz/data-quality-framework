# OMICSbox

A safe sandbox environment for evaluating the quality of mounted omics data.
The sandbox is offline and executes user-provided Python scripts against the
mounted dataset without exposing the host system or network.

The omicsbox acts as a **datastore for the Data Quality Agent**: besides
script execution it exposes health, entity-count and entity-metadata
endpoints mirroring the agent's `DataStore` operations.

## API

### `POST /run`

Executes the request body (raw script bytes or multipart `file` field) as a
Python script in a fresh `python -I` subprocess with the data directory as
working directory. Runs are serialized — one script at a time.

Response `200 OK`:

```json
{
  "stdout": "...",
  "stderr": "...",
  "returncode": 0,
  "duration_ms": 42
}
```

Errors: `400` invalid body, `413` payload too large, `500` internal error.

### Script result contract

A quality check script reports its result as **one JSON object on the last
line of stdout**:

```json
{"count": 3, "entityType": "File", "ids": ["a.dcm", "b.dcm"], "error": null}
```

* `returncode != 0`, a timeout, or missing/malformed JSON ⇒ the agent treats
  the check as failed and records `stderr` as the error.
* Output is capped (`MAX_OUTPUT_BYTES`, 1 MiB by default) and truncated
  beyond the cap — keep `ids` reasonably small.

See `examples/example_check.py` for a complete check.

### `GET /health`

Sandbox health probe: data directory mounted/readable, tmp writable.

```json
{"status": "UP", "details": {"data_dir_mounted": true, "data_dir_readable": true, "tmp_writable": true}}
```

### `GET /counts`

Entity counts **from configuration** (the corpus is never scanned):

```json
{"patients": 100, "secondary_entities": 300, "configured": true, "source": "derived"}
```

### `GET /entities/{entityType}/{entityId}`

Read-only metadata for an entity under the data directory. `entityType` is
currently always `file`; `entityId` is the path relative to the data dir
(path traversal is rejected with `400`).

```json
{"name": "slide-001.svs", "entity_id": "cohort-a/slide-001.svs", "type": "file", "size_bytes": 123456, "last_modified": "2026-10-16T10:00:00+00:00"}
```

## Configuration

| Env var | Default | Description |
|---|---|---|
| `TIMEOUT` | `60` | Script execution timeout (seconds) |
| `MAX_REQUEST_BYTES` | `5242880` | Max script request size |
| `MAX_OUTPUT_BYTES` | `1048576` | Max captured stdout/stderr size |
| `OMICSBOX_PATIENT_COUNT` | unset | Number of patients reported by `/counts` |
| `OMICSBOX_IMAGES_PER_PATIENT` | `1` | Derives `secondary_entities = patients × images` |
| `OMICSBOX_SECONDARY_ENTITY_COUNT` | unset | Explicit override for `secondary_entities` |
| `OMICSBOX_DATA_PATH` | `./data` | (compose) Host path mounted read-only at `/sandbox/data` |

## Running

```bash
docker compose up -d --build
curl -s http://localhost:8000/health        # only reachable inside the sandbox network
```

The sandbox network is `internal` (no egress). External clients (e.g. the
agent) must join the network defined in `compose.yaml`.

## Development

Python 3.13+. Create a venv and install dev dependencies:

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt -r requirements-dev.txt
.venv/bin/python -m pytest tests/
```

`requirements.txt` includes the imaging stack used *by user scripts*
(pydicom, wsidicom, …); the service itself only needs Flask.

## Security model

* container filesystem read-only; data mounted read-only; `/tmp` is a small
  `noexec,nosuid` tmpfs
* all capabilities dropped, `no-new-privileges`, memory/CPU limits
* scripts run as an unprivileged user in a fresh `python -I` subprocess
  (isolated mode: no site/user customization beyond preinstalled packages)
* network has no external connectivity

> [!WARNING]
> Under active development.
