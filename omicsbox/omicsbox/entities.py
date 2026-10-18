"""Read-only entity lookup against the mounted data directory.

The corpus layout is generic: an entity is identified by its path relative
to the data directory. Only the ``file`` entity type exists; it describes
plain files and directories.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Literal


@dataclass(frozen=True)
class EntityMetadata:
    name: str
    entity_id: str
    kind: Literal["file", "directory"]
    size_bytes: int
    last_modified: str


class EntityError(Exception):
    """Base class for entity lookup failures."""


class EntityLookupError(EntityError):
    """The request cannot be fulfilled (unsupported type, invalid path)."""


class EntityNotFoundError(EntityError):
    """The referenced entity does not exist in the data directory."""


def get_entity_metadata(data_dir: str, entity_type: str, entity_id: str) -> EntityMetadata:
    """Return metadata for the file or directory at ``entity_id`` under ``data_dir``.

    Raises:
        EntityLookupError: unsupported entity type, invalid id, or a path
            that escapes the data directory (path traversal guard).
        EntityNotFoundError: the entity does not exist.
    """
    if entity_type != "file":
        raise EntityLookupError(f"unsupported entity type: {entity_type!r}")
    if not entity_id or entity_id.startswith(("/", "\\")):
        raise EntityLookupError("entity id must be a non-empty relative path")
    if "\x00" in entity_id:
        raise EntityLookupError("entity id contains invalid characters")
    normalized = os.path.normpath(entity_id)
    if normalized in (".", ""):
        raise EntityLookupError("entity id must reference a file or directory")
    base = os.path.realpath(data_dir)
    target = os.path.realpath(os.path.join(base, normalized))
    if target != base and not target.startswith(base + os.sep):
        raise EntityLookupError("entity id escapes the data directory")
    try:
        stat = os.stat(target)
    except FileNotFoundError:
        raise EntityNotFoundError(f"no such file: {entity_id}") from None
    except (OSError, ValueError) as e:
        raise EntityLookupError(f"invalid entity id: {e}") from None
    if not os.path.isfile(target) and not os.path.isdir(target):
        raise EntityNotFoundError(f"no such file: {entity_id}")
    kind: Literal["file", "directory"] = "directory" if os.path.isdir(target) else "file"
    return EntityMetadata(
        name=os.path.basename(target),
        entity_id=normalized,
        kind=kind,
        size_bytes=stat.st_size if kind == "file" else 0,
        last_modified=datetime.fromtimestamp(stat.st_mtime, tz=UTC).isoformat(),
    )
