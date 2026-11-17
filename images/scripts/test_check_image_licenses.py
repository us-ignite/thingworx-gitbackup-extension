import io
import json
import tarfile
import unittest
import importlib.util
from pathlib import Path

spec = importlib.util.spec_from_file_location('scanner', Path(__file__).with_name('check-image-licenses.py'))
scanner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(scanner)


def layer(files):
    data = io.BytesIO()
    with tarfile.open(fileobj=data, mode='w') as tar:
        for name in files:
            payload = b'test'
            info = tarfile.TarInfo(name)
            info.size = len(payload)
            tar.addfile(info, io.BytesIO(payload))
    return data.getvalue()


def image(layers, manifest=None):
    data = io.BytesIO()
    with tarfile.open(fileobj=data, mode='w') as tar:
        def put(name, content):
            info = tarfile.TarInfo(name)
            info.size = len(content)
            tar.addfile(info, io.BytesIO(content))
        for name, contents in layers.items():
            put(name, contents)
        if manifest is None:
            manifest = [{'Config': 'config.json', 'RepoTags': ['test:latest'], 'Layers': list(layers)}]
        put('manifest.json', json.dumps(manifest).encode())
    data.seek(0)
    return data


class ImageLicenseTest(unittest.TestCase):
    def test_detects_file_hidden_by_later_whiteout(self):
        archive = image({'old/layer.tar': layer(['opt/Thingworx/license.bin']),
                         'new/layer.tar': layer(['opt/Thingworx/.wh.license.bin'])})
        self.assertEqual([('old/layer.tar', 'opt/Thingworx/license.bin')], scanner.check_archive(archive))

    def test_clean_layers_pass(self):
        archive = image({'layer.tar': layer(['etc/readme', 'tmp/.wh.trial.bin'])})
        self.assertEqual([], scanner.check_archive(archive))

    def test_detects_trial_in_any_path_and_case(self):
        archive = image({'layer.tar': layer(['tmp/TRIAL.BIN'])})
        self.assertEqual([('layer.tar', 'tmp/TRIAL.BIN')], scanner.check_archive(archive))

    def test_missing_referenced_layer_fails_closed(self):
        archive = image({}, [{'Layers': ['missing.tar']}])
        with self.assertRaises(KeyError):
            scanner.check_archive(archive)

    def test_empty_manifest_fails_closed(self):
        archive = image({}, [])
        with self.assertRaises(ValueError):
            scanner.check_archive(archive)


if __name__ == '__main__':
    unittest.main()
