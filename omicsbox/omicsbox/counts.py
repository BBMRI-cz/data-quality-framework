"""Configured entity counts reported by the sandbox.

Counts are pure configuration: the omicsbox never scans the mounted corpus
to derive them. ``secondary_entities`` can be set explicitly or derived from
``patient_count * images_per_patient``.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Literal

from .config import Settings


@dataclass(frozen=True)
class Counts:
    """Entity counts as exposed by ``GET /counts``.

    ``patients``/``secondary_entities`` are ``None`` when not configured so
    consumers can distinguish "zero" from "unknown".
    """

    patients: int | None
    secondary_entities: int | None
    source: Literal["env", "derived", "unconfigured"]


def resolve_counts(settings: Settings) -> Counts:
    """Resolve the configured entity counts from settings."""
    patients = settings.patient_count
    if settings.secondary_entity_count is not None:
        secondary = settings.secondary_entity_count
        source: Literal["env", "derived", "unconfigured"] = "env"
    elif patients is not None:
        secondary = patients * settings.images_per_patient
        source = "derived"
    else:
        secondary = None
        source = "unconfigured"
    return Counts(patients=patients, secondary_entities=secondary, source=source)
