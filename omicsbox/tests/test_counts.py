from omicsbox.counts import resolve_counts


def test_unconfigured(make_settings):
    counts = resolve_counts(make_settings())
    assert counts.patients is None
    assert counts.secondary_entities is None


def test_patients_and_secondary_entities(make_settings):
    counts = resolve_counts(make_settings(patient_count=100, secondary_entity_count=300))
    assert counts.patients == 100
    assert counts.secondary_entities == 300


def test_secondary_only(make_settings):
    counts = resolve_counts(make_settings(secondary_entity_count=4))
    assert counts.patients is None
    assert counts.secondary_entities == 4


def test_zero_is_a_valid_count(make_settings):
    counts = resolve_counts(make_settings(patient_count=0, secondary_entity_count=0))
    assert counts.patients == 0
    assert counts.secondary_entities == 0


def test_counts_endpoint(make_client):
    body = make_client(patient_count=2, secondary_entity_count=6).get("/counts").get_json()
    assert body == {"patients": 2, "secondary_entities": 6}


def test_counts_endpoint_unconfigured(make_client):
    body = make_client().get("/counts").get_json()
    assert body == {"patients": None, "secondary_entities": None}
