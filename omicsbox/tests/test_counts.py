from omicsbox.counts import resolve_counts


def test_unconfigured(make_settings):
    counts = resolve_counts(make_settings())
    assert counts.patients is None
    assert counts.secondary_entities is None
    assert counts.source == "unconfigured"


def test_derived_from_patients(make_settings):
    counts = resolve_counts(make_settings(patient_count=5, images_per_patient=3))
    assert counts.patients == 5
    assert counts.secondary_entities == 15
    assert counts.source == "derived"


def test_explicit_override_wins(make_settings):
    counts = resolve_counts(
        make_settings(patient_count=5, images_per_patient=3, secondary_entity_count=7)
    )
    assert counts.patients == 5
    assert counts.secondary_entities == 7
    assert counts.source == "env"


def test_secondary_only(make_settings):
    counts = resolve_counts(make_settings(secondary_entity_count=4))
    assert counts.patients is None
    assert counts.secondary_entities == 4
    assert counts.source == "env"


def test_counts_endpoint(make_client):
    body = make_client(patient_count=2).get("/counts").get_json()
    assert body == {"patients": 2, "secondary_entities": 2, "source": "derived"}
