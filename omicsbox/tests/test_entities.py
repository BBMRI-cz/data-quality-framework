def _seed_data_dir(tmp_path):
    data = tmp_path / "data"
    data.mkdir(exist_ok=True)
    (data / "file.txt").write_text("hello")
    (data / "cohort-a").mkdir(exist_ok=True)
    (data / "cohort-a" / "slide-001.svs").write_bytes(b"\x01\x02\x03")
    return data


def test_file_metadata(make_client, tmp_path):
    _seed_data_dir(tmp_path)
    resp = make_client().get("/entities/file/file.txt")
    assert resp.status_code == 200
    body = resp.get_json()
    assert body["name"] == "file.txt"
    assert body["entity_id"] == "file.txt"
    assert body["type"] == "file"
    assert body["size_bytes"] == 5
    assert body["last_modified"]


def test_directory_metadata(make_client, tmp_path):
    _seed_data_dir(tmp_path)
    body = make_client().get("/entities/file/cohort-a").get_json()
    assert body["type"] == "directory"
    assert body["size_bytes"] == 0


def test_nested_path(make_client, tmp_path):
    _seed_data_dir(tmp_path)
    body = make_client().get("/entities/file/cohort-a/slide-001.svs").get_json()
    assert body["entity_id"] == "cohort-a/slide-001.svs"
    assert body["size_bytes"] == 3


def test_missing_entity_returns_404(make_client, tmp_path):
    _seed_data_dir(tmp_path)
    resp = make_client().get("/entities/file/does-not-exist.dcm")
    assert resp.status_code == 404
    assert "error" in resp.get_json()


def test_unsupported_entity_type_rejected(make_client, tmp_path):
    _seed_data_dir(tmp_path)
    resp = make_client().get("/entities/patient/42")
    assert resp.status_code == 400


def test_path_traversal_rejected(make_client, tmp_path):
    _seed_data_dir(tmp_path)
    resp = make_client().get("/entities/file/..%2Fsecret.txt")
    assert resp.status_code == 400
    assert "error" in resp.get_json()


def test_unknown_route_returns_json_404(make_client):
    resp = make_client().get("/nope")
    assert resp.status_code == 404
    assert resp.get_json() == {"error": "not found"}
