import os
import signal
import subprocess
import sys
import tempfile
import threading
import time
from dataclasses import asdict, dataclass

from flask import Flask, jsonify, request
from werkzeug.exceptions import InternalServerError
from werkzeug.serving import make_server

HOST = "0.0.0.0"
PORT = 8000
MAX_SIZE = 5 * 1024 * 1024
TIMEOUT = 60
DATA_DIR = "/sandbox/data"
MAX_OUTPUT_SIZE = 1024 * 1024

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


def _truncate_output(data: bytes) -> str:
    text = data.decode("utf-8", errors="replace")
    if len(text) > MAX_OUTPUT_SIZE:
        truncated = text[:MAX_OUTPUT_SIZE]
        return truncated + f"\n[output truncated: {len(text) - MAX_OUTPUT_SIZE} additional characters]"
    return text


def _run_script(body: bytes) -> RunResult:
    os.makedirs(DATA_DIR, exist_ok=True)
    with tempfile.NamedTemporaryFile(
        mode="wb", suffix=".py", dir="/tmp", delete=False
    ) as f:
        f.write(body)
        script_path = f.name

    stdout_fd, stdout_path = tempfile.mkstemp(suffix=".out", dir="/tmp")
    stderr_fd, stderr_path = tempfile.mkstemp(suffix=".err", dir="/tmp")
    try:
        start = time.time()
        proc = subprocess.Popen(
            [sys.executable, "-I", script_path],
            stdout=stdout_fd,
            stderr=stderr_fd,
            stdin=subprocess.DEVNULL,
            cwd=DATA_DIR,
            start_new_session=True,
        )
        os.close(stdout_fd)
        stdout_fd = -1
        os.close(stderr_fd)
        stderr_fd = -1
        try:
            returncode = proc.wait(timeout=TIMEOUT)
            duration_ms = round((time.time() - start) * 1000)
        except subprocess.TimeoutExpired:
            try:
                os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
            except ProcessLookupError:
                pass
            proc.wait()
            duration_ms = round((time.time() - start) * 1000)
            return RunResult(
                stdout="",
                stderr="execution timed out",
                returncode=-1,
                duration_ms=duration_ms,
            )

        with open(stdout_path, "rb") as out, open(stderr_path, "rb") as err:
            stdout_data = out.read(MAX_OUTPUT_SIZE + 1)
            stderr_data = err.read(MAX_OUTPUT_SIZE + 1)

        return RunResult(
            stdout=_truncate_output(stdout_data),
            stderr=_truncate_output(stderr_data),
            returncode=returncode,
            duration_ms=duration_ms,
        )
    finally:
        for fd in (stdout_fd, stderr_fd):
            if fd != -1:
                try:
                    os.close(fd)
                except OSError:
                    pass
        for path in (script_path, stdout_path, stderr_path):
            try:
                os.unlink(path)
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


@app.errorhandler(InternalServerError)
def server_error(_):
    _schedule_shutdown()
    return jsonify(asdict(ErrorResponse("internal server error"))), 500


def main():
    global server
    print(f"OmicsBox sandbox starting on http://{HOST}:{PORT}")
    server = make_server(HOST, PORT, app)
    server.serve_forever()


if __name__ == "__main__":
    main()
