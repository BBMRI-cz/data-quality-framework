"""Environment-driven configuration for the omicsbox sandbox service."""

from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Mapping

DEFAULT_DATA_DIR = "/sandbox/data"
DEFAULT_HOST = "0.0.0.0"
DEFAULT_PORT = 8000
DEFAULT_TIMEOUT_SECONDS = 60
DEFAULT_MAX_REQUEST_BYTES = 5 * 1024 * 1024
DEFAULT_MAX_OUTPUT_BYTES = 1024 * 1024

ENV_TIMEOUT = "TIMEOUT"
ENV_MAX_REQUEST_BYTES = "MAX_REQUEST_BYTES"
ENV_MAX_OUTPUT_BYTES = "MAX_OUTPUT_BYTES"
ENV_PATIENT_COUNT = "OMICSBOX_PATIENT_COUNT"
ENV_IMAGES_PER_PATIENT = "OMICSBOX_IMAGES_PER_PATIENT"
ENV_SECONDARY_ENTITY_COUNT = "OMICSBOX_SECONDARY_ENTITY_COUNT"


@dataclass(frozen=True)
class Settings:
    """Immutable runtime configuration of the sandbox service."""

    data_dir: str
    host: str
    port: int
    timeout_seconds: int
    max_request_bytes: int
    max_output_bytes: int
    patient_count: int | None
    images_per_patient: int
    secondary_entity_count: int | None

    @classmethod
    def from_env(cls, environ: Mapping[str, str] | None = None) -> Settings:
        """Build settings from environment variables, validating all values."""
        env = os.environ if environ is None else environ

        def optional_int(name: str) -> int | None:
            raw = env.get(name)
            if raw is None or raw.strip() == "":
                return None
            return _parse_int(name, raw)

        def int_with_default(name: str, default: int) -> int:
            value = optional_int(name)
            return default if value is None else value

        settings = cls(
            data_dir=DEFAULT_DATA_DIR,
            host=DEFAULT_HOST,
            port=DEFAULT_PORT,
            timeout_seconds=int_with_default(ENV_TIMEOUT, DEFAULT_TIMEOUT_SECONDS),
            max_request_bytes=int_with_default(ENV_MAX_REQUEST_BYTES, DEFAULT_MAX_REQUEST_BYTES),
            max_output_bytes=int_with_default(ENV_MAX_OUTPUT_BYTES, DEFAULT_MAX_OUTPUT_BYTES),
            patient_count=optional_int(ENV_PATIENT_COUNT),
            images_per_patient=int_with_default(ENV_IMAGES_PER_PATIENT, 1),
            secondary_entity_count=optional_int(ENV_SECONDARY_ENTITY_COUNT),
        )
        settings.validate()
        return settings

    def validate(self) -> None:
        """Raise ValueError on the first invalid setting value."""
        if self.timeout_seconds <= 0:
            raise ValueError(f"{ENV_TIMEOUT} must be a positive integer")
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


def _parse_int(name: str, raw: str) -> int:
    try:
        return int(raw, 10)
    except ValueError:
        raise ValueError(f"{name} must be an integer, got {raw!r}") from None
