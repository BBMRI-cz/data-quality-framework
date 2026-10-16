def test_health_up(make_client):
    resp = make_client().get("/health")
    assert resp.status_code == 200
    body = resp.get_json()
    assert body["status"] == "UP"
    assert body["details"]["data_dir_mounted"] is True
    assert body["details"]["data_dir_readable"] is True
    assert body["details"]["tmp_writable"] is True


def test_health_down_returns_503(make_client, tmp_path):
    resp = make_client(data_dir=str(tmp_path / "nope")).get("/health")
    assert resp.status_code == 503
    body = resp.get_json()
    assert body["status"] == "DOWN"
    assert body["details"]["data_dir_mounted"] is False
