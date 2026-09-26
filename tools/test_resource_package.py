from pathlib import Path
import hashlib
import importlib.util
import json
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / 'resource-pack'
ASSETS = PACK / 'assets'


class ResourcePackageTest(unittest.TestCase):
    def test_resource_pack_has_only_native_assets_and_metadata(self):
        self.assertEqual({'assets', 'pack.mcmeta'}, {p.name for p in PACK.iterdir()})
        self.assertFalse((ROOT / 'craftengine').exists())
        self.assertFalse((ROOT / 'tools/package-resources.py').exists())

    def test_approved_baseline_is_unchanged_except_namespace(self):
        manifest = json.loads((ROOT / 'tools/resource-baseline.json').read_text())
        for name, digest in manifest.items():
            with self.subTest(file=name):
                data = (ROOT / name).read_bytes()
                if name.endswith(('.json', '.yml')):
                    data = data.decode('utf-8').replace('\r\n', '\n').encode('utf-8')
                self.assertEqual(digest, hashlib.sha256(data).hexdigest())

    def assert_reference(self, value, folder, extension):
        if ':' not in value or value.startswith('#'):
            return
        namespace, path = value.split(':', 1)
        if namespace == 'minecraft':
            return
        self.assertEqual(namespace, 'casino')
        self.assertTrue((ASSETS / namespace / folder / (path + extension)).is_file(), value)

    def test_all_model_font_item_references_resolve(self):
        for path in ASSETS.rglob('*.json'):
            data = json.loads(path.read_text(encoding='utf-8'))
            for value in data.get('textures', {}).values():
                self.assert_reference(value, 'textures', '.png')
            if 'parent' in data:
                self.assert_reference(data['parent'], 'models', '.json')
            model = data.get('model', {})
            if isinstance(model, dict) and model.get('type') == 'minecraft:model':
                self.assert_reference(model['model'], 'models', '.json')
            for provider in data.get('providers', []):
                if provider['type'] == 'bitmap':
                    self.assert_reference(provider['file'], 'textures', '')

    def test_only_current_namespace_is_packaged(self):
        self.assertEqual({'casino'}, {p.name for p in ASSETS.iterdir() if p.is_dir()})

    def test_packaging_is_reproducible(self):
        spec = importlib.util.spec_from_file_location('packager', ROOT / 'tools/package-client-pack.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as temporary:
            first = module.package(Path(temporary) / 'first.zip').read_bytes()
            second = module.package(Path(temporary) / 'second.zip').read_bytes()
            self.assertEqual(first, second)

    def test_client_pack_contains_only_26_2_assets(self):
        spec = importlib.util.spec_from_file_location('client_packager', ROOT / 'tools/package-client-pack.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as temporary:
            output = module.package(Path(temporary) / 'client.zip')
            second = module.package(Path(temporary) / 'again.zip')
            self.assertEqual(output.read_bytes(), second.read_bytes())
            with zipfile.ZipFile(output) as archive:
                names = set(archive.namelist())
                expected = {'assets/' + path.relative_to(ASSETS).as_posix()
                            for path in ASSETS.rglob('*') if path.is_file()}
                self.assertEqual(expected | {'pack.mcmeta'}, names)
                self.assertEqual([88, 0], json.loads(archive.read('pack.mcmeta'))['pack']['min_format'])
                self.assertEqual([88, 0], json.loads(archive.read('pack.mcmeta'))['pack']['max_format'])
                self.assertEqual((ASSETS / 'casino/items/cabinet_blackjack.json').read_bytes(),
                                 archive.read('assets/casino/items/cabinet_blackjack.json'))

    def test_source_package_excludes_build_outputs_and_local_records(self):
        spec = importlib.util.spec_from_file_location('source_packager', ROOT / 'tools/package-source.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as temporary:
            output = module.package(Path(temporary) / 'source.zip')
            with zipfile.ZipFile(output) as archive:
                names = archive.namelist()
            self.assertIn('server-casino/LICENSE', names)
            self.assertIn('server-casino/tools/showcase-model-baseline.json', names)
            self.assertIn('server-casino/resource-pack/pack.mcmeta', names)
            self.assertIn('server-casino/.github/workflows/ci.yml', names)
            for name in names:
                self.assertFalse(any(part in name.split('/') for part in
                                     ('target', 'reports', 'artwork', '__pycache__', 'fonts', '.git', 'craftengine', 'craftengine-v3')))
                self.assertFalse(name.endswith(('.jar', '.ttf', '.ttc', '.otf', '.pyc', 'fonts.local.json')))


if __name__ == '__main__':
    unittest.main()
