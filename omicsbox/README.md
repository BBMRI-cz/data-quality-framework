# OMICSbox

A safe sandbox environment for evaluating the quality of mounted omics data.
The sandbox is offline and executes user-provided Python scripts against the
mounted dataset without exposing the host system or network.

The omicsbox acts as a **datastore for the Data Quality Agent**: besides
script execution it exposes health, entity-count and entity-metadata
endpoints mirroring the agent's `DataStore` operations.

## API

All errors are JSON: `{"error": "<message>"}`.

### `POST /run`

Executes the request body (raw script bytes or multipart `file` field) as a
Python script in a fresh `python -I` subprocess with the data directory as
working directory. One script at a time — concurrent callers queue for up to
`OMICSBOX_RUN_QUEUE_TIMEOUT` seconds and then get `503`.

Response `200 OK`:

```json
{
  "stdout": "...",
  "stderr": "...",
  "returncode": 0,
  "duration_ms": 42,
  "status": "ok"
}
```

`status` is one of `ok` | `failed` | `timeout` | `spawn_error`. On timeout,
partial output captured before the kill is preserved and stderr carries an
`[execution timed out after Ns]` marker. Truncated output ends with
`[output truncated]`.

Errors: `400` invalid/empty body, `401` unauthorized, `413` payload too
large, `503` sandbox busy.

### Script result contract

A quality check script reports its result as **one JSON object on the last
line of stdout**:

```json
{"count": 3, "entityType": "File", "ids": ["a.dcm", "b.dcm"], "error": null}
```

* `returncode != 0`, a timeout, or missing/malformed JSON ⇒ the agent treats
  the check as failed.
* Output is capped (`OMICSBOX_MAX_OUTPUT_BYTES`, 1 MiB by default) — keep
  `ids` reasonably small.
* Scripts **must not print raw patient data**: stdout is returned to the
  caller verbatim, so only aggregated counts and minimal identifier lists
  belong there. This is a contract, not a technical barrier — see
  [Known limitations](#known-limitations).

See `examples/example_check.py` for a complete check.

### `GET /health`

Sandbox health probe (no authentication): data directory mounted/readable,
tmp writable. `200` when `UP`, **`503` when `DOWN`**.

```json
{"status": "UP", "details": {"data_dir_mounted": true, "data_dir_readable": true, "tmp_writable": true}}
```

### `GET /counts`

Entity counts **from configuration** (the corpus is never scanned):

```json
{"patients": 100, "secondary_entities": 300, "source": "derived"}
```

`patients`/`secondary_entities` are `null` and `source` is `"unconfigured"`
when not configured — distinguish "zero" from "unknown".

### `GET /entities/{entityType}/{entityId}`

Read-only metadata for an entity under the data directory. `entityType` is
currently always `file`; `entityId` is the path relative to the data dir
(path traversal and NUL bytes are rejected with `400`).

```json
{"name": "slide-001.svs", "entity_id": "cohort-a/slide-001.svs", "kind": "file", "size_bytes": 12, "last_modified": "2026-10-16T10:00:00+00:00"}
```

## Authentication

`POST /run`, `GET /counts` and `GET /entities/...` require a bearer token
when `OMICSBOX_API_TOKEN` is set:

```
Authorization: Bearer <token>
```

When unset, the sandbox runs **without authentication** (a warning is logged
on startup). Every submitted script is audit-logged via its SHA-256 hash
together with status, duration and caller address.

## Configuration

| Env var | Default | Description |
|---|---|---|
| `OMICSBOX_DATA_DIR` | `/sandbox/data` | Corpus root inside the container |
| `OMICSBOX_HOST` | `0.0.0.0` | Bind address |
| `OMICSBOX_PORT` | `8000` | Bind port |
| `OMICSBOX_THREADS` | `8` | Worker threads (non-running endpoints stay responsive) |
| `OMICSBOX_API_TOKEN` | unset | Bearer token (unset = auth disabled) |
| `OMICSBOX_TIMEOUT` | `60` | Script execution timeout (seconds) |
| `OMICSBOX_RUN_QUEUE_TIMEOUT` | `120` | Queue wait before `503 busy` |
| `OMICSBOX_MAX_REQUEST_BYTES` | `5242880` | Max script request size |
| `OMICSBOX_MAX_OUTPUT_BYTES` | `1048576` | Max captured stdout/stderr size |
| `OMICSBOX_PATIENT_COUNT` | unset | Number of patients reported by `/counts` |
| `OMICSBOX_IMAGES_PER_PATIENT` | `1` | Derives `secondary_entities = patients × images` |
| `OMICSBOX_SECONDARY_ENTITY_COUNT` | unset | Explicit override for `secondary_entities` |
| `OMICSBOX_DATA_PATH` | `./data` | (compose) Host path mounted read-only at `/sandbox/data` |

### Migration from the one-shot sandbox

* env vars `TIMEOUT`, `MAX_REQUEST_BYTES`, `MAX_OUTPUT_BYTES` are now
  prefixed: `OMICSBOX_TIMEOUT`, `OMICSBOX_MAX_REQUEST_BYTES`,
  `OMICSBOX_MAX_OUTPUT_BYTES`
* the compose mount default moved from `./data/wsi` to `./data` —
  set `OMICSBOX_DATA_PATH` explicitly if needed
* the container no longer shuts down after a request;
  `restart: unless-stopped`, `pids_limit` and a `HEALTHCHECK` are
  configured in `compose.yaml`; the server stays PID 1 for signal immunity

## Running

```bash
docker compose up -d --build
```

The sandbox network is `internal` (no egress) and no port is published —
clients (e.g. the agent) must join the network. To poke it ad-hoc:

```bash
docker run --rm --network omicsbox_isolated curlimages/curl:latest \
  -s -H "Authorization: Bearer $TOKEN" http://omicsbox:8000/health
docker stats omicsbox-sandbox
docker inspect --format "{{.State.Health.Status}}" omicsbox-sandbox
```

## Isolation model

The service is long-running — per-run isolation is enforced per execution:

* each run executes in a fresh `python -I` subprocess; the subprocess gets
  a **private temporary directory** (script, captured output, private
  `TMPDIR`) that is deleted when the run ends
* after **every** run — not only timeouts — leftover processes are killed
  with `kill(-1, SIGKILL)` and reaped via `waitpid`, so background children
  and `setsid` daemons cannot survive into the next run. The container is
  single-purpose and the server is PID 1: every other process it can signal
  belongs to a script, while the server is signal-immune (kernel semantics
  for PID 1; `kill(-1)` also excludes the caller and init). This cleanup
  engages only when running as PID 1, so development/test hosts are safe
* files a script dropped in shared tmp outside its run directory are purged
  after each run (container mode only), so leftovers cannot foul later runs
  and tmpfs pressure never persists across runs
* `RLIMIT_NPROC`, `RLIMIT_FSIZE` per run, plus `pids_limit`/`mem_limit`/
  `cpus` at container level; `noexec,nosuid` tmpfs
* script processes get a minimal environment (`PATH`, `LANG`, `TMPDIR`, …),
  never the server environment
* one execution at a time (queue with `OMICSBOX_RUN_QUEUE_TIMEOUT`),
  bounded worker threads (waitress), audit log of script SHA-256 hashes
* the server code in the image is owned by root and immutable to the
  unprivileged uid the service runs as (uid 9999, `cap_drop: ALL`)

## Known limitations

* **stdout egress**: up to `OMICSBOX_MAX_OUTPUT_BYTES` of stdout leaves the
  sandbox verbatim. The agent only consumes the last-line JSON contract;
  deliberately printing raw data is possible and is currently prevented
  only by contract — enforce it in check review.
* **tmpfs pressure**: a run that fills the 100 MiB tmpfs fails that run;
  the next clean run recovers (per-run cleanup), but mid-run failures from
  a full tmpfs surface as `spawn_error`-like outcomes.
* `/entities` is a generic file-metadata view; DICOM/WSI-specific lookups
  are the scripts' job.

## Development

Python 3.13+. Create a venv and install dev dependencies:

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements-dev.txt
.venv/bin/python -m pytest tests/ --cov=omicsbox
.venv/bin/ruff check && .venv/bin/ruff format --check
```

* `requirements.txt` — pinned service dependencies (runtime)
* `requirements-imaging.txt` — pinned imaging stack available **to user
  scripts** (pydicom, wsidicom, …)
* `requirements-dev.txt` — service deps + pytest/ruff

## Security model

See [Isolation model](#isolation-model). In addition: container filesystem
read-only; data mounted read-only; all capabilities dropped except
`CAP_SETUID`/`CAP_SETGID`; `no-new-privileges`; the network has no external
connectivity.

> [!WARNING]
> Under active development.
