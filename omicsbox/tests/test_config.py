import pytest

from omicsbox.config import Settings


def _base_kwargs():
    return dict(
        data_dir="/sandbox/data",
        threads=2,
        api_token=None,
        timeout_seconds=30,
        run_queue_timeout_seconds=5,
        max_request_bytes=1024,
        max_output_bytes=1024,
        patient_count=None,
        secondary_entity_count=None,
    )


def test_defaults():
    settings = Settings.from_env({})
    assert settings.data_dir == "/sandbox/data"
    assert settings.threads == 8
    assert settings.api_token is None
    assert settings.timeout_seconds == 60
    assert settings.run_queue_timeout_seconds == 120
    assert settings.max_request_bytes == 5 * 1024 * 1024
    assert settings.max_output_bytes == 1024 * 1024
    assert settings.patient_count is None
    assert settings.secondary_entity_count is None


def test_overrides():
    settings = Settings.from_env(
        {
            "OMICSBOX_DATA_DIR": "/data",
            "OMICSBOX_THREADS": "4",
            "OMICSBOX_API_TOKEN": "secret",
            "OMICSBOX_TIMEOUT": "5",
            "OMICSBOX_RUN_QUEUE_TIMEOUT": "2",
            "OMICSBOX_MAX_REQUEST_BYTES": "100",
            "OMICSBOX_MAX_OUTPUT_BYTES": "10",
            "OMICSBOX_PATIENT_COUNT": "7",
            "OMICSBOX_SECONDARY_ENTITY_COUNT": "21",
        }
    )
    assert settings.data_dir == "/data"
    assert settings.threads == 4
    assert settings.api_token == "secret"
    assert settings.timeout_seconds == 5
    assert settings.run_queue_timeout_seconds == 2
    assert settings.max_request_bytes == 100
    assert settings.max_output_bytes == 10
    assert settings.patient_count == 7
    assert settings.secondary_entity_count == 21


def test_empty_strings_are_unset():
    settings = Settings.from_env({"OMICSBOX_PATIENT_COUNT": "", "OMICSBOX_API_TOKEN": "   "})
    assert settings.patient_count is None
    assert settings.api_token is None


@pytest.mark.parametrize(
    "env",
    [
        {"OMICSBOX_TIMEOUT": "0"},
        {"OMICSBOX_TIMEOUT": "-5"},
        {"OMICSBOX_TIMEOUT": "abc"},
        {"OMICSBOX_THREADS": "0"},
        {"OMICSBOX_RUN_QUEUE_TIMEOUT": "-1"},
        {"OMICSBOX_MAX_REQUEST_BYTES": "0"},
        {"OMICSBOX_MAX_OUTPUT_BYTES": "-1"},
        {"OMICSBOX_PATIENT_COUNT": "-1"},
        {"OMICSBOX_PATIENT_COUNT": "1_0"},  # int() would accept this
        {"OMICSBOX_PATIENT_COUNT": "٥"},  # int() accepts non-ASCII digits
        {"OMICSBOX_SECONDARY_ENTITY_COUNT": "-1"},
    ],
)
def test_invalid_env_values_raise(env):
    with pytest.raises(ValueError):
        Settings.from_env(env)


@pytest.mark.parametrize(
    "overrides",
    [
        {"threads": 0},
        {"timeout_seconds": 0},
        {"run_queue_timeout_seconds": -1},
        {"max_request_bytes": 0},
        {"patient_count": -1},
        {"data_dir": ""},
    ],
)
def test_constructor_validates(overrides):
    with pytest.raises(ValueError):
        Settings(**{**_base_kwargs(), **overrides})
