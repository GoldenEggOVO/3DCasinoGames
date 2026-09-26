"""Package Casino assets as a standalone Minecraft 26.2 client resource pack."""
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'resource-pack/assets'
PACK_META = (ROOT / 'resource-pack/pack.mcmeta').read_bytes()


def package(destination=None):
    destination = Path(destination) if destination else ROOT / 'target/3dcasino-client-pack-26.2.zip'
    destination.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(destination, 'w', compression=zipfile.ZIP_STORED) as archive:
        files = [('pack.mcmeta', PACK_META)]
        files.extend(('assets/' + path.relative_to(ASSETS).as_posix(), path.read_bytes())
                     for path in sorted(ASSETS.rglob('*')) if path.is_file())
        for name, data in files:
            info = zipfile.ZipInfo(name, (2026, 1, 1, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            archive.writestr(info, data)
    return destination


if __name__ == '__main__':
    print(package())
