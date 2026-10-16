import io
import json
import os
import threading
import time

from omicsbox.server import create_app


def _post_script(client, script: bytes):
    return client.post("/run", data=script, content_type="application/octet-stream")


def test_successful_run(make_client):
    resp = _post_script(make_client(), b"print('hello')")
    assert resp.status_code == 200
    body = resp.get_json()
    assert body["returncode"] == 0
    assert body["status"] == "ok"
    assert body["stdout"].strip() == "hello"
    assert body["stderr"] == ""
    assert body["duration_ms"] >= 0


def test_script_runs_in_data_dir_and_emits_contract_json(make_client):
    script = (
        b"import json, os\n"
        b"print(json.dumps({'count': 0, 'entityType': 'File', "
        b"'ids': sorted(os.listdir()), 'error': None}))"
    )
    body = _post_script(make_client(), script).get_json()
    assert body["returncode"] == 0
    payload = json.loads(body["stdout"].strip().splitlines()[-1])
    assert payload == {"count": 0, "entityType": "File", "ids": [], "error": None}


def test_form_urlencoded_body_is_read_raw(make_client):
    # curl defaults to application/x-www-form-urlencoded; the body must still
    # reach the runner instead of being consumed by werkzeug's form parser.
    resp = make_client().post(
        "/run", data=b"print('curl-default')", content_type="application/x-www-form-urlencoded"
    )
    assert resp.status_code == 200
    body = resp.get_json()
    assert body["returncode"] == 0
    assert body["stdout"].strip() == "curl-default"


def test_nonzero_exit_code_and_stderr(make_client):
    body = _post_script(
        make_client(), b"import sys; sys.stderr.write('boom\\n'); sys.exit(3)"
    ).get_json()
    assert body["returncode"] == 3
    assert body["status"] == "failed"
    assert "boom" in body["stderr"]


def test_timeout_kills_script_and_keeps_partial_output(make_client):
    start = time.monotonic()
    body = _post_script(
        make_client(timeout_seconds=1),
        b"import time; print('partial', flush=True); time.sleep(30)",
    ).get_json()
    elapsed = time.monotonic() - start
    assert body["returncode"] == -1
    assert body["status"] == "timeout"
    assert "partial" in body["stdout"]
    assert "[execution timed out after 1s]" in body["stderr"]
    assert elapsed < 10


def test_output_is_truncated_with_marker(make_client):
    body = _post_script(make_client(max_output_bytes=16), b"print('x' * 1000)").get_json()
    assert body["stdout"].startswith("x" * 16)
    assert "[output truncated]" in body["stdout"]


def test_multibyte_output_is_truncated_with_marker(make_client):
    body = _post_script(make_client(max_output_bytes=16), "print('é' * 1000)".encode()).get_json()
    assert "[output truncated]" in body["stdout"]


def test_empty_body_rejected(make_client):
    resp = make_client().post("/run", data=b"")
    assert resp.status_code == 400
    assert "error" in resp.get_json()


def test_oversized_body_rejected_with_413(make_client):
    resp = _post_script(make_client(max_request_bytes=10), b"print('this is way too long')")
    assert resp.status_code == 413
    assert resp.get_json() == {"error": "payload too large"}


def test_multipart_script_is_executed(make_client):
    resp = make_client().post(
        "/run",
        data={"file": (io.BytesIO(b"print('from-multipart')"), "check.py")},
        content_type="multipart/form-data",
    )
    assert resp.status_code == 200
    assert resp.get_json()["stdout"].strip() == "from-multipart"


def test_multipart_missing_file_field_rejected(make_client):
    resp = make_client().post(
        "/run",
        data={"other": (io.BytesIO(b"print(1)"), "check.py")},
        content_type="multipart/form-data",
    )
    assert resp.status_code == 400


def test_multipart_empty_file_rejected(make_client):
    resp = make_client().post(
        "/run",
        data={"file": (io.BytesIO(b""), "check.py")},
        content_type="multipart/form-data",
    )
    assert resp.status_code == 400


def test_missing_data_dir_returns_spawn_error(make_client, tmp_path):
    body = _post_script(make_client(data_dir=str(tmp_path / "missing")), b"print('x')").get_json()
    assert body["returncode"] == -1
    assert body["status"] == "spawn_error"
    assert "failed to launch script process" in body["stderr"]


def test_script_environment_is_minimal_and_tmp_is_private(make_client):
    script = (
        b"import json, os\n"
        b"print(json.dumps(sorted(os.environ)))\n"
        b"print('TMP=' + os.environ['TMPDIR'])"
    )
    client = make_client()
    body = _post_script(client, script).get_json()
    lines = body["stdout"].strip().splitlines()
    env_keys = set(json.loads(lines[0]))
    # __CF_USER_TEXT_ENCODING is force-injected by the macOS kernel.
    assert env_keys - {"__CF_USER_TEXT_ENCODING"} == {
        "LANG",
        "PATH",
        "PYTHONDONTWRITEBYTECODE",
        "TEMP",
        "TMP",
        "TMPDIR",
    }
    tmp_dir = lines[1][len("TMP=") :]
    # The private TMPDIR lives in the run directory, which is deleted
    # after the run finishes.
    assert not os.path.exists(tmp_dir)


def test_runs_are_serialized(make_settings):
    app = create_app(make_settings(timeout_seconds=10))
    client_one = app.test_client()
    client_two = app.test_client()
    results = []

    def run(client):
        resp = _post_script(client, b"import time; time.sleep(0.5)")
        results.append(resp.get_json()["returncode"])

    start = time.monotonic()
    threads = [threading.Thread(target=run, args=(c,)) for c in (client_one, client_two)]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    assert time.monotonic() - start >= 0.9
    assert sorted(results) == [0, 0]


def test_busy_sandbox_returns_503(make_settings):
    app = create_app(make_settings(run_queue_timeout_seconds=0))
    app.config.update(TESTING=True)
    client_one = app.test_client()
    client_two = app.test_client()
    done = threading.Event()

    def long_run():
        _post_script(client_one, b"import time; time.sleep(2)")
        done.set()

    thread = threading.Thread(target=long_run)
    thread.start()
    time.sleep(0.3)  # let the first request grab the execution lock
    resp = _post_script(client_two, b"print('second')")
    assert resp.status_code == 503
    assert "error" in resp.get_json()
    thread.join()
