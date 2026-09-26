"""Render the actual server Display snapshots; compare identical poses with resource models.

Usage: blender -b -t 4 --python render_runtime.py -- <snapshots> <output>
Optional: --vanilla-assets <assets-directory> supplies dragon tile block textures.
These are entity-geometry previews, not Minecraft screenshots. Vanilla materials
use measured palette colours; client textures/font rasterization are not simulated.
"""
import json
import math
import sys
from pathlib import Path
import bpy
from mathutils import Matrix, Quaternion, Vector

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
# Do not import the exporter: Blender does not need Pillow.
PALETTE = json.loads((ROOT / 'tools/vanilla-preview/palette.json').read_text())
ASSETS = ROOT / 'resource-pack/assets'
source = Path(sys.argv[sys.argv.index('--')+1])
output = Path(sys.argv[sys.argv.index('--')+2]).resolve(); output.mkdir(parents=True, exist_ok=True)
selected = sys.argv[sys.argv.index('--')+3:]
vanilla_assets = None
if '--vanilla-assets' in selected:
 index=selected.index('--vanilla-assets'); vanilla_assets=Path(selected[index+1]).resolve(); del selected[index:index+2]
before = None
if '--before' in selected:
 index=selected.index('--before'); before=Path(selected[index+1]); del selected[index:index+2]
vanilla_only = '--vanilla-only' in selected
if vanilla_only: selected.remove('--vanilla-only')
rear = '--rear' in selected
if rear: selected.remove('--rear')
view_direction = Vector((7,10,6) if rear else (7,-10,7))
BASIS = Matrix(((1,0,0,0),(0,0,-1,0),(0,1,0,0),(0,0,0,1)))
SIDES = {
 'up': ((0,1,0),(1,1,0),(1,1,1),(0,1,1)),
 'down': ((0,0,1),(1,0,1),(1,0,0),(0,0,0)),
 'north': ((1,1,0),(0,1,0),(0,0,0),(1,0,0)),
 'south': ((0,0,1),(1,0,1),(1,1,1),(0,1,1)),
 'east': ((1,0,1),(1,0,0),(1,1,0),(1,1,1)),
 'west': ((0,0,0),(0,0,1),(0,1,1),(0,1,0))}


def linear(v):
 return v/12.92 if v <= .04045 else ((v+.055)/1.055)**2.4


def material(name):
 if name not in bpy.data.materials:
  color = PALETTE.get(name, '#899b99')
  rgb = tuple(linear(int(color[i:i+2],16)/255) for i in (1,3,5))
  mat=bpy.data.materials.new(name); mat.diffuse_color=(*rgb,1);mat.use_nodes=True
  shader=next(n for n in mat.node_tree.nodes if n.type == 'BSDF_PRINCIPLED');shader.inputs['Base Color'].default_value=(*rgb,1)
  shader.inputs['Roughness'].default_value=.85
 return bpy.data.materials[name]


def textured(path):
 name=str(path)
 if name not in bpy.data.materials:
  mat=bpy.data.materials.new(name);mat.use_nodes=True
  shader=next(n for n in mat.node_tree.nodes if n.type == 'BSDF_PRINCIPLED')
  shader.inputs['Roughness'].default_value=.85
  node=mat.node_tree.nodes.new('ShaderNodeTexImage');node.image=bpy.data.images.load(name,check_existing=True);node.interpolation='Closest'
  mat.node_tree.links.new(node.outputs['Color'],shader.inputs['Base Color'])
  mat.node_tree.links.new(node.outputs['Alpha'],shader.inputs['Alpha'])
 return bpy.data.materials[name]


def pose(entry):
 def quat(key):
  x,y,z,w=entry[key];return Quaternion((w,x,y,z)).to_matrix().to_4x4()
 return Matrix.Translation(Vector(entry['position'])) @ Matrix.Translation(Vector(entry['translation'])) @ quat('leftRotation') @ Matrix.Diagonal((*entry['scale'],1)) @ quat('rightRotation')


def mesh(name, vertices, faces, materials, indices=None, uvs=None):
 data=bpy.data.meshes.new(name);data.from_pydata(vertices,[],faces);data.update()
 obj=bpy.data.objects.new(name,data);bpy.context.collection.objects.link(obj)
 for mat in materials:data.materials.append(mat)
 if indices:
  for poly,index in zip(data.polygons,indices):poly.material_index=index
 if uvs:
  layer=data.uv_layers.new()
  for poly,quad in zip(data.polygons,uvs):
   for loop,uv in zip(poly.loop_indices,quad):layer.data[loop].uv=uv
 return obj


def cube(matrix,name):
 vertices=[];faces=[];uvs=[];indices=[];mats=[]
 block_textures={'GRAY_TERRACOTTA':('gray_terracotta',)*3,
                 'EMERALD_BLOCK':('emerald_block',)*3,'TNT':('tnt_top','tnt_bottom','tnt_side')}
 if vanilla_assets and name in block_textures:
  mats=[textured(vanilla_assets/'minecraft/textures/block'/f'{texture}.png') for texture in block_textures[name]]
 for side,corners in SIDES.items():
  start=len(vertices);vertices += [tuple(BASIS @ matrix @ Vector((*p,1)))[:3] for p in corners]
  faces.append(tuple(range(start,start+4)))
  indices.append(0 if side=='up' else 1 if side=='down' else 2)
  uvs.append([(0,1),(1,1),(1,0),(0,0)] if side=='north' else [(0,0),(1,0),(1,1),(0,1)])
 return mesh(name,vertices,faces,mats or [material(name)],indices if mats else None,uvs if mats else None)


def original_model(entry):
 name=entry['model'].replace('cabinet_mines_refined','cabinet_mines');data=json.loads((ASSETS/'3dcasino/models/item'/f'{name}.json').read_text())
 matrix=pose(entry) @ Matrix.Rotation(math.pi,4,'Y')
 textures={};mats=[]
 for key,value in data['textures'].items():
  if ':' not in value:continue
  namespace,path=value.split(':',1);textures['#'+key]=len(mats);mats.append(textured(ASSETS/namespace/'textures'/(path+'.png')))
 vertices=[];faces=[];uvs=[];indices=[]
 for e in data['elements']:
  for side,face in e['faces'].items():
   start=len(vertices)
   for corner in SIDES[side]:
    p=Vector(tuple((e['to' if corner[i] else 'from'][i]-8)/16 for i in range(3)))
    vertices.append(tuple(BASIS @ matrix @ p.to_4d())[:3])
   faces.append(tuple(range(start,start+4)));indices.append(textures[face['texture']])
   u0,v0,u1,v1=face.get('uv',[0,0,16,16]);uvs.append([(u0/16,1-v1/16),(u1/16,1-v1/16),(u1/16,1-v0/16),(u0/16,1-v0/16)])
 return mesh(name,vertices,faces,mats,indices,uvs)


def entity(entry,mode):
 if mode=='resource' and entry.get('vanillaChild'):return
 if mode=='resource' and entry.get('model'):return original_model(entry)
 if not entry.get('visible',True):return
 matrix=pose(entry);kind=entry['kind'];name=entry.get('material','GRAY_CONCRETE')
 if kind.endswith('TextDisplay'):
  text=entry.get('text','')
  if not text:return
  bpy.ops.object.text_add();obj=bpy.context.object;obj.data.body=text
  obj.data.align_x='CENTER';obj.data.size=.25;obj.data.space_line=1.1
  obj.data.font=font;obj.matrix_world=BASIS @ matrix
  color=entry.get('color',0xffffff);key='text-'+hex(color);PALETTE[key]=f'#{color:06x}'
  obj.data.materials.append(material(key));return
 if kind.endswith('ItemDisplay'):
  matrix=matrix @ Matrix.Rotation(math.pi,4,'Y')
  # Native block items are centred. Sprite props are an explicitly approximate thin plane.
  sprite=name in ('EMERALD','SLIME_BALL','DIAMOND','GOLD_INGOT','PAPER')
  matrix=matrix @ Matrix.Diagonal((.7,.7,.03,1) if sprite else (1,1,1,1)) @ Matrix.Translation(Vector((-.5,-.5,-.5)))
 return cube(matrix,name)


def camera(location,focus,size):
 obj=bpy.data.objects.get('Preview Camera')
 if obj is None:
  bpy.ops.object.camera_add();obj=bpy.context.object;obj.name='Preview Camera'
 obj.location=location;obj.rotation_euler=(Vector(focus)-obj.location).to_track_quat('-Z','Y').to_euler()
 obj.data.type='ORTHO';obj.data.ortho_scale=size;bpy.context.scene.camera=obj


scene=bpy.context.scene;scene.render.engine='BLENDER_EEVEE'
scene.render.resolution_x=900;scene.render.resolution_y=900;scene.render.resolution_percentage=100
scene.render.image_settings.file_format='PNG';scene.world.color=(.10,.11,.13)
scene.view_settings.view_transform='Standard';scene.view_settings.look='Medium High Contrast'
font=bpy.data.fonts.load('C:/Windows/Fonts/msyh.ttc')
bpy.ops.object.light_add(type='AREA', location=(0,-5,7));light=bpy.context.object
light.data.energy=1300;light.data.shape='DISK';light.data.size=7
for path in sorted(source.glob('*.json')):
 if selected and path.stem not in selected:continue
 entries=json.loads(path.read_text(encoding='utf-8'))
 framing=None
 variants=[('vanilla',entries,'vanilla')]
 if before and (before/path.name).exists():
  variants.insert(0,('before',json.loads((before/path.name).read_text(encoding='utf-8')),'vanilla'))
 elif not vanilla_only and not before:
  variants.append(('resource',entries,'resource'))
 if before:
  for obj in list(bpy.data.objects):
   if obj.type in ('MESH','FONT'):bpy.data.objects.remove(obj,do_unlink=True)
  for _,data,mode in variants:
   for entry in data:entity(entry,mode)
  vertices=[obj.matrix_world @ Vector(v) for obj in bpy.context.scene.objects if obj.type=='MESH' for v in obj.bound_box]
  lo=Vector(tuple(min(v[i] for v in vertices) for i in range(3)));hi=Vector(tuple(max(v[i] for v in vertices) for i in range(3)))
  focus=(lo+hi)/2
  view_rotation=(-view_direction).to_track_quat('-Z','Y').inverted()
  projected=[view_rotation @ (v-focus) for v in vertices]
  framing=max(max(abs(v.x),abs(v.y)) for v in projected)*2.16
 for name,data,mode in variants:
  for obj in list(bpy.data.objects):
   if obj.type in ('MESH','FONT'):bpy.data.objects.remove(obj,do_unlink=True)
  for entry in data:entity(entry,mode)
  if framing is None:
   vertices=[obj.matrix_world @ Vector(v) for obj in bpy.context.scene.objects if obj.type=='MESH' for v in obj.bound_box]
   lo=Vector(tuple(min(v[i] for v in vertices) for i in range(3)));hi=Vector(tuple(max(v[i] for v in vertices) for i in range(3)))
   focus=(lo+hi)/2
   view_rotation=(-view_direction).to_track_quat('-Z','Y').inverted()
   projected=[view_rotation @ (v-focus) for v in vertices]
   framing=max(max(abs(v.x),abs(v.y)) for v in projected)*2.16
  camera(focus+view_direction,focus,framing)
  suffix='-rear' if rear else ''
  scene.render.filepath=str(output/f'{path.stem}-{name}{suffix}.png');bpy.ops.render.render(write_still=True)
  print(path.stem,mode,flush=True)
