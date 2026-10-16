"""HTTP API of the omicsbox sandbox service.

Endpoints:

* ``POST /run`` — execute the request body as a Python script.
* ``GET /health`` — sandbox health probe.
* ``GET /counts`` — configured entity counts.
* ``GET /entities/<entity_type>/<entity_id>`` — read-only entity metadata.
"""

from __future__ import annotations

from dataclasses import asdict

from flask import Flask, jsonify, request
from werkzeug.exceptions import InternalServerError

from .config import Settings
from .counts import resolve_counts
from .entities import EntityLookupError, EntityNotFoundError, get_entity_metadata
from .health import check_health
from .runner import ScriptRunner


def _error(message: str, status: int):
    return jsonify({"error": message}), status


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


def create_app(settings: Settings, runner: ScriptRunner | None = None) -> Flask:
    app = Flask(__name__)
    app.config["MAX_CONTENT_LENGTH"] = settings.max_request_bytes
    runner = runner or ScriptRunner(settings)

    @app.route("/run", methods=["POST"])
    def run():
        content_length = request.content_length
        if (
            content_length is None
            or content_length <= 0
            or content_length > settings.max_request_bytes
        ):
            return _error("invalid content length", 400)
        try:
            body = _read_script_body()
        except ValueError as e:
            return _error(str(e), 400)
        return jsonify(asdict(runner.run(body)))

    @app.get("/health")
    def health():
        status = check_health(settings)
        return jsonify({"status": status.status, "details": status.details})

    @app.get("/counts")
    def counts():
        return jsonify(asdict(resolve_counts(settings)))

    @app.get("/entities/<entity_type>/<path:entity_id>")
    def entity(entity_type: str, entity_id: str):
        try:
            metadata = get_entity_metadata(settings.data_dir, entity_type, entity_id)
        except EntityLookupError as e:
            return _error(str(e), 400)
        except EntityNotFoundError as e:
            return _error(str(e), 404)
        return jsonify(asdict(metadata))

    @app.errorhandler(404)
    def not_found(_):
        return _error("not found", 404)

    @app.errorhandler(413)
    def too_large(_):
        return _error("payload too large", 413)

    @app.errorhandler(InternalServerError)
    def internal_error(_):
        return _error("internal server error", 500)

    return app
