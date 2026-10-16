# OMICSbox

A safe, offline sandbox for evaluating the quality of mounted omics data.
It executes user-provided Python scripts against a read-only dataset and —
as a datastore for the Data Quality Agent — also exposes health, entity-count
and entity-metadata endpoints.

## Running

Requires Docker with Compose:

```bash
docker compose up -d --build
```

Put your omics data into `./data` (or point `OMICSBOX_DATA_PATH` at it).
Configuration happens via environment variables — see `compose.yaml` for the
available options (auth token, entity counts, timeouts, caps).

## Testing

Requires Python 3.13+:

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements-dev.txt
.venv/bin/python -m pytest tests/
```

## Documentation

Comprehensive documentation (API reference, script result contract,
isolation model, configuration) lives in the project `docs/`.

> [!WARNING]
> Under active development.
