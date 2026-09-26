"""Public source archive includes the runtime model data, not a client pack."""
import importlib.util
import io
from pathlib import Path
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]


class SourcePackageTest(unittest.TestCase):
    def test_source_package_is_reproducible_and_pack_free(self):
        spec = importlib.util.spec_from_file_location('source_packager', ROOT / 'tools/package-source.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as temporary:
            first = module.package(Path(temporary) / 'first.zip').read_bytes()
            second = module.package(Path(temporary) / 'second.zip').read_bytes()
        self.assertEqual(first, second)
        with zipfile.ZipFile(io.BytesIO(first)) as archive:
            names = set(archive.namelist())
        self.assertIn('3dcasino/src/main/resources/vanilla-models.json', names)
        self.assertIn('3dcasino/LICENSE', names)
        self.assertIn('3dcasino/.github/workflows/ci.yml', names)
        self.assertFalse(any('resource-pack' in name or '/target/' in name or '/reports/' in name
                             for name in names))


if __name__ == '__main__':
    unittest.main()
