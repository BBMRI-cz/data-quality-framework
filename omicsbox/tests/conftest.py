import pytest

from omicsbox.config import Settings
from omicsbox.server import create_app


@pytest.fixture
def make_settings(tmp_path):
    """Settings factory; creates the data dir unless overridden."""

    def factory(**overrides):
        data_dir = overrides.pop("data_dir", None)
        if data_dir is None:
            data_dir = tmp_path / "data"
            data_dir.mkdir(exist_ok=True)
        kwargs = dict(
            data_dir=str(data_dir),
            host="127.0.0.1",
            port=8000,
            timeout_seconds=30,
            max_request_bytes=5 * 1024 * 1024,
            max_output_bytes=1024 * 1024,
            patient_count=None,
            images_per_patient=1,
            secondary_entity_count=None,
        )
        kwargs.update(overrides)
        return Settings(**kwargs)

    return factory


@pytest.fixture
def make_client(make_settings):
    """Flask test client factory built from fresh settings."""

    def factory(**overrides):
        app = create_app(make_settings(**overrides))
        app.config.update(TESTING=True)
        return app.test_client()

    return factory
