#!/usr/bin/env python3
"""Refuse to publish image layers containing runtime ThingWorx licence files.

The final container filesystem is insufficient: removing a file in a later layer
leaves the earlier layer available to anyone who pulls the image. Inspect every
layer listed in the Docker image archive's manifest, without extracting files.
"""
import argparse
import json
import subprocess
import sys
import tarfile
import tempfile
from pathlib import PurePosixPath

FORBIDDEN = frozenset(('license.bin', 'trial.bin'))


def check_archive(fileobj):
    """Return offending (layer, member) pairs; fail closed for invalid archives."""
    found = []
    with tarfile.open(fileobj=fileobj, mode='r:*') as image:
        manifest_entry = image.getmember('manifest.json')
        manifest_file = image.extractfile(manifest_entry)
        if manifest_file is None:
            raise ValueError('image manifest is not a file')
        manifest = json.load(manifest_file)
        if not isinstance(manifest, list) or not manifest:
            raise ValueError('empty or malformed image manifest')
        layers = set()
        for entry in manifest:
            declared = entry.get('Layers')
            if not isinstance(declared, list) or not declared:
                raise ValueError('manifest has no layers')
            layers.update(declared)
        for layer in sorted(layers):
            layer_file = image.extractfile(image.getmember(layer))
            if layer_file is None:
                raise ValueError(f'layer is not a file: {layer}')
            with layer_file, tarfile.open(fileobj=layer_file, mode='r|*') as archive:
                for member in archive:
                    # OCI whiteouts are metadata, not licence files. Look at all
                    # actual entries including links, even in overwritten layers.
                    if PurePosixPath(member.name).name.lower() in FORBIDDEN:
                        found.append((layer, member.name))
    return found


def check_image(tag, seen):
    image_id = subprocess.check_output(
        ['docker', 'image', 'inspect', '--format', '{{.Id}}', tag], text=True
    ).strip()
    if not image_id:
        raise ValueError(f'no image ID for {tag}')
    if image_id in seen:
        print(f'{tag}: already checked {image_id}')
        return
    # Use the immutable ID for export so the scan covers the image resolved
    # at inspection. The temporary archive is automatically removed.
    with tempfile.TemporaryFile() as tmp:
        subprocess.run(['docker', 'image', 'save', image_id], stdout=tmp, check=True)
        tmp.seek(0)
        matches = check_archive(tmp)
    if matches:
        for layer, name in matches:
            print(f'{tag}: forbidden licence path in layer {layer}: {name}', file=sys.stderr)
        raise ValueError(f'refusing to publish {tag}')
    seen.add(image_id)
    print(f'{tag}: all layers passed')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('images', nargs='+', help='local Docker image tags to check')
    args = parser.parse_args()
    try:
        seen = set()
        for tag in args.images:
            check_image(tag, seen)
    except (OSError, ValueError, KeyError, tarfile.TarError, subprocess.SubprocessError) as exc:
        print(f'image licence scan failed: {exc}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
