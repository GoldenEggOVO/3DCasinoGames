"""Render identical close-ups of old JSON and the new JAR's actual display parts.

blender -b -t 4 --python render_curves.py -- old.json plugin.jar output
"""
import json
import sys
import zipfile
from pathlib import Path
import bpy
from mathutils import Matrix, Vector

before,jar,output = map(Path,sys.argv[sys.argv.index('--')+1:])
output = output.resolve(); output.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(jar) as archive:
    versions = [json.loads(before.read_text()),json.loads(archive.read('vanilla-models.json'))]
palette = json.loads(Path(__file__).with_name('palette.json').read_text())
basis = Matrix(((1,0,0,0),(0,0,-1,0),(0,1,0,0),(0,0,0,1)))
corners = [Vector((x,y,z,1)) for x,y,z in [(0,0,0),(1,0,0),(1,1,0),(0,1,0),
                                         (0,0,1),(1,0,1),(1,1,1),(0,1,1)]]
faces = [(3,2,1,0),(4,5,6,7),(0,1,5,4),(2,3,7,6),(1,2,6,5),(3,0,4,7)]


def matrix(part):
    if 'matrix' in part:
        m = part['matrix']; return Matrix([m[i:i+4] for i in range(0,16,4)])
    a,b = Vector(part['from']),Vector(part['to'])
    return (Matrix.Translation((a+b)/2) @ Matrix.Rotation(part.get('roll',0),4,'Z')
            @ Matrix.Rotation(part.get('pitch',0),4,'X') @ Matrix.Translation((a-b)/2)
            @ Matrix.Diagonal((*(b-a),1)))


def material(name):
    if name not in bpy.data.materials:
        rgb = [int(palette[name][i:i+2],16)/255 for i in (1,3,5)]
        rgb = [v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4 for v in rgb]
        mat = bpy.data.materials.new(name); mat.diffuse_color=(*rgb,1); mat.use_nodes=True
        shader = next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED')
        shader.inputs['Base Color'].default_value=(*rgb,1)
        shader.inputs['Roughness'].default_value=.85
    return bpy.data.materials[name]


scene = bpy.context.scene
for obj in list(bpy.data.objects): bpy.data.objects.remove(obj,do_unlink=True)
scene.render.engine='BLENDER_EEVEE'
scene.render.resolution_x=600; scene.render.resolution_y=600
scene.render.resolution_percentage=100; scene.render.image_settings.file_format='PNG'
scene.world.color=(.10,.11,.13)
scene.view_settings.view_transform='Standard'; scene.view_settings.look='Medium High Contrast'
bpy.ops.object.light_add(type='AREA',location=(0,-5,7))
light=bpy.context.object; light.data.energy=1300; light.data.size=7
bpy.ops.object.camera_add(); camera=bpy.context.object; scene.camera=camera; camera.data.type='ORTHO'
names=['showcase_wheel_fortune','showcase_button_round_spin','showcase_tile',
       'cabinet_button_play','showcase_slider','cabinet_blackjack','showcase_hilo','showcase_dragon_tower']
for name in names:
    framing=None
    for version,models in zip(('before','after'),versions):
        for obj in list(bpy.data.objects):
            if obj.type=='MESH': bpy.data.objects.remove(obj,do_unlink=True)
        all_vertices=[]
        model_name = 'showcase_dragon_tower_compact' if name=='showcase_dragon_tower' and version=='after' else name
        for part in models[model_name]['boxes']:
            m=basis @ matrix(part)
            vertices=[tuple(m @ p)[:3] for p in corners]; all_vertices += vertices
            data=bpy.data.meshes.new('part'); data.from_pydata(vertices,[],faces); data.update()
            obj=bpy.data.objects.new('part',data); bpy.context.collection.objects.link(obj)
            obj.data.materials.append(material(part['material']))
        if framing is None:
            lo=Vector([min(v[i] for v in all_vertices) for i in range(3)])
            hi=Vector([max(v[i] for v in all_vertices) for i in range(3)])
            focus=(lo+hi)/2
            direction=Vector((2,-8,3)) if name!='cabinet_blackjack' else Vector((5,-8,7))
            orientation=(-direction).to_track_quat('-Z','Y')
            projected=[orientation.inverted() @ (Vector(v)-focus) for v in all_vertices]
            framing=max(max(abs(v.x),abs(v.y)) for v in projected)*2.15
            camera.location=focus+direction; camera.rotation_euler=orientation.to_euler()
            camera.data.ortho_scale=framing
        scene.render.filepath=str(output/f'{name}-{version}.png')
        bpy.ops.render.render(write_still=True)
