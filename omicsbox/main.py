import os
import subprocess
import sys
import tempfile
import threading
import time
from dataclasses import asdict, dataclass

from flask import Flask, jsonify, request
from werkzeug.serving import make_server

HOST = "0.0.0.0"
PORT = 8000
MAX_SIZE = 5 * 1024 * 1024
TIMEOUT = 60
DATA_DIR = "/sandbox/data"

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = MAX_SIZE

server = None


@dataclass(frozen=True)
class RunResult:
    stdout: str
    stderr: str
    returncode: int
    duration_ms: int


@dataclass(frozen=True)
class ErrorResponse:
    error: str


def _schedule_shutdown():
    if server is not None:
        threading.Thread(target=server.shutdown, daemon=True).start()


def _read_body() -> bytes:
    if request.files:
        file = request.files.get("file")
        if file is None:
            raise ValueError("expected 'file' field in multipart request")
        return file.read()
    return request.get_data()


def _run_script(body: bytes) -> RunResult:
    os.makedirs(DATA_DIR, exist_ok=True)
    with tempfile.NamedTemporaryFile(
        mode="wb", suffix=".py", dir="/tmp", delete=False
    ) as f:
        f.write(body)
        script_path = f.name

    try:
        start = time.time()
        proc = subprocess.run(
            [sys.executable, script_path],
            capture_output=True,
            text=True,
            timeout=TIMEOUT,
            cwd=DATA_DIR,
        )
        duration_ms = round((time.time() - start) * 1000)
        return RunResult(
            stdout=proc.stdout,
            stderr=proc.stderr,
            returncode=proc.returncode,
            duration_ms=duration_ms,
        )
    except subprocess.TimeoutExpired:
        return RunResult(
            stdout="",
            stderr="execution timed out",
            returncode=-1,
            duration_ms=round((time.time() - start) * 1000),
        )
    finally:
        try:
            os.unlink(script_path)
        except OSError:
            pass


@app.route("/run", methods=["POST"])
def run():
    content_length = request.content_length
    if content_length is None or content_length <= 0 or content_length > MAX_SIZE:
        _schedule_shutdown()
        return jsonify(asdict(ErrorResponse("invalid content length"))), 400

    try:
        body = _read_body()
    except ValueError as e:
        _schedule_shutdown()
        return jsonify(asdict(ErrorResponse(str(e)))), 400

    result = _run_script(body)
    _schedule_shutdown()
    return jsonify(asdict(result))


@app.errorhandler(404)
def not_found(_):
    _schedule_shutdown()
    return jsonify(asdict(ErrorResponse("not found"))), 404


@app.errorhandler(413)
def too_large(_):
    _schedule_shutdown()
    return jsonify(asdict(ErrorResponse("payload too large"))), 413


@app.errorhandler(Exception)
def server_error(e):
    _schedule_shutdown()
    return jsonify(asdict(ErrorResponse(str(e)))), 500


def main():
    global server
    print(f"OmicsBox sandbox starting on http://{HOST}:{PORT}")
    server = make_server(HOST, PORT, app)
    server.serve_forever()


if __name__ == "__main__":
    main()
