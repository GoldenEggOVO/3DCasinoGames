"""Export original solid geometry as vanilla displays; never rewrite resource-pack assets.

Run with Python + Pillow. The original generators are loaded with their file writers
replaced, then their exposed surfaces are checked against the checked-in models.
Coordinates stay in original physical units (the runtime applies item scale / 4).
"""
import copy
import importlib.util
import json
import math
from pathlib import Path
from unittest.mock import patch
from PIL import Image
import vanilla_curves as curves

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'resource-pack/assets/3dcasino'
PALETTE = {
    'BLACK_CONCRETE': '#101116', 'GRAY_CONCRETE': '#373a3e',
    'LIGHT_GRAY_CONCRETE': '#7d7d73', 'WHITE_CONCRETE': '#d6d6ce',
    'RED_CONCRETE': '#8e2020', 'RED_TERRACOTTA': '#8f3d2f',
    'ORANGE_CONCRETE': '#e06101', 'YELLOW_CONCRETE': '#f1af15',
    'LIME_CONCRETE': '#5ea818', 'GREEN_CONCRETE': '#495b24',
    'CYAN_CONCRETE': '#157788', 'LIGHT_BLUE_CONCRETE': '#2489c7',
    'BLUE_CONCRETE': '#2c2e8f', 'PURPLE_CONCRETE': '#64209c',
    'MAGENTA_CONCRETE': '#a9309f', 'PINK_CONCRETE': '#d5658e',
    'BROWN_CONCRETE': '#603b1f', 'DARK_OAK_PLANKS': '#432b14',
    'OAK_PLANKS': '#a0804e', 'SMOOTH_SANDSTONE': '#d5c999',
    'GOLD_BLOCK': '#e5b52b', 'SEA_LANTERN': '#a8c6bb',
    'DARK_PRISMARINE': '#335b4b', 'WARPED_PLANKS': '#2b6863',
    'PURPUR_BLOCK': '#aa7eaa', 'BLUE_TERRACOTTA': '#4a3c5b',
    'POLISHED_DEEPSLATE': '#48484b', 'SMOOTH_QUARTZ': '#e9e4db',
}


def rgb(color):
    return tuple(bytes.fromhex(color.lstrip('#')))


def nearest(color):
    color = rgb(color) if isinstance(color, str) else color[:3]
    return min(PALETTE, key=lambda name: sum((a-b)**2
               for a, b in zip(color, rgb(PALETTE[name]))))


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def rectangles(grid):
    """Merge equal adjacent pixels without filling transparent pixels."""
    grid = [row[:] for row in grid]
    for y, row in enumerate(grid):
        for x, material in enumerate(row):
            if material is None:
                continue
            right = x + 1
            while right < len(row) and grid[y][right] == material:
                right += 1
            bottom = y + 1
            while bottom < len(grid) and all(grid[bottom][i] == material for i in range(x, right)):
                bottom += 1
            for j in range(y, bottom):
                for i in range(x, right):
                    grid[j][i] = None
            yield x, y, right, bottom, material


def box(lo, hi, material):
    return {'from': [round(v, 6) for v in lo], 'to': [round(v, 6) for v in hi], 'material': material}


def subtract(part, cutter):
    """Split a solid around its intersection; later decorative materials win."""
    lo = [max(a, b) for a, b in zip(part['from'], cutter['from'])]
    hi = [min(a, b) for a, b in zip(part['to'], cutter['to'])]
    if any(b-a < 1e-7 for a, b in zip(lo, hi)):
        return [part]
    remaining, a, b = [], part['from'][:], part['to'][:]
    for axis in range(3):
        if lo[axis] - a[axis] > 1e-7:
            edge = b[:]; edge[axis] = lo[axis]
            remaining.append(box(a, edge, part['material']))
            a[axis] = lo[axis]
        if b[axis] - hi[axis] > 1e-7:
            edge = a[:]; edge[axis] = hi[axis]
            remaining.append(box(edge, b, part['material']))
            b[axis] = hi[axis]
    return remaining


def disjoint(boxes):
    result = []
    for part in boxes:
        if part.get('matrix') or part.get('roll') or part.get('pitch'):
            result.append(part)
            continue  # Raised rotated trim occupies its own depth layer.
        result = [piece for previous in result for piece in
                  ([previous] if previous.get('matrix') or previous.get('roll') or previous.get('pitch')
                   else subtract(previous, part))]
        result.append(part)
    return result


def wheel(name, showcase):
    segments = showcase.FORTUNE_SEGMENTS if name.endswith('fortune') else showcase.MONEY_SEGMENTS
    materials = ({-1:'PURPLE_CONCRETE', 0:'GRAY_CONCRETE', .1:'CYAN_CONCRETE',
                  .25:'LIGHT_BLUE_CONCRETE', .5:'LIME_CONCRETE', 2:'ORANGE_CONCRETE',
                  3:'PINK_CONCRETE', 5:'GOLD_BLOCK'} if name.endswith('fortune') else
                 {0:'DARK_PRISMARINE', 1:'BLUE_CONCRETE', 2:'RED_TERRACOTTA', 3:'GOLD_BLOCK'})
    return curves.wheel(segments, materials)


def refined_mines(original):
    # Preserve the table footprint and legs; the approved reference changes its finish
    # and replaces the old shelf with one continuous sloping control console.
    boxes = copy.deepcopy([p for p in original['boxes'] if p['from'][2] < 1.39])
    for part in boxes:
        part['material'] = {'DARK_PRISMARINE': 'BLACK_CONCRETE',
                            'CYAN_CONCRETE': 'ORANGE_CONCRETE',
                            'SMOOTH_SANDSTONE': 'ORANGE_CONCRETE'}.get(part['material'], part['material'])
    boxes += [box((-1.45, 1.0, 1.30), (1.45, 1.10, 1.52), 'ORANGE_CONCRETE'),
              box((-1.50, 1.10, -1.54), (1.50, 1.25, -1.36), 'BROWN_CONCRETE')]
    for x in (-1.52, 1.52):
        for z in (-1.43, 1.31):
            boxes += [box((x-.075, 1.105, z-.075), (x+.075, 1.20, z+.075), 'BLACK_CONCRETE'),
                      box((x-.045, 1.20, z-.045), (x+.045, 1.23, z+.045), 'GRAY_CONCRETE')]
    boxes = disjoint(boxes)
    console = [box((-1.28, -.04, -.14), (1.28, .73, -.12), 'BLACK_CONCRETE'),
               box((-1.28, -.04, -.12), (1.28, .73, 0), 'ORANGE_CONCRETE'),
               box((-1.15, .035, 0), (1.15, .67, .018), 'BROWN_CONCRETE')]
    for x in (-1.215, 1.215):
        for y in (.025, .66):
            console.append(box((x-.016, y-.016, 0), (x+.016, y+.016, .014), 'BLACK_CONCRETE'))
    pitch = -math.radians(35)
    for part in disjoint(console):
        a, b = part['from'], part['to']
        c = [(lo+hi)/2 for lo, hi in zip(a, b)]
        center = [c[0], .67+c[1]*math.cos(pitch)-c[2]*math.sin(pitch),
                  1.96+c[1]*math.sin(pitch)+c[2]*math.cos(pitch)]
        rotated = box([v-(hi-lo)/2 for v, lo, hi in zip(center,a,b)],
                      [v+(hi-lo)/2 for v, lo, hi in zip(center,a,b)], part['material'])
        rotated['pitch'] = pitch
        boxes.append(rotated)
    return {'boxes': boxes, 'labels': []}


def compact_dragon(original):
    # Preserve the original four-column curvature; remove the table and lower the board.
    back = [copy.deepcopy(p) for p in original['boxes']
            if p.get('matrix') and abs(p['matrix'][5]-1.9797)<1e-6]
    boxes = curves.transform(back,[1,0,0,0, 0,1,0,-.5, 0,0,1,0, 0,0,0,1])
    for x in (-.775,.775):
        boxes.append(box((x-.065,.15,-.04),(x+.065,2.62,.27),'RED_CONCRETE'))
    boxes.append(box((-.65,.18,.28),(.65,.57,.45),'RED_CONCRETE'))
    for radius,depth,y0,y1,material in [(.9,.45,0,.15,'BLACK_CONCRETE'),
                                       (.86,.40,2.56,2.73,'RED_CONCRETE')]:
        cap = curves.disc(0,0,1,-y1,-y0,material,32)
        boxes += curves.transform(cap,[radius,0,0,0, 0,0,-1,0, 0,depth,0,.13, 0,0,0,1])
    boxes.append(box((-.64,2.59,.30),(.64,2.71,.55),'BLACK_CONCRETE'))
    return {'boxes':disjoint(boxes),'labels':[{'text':'DRAGON TOWER',
             'position':[0,2.65,.553],'width':1.20,'height':.085,'color':0xffffff}]}


def dragon_tiles():
    models={}
    for state,material in [('hidden','GRAY_TERRACOTTA'),('safe','EMERALD_BLOCK'),('trap','TNT')]:
        models['dragon_tile_'+state]={
            'boxes':[box((-.14,-.14,-.14),(.14,.14,.14),material)],'labels':[]}
    return models


def export():
    original = load('original_cabinets', ROOT / 'tools/build-casino-cabinets.py')
    showcase = load('original_showcase', ROOT / 'tools/build-showcase-machines.py')
    raw, labels = {}, {}

    def capture(body):
        raw['cabinet_' + body.name] = copy.deepcopy(body.elements)
        labels['cabinet_' + body.name] = original.LABELS.get(body.name, '')

    def emit(name, elements, model_texture=None, clipped=False):
        if name.startswith('number_'):
            return
        if name.startswith('button_') and name != 'button_round_spin':
            elements = raw['cabinet_button_play']
        raw['showcase_' + name] = copy.deepcopy(elements)

    def texture(name, label='', color=None, button=False):
        labels['showcase_' + name] = label

    with patch.object(original.Body, 'write', capture), patch.object(Image.Image, 'save', lambda *a, **k: None):
        original.plinko(); original.mines(); original.blackjack(); original.blackjack_screen()
        original.crash(); original.accessories()
        with patch.object(showcase, 'emit', emit), patch.object(showcase, 'texture', texture), \
                patch.object(showcase, 'centered', lambda *a, **k: None):
            showcase.cabinets(); showcase.accessories()

    models = {}
    for name, elements in raw.items():
        path = ASSETS / f'models/item/{name}.json'
        if not path.exists():
            continue  # Retired colour variants are still defined in the original generator.
        source = json.loads(path.read_text())
        # Exact surface equality guards against a stale generator or changed resource geometry.
        assert original.exposed_faces(elements) == source['elements'], name
        image = Image.open(ASSETS / f'textures/item/{name}.png').convert('RGBA')
        boxes, text = [], []
        for element in elements:
            lo, hi = [[(v-8)/4 for v in element[k]] for k in ('from', 'to')]
            face = element['faces'].get('up', next(iter(element['faces'].values())))
            u0, v0, u1, v1 = face['uv']
            color = image.getpixel((min(image.width-1, int((u0+u1)/32*image.width)),
                                    min(image.height-1, int((v0+v1)/32*image.height))))
            material = nearest(color)
            tile = int(v0//4)*4+int(u0//4)
            if name.startswith('showcase_') and tile == 2 and (u1-u0) < 4:
                material = 'GOLD_BLOCK'
            for i, selected in enumerate(['YELLOW_CONCRETE', 'PINK_CONCRETE', 'LIGHT_BLUE_CONCRETE', 'LIME_CONCRETE'], 1):
                if name in (f'showcase_duck_{i}', f'showcase_button_duck_{i}') and tile in (0, 5):
                    material = selected
            boxes.append(box(lo, hi, material))
            if element['faces'].get('south', {}).get('uv') in ([0, 12, 16, 16], [0, 8, 16, 16]):
                label = labels.get(name)
                if label:
                    sign = element['faces']['south']['uv'] == [0,12,16,16]
                    if sign:
                        # Replace the thin front slice with a black sign and gold frame.
                        boxes[-1]['to'][2] -= .001
                        x0,y0,z0 = lo; x1,y1,z1 = hi
                        border = min((y1-y0)*.045, (x1-x0)*.018)
                        boxes.extend([
                            box((x0,y0,z1-.001),(x1,y0+border,z1),'GOLD_BLOCK'),
                            box((x0,y1-border,z1-.001),(x1,y1,z1),'GOLD_BLOCK'),
                            box((x0,y0+border,z1-.001),(x0+border,y1-border,z1),'GOLD_BLOCK'),
                            box((x1-border,y0+border,z1-.001),(x1,y1-border,z1),'GOLD_BLOCK'),
                            box((x0+border,y0+border,z1-.001),(x1-border,y1-border,z1),'BLACK_CONCRETE')])
                    text.append({'text': label, 'position': [(lo[0]+hi[0])/2, (lo[1]+hi[1])/2, hi[2]+.003],
                                 'width': (hi[0]-lo[0])*.88, 'height': (hi[1]-lo[1])*.72,
                                 'color': 0xf6edcf if sign else 0x172b28})
        if name == 'showcase_slider':
            boxes = curves.ring(0,0,.13,.025,-.013,.010,'SMOOTH_SANDSTONE',32)
            boxes += curves.disc(0,0,.111,-.010,.013,'YELLOW_CONCRETE',32)
        if name == 'showcase_button_round_spin':
            boxes = curves.disc(0,.2,.2,0,.16,'ORANGE_CONCRETE')
            text = [{'text': 'SPIN', 'position': [0, .2, .163], 'width': .29, 'height': .10, 'color': 0x172b28}]
        elif name.startswith(('cabinet_button_', 'showcase_button_')):
            boxes = curves.buttons(name)
        if name in ('showcase_tile', 'showcase_tile_selected'):
            boxes = curves.rounded_rectangle(0,0,.24,.24,.04,-.035,.035,
                        'ORANGE_CONCRETE' if name.endswith('_selected') else 'BLUE_TERRACOTTA')
        if name == 'showcase_hilo':
            boxes = curves.hilo(boxes)
        if name == 'showcase_dragon_tower':
            boxes = curves.dragon(boxes)
        if name == 'cabinet_blackjack':
            boxes = curves.blackjack(boxes)
            for part in boxes:
                part['material']={'DARK_PRISMARINE':'GREEN_CONCRETE',
                                  'DARK_OAK_PLANKS':'BROWN_CONCRETE','OAK_PLANKS':'BROWN_CONCRETE',
                                  'GOLD_BLOCK':'YELLOW_CONCRETE'}.get(part['material'],part['material'])
        if name == 'showcase_slots':
            boxes = [p for p in boxes if p['from'] != [-.72,.88,.55]]
        if name in ('showcase_wheel_fortune', 'showcase_wheel_money'):
            boxes = wheel(name, showcase)
        if name == 'showcase_hilo_panel':
            boxes[0]['material'] = 'GRAY_CONCRETE'
            for i in range(11):
                x = -1.1 + i*.22
                boxes.append(box((x-.004, -.12, .036), (x+.004, -.07, .04), 'SMOOTH_QUARTZ'))
                text.append({'text': str(i*10), 'position': [x, -.21, .041], 'width': .16, 'height': .08})
        models[name] = {'boxes': disjoint(boxes), 'labels': text}
    models['cabinet_mines_refined'] = refined_mines(models['cabinet_mines'])
    models['showcase_dragon_tower_compact'] = compact_dragon(models['showcase_dragon_tower'])
    models.update(dragon_tiles())
    models.update(cards())
    return models


# Original pixel lettering and suits; no font-dependent TextDisplay sizing on cards.
GLYPHS = dict(zip('A234567890JQK', [
    '01110/11011/10001/11111/10001/10001/10001',
    '01110/10001/00001/00110/01000/10000/11111',
    '11110/00001/00001/01110/00001/00001/11110',
    '00010/00110/01010/10010/11111/00010/00010',
    '11111/10000/10000/11110/00001/00001/11110',
    '01110/10000/10000/11110/10001/10001/01110',
    '11111/00001/00010/00100/01000/01000/01000',
    '01110/10001/10001/01110/10001/10001/01110',
    '01110/10001/10001/01111/00001/00001/01110',
    '01110/10001/10011/10101/11001/10001/01110',
    '00111/00010/00010/00010/10010/10010/01100',
    '01110/10001/10001/10001/10101/10010/01101',
    '10001/10010/10100/11000/10100/10010/10001']))
GLYPHS['1'] = '00100/01100/00100/00100/00100/00100/01110'
def suit_pattern(suit):
    def contains(x, y):
        def circle(cx, cy): return (x-cx)**2+(y-cy)**2 <= 16
        stem = y >= 10 and abs(x-8) <= max(1, (y-13)*.75)
        if suit == 0:  # Pointed spade, two lower lobes, solid central stem.
            return (y <= 9 and abs(x-8) <= y*7/9) or circle(5,9) or circle(11,9) or stem
        if suit == 1:
            return circle(5,5) or circle(11,5) or (5 <= y <= 16 and abs(x-8) <= (16-y)*7/11)
        if suit == 2:  # Three distinct round lobes distinguish clubs from spades.
            lobes = any((x-cx)**2+(y-cy)**2 <= 9 for cx,cy in [(8,3),(3,10),(13,10)])
            return lobes or (y >= 5 and abs(x-8) <= 1) or (9 <= y <= 11 and 3 <= x <= 13) or stem
        return abs(x-8)/6 + abs(y-8)/8 <= 1
    pixels = [(x,y) for y in range(17) for x in range(17) if contains(x,y)]
    x0,x1 = min(p[0] for p in pixels), max(p[0] for p in pixels)
    y0,y1 = min(p[1] for p in pixels), max(p[1] for p in pixels)
    return '/'.join(''.join('1' if contains(x,y) else '0' for x in range(x0,x1+1)) for y in range(y0,y1+1))


SUITS = [suit_pattern(suit) for suit in range(4)]


def ink(pattern, cx, cy, width, height, material, inverted=False):
    rows = pattern.split('/')
    if inverted:
        rows = [row[::-1] for row in rows[::-1]]
    grid = [[material if c == '1' else None for c in row] for row in rows]
    return [box((cx-width/2+x/len(rows[0])*width, cy+height/2-b/len(rows)*height, .025),
                (cx-width/2+r/len(rows[0])*width, cy+height/2-y/len(rows)*height, .030), m)
            for x,y,r,b,m in rectangles(grid)]


def pip_positions(value):
    if value == 1:
        return [(0, 0)]
    if value <= 3:
        return [(0, 1.25), (0, -1.25)] + ([(0, 0)] if value == 3 else [])
    rows = [-1.25, 1.25] if value <= 5 else [-1.25, 0, 1.25] if value <= 8 else [-1.35, -.45, .45, 1.35]
    points = [(x, y) for x in [-.78, .78] for y in rows]
    if value in (5, 9): points.append((0, 0))
    if value == 7: points.append((0, .625))
    if value in (8, 10): points.extend([(0, .8), (0, -.8)])
    return points


def cards():
    result = {}
    for index in range(53):
        # Match original card width/height at the existing .43 ItemDisplay scale.
        boxes = [box((-2, -8/3, -.02), (2, 8/3, .02), 'WHITE_CONCRETE')]
        if index == 52:
            boxes.append(box((-1.78, -2.44, .021), (1.78, 2.44, .025), 'BLUE_CONCRETE'))
            for y in [-1.6, -.8, 0, .8, 1.6]:
                for x in [-1.15, -.575, 0, .575, 1.15]:
                    boxes += ink(SUITS[3], x, y, .28, .40, 'LIGHT_BLUE_CONCRETE')
        else:
            rank = ['A', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K'][index % 13]
            suit = SUITS[index // 13]
            color = 'RED_CONCRETE' if index // 13 in (1, 3) else 'BLACK_CONCRETE'
            rank_pattern = '/'.join('0'.join(GLYPHS[c].split('/')[row] for c in rank) for row in range(7))
            for inverted in [False, True]:
                sign = -1 if inverted else 1
                boxes += ink(rank_pattern, -1.56*sign, 2.08*sign, .55, .65, color, inverted)
                boxes += ink(suit, -1.56*sign, 1.49*sign, .43, .47, color, inverted)
            value = index % 13 + 1
            if value > 10:
                boxes += ink(rank_pattern, 0, .38, 1.10, 1.55, color)
                boxes += ink(suit, 0, -1.05, .74, .80, color)
            else:
                for x, y in pip_positions(value):
                    size = 1.6 if value == 1 else .56
                    boxes += ink(suit, x, y, size, size*1.10, color, y < 0)
        result[f'card_{index}'] = {'boxes': boxes, 'labels': []}
    return result


if __name__ == '__main__':
    models = export()
    destination = ROOT / 'src/main/resources/vanilla-models.json'
    destination.write_text(json.dumps(models, separators=(',', ':')) + '\n', encoding='utf-8')
    print(f'Exported {len(models)} models; all original solid surfaces verified')
    print({name: len(model['boxes']) for name, model in models.items() if name in
           ('cabinet_plinko', 'showcase_wheel_fortune', 'showcase_slots', 'card_47')})
