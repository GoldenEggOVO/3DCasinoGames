"""Preview original Casino cuboids as resource-pack-free BlockDisplay parts.

Run with Blender: blender -b -t 4 --python this_file -- <item assets dir> <output dir>
This is a visual prototype; it does not alter machine runtime definitions.
"""
import json
import math
import sys
from collections import defaultdict
from pathlib import Path

import bpy
from mathutils import Matrix, Vector


assets = Path(sys.argv[sys.argv.index("--") + 1]).resolve()
output = Path(sys.argv[sys.argv.index("--") + 2]).resolve()
output.mkdir(parents=True, exist_ok=True)
names = [
    "cabinet_blackjack", "cabinet_crash", "cabinet_mines", "cabinet_plinko",
    "showcase_dragon_tower", "showcase_duck_race", "showcase_hilo",
    "showcase_keno", "showcase_money_wheel", "showcase_penguin_cross",
    "showcase_slots", "showcase_wheel_of_fortune",
]

# Approximate vanilla block colors for a preview of the chosen block palette.
PALETTE = {
    "black_concrete": (0.06, 0.065, 0.075),
    "gray_concrete": (0.22, 0.24, 0.25),
    "light_gray_concrete": (0.50, 0.51, 0.51),
    "white_concrete": (0.84, 0.86, 0.85),
    "red_concrete": (0.57, 0.11, 0.12),
    "red_terracotta": (0.54, 0.25, 0.20),
    "orange_concrete": (0.88, 0.38, 0.10),
    "yellow_concrete": (0.94, 0.69, 0.15),
    "lime_concrete": (0.42, 0.69, 0.13),
    "green_concrete": (0.27, 0.35, 0.15),
    "cyan_concrete": (0.08, 0.45, 0.47),
    "light_blue_concrete": (0.22, 0.55, 0.77),
    "blue_concrete": (0.12, 0.20, 0.55),
    "purple_concrete": (0.38, 0.15, 0.55),
    "magenta_concrete": (0.67, 0.20, 0.58),
    "pink_concrete": (0.90, 0.48, 0.59),
    "brown_concrete": (0.38, 0.25, 0.18),
    "dark_oak_planks": (0.26, 0.18, 0.11),
    "oak_planks": (0.63, 0.48, 0.28),
    "smooth_sandstone": (0.79, 0.72, 0.52),
    "gold_block": (0.90, 0.66, 0.15),
    "sea_lantern": (0.67, 0.86, 0.84),
    "polished_deepslate": (0.18, 0.19, 0.20),
}


def srgb_to_linear(value):
    return value / 12.92 if value <= 0.04045 else ((value + 0.055) / 1.055) ** 2.4


BASIS = Matrix(((1, 0, 0), (0, 0, -1), (0, 1, 0)))
SIDES = {
    "up": ((0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)),
    "down": ((0, 0, 1), (1, 0, 1), (1, 0, 0), (0, 0, 0)),
    "north": ((1, 1, 0), (0, 1, 0), (0, 0, 0), (1, 0, 0)),
    "south": ((0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)),
    "east": ((1, 0, 1), (1, 0, 0), (1, 1, 0), (1, 1, 1)),
    "west": ((0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)),
}


def sample_color(image, uv):
    x0, y0, x1, y1 = uv
    colors = []
    for u in (0.35, 0.5, 0.65):
        for v in (0.35, 0.5, 0.65):
            x = min(image.size[0] - 1, max(0, int((x0 + (x1 - x0) * u) * image.size[0] / 16)))
            y = min(image.size[1] - 1, max(0, int((16 - (y0 + (y1 - y0) * v)) * image.size[1] / 16)))
            i = (y * image.size[0] + x) * 4
            pixel = image.pixels[i:i + 4]
            if pixel[3] > 0.5:
                colors.append(pixel[:3])
    if not colors:
        return (0.2, 0.2, 0.2)
    return tuple(sum(p[c] for p in colors) / len(colors) for c in range(3))


def nearest_block(color):
    # Compare in display color space, where the preview palette is defined.
    def distance(rgb):
        return sum((a - b) ** 2 * w for a, b, w in zip(color, rgb, (0.3, 0.59, 0.11)))
    return min(PALETTE, key=lambda name: distance(PALETTE[name]))


def point(box, corner, rotation):
    vector = Vector(tuple(box[corner[i]][i] for i in range(3)))
    if rotation:
        origin = Vector(rotation["origin"])
        axis = {"x": Vector((1, 0, 0)), "y": Vector((0, 1, 0)),
                "z": Vector((0, 0, 1))}[rotation["axis"]]
        vector = origin + Matrix.Rotation(math.radians(rotation["angle"]), 4, axis) @ (vector - origin)
    return tuple(BASIS @ ((vector - Vector((8, 8, 8))) / 4))


def material(name):
    if name not in bpy.data.materials:
        mat = bpy.data.materials.new(name)
        linear = tuple(srgb_to_linear(c) for c in PALETTE[name])
        mat.diffuse_color = (*linear, 1)
        mat.use_nodes = True
        bsdf = next(node for node in mat.node_tree.nodes if node.type == "BSDF_PRINCIPLED")
        bsdf.inputs["Base Color"].default_value = (*linear, 1)
        bsdf.inputs["Roughness"].default_value = 0.82
    return bpy.data.materials[name]


def build(name):
    data = json.loads((assets / "models" / "item" / (name + ".json")).read_text())
    atlas_id = data["textures"]["atlas"]
    namespace, path = atlas_id.split(":", 1)
    image = bpy.data.images.load(str(assets.parent / namespace / "textures" / (path + ".png")), check_existing=True)
    cuboids = defaultdict(list)
    for element in data["elements"]:
        key = (tuple(element["from"]), tuple(element["to"]),
               json.dumps(element.get("rotation"), sort_keys=True))
        for face in element["faces"].values():
            cuboids[key].append(sample_color(image, face["uv"]))
    vertices, faces, blocks = [], [], []
    for (lower, upper, rotation_json), colors in cuboids.items():
        average = tuple(sum(c[i] for c in colors) / len(colors) for i in range(3))
        block = nearest_block(average)
        box = (lower, upper)
        rotation = json.loads(rotation_json)
        for corners in SIDES.values():
            index = len(vertices)
            vertices.extend(point(box, corner, rotation) for corner in corners)
            faces.append((index, index + 1, index + 2, index + 3))
            blocks.append(block)
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(vertices, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    slots = {block: i for i, block in enumerate(sorted(set(blocks)))}
    for block in slots:
        mesh.materials.append(material(block))
    for polygon, block in zip(mesh.polygons, blocks):
        polygon.material_index = slots[block]
    return obj, len(cuboids), sorted(slots)


def camera(location, focus, size):
    obj = bpy.data.objects.get("Preview Camera")
    if obj is None:
        bpy.ops.object.camera_add()
        obj = bpy.context.object
        obj.name = "Preview Camera"
    obj.location = location
    obj.rotation_euler = (Vector(focus) - obj.location).to_track_quat("-Z", "Y").to_euler()
    obj.data.type = "ORTHO"
    obj.data.ortho_scale = size
    bpy.context.scene.camera = obj


scene = bpy.context.scene
scene.render.engine = "BLENDER_EEVEE"
scene.render.resolution_x = 720
scene.render.resolution_y = 720
scene.render.resolution_percentage = 100
scene.render.image_settings.file_format = "PNG"
scene.world.color = (0.10, 0.11, 0.13)
scene.view_settings.view_transform = "Standard"
scene.view_settings.look = "Medium High Contrast"
bpy.ops.object.light_add(type="AREA", location=(0, -5, 7))
light = bpy.context.object
light.data.energy = 1300
light.data.shape = "DISK"
light.data.size = 7
manifest = {}
for name in names:
    for obj in list(bpy.data.objects):
        if obj.type == "MESH":
            bpy.data.objects.remove(obj, do_unlink=True)
    model, count, blocks = build(name)
    ymin = min(v.co.z for v in model.data.vertices)
    ymax = max(v.co.z for v in model.data.vertices)
    focus = (0, 0, (ymin + ymax) / 2)
    framing = max(3.8, (ymax - ymin) * 1.35)
    camera((7, -10, max(3.5, ymax * 1.1)), focus, framing * 1.12)
    scene.render.filepath = str(output / (name + "-angle.png"))
    bpy.ops.render.render(write_still=True)
    manifest[name] = {"cuboids": count, "blocks": blocks}
    print(name, count, blocks)
(output / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
