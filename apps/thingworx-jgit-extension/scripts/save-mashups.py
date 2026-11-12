#!/usr/bin/env python3
"""Fetch the repository's mashups from a ThingWorx exporter."""
import argparse
import base64
import json
import os
from pathlib import Path
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-dir', type=Path, required=True)
    parser.add_argument('--output-dir', type=Path, required=True)
    parser.add_argument('--mashup', help='GIT mashup suffix, such as Changes')
    args = parser.parse_args()
    url = os.environ.get('TWX_URL', 'http://localhost:8080/Thingworx').rstrip('/')
    username = os.environ.get('TWX_USERNAME', 'Administrator')
    password = os.environ.get('TWX_PASSWORD', 'AdminP@ssw0rd!123')
    auth = base64.b64encode(f'{username}:{password}'.encode()).decode()
    files = sorted(args.source_dir.glob('Mashups_GIT.Mashup.*.xml'))
    if args.mashup:
        files = [p for p in files if p.name == f'Mashups_GIT.Mashup.{args.mashup}.xml']
    if not files:
        raise ValueError(f'No GIT mashup source files found in {args.source_dir}')
    exports = []
    for source in files:
        name = source.name.removeprefix('Mashups_').removesuffix('.xml')
        request = urllib.request.Request(
            f'{url}/Exporter/Mashups/{urllib.parse.quote(name, safe="")}',
            headers={'Authorization': f'Basic {auth}', 'Accept': 'text/xml'})
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                data = response.read()
        except urllib.error.URLError as error:
            raise RuntimeError(f'Could not export {name}: {error}') from error
        root = ET.fromstring(data)
        entities = root.findall('./Mashups/Mashup')
        if root.tag != 'Entities' or len(entities) != 1 or entities[0].get('name') != name:
            raise ValueError(f'Unexpected exporter response for {name}')
        content = entities[0].find('mashupContent')
        if content is None or not content.text or not isinstance(json.loads(content.text), dict):
            raise ValueError(f'Invalid mashup content for {name}')
        if b'<mashupContent><![CDATA[' not in data:
            raise ValueError(f'Export for {name} is not in the CDATA format required by the build')
        exports.append((args.output_dir / source.name, data, source.stat().st_mode & 0o777))
    # Fetch and validate every entity before replacing any local files.
    args.output_dir.mkdir(parents=True, exist_ok=True)
    for destination, data, mode in exports:
        with tempfile.NamedTemporaryFile(dir=args.output_dir, delete=False) as output:
            temporary = Path(output.name)
            try:
                output.write(data)
            except BaseException:
                temporary.unlink(missing_ok=True)
                raise
        try:
            temporary.chmod(mode)
            os.replace(temporary, destination)
        finally:
            temporary.unlink(missing_ok=True)
        print(f'Saved {destination}')
    print('Embedded TypeScript and CSS are generated from ui/src during packaging; sync edits there as well.')


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, RuntimeError, ET.ParseError) as error:
        print(f'Save mashups failed: {error}', file=sys.stderr)
        sys.exit(1)
