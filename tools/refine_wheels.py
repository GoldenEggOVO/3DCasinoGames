"""Rebuild only Casino wheel assets; pure vanilla cuboids, no client resource pack."""
import json
import math
from pathlib import Path
from vanilla_curves import cuboid, disc, ring

ROOT = Path(__file__).resolve().parents[1]
FORTUNE = [-1,3,.1,.5,.25,5,.1,.25,2,.1,-1,3,.5,.1,.25,0,.5,.1,2,.25]
MONEY = [0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,1,0,2,0,1]
COLORS = ['LIME_CONCRETE','BLUE_CONCRETE','RED_CONCRETE','ORANGE_CONCRETE']

def cabinet(money):
    color = 'BROWN_CONCRETE' if money else 'PURPLE_CONCRETE'
    trim = 'YELLOW_CONCRETE' if money else 'PINK_CONCRETE'
    parts = disc(0,2,.975,-.22,.10,color,64)
    parts += ring(0,2,.972,.065,.101,.214,'BLACK_CONCRETE',64)
    parts += ring(0,2,.963,.022,.215,.232,trim,64)
    parts += [cuboid((0,.07,.03),(1.58,.14,1.0),color)]
    # A filled tapered stand with angled rails defining its continuous silhouette.
    for i in range(9):
        y=.15+i*.19
        parts.append(cuboid((0,y+.095,-.045),(1.30-i*.085,.19,.53),color))
    for sign in (-1,1):
        parts.append(cuboid((sign*.48,.96,-.07),(.14,1.82,.64),color,roll=sign*math.radians(13)))
    parts += [cuboid((0,1.97,-.30),(.25,.25,.22),'YELLOW_CONCRETE',roll=math.pi/4),
              cuboid((0,.50,-.324),(.55,.40,.026),'BLACK_CONCRETE')]
    for i in range(5):
        parts.append(cuboid((0,.365+i*.065,-.344),(.43,.018,.012),'GRAY_CONCRETE'))
    for x in (-.235,.235):
        for y in (.34,.66):
            parts.append(cuboid((x,y,-.347),(.022,.022,.018),'LIGHT_GRAY_CONCRETE',roll=math.pi/4))
    for i in range(40):
        a=i*math.tau/40
        x,y=math.sin(a)*.925,2+math.cos(a)*.925
        parts.append(cuboid((x,y,.246),(.034,.034,.024),trim,roll=math.pi/4))
    if money:
        # Thin bill ornaments, outside the choice-button sight lines.
        for sign in (-1,1):
            for i in range(3):
                parts.append(cuboid((sign*(.90+i*.032),.98+i*.026,.35-i*.024),
                    (.22,.42,.014),'GREEN_CONCRETE',roll=-sign*math.radians(10+i*5)))
        parts += ring(0,2,.24,.025,.278,.298,'ORANGE_CONCRETE',32)
    return {'boxes':parts,'labels':[]}

def wheel(money):
    parts=[]
    if money:
        fill = disc(0,0,.86,-.015,.015,'BLACK_CONCRETE',64)
        # Float decomposition of a spinning carrier can turn touching row edges
        # into coplanar overlaps. A 0.04 mm clearance keeps them disjoint.
        for box in fill[:-64]:
            box['matrix'][5] -= .00004
            box['matrix'][7] += .00002
        parts += fill
        parts += ring(0,0,.64,.014,.016,.022,'WHITE_CONCRETE',64)
        parts += ring(0,0,.32,.014,.023,.028,'ORANGE_CONCRETE',32)
    values=MONEY if money else FORTUNE
    materials={-1:'GRAY_CONCRETE',0:'RED_CONCRETE',.1:'RED_CONCRETE',.25:'ORANGE_CONCRETE',
               .5:'BROWN_CONCRETE',2:'BLUE_CONCRETE',3:'BLUE_CONCRETE',5:'PURPLE_CONCRETE'}
    inner=.72 if money else .16
    rows=6 if money else 22
    for i,value in enumerate(values):
        angle=-(i+.5)*math.tau/20
        for row in range(rows):
            low=inner+(.848-inner)*row/rows
            high=inner+(.848-inner)*(row+1)/rows
            r=(low+high)/2
            parts.append(cuboid((-math.sin(angle)*r,math.cos(angle)*r,.036+(i%2)*.00035),
                (2*r*math.tan(math.pi/20)-.006,high-low,.018),
                COLORS[value] if money else materials[value],roll=angle))
    parts += ring(0,0,.869,.023,.048,.064,'YELLOW_CONCRETE' if money else 'ORANGE_CONCRETE',64)
    if money:
        parts += ring(0,0,.716,.014,.049,.061,'YELLOW_CONCRETE',64)
    else:
        for i in range(20):
            a=-i*math.tau/20;r=(.16+.853)/2
            parts.append(cuboid((-math.sin(a)*r,math.cos(a)*r,.048),(.004,.693,.007),'ORANGE_CONCRETE',roll=a))
    return {'boxes':parts,'labels':[]}

def models():
    return {'showcase_wheel_of_fortune':cabinet(False),'showcase_money_wheel':cabinet(True),
            'showcase_wheel_fortune':wheel(False),'showcase_wheel_money':wheel(True),
            'showcase_pointer_fortune':pointer('BLACK_CONCRETE'),
            'showcase_pointer_money':pointer('WHITE_CONCRETE')}

def pointer(color):
    return {'boxes':[cuboid((0,0,0),(.105,.105,.065),color,roll=math.pi/4),
                     cuboid((0,.011,.037),(.063,.063,.012),
                            'GRAY_CONCRETE' if color == 'BLACK_CONCRETE' else 'LIGHT_GRAY_CONCRETE',
                            roll=math.pi/4)],'labels':[]}

if __name__ == '__main__':
    path=ROOT/'src/main/resources/vanilla-models.json'
    data=json.loads(path.read_text(encoding='utf-8'))
    data.update(models())
    # Preserve approved hitbox dimensions and the shared circular SPIN geometry.
    for i,color in enumerate(COLORS):
        name=f'showcase_button_money_{i}'
        for box in data[name]['boxes'][2:]: box['material']=color
    for box in data['showcase_button_round_spin']['boxes']:
        if box['material'] not in ('BLACK_CONCRETE','GRAY_CONCRETE'): box['material']='LIME_CONCRETE'
    path.write_text(json.dumps(data,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
    print({name:len(model['boxes']) for name,model in models().items()})
