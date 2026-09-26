"""Convert Casino's original Java item models to editable Blockbench projects."""

import base64
import json
import struct
import sys
import uuid
from pathlib import Path


ASSETS = Path(__file__).resolve().parents[1] / "resource-pack/assets"
MODELS = ASSETS / "3dcasino/models/item"
FACES = ("north", "east", "south", "west", "up", "down")


def image_size(path):
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"Not a PNG: {path}")
    return struct.unpack(">II", data[16:24]), data


def convert(path, vanilla_assets=None):
    source = json.loads(path.read_text(encoding="utf-8"))
    aliases = {}
    textures = []

    def texture_index(reference):
        key = reference.removeprefix("#")
        if key not in aliases:
            asset_id = source["textures"][key]
            namespace, name = asset_id.split(":", 1)
            texture_path = ASSETS / namespace / "textures" / f"{name}.png"
            if not texture_path.is_file() and namespace == "minecraft" and vanilla_assets:
                texture_path = vanilla_assets / f"{name}.png"
            index = len(textures)
            aliases[key] = index
            entry = {
                "name": texture_path.name,
                "id": str(index),
                "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"3dcasino:{path.stem}:texture:{key}")),
                "uv_width": 16,
                "uv_height": 16,
                "particle": key == "atlas",
                "visible": True,
            }
            if texture_path.is_file():
                (width, height), data = image_size(texture_path)
                entry.update({
                    "width": width,
                    "height": height,
                    "source": "data:image/png;base64," + base64.b64encode(data).decode("ascii"),
                })
            elif namespace != "minecraft":
                raise FileNotFoundError(texture_path)
            else:
                # Minecraft's own textures are supplied by the client, not this resource pack.
                entry.update({"width": 16, "height": 16, "namespace": namespace,
                              "folder": name.rpartition("/")[0]})
            textures.append(entry)
        return aliases[key]

    elements = []
    for index, part in enumerate(source["elements"]):
        faces = {}
        for direction in FACES:
            face = part["faces"].get(direction)
            faces[direction] = (
                {"uv": face["uv"], "texture": texture_index(face["texture"])}
                if face else {"uv": [0, 0, 0, 0], "texture": None}
            )
        elements.append({
            "name": f"part_{index + 1:03d}",
            "type": "cube",
            "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"3dcasino:{path.stem}:part:{index}")),
            "from": part["from"],
            "to": part["to"],
            "origin": [(a + b) / 2 for a, b in zip(part["from"], part["to"])],
            "box_uv": False,
            "autouv": 0,
            "color": index % 8,
            "faces": faces,
        })

    return {
        "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
        "name": path.stem,
        "resolution": {"width": 16, "height": 16},
        "elements": elements,
        "groups": [],
        "outliner": [element["uuid"] for element in elements],
        "textures": textures,
    }


def main(output, vanilla_assets=None):
    output.mkdir(parents=True, exist_ok=True)
    for path in sorted(MODELS.glob("*.json")):
        project = convert(path, vanilla_assets)
        (output / f"{path.stem}.bbmodel").write_text(
            json.dumps(project, ensure_ascii=False, separators=(",", ":")), encoding="utf-8"
        )
    print(f"Converted {len(list(output.glob('*.bbmodel')))} models to {output}")


if __name__ == "__main__":
    main(Path(sys.argv[1]).resolve(), Path(sys.argv[2]).resolve() if len(sys.argv) > 2 else None)
