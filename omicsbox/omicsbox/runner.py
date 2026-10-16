"""Isolated execution of user-provided Python scripts.

Every run executes in a fresh ``python -I`` subprocess with output captured
to temporary files. Runs are serialized through a lock so the sandbox
executes at most one script at a time; concurrent ``POST /run`` callers
queue behind the lock while other endpoints stay responsive.
"""

from __future__ import annotations

import os
import signal
import subprocess
import sys
import tempfile
import threading
import time
from dataclasses import dataclass

from .config import Settings

TRUNCATION_MARKER = "[output truncated: {} additional characters]"


@dataclass(frozen=True)
class RunResult:
    stdout: str
    stderr: str
    returncode: int
    duration_ms: int


class ScriptRunner:
    """Executes scripts serially with a timeout and output caps."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._lock = threading.Lock()

    def run(self, body: bytes) -> RunResult:
        """Execute *body* as a Python script, waiting if another run is active."""
        with self._lock:
            return self._execute(body)

    def _execute(self, body: bytes) -> RunResult:
        settings = self._settings
        with tempfile.NamedTemporaryFile(mode="wb", suffix=".py", delete=False) as f:
            f.write(body)
            script_path = f.name
        stdout_fd, stdout_path = tempfile.mkstemp(suffix=".out")
        stderr_fd, stderr_path = tempfile.mkstemp(suffix=".err")
        try:
            start = time.monotonic()
            try:
                proc = subprocess.Popen(
                    [sys.executable, "-I", script_path],
                    stdout=stdout_fd,
                    stderr=stderr_fd,
                    stdin=subprocess.DEVNULL,
                    cwd=settings.data_dir,
                    start_new_session=True,
                )
            except OSError as e:
                return RunResult(
                    stdout="",
                    stderr=f"failed to start interpreter: {e}",
                    returncode=-1,
                    duration_ms=round((time.monotonic() - start) * 1000),
                )
            os.close(stdout_fd)
            stdout_fd = -1
            os.close(stderr_fd)
            stderr_fd = -1
            try:
                returncode = proc.wait(timeout=settings.timeout_seconds)
                duration_ms = round((time.monotonic() - start) * 1000)
            except subprocess.TimeoutExpired:
                try:
                    os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
                except ProcessLookupError:
                    pass
                proc.wait()
                return RunResult(
                    stdout="",
                    stderr="execution timed out",
                    returncode=-1,
                    duration_ms=round((time.monotonic() - start) * 1000),
                )
            with open(stdout_path, "rb") as out, open(stderr_path, "rb") as err:
                stdout_data = out.read(settings.max_output_bytes + 1)
                stderr_data = err.read(settings.max_output_bytes + 1)
            return RunResult(
                stdout=_truncate(stdout_data, settings.max_output_bytes),
                stderr=_truncate(stderr_data, settings.max_output_bytes),
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


def _truncate(data: bytes, limit: int) -> str:
    text = data.decode("utf-8", errors="replace")
    if len(text) > limit:
        return text[:limit] + "\n" + TRUNCATION_MARKER.format(len(text) - limit)
    return text
