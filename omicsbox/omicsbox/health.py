"""Health probing for the sandbox service."""

from __future__ import annotations

import os
import tempfile
from dataclasses import dataclass
from typing import Literal

from .config import Settings


@dataclass(frozen=True)
class HealthStatus:
    status: Literal["UP", "DOWN"]
    details: dict[str, bool]


def check_health(settings: Settings) -> HealthStatus:
    """Verify the sandbox prerequisites: data dir mounted/readable, tmp writable."""
    data_dir_mounted = os.path.isdir(settings.data_dir)
    data_dir_readable = data_dir_mounted and os.access(settings.data_dir, os.R_OK | os.X_OK)
    tmp_writable = _probe_tmp_writable()
    up = data_dir_mounted and data_dir_readable and tmp_writable
    return HealthStatus(
        status="UP" if up else "DOWN",
        details={
            "data_dir_mounted": data_dir_mounted,
            "data_dir_readable": data_dir_readable,
            "tmp_writable": tmp_writable,
        },
    )


def _probe_tmp_writable() -> bool:
    try:
        fd, path = tempfile.mkstemp(prefix=".healthcheck-")
    except OSError:
        return False
    try:
        os.close(fd)
    finally:
        try:
            os.unlink(path)
        except OSError:
            pass
    return True
