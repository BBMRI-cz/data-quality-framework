"""Configured entity counts reported by the sandbox.

Counts are pure configuration: the omicsbox never scans the mounted corpus
to derive them. Both values are optional; an unset value is reported as
``null`` so consumers can distinguish "zero" from "unknown".
"""

from __future__ import annotations

from dataclasses import dataclass

from .config import Settings


@dataclass(frozen=True)
class Counts:
    """Entity counts as exposed by ``GET /counts``."""

    patients: int | None
    secondary_entities: int | None


def resolve_counts(settings: Settings) -> Counts:
    """Resolve the configured entity counts from settings."""
    return Counts(
        patients=settings.patient_count,
        secondary_entities=settings.secondary_entity_count,
    )
