"""Environment-driven configuration for the omicsbox sandbox service."""

from __future__ import annotations

import os
import re
from collections.abc import Mapping
from dataclasses import dataclass

DEFAULT_DATA_DIR = "/sandbox/data"
DEFAULT_HOST = "0.0.0.0"
DEFAULT_PORT = 8000
DEFAULT_THREADS = 8
DEFAULT_TIMEOUT_SECONDS = 60
DEFAULT_RUN_QUEUE_TIMEOUT_SECONDS = 120
DEFAULT_MAX_REQUEST_BYTES = 5 * 1024 * 1024
DEFAULT_MAX_OUTPUT_BYTES = 1024 * 1024

ENV_DATA_DIR = "OMICSBOX_DATA_DIR"
ENV_HOST = "OMICSBOX_HOST"
ENV_PORT = "OMICSBOX_PORT"
ENV_THREADS = "OMICSBOX_THREADS"
ENV_API_TOKEN = "OMICSBOX_API_TOKEN"
ENV_TIMEOUT = "OMICSBOX_TIMEOUT"
ENV_RUN_QUEUE_TIMEOUT = "OMICSBOX_RUN_QUEUE_TIMEOUT"
ENV_MAX_REQUEST_BYTES = "OMICSBOX_MAX_REQUEST_BYTES"
ENV_MAX_OUTPUT_BYTES = "OMICSBOX_MAX_OUTPUT_BYTES"
ENV_PATIENT_COUNT = "OMICSBOX_PATIENT_COUNT"
ENV_IMAGES_PER_PATIENT = "OMICSBOX_IMAGES_PER_PATIENT"
ENV_SECONDARY_ENTITY_COUNT = "OMICSBOX_SECONDARY_ENTITY_COUNT"
_NON_NEGATIVE_INT = re.compile(r"[0-9]+")


@dataclass(frozen=True)
class Settings:
    """Immutable runtime configuration of the sandbox service."""

    data_dir: str
    host: str
    port: int
    threads: int
    api_token: str | None
    timeout_seconds: int
    run_queue_timeout_seconds: int
    max_request_bytes: int
    max_output_bytes: int
    patient_count: int | None
    images_per_patient: int
    secondary_entity_count: int | None

    def __post_init__(self) -> None:
        """Validate all values; raises ValueError on the first invalid one."""
        if not self.data_dir:
            raise ValueError(f"{ENV_DATA_DIR} must not be empty")
        if not self.host:
            raise ValueError(f"{ENV_HOST} must not be empty")
        if not 1 <= self.port <= 65535:
            raise ValueError(f"{ENV_PORT} must be between 1 and 65535")
        if self.threads < 1:
            raise ValueError(f"{ENV_THREADS} must be a positive integer")
        if self.timeout_seconds <= 0:
            raise ValueError(f"{ENV_TIMEOUT} must be a positive integer")
        if self.run_queue_timeout_seconds < 0:
            raise ValueError(f"{ENV_RUN_QUEUE_TIMEOUT} must be a non-negative integer")
        if self.max_request_bytes <= 0:
            raise ValueError(f"{ENV_MAX_REQUEST_BYTES} must be a positive integer")
        if self.max_output_bytes <= 0:
            raise ValueError(f"{ENV_MAX_OUTPUT_BYTES} must be a positive integer")
        if self.patient_count is not None and self.patient_count < 0:
            raise ValueError(f"{ENV_PATIENT_COUNT} must be a non-negative integer")
        if self.images_per_patient < 0:
            raise ValueError(f"{ENV_IMAGES_PER_PATIENT} must be a non-negative integer")
        if self.secondary_entity_count is not None and self.secondary_entity_count < 0:
            raise ValueError(f"{ENV_SECONDARY_ENTITY_COUNT} must be a non-negative integer")

    @classmethod
    def from_env(cls, environ: Mapping[str, str] | None = None) -> Settings:
        """Build settings from environment variables, validating all values."""
        env = os.environ if environ is None else environ

        def optional_int(name: str) -> int | None:
            raw = env.get(name)
            if raw is None or raw.strip() == "":
                return None
            if not _NON_NEGATIVE_INT.fullmatch(raw.strip()):
                raise ValueError(f"{name} must be a non-negative integer, got {raw!r}")
            return int(raw.strip())

        def int_with_default(name: str, default: int) -> int:
            value = optional_int(name)
            return default if value is None else value

        api_token = env.get(ENV_API_TOKEN)
        if api_token is not None and not api_token.strip():
            api_token = None

        return cls(
            data_dir=env.get(ENV_DATA_DIR) or DEFAULT_DATA_DIR,
            host=env.get(ENV_HOST) or DEFAULT_HOST,
            port=int_with_default(ENV_PORT, DEFAULT_PORT),
            threads=int_with_default(ENV_THREADS, DEFAULT_THREADS),
            api_token=api_token,
            timeout_seconds=int_with_default(ENV_TIMEOUT, DEFAULT_TIMEOUT_SECONDS),
            run_queue_timeout_seconds=int_with_default(
                ENV_RUN_QUEUE_TIMEOUT, DEFAULT_RUN_QUEUE_TIMEOUT_SECONDS
            ),
            max_request_bytes=int_with_default(ENV_MAX_REQUEST_BYTES, DEFAULT_MAX_REQUEST_BYTES),
            max_output_bytes=int_with_default(ENV_MAX_OUTPUT_BYTES, DEFAULT_MAX_OUTPUT_BYTES),
            patient_count=optional_int(ENV_PATIENT_COUNT),
            images_per_patient=int_with_default(ENV_IMAGES_PER_PATIENT, 1),
            secondary_entity_count=optional_int(ENV_SECONDARY_ENTITY_COUNT),
        )
