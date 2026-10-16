#!/usr/bin/env python3
"""Entry point of the omicsbox sandbox service."""

import logging
import signal
import sys

from waitress import serve

from omicsbox.config import HOST, PORT, Settings
from omicsbox.counts import resolve_counts
from omicsbox.server import create_app

log = logging.getLogger("omicsbox")

_PR_SET_DUMPABLE = 4  # from <sys/prctl.h>


def _install_signal_handlers() -> None:
    """Exit cleanly on SIGTERM so `docker stop` doesn't wait out the grace period.

    The server is the container's PID 1; the kernel otherwise ignores SIGTERM
    sent to it (PID 1 only receives signals it handles).
    """

    def on_term(_signum, _frame):
        log.info("received termination signal, shutting down")
        sys.exit(0)

    signal.signal(signal.SIGTERM, on_term)


def _hide_process_secrets() -> None:
    """Mark this process non-dumpable (Linux); best-effort no-op elsewhere.

    Scripts run under the same uid as the server and could otherwise read
    its environment — including OMICSBOX_API_TOKEN — via /proc/<pid>/environ.
    """
    import ctypes

    try:
        libc = ctypes.CDLL(None)
        libc.prctl(_PR_SET_DUMPABLE, 0, 0, 0, 0)
    except (AttributeError, OSError):
        pass


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
    try:
        settings = Settings.from_env()
    except ValueError as e:
        log.error("invalid configuration: %s", e)
        sys.exit(1)
    if settings.api_token is None:
        log.warning(
            "OMICSBOX_API_TOKEN is not set - running WITHOUT authentication; "
            "do not use this in production"
        )
    _install_signal_handlers()
    _hide_process_secrets()
    counts = resolve_counts(settings)
    log.info(
        "OmicsBox sandbox listening on %s:%d (patients=%s, secondary=%s)",
        HOST,
        PORT,
        counts.patients,
        counts.secondary_entities,
    )
    serve(
        create_app(settings),
        host=HOST,
        port=PORT,
        threads=settings.threads,
        # waitress buffers the body before Flask checks authentication or
        # its own size limit, so the cap must be applied at the server layer
        max_request_body_size=settings.max_request_bytes,
    )


if __name__ == "__main__":
    main()
