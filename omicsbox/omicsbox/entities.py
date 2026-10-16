"""Read-only entity lookup against the mounted data directory.

The corpus layout is generic: an entity is identified by its path relative
to the data directory. Only the ``file`` entity type exists; it describes
plain files and directories.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from datetime import datetime, timezone


@dataclass(frozen=True)
class EntityMetadata:
    name: str
    entity_id: str
    type: str  # "file" | "directory"
    size_bytes: int
    last_modified: str


class EntityLookupError(Exception):
    """The request cannot be fulfilled (unsupported type, invalid path)."""


class EntityNotFoundError(Exception):
    """The referenced entity does not exist in the data directory."""


def get_entity_metadata(data_dir: str, entity_type: str, entity_id: str) -> EntityMetadata:
    """Return metadata for the file or directory at ``entity_id`` under ``data_dir``.

    Raises:
        EntityLookupError: unsupported entity type or a path that escapes
            the data directory (path traversal guard).
        EntityNotFoundError: the entity does not exist.
    """
    if entity_type != "file":
        raise EntityLookupError(f"unsupported entity type: {entity_type!r}")
    if not entity_id or entity_id.startswith(("/", "\\")):
        raise EntityLookupError("entity id must be a non-empty relative path")
    base = os.path.realpath(data_dir)
    target = os.path.realpath(os.path.join(base, entity_id))
    if target != base and not target.startswith(base + os.sep):
        raise EntityLookupError("entity id escapes the data directory")
    if not os.path.isfile(target) and not os.path.isdir(target):
        raise EntityNotFoundError(f"no such file: {entity_id}")
    stat = os.stat(target)
    kind = "directory" if os.path.isdir(target) else "file"
    return EntityMetadata(
        name=os.path.basename(target),
        entity_id=os.path.relpath(target, base),
        type=kind,
        size_bytes=stat.st_size if kind == "file" else 0,
        last_modified=datetime.fromtimestamp(stat.st_mtime, tz=timezone.utc).isoformat(),
    )
