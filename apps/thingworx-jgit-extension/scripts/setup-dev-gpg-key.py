#!/usr/bin/env python3
"""Provision a local-only GPG key in ThingWorx and register it with Gitea."""
import base64
import json
from pathlib import Path
import subprocess
import tempfile
import urllib.request

BASE_URL = "http://localhost:8080/Thingworx/Things/GIT.Utility.Thing/Services/"
GITEA_URL = "http://localhost:3000/api/v1/user/gpg_keys"
LABEL = "Dev Setup Test Key"
PUBLIC_KEY_FILE = Path(__file__).resolve().parent.parent / "build" / "dev-setup-gpg-public.asc"
AUTH = base64.b64encode(b"Administrator:AdminP@ssw0rd!123").decode("ascii")
GITEA_AUTH = base64.b64encode(b"giteauser:giteapass123").decode("ascii")


def request_json(url, auth, payload=None, extra_headers=None):
    headers = {"Authorization": "Basic " + auth, "Accept": "application/json"}
    headers.update(extra_headers or {})
    data = None
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=data, headers=headers)
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def invoke(service, payload):
    result = request_json(BASE_URL + service, AUTH, payload, {
        "X-XSRF-TOKEN": "TWX-XSRF-TOKEN-VALUE",
        "X-Requested-By": "ThingWorx",
    })
    rows = result["rows"]
    if len(rows) != 1 or "Error" not in rows[0]:
        raise RuntimeError(f"{service} returned an invalid service result")
    if rows[0]["Error"]:
        raise RuntimeError(f"{service} failed: {rows[0].get('Message', '')}")
    return rows[0]


def fingerprint(public_key):
    with tempfile.TemporaryDirectory(prefix="thingworx-dev-gpg-inspect-") as home:
        result = subprocess.run(
            ["gpg", "--homedir", home, "--batch", "--with-colons", "--import-options", "show-only", "--import"],
            input=public_key, text=True, capture_output=True, check=True,
        )
    return next(line.split(":")[9] for line in result.stdout.splitlines() if line.startswith("fpr:"))


def registered(fingerprint_value):
    page = 1
    while True:
        keys = request_json(f"{GITEA_URL}?page={page}&limit=50", GITEA_AUTH)
        if any(key.get("key_id", "").upper() == fingerprint_value[-16:].upper() for key in keys):
            return True
        if len(keys) < 50:
            return False
        page += 1


def main():
    keys = invoke("GpgKeyList", {})["Result"]["rows"]
    existing = next((key for key in keys if key.get("GpgKeyLabel") == LABEL), None)
    if existing and registered(existing["GpgKeyFingerprint"]):
        print(f"GPG key '{LABEL}' is already registered with ThingWorx and Gitea.")
        return

    public_key = None
    if existing and PUBLIC_KEY_FILE.exists():
        candidate = PUBLIC_KEY_FILE.read_text(encoding="utf-8")
        if fingerprint(candidate).upper() == existing["GpgKeyFingerprint"].upper():
            public_key = candidate

    with tempfile.TemporaryDirectory(prefix="thingworx-dev-gpg-") as home:
        common = ["gpg", "--homedir", home, "--batch", "--pinentry-mode", "loopback", "--passphrase", ""]
        if not existing:
            subprocess.run(common + ["--quick-generate-key", "ThingWorx Dev Test <test@example.com>", "rsa2048", "sign", "0"], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            private_key = subprocess.check_output(common + ["--armor", "--export-secret-keys"], text=True, stderr=subprocess.DEVNULL)
            public_key = subprocess.check_output(common + ["--armor", "--export"], text=True, stderr=subprocess.DEVNULL)
            if not private_key.strip() or not public_key.strip():
                raise RuntimeError("GPG did not export the generated test key")
            # Save the public half before creating the ThingWorx key, so an interrupted
            # setup can register the same key on its next run.
            PUBLIC_KEY_FILE.parent.mkdir(parents=True, exist_ok=True)
            PUBLIC_KEY_FILE.write_text(public_key, encoding="utf-8")
            invoke("GpgKeyCreate", {"GpgPrivateKey": private_key, "GpgKeyPassphrase": "", "GpgKeyLabel": LABEL})
        elif public_key is None:
            # Older devSetup versions did not save the public half. Recover it from
            # the user's existing key when ThingWorx exposes the protected value.
            private_key = invoke("GpgKeyGet", {"GpgKeyLabel": LABEL})["Result"]["rows"][0].get("GpgPrivateKey", "")
            if not private_key or "PRIVATE KEY BLOCK" not in private_key:
                raise RuntimeError("Cannot recover the existing dev key's public half. Delete the 'Dev Setup Test Key' using GpgKeyDelete and rerun devSetup, or register its public key in Gitea manually.")
            subprocess.run(common + ["--import"], input=private_key, text=True, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            public_key = subprocess.check_output(common + ["--armor", "--export"], text=True, stderr=subprocess.DEVNULL)
            if fingerprint(public_key).upper() != existing["GpgKeyFingerprint"].upper():
                raise RuntimeError("Recovered public key does not match the ThingWorx key")
            PUBLIC_KEY_FILE.parent.mkdir(parents=True, exist_ok=True)
            PUBLIC_KEY_FILE.write_text(public_key, encoding="utf-8")

    if not registered(fingerprint(public_key)):
        request_json(GITEA_URL, GITEA_AUTH, {"armored_public_key": public_key})
    print(f"GPG key '{LABEL}' is registered with ThingWorx and Gitea.")


if __name__ == "__main__":
    main()
