#!/usr/bin/env python3
"""Entry point of the omicsbox sandbox service."""

import sys

from werkzeug.serving import make_server

from omicsbox.config import Settings
from omicsbox.counts import resolve_counts
from omicsbox.server import create_app


def main() -> None:
    try:
        settings = Settings.from_env()
    except ValueError as e:
        print(f"omicsbox: invalid configuration: {e}", file=sys.stderr)
        sys.exit(1)
    counts = resolve_counts(settings)
    print(
        f"OmicsBox sandbox starting on http://{settings.host}:{settings.port} "
        f"(patients={counts.patients}, secondary={counts.secondary_entities}, "
        f"source={counts.source})"
    )
    server = make_server(settings.host, settings.port, create_app(settings), threaded=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
