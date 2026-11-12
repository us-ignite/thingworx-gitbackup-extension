#!/usr/bin/env python3
"""Create a development package newer than the one installed in ThingWorx."""
import base64
import json
import re
import sys
import urllib.error
import urllib.request
import zipfile
from pathlib import Path


def version(value):
    if not isinstance(value, str) or not re.fullmatch(r"\d+\.\d+\.\d+", value):
        raise ValueError(f"Expected a three-part extension version, got {value!r}")
    return tuple(map(int, value.split(".")))


def main(source, destination):
    request = urllib.request.Request(
        "http://localhost:8080/Thingworx/ExtensionPackages/JGitExtension",
        headers={"Authorization": "Basic " + base64.b64encode(b"Administrator:AdminP@ssw0rd!123").decode(),
                 "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            installed = version(json.load(response)["packageVersion"])
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise
        installed = None

    with zipfile.ZipFile(source) as original:
        metadata = original.read("metadata.xml").decode("utf-8")
        pattern = r'(<ExtensionPackage\b[^>]*\bpackageVersion=")([^"]+)(")'
        matches = list(re.finditer(pattern, metadata))
        if len(matches) != 1:
            raise ValueError("Expected exactly one ExtensionPackage version in metadata.xml")
        built = version(matches[0].group(2))
        base = max(built, installed) if installed else built
        dev_version = f"{base[0]}.{base[1]}.{base[2] + 1}"
        updated = metadata[:matches[0].start(2)] + dev_version + metadata[matches[0].end(2):]
        with zipfile.ZipFile(destination, "w") as output:
            for entry in original.infolist():
                data = updated.encode("utf-8") if entry.filename == "metadata.xml" else original.read(entry)
                output.writestr(entry, data)
    print(dev_version)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: package-dev-extension.py SOURCE_ZIP DESTINATION_ZIP")
    main(Path(sys.argv[1]), Path(sys.argv[2]))
