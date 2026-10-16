import pytest

from omicsbox.config import Settings


def test_defaults():
    settings = Settings.from_env({})
    assert settings.data_dir == "/sandbox/data"
    assert settings.host == "0.0.0.0"
    assert settings.port == 8000
    assert settings.timeout_seconds == 60
    assert settings.max_request_bytes == 5 * 1024 * 1024
    assert settings.max_output_bytes == 1024 * 1024
    assert settings.patient_count is None
    assert settings.images_per_patient == 1
    assert settings.secondary_entity_count is None


def test_overrides():
    settings = Settings.from_env(
        {
            "TIMEOUT": "5",
            "MAX_REQUEST_BYTES": "100",
            "MAX_OUTPUT_BYTES": "10",
            "OMICSBOX_PATIENT_COUNT": "7",
            "OMICSBOX_IMAGES_PER_PATIENT": "3",
            "OMICSBOX_SECONDARY_ENTITY_COUNT": "21",
        }
    )
    assert settings.timeout_seconds == 5
    assert settings.max_request_bytes == 100
    assert settings.max_output_bytes == 10
    assert settings.patient_count == 7
    assert settings.images_per_patient == 3
    assert settings.secondary_entity_count == 21


def test_empty_strings_are_unset():
    settings = Settings.from_env(
        {"OMICSBOX_PATIENT_COUNT": "", "OMICSBOX_SECONDARY_ENTITY_COUNT": "  "}
    )
    assert settings.patient_count is None
    assert settings.secondary_entity_count is None


@pytest.mark.parametrize(
    "env",
    [
        {"TIMEOUT": "0"},
        {"TIMEOUT": "-5"},
        {"TIMEOUT": "abc"},
        {"MAX_REQUEST_BYTES": "0"},
        {"MAX_OUTPUT_BYTES": "-1"},
        {"OMICSBOX_PATIENT_COUNT": "-1"},
        {"OMICSBOX_PATIENT_COUNT": "not-a-number"},
        {"OMICSBOX_IMAGES_PER_PATIENT": "-1"},
        {"OMICSBOX_SECONDARY_ENTITY_COUNT": "-1"},
    ],
)
def test_invalid_values_raise(env):
    with pytest.raises(ValueError):
        Settings.from_env(env)
