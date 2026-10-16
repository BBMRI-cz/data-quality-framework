"""HTTP API of the omicsbox sandbox service.

Endpoints:

* ``POST /run`` — execute the request body as a Python script (auth required).
* ``GET /health`` — sandbox health probe (unauthenticated).
* ``GET /counts`` — configured entity counts (auth required).
* ``GET /entities/<entity_type>/<entity_id>`` — entity metadata (auth required).

All errors are JSON: ``{"error": "<message>"}`` with an appropriate status.
"""

from __future__ import annotations

import hashlib
import hmac
import logging
import time
from dataclasses import asdict
from typing import Any

from flask import Flask, Response, jsonify, request
from werkzeug.exceptions import HTTPException

from .config import Settings
from .counts import resolve_counts
from .entities import EntityLookupError, EntityNotFoundError, get_entity_metadata
from .health import check_health
from .runner import ScriptBusyError, ScriptRunner

log = logging.getLogger(__name__)

_ERROR_MESSAGES = {
    400: "bad request",
    401: "unauthorized",
    403: "forbidden",
    404: "not found",
    405: "method not allowed",
    413: "payload too large",
    429: "too many requests",
    500: "internal server error",
    503: "service unavailable",
}


def _error(message: str, status: int) -> tuple[Response, int]:
    response: Response = jsonify({"error": message})
    return response, status


def _read_script_body() -> bytes:
    # Only touch request.files for true multipart requests: accessing it (or
    # request.form) on other content types consumes the WSGI input stream
    # during form parsing and request.get_data() would return an empty body.
    if request.mimetype == "multipart/form-data":
        file = request.files.get("file")
        if file is None:
            raise ValueError("expected 'file' field in multipart request")
        return file.read()
    return request.get_data()


def _authenticate(settings: Settings) -> tuple[Response, int] | None:
    """Bearer-token check; None means allowed."""
    token = settings.api_token
    if token is None:
        return None
    provided = request.headers.get("Authorization", "")
    if provided.startswith("Bearer ") and hmac.compare_digest(provided[7:], token):
        return None
    return _error("unauthorized", 401)


def create_app(settings: Settings, runner: ScriptRunner | None = None) -> Flask:
    """Application factory for the sandbox service."""
    app = Flask(__name__)
    app.config["MAX_CONTENT_LENGTH"] = settings.max_request_bytes
    runner = runner or ScriptRunner(settings)

    @app.errorhandler(HTTPException)
    def handle_http_error(e: HTTPException) -> tuple[Response, int]:
        code = e.code or 500
        return _error(_ERROR_MESSAGES.get(code, e.description or e.name), code)

    @app.before_request
    def require_auth() -> tuple[Response, int] | None:
        if request.endpoint == "health":
            return None
        return _authenticate(settings)

    @app.post("/run")
    def run() -> Any:
        content_length = request.content_length
        if content_length is None or content_length <= 0:
            return _error("invalid content length", 400)
        if content_length > settings.max_request_bytes:
            return _error("payload too large", 413)
        try:
            body = _read_script_body()
        except ValueError as e:
            return _error(str(e), 400)
        if not body:
            return _error("empty script body", 400)
        script_hash = hashlib.sha256(body).hexdigest()
        start = time.monotonic()
        try:
            result = runner.run(body)
        except ScriptBusyError:
            log.info("script rejected (busy) sha256=%s addr=%s", script_hash, request.remote_addr)
            return _error("sandbox busy, try again later", 503)
        log.info(
            "script completed sha256=%s status=%s rc=%d duration_ms=%d total_ms=%d addr=%s",
            script_hash,
            result.status,
            result.returncode,
            result.duration_ms,
            round((time.monotonic() - start) * 1000),
            request.remote_addr,
        )
        return jsonify(asdict(result))

    @app.get("/health")
    def health() -> tuple[Response, int]:
        status = check_health(settings)
        code = 200 if status.status == "UP" else 503
        return jsonify({"status": status.status, "details": status.details}), code

    @app.get("/counts")
    def counts() -> Response:
        return jsonify(asdict(resolve_counts(settings)))

    @app.get("/entities/<entity_type>/<path:entity_id>")
    def entity(entity_type: str, entity_id: str) -> Any:
        try:
            metadata = get_entity_metadata(settings.data_dir, entity_type, entity_id)
        except EntityLookupError as e:
            return _error(str(e), 400)
        except EntityNotFoundError as e:
            return _error(str(e), 404)
        return jsonify(asdict(metadata))

    return app
