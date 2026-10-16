"""Isolated execution of user-provided Python scripts.

Isolation model (the sandbox is a long-running service, one script at a time):

* every run gets a private ``TemporaryDirectory`` for its script, captured
  output, and a private ``TMPDIR`` scratch area; the directory tree is
  deleted when the run ends, so no run can poison a later one
* after **every** run — not only timeouts — leftover processes are killed
  with ``kill(-1, SIGKILL)`` and orphans are reaped with ``waitpid``. That
  catches background children, ``setsid`` daemons, and anything else the
  script spawned: the container is single-purpose and the server runs as
  PID 1, so every other process it can signal belongs to a script, and the
  server itself is immune (signals to PID 1 from without are ignored by the
  kernel; ``kill(-1)`` excludes the caller and PID 1)
* process cleanup is enabled only when the server is PID 1 (i.e. inside the
  container) so local development and the test suite are never affected
* shared-tmp files a script dropped outside its run directory are purged
  after the run (container mode only)
* resource limits (process count, file size) are applied to every child;
  the container-level ``pids_limit`` is defense in depth
* children receive a minimal environment, never the server environment
"""

from __future__ import annotations

import logging
import os
import resource
import shutil
import signal
import subprocess
import sys
import tempfile
import threading
import time
from dataclasses import dataclass
from typing import Literal

from .config import Settings

log = logging.getLogger(__name__)

TRUNCATION_MARKER = "\n[output truncated]"
TIMEOUT_MARKER = "\n[execution timed out after {}s]"

MAX_SCRIPT_PROCESSES = 128
MAX_SCRIPT_FILE_BYTES = 64 * 1024 * 1024

_RUN_DIR_PREFIX = "omicsbox-run-"
_HEALTH_PROBE_PREFIX = ".healthcheck-"


def in_container() -> bool:
    """True when running as the container's PID 1 (cleanup is safe then)."""
    return os.getpid() == 1


class ScriptBusyError(Exception):
    """The sandbox is busy and the queue wait exceeded the configured timeout."""


RunStatus = Literal["ok", "failed", "timeout", "spawn_error"]


@dataclass(frozen=True)
class RunResult:
    stdout: str
    stderr: str
    returncode: int
    duration_ms: int
    status: RunStatus


def _elapsed_ms(start: float) -> int:
    return round((time.monotonic() - start) * 1000)


def _truncate(data: bytes, limit: int) -> str:
    if len(data) <= limit:
        return data.decode("utf-8", errors="replace")
    return data[:limit].decode("utf-8", errors="replace") + TRUNCATION_MARKER


def _read_capped(path: str, limit: int) -> bytes:
    with open(path, "rb") as f:
        return f.read(limit + 1)


def _kill_process_group(pgid: int) -> None:
    """SIGKILL the script's own process group; safe no-op once it's gone."""
    try:
        os.killpg(pgid, signal.SIGKILL)
    except (ProcessLookupError, PermissionError, OSError):
        pass


def _kill_leftover_processes() -> None:
    """SIGKILL every process we may signal: all script leftovers own the same
    uid as this single-purpose container's processes. ``kill(-1)`` never
    signals the caller, and PID 1 (the server) is immune by definition."""
    try:
        os.kill(-1, signal.SIGKILL)
    except (OSError, PermissionError) as e:
        log.warning("failed to kill leftover processes: %s", e)
    # Reap leftovers (killed above) and any orphans reparented to PID 1 so
    # no zombies accumulate.
    while True:
        try:
            pid, _ = os.waitpid(-1, os.WNOHANG)
        except (ChildProcessError, OSError):
            return
        if pid == 0:
            return


def _purge_tmp_leftovers(keep_dir: str) -> None:
    """Remove files scripts dropped in shared tmp outside their run directory.

    Only entries that could not possibly belong to the server are removed:
    the current run directory is excluded by path and the health probe's
    transient files by name prefix. Container mode only.
    """
    base = tempfile.gettempdir()
    for entry in os.scandir(base):
        if entry.path == keep_dir or entry.name.startswith(_HEALTH_PROBE_PREFIX):
            continue
        try:
            if entry.is_dir(follow_symlinks=False):
                shutil.rmtree(entry.path, ignore_errors=True)
            else:
                os.unlink(entry.path)
        except OSError:
            continue


class ScriptRunner:
    """Executes scripts serially with a timeout, output caps and isolation."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._lock = threading.Lock()

    def run(self, body: bytes) -> RunResult:
        """Execute *body* as a Python script.

        Raises:
            ScriptBusyError: another run is in progress and the queue wait
                exceeded ``run_queue_timeout_seconds``.
        """
        if not self._lock.acquire(timeout=self._settings.run_queue_timeout_seconds):
            raise ScriptBusyError("another script is still running")
        try:
            return self._execute(body)
        finally:
            self._lock.release()

    def _demote_child(self):
        """preexec_fn: wall the child in with resource limits.

        RLIMIT_NPROC caps fork bombs, RLIMIT_FSIZE caps single-file writes
        into the shared tmpfs; both are per-process and need no privilege.
        """

        def preexec() -> None:
            nproc = (MAX_SCRIPT_PROCESSES, MAX_SCRIPT_PROCESSES)
            resource.setrlimit(resource.RLIMIT_NPROC, nproc)
            fsize = (MAX_SCRIPT_FILE_BYTES, MAX_SCRIPT_FILE_BYTES)
            resource.setrlimit(resource.RLIMIT_FSIZE, fsize)

        return preexec

    def _prepare_run_dir(self, run_dir: str, body: bytes) -> tuple[str, str]:
        """Create the script file and private tmp area; return (script, tmp)."""
        script_path = os.path.join(run_dir, "script.py")
        tmp_dir = os.path.join(run_dir, "tmp")
        with open(script_path, "wb") as f:
            f.write(body)
        os.mkdir(tmp_dir)
        return script_path, tmp_dir

    def _script_env(self, tmp_dir: str) -> dict[str, str]:
        """Minimal, deterministic environment; never the server environment."""
        return {
            "PATH": "/usr/local/bin:/usr/bin:/bin",
            "LANG": "C.UTF-8",
            "PYTHONDONTWRITEBYTECODE": "1",
            "TMPDIR": tmp_dir,
            "TEMP": tmp_dir,
            "TMP": tmp_dir,
        }

    def _execute(self, body: bytes) -> RunResult:
        settings = self._settings
        start = time.monotonic()
        timed_out = False
        cleanup = in_container()
        with tempfile.TemporaryDirectory(prefix=_RUN_DIR_PREFIX) as run_dir:
            try:
                script_path, tmp_dir = self._prepare_run_dir(run_dir, body)
            except OSError as e:
                return RunResult(
                    "",
                    f"failed to prepare run directory: {e}",
                    -1,
                    _elapsed_ms(start),
                    "spawn_error",
                )
            stdout_path = os.path.join(run_dir, "stdout")
            stderr_path = os.path.join(run_dir, "stderr")
            with open(stdout_path, "wb") as stdout_f, open(stderr_path, "wb") as stderr_f:
                try:
                    proc = subprocess.Popen(
                        [sys.executable, "-I", script_path],
                        stdout=stdout_f,
                        stderr=stderr_f,
                        stdin=subprocess.DEVNULL,
                        cwd=settings.data_dir,
                        env=self._script_env(tmp_dir),
                        preexec_fn=self._demote_child(),
                        start_new_session=True,  # the child's pgid equals its pid
                    )
                except OSError as e:
                    return RunResult(
                        "",
                        f"failed to launch script process: {e}",
                        -1,
                        _elapsed_ms(start),
                        "spawn_error",
                    )
                try:
                    returncode = proc.wait(timeout=settings.timeout_seconds)
                    status: RunStatus = "ok" if returncode == 0 else "failed"
                except subprocess.TimeoutExpired:
                    timed_out = True
                    returncode = -1
                    status = "timeout"
                _kill_process_group(proc.pid)
                if timed_out:
                    proc.wait()
                if cleanup:
                    _kill_leftover_processes()
                    _purge_tmp_leftovers(keep_dir=run_dir)
                duration_ms = _elapsed_ms(start)
                max_out = settings.max_output_bytes
                stdout = _truncate(_read_capped(stdout_path, max_out), max_out)
                stderr = _truncate(_read_capped(stderr_path, max_out), max_out)
                if timed_out:
                    stderr += TIMEOUT_MARKER.format(settings.timeout_seconds)
                return RunResult(
                    stdout=stdout,
                    stderr=stderr,
                    returncode=returncode,
                    duration_ms=duration_ms,
                    status=status,
                )
