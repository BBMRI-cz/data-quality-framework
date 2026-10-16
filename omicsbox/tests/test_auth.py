import io


def test_everything_open_when_no_token_configured(make_client):
    client = make_client()
    assert client.get("/counts").status_code == 200
    assert client.get("/entities/file/x").status_code in (400, 404)


def test_missing_token_rejected(make_client):
    client = make_client(api_token="secret-token")
    resp = client.get("/counts")
    assert resp.status_code == 401
    assert resp.get_json() == {"error": "unauthorized"}


def test_wrong_token_rejected(make_client):
    client = make_client(api_token="secret-token")
    resp = client.get("/counts", headers={"Authorization": "Bearer wrong"})
    assert resp.status_code == 401


def test_bearer_prefix_required(make_client):
    client = make_client(api_token="secret-token")
    resp = client.get("/counts", headers={"Authorization": "secret-token"})
    assert resp.status_code == 401


def test_valid_token_accepted(make_client):
    client = make_client(api_token="secret-token")
    resp = client.get("/counts", headers={"Authorization": "Bearer secret-token"})
    assert resp.status_code == 200


def test_run_requires_token(make_client):
    client = make_client(api_token="secret-token")
    resp = client.post("/run", data=b"print('x')")
    assert resp.status_code == 401


def test_run_accepts_token(make_client):
    client = make_client(api_token="secret-token")
    resp = client.post(
        "/run",
        data=io.BytesIO(b"print('x')"),
        content_type="application/octet-stream",
        headers={"Authorization": "Bearer secret-token"},
    )
    assert resp.status_code == 200


def test_entities_requires_token(make_client):
    client = make_client(api_token="secret-token")
    assert client.get("/entities/file/x").status_code == 401


def test_health_is_public(make_client):
    client = make_client(api_token="secret-token")
    assert client.get("/health").status_code == 200


def test_non_ascii_authorization_rejected_with_401(make_client):
    # compare_digest raises TypeError on non-ASCII str; must not become a 500
    client = make_client(api_token="secret-token")
    resp = client.get("/counts", headers={"Authorization": "Bearer secret-töken"})
    assert resp.status_code == 401
    assert resp.get_json() == {"error": "unauthorized"}


def test_non_ascii_configured_token_accepted(make_client):
    client = make_client(api_token="sekret-tökén")
    resp = client.get("/counts", headers={"Authorization": "Bearer sekret-tökén"})
    assert resp.status_code == 200
