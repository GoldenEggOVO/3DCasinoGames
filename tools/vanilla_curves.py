"""Faceted vanilla display geometry, in physical blocks and row-major matrices.

Overlapping fills use separate depth planes. This avoids the coplanar faces of
the common BDEngine diameter-bar technique without requiring custom models.
"""
import math


def multiply(a, b):
    return [sum(a[r*4+k]*b[k*4+c] for k in range(4)) for r in range(4) for c in range(4)]


def cuboid(center, size, material, roll=0, pitch=0, yaw=0):
    c, s = math.cos(roll), math.sin(roll)
    cp, sp = math.cos(pitch), math.sin(pitch)
    cy, sy = math.cos(yaw), math.sin(yaw)
    rotation = multiply([c,-s,0,0, s,c,0,0, 0,0,1,0, 0,0,0,1],
                        multiply([1,0,0,0, 0,cp,-sp,0, 0,sp,cp,0, 0,0,0,1],
                                 [cy,0,sy,0, 0,1,0,0, -sy,0,cy,0, 0,0,0,1]))
    matrix = rotation[:]
    for r in range(3):
        for axis in range(3):
            matrix[r*4+axis] *= size[axis]
        matrix[r*4+3] = center[r] - sum(matrix[r*4:r*4+3])/2
    return {'from': [0,0,0], 'to': [1,1,1], 'material': material,
            'matrix': [round(v, 9) for v in matrix]}


def transform(parts, matrix):
    for part in parts:
        m = multiply(matrix, part['matrix'])
        determinant = (m[0]*(m[5]*m[10]-m[6]*m[9]) - m[1]*(m[4]*m[10]-m[6]*m[8])
                       + m[2]*(m[4]*m[9]-m[5]*m[8]))
        if determinant < 0:
            # Reverse a unit-cube parameter without moving any surface. Mirrored
            # corners then retain outward winding when Minecraft culls faces.
            for row in range(3):
                m[row*4+3] += m[row*4]
                m[row*4] = -m[row*4]
        part['matrix'] = [round(v, 9) for v in m]
    return parts


def layered(shapes, z0, z1, material, spread=None):
    """Extrude (cx,cy,width,height,angle) with distinct front AND back planes."""
    spread = min((len(shapes)-1)*.00022, (z1-z0)*.25) if spread is None else spread
    result = []
    for i, (x,y,w,h,angle) in enumerate(shapes):
        offset = spread*i/max(1, len(shapes)-1)
        result.append(cuboid((x,y,(z0+z1-spread)/2+offset),
                             (w,h,z1-z0-spread), material, roll=angle))
    return result


def disc(cx, cy, radius, z0, z1, material, sides=48):
    # A flat, nonoverlapping inset removes the diametric seam produced by stacked
    # diameter bars. The tangent ring hides its stair edges and carries the outline.
    result = []
    fill_radius = radius-.001
    for row in range(32):
        low,high = [-fill_radius+2*fill_radius*i/32 for i in (row,row+1)]
        half_width = math.sqrt(max(0,fill_radius**2-max(abs(low),abs(high))**2))
        if half_width < 1e-8: continue
        result.append(cuboid((cx,cy+(low+high)/2,(z0+z1)/2),
                             (2*half_width,high-low,z1-z0-.0012),material))
    return result + ring(cx,cy,radius,radius*.12,z0,z1,material,sides)


def ring(cx, cy, radius, rim, z0, z1, material, sides=48):
    result = []
    distance = radius*math.cos(math.pi/sides)-rim/2
    for i in range(sides):
        angle = (i+.5)*math.tau/sides
        offset = .0003*(i%2)
        result.append(cuboid((cx+math.cos(angle)*distance,cy+math.sin(angle)*distance,
                              (z0+z1-.0003)/2+offset),
                             (rim,2*radius*math.sin(math.pi/sides),z1-z0-.0003),
                             material,roll=angle))
    return result


def rounded_rectangle(cx, cy, width, height, radius, z0, z1, material):
    shapes = [(cx,cy,width-2*radius,height,0)]
    shapes += [(cx+sx*(width-radius)/2,cy,radius,height-2*radius,0) for sx in (-1,1)]
    for sx in (-1,1):
        for sy in (-1,1):
            x, y = cx+sx*(width/2-radius), cy+sy*(height/2-radius)
            for k in range(3):
                angle = (k+.5)*math.pi/6
                length = radius*math.cos(math.pi/12)
                dx, dy = sx*math.cos(angle), sy*math.sin(angle)
                shapes.append((x+dx*length/2,y+dy*length/2,length,
                               2*radius*math.sin(math.pi/12),math.atan2(dy,dx)))
    return layered(shapes,z0,z1,material)


def rounded_key(cx, cy, width, height, radius, z0, z1, material):
    """Flat inset and six tangent facets per corner; no stacked corner fans."""
    result=[]
    inset, rim = .001, radius*.26
    inner_radius=radius-inset
    half_width=width/2-inset
    straight=height/2-radius
    rows=[(-straight,straight)]
    for sign in (-1,1):
        for i in range(8):
            rows.append(tuple(sorted((sign*(straight+inner_radius*i/8),
                                      sign*(straight+inner_radius*(i+1)/8)))))
    for low,high in rows:
        dy=max(0,max(abs(low),abs(high))-straight)
        half=half_width-inner_radius+math.sqrt(max(0,inner_radius**2-dy**2))
        result.append(cuboid((cx,cy+(low+high)/2,(z0+z1)/2),
                             (2*half,high-low,z1-z0-.0012),material))
    # Separate the four straight border planes from the overlapping corner caps.
    for sign in (-1,1):
        result.append(cuboid((cx,cy+sign*(height-rim)/2,(z0+z1-.0005)/2),
                             (width-2*radius+.0004,rim,z1-z0-.0005),material))
        result.append(cuboid((cx+sign*(width-rim)/2,cy,(z0+z1+.0005)/2),
                             (rim,height-2*radius+.0004,z1-z0-.0005),material))
    angle_step=math.pi/12
    distance=radius*math.cos(angle_step/2)-rim/2
    for sx in (-1,1):
        for sy in (-1,1):
            for i in range(6):
                angle=(i+.5)*angle_step
                dx,dy=sx*math.cos(angle),sy*math.sin(angle)
                offset=.00025*(i%2)
                result.append(cuboid((cx+sx*(width/2-radius)+dx*distance,
                                      cy+sy*(height/2-radius)+dy*distance,
                                      (z0+z1-.00025)/2+offset),
                                     (rim,2*radius*math.sin(angle_step/2),z1-z0-.00025),
                                     material,roll=math.atan2(dy,dx)))
    return result


def buttons(name):
    action=name.split('_button_',1)[1]
    color={'play':'LIME_CONCRETE','hit':'PINK_CONCRETE','stand':'YELLOW_CONCRETE',
           'double':'PURPLE_CONCRETE','cash':'ORANGE_CONCRETE','cashout':'ORANGE_CONCRETE',
           'plus':'LIME_CONCRETE','minus':'PURPLE_CONCRETE','step':'YELLOW_CONCRETE',
           'spin':'YELLOW_CONCRETE','under':'PURPLE_CONCRETE','over':'ORANGE_CONCRETE',
           'flip':'PURPLE_CONCRETE','select':'CYAN_CONCRETE','reset':'RED_CONCRETE',
           'money_0':'CYAN_CONCRETE','money_1':'BLUE_CONCRETE',
           'money_2':'RED_CONCRETE','money_3':'YELLOW_CONCRETE',
           'duck_1':'YELLOW_CONCRETE','duck_2':'PINK_CONCRETE',
           'duck_3':'LIGHT_BLUE_CONCRETE','duck_4':'LIME_CONCRETE'}[action]
    result=[{'from':[-.30,0,-.05],'to':[.30,.40,0],'material':'BLACK_CONCRETE'},
            {'from':[-.28,.02,0],'to':[.28,.38,.025],'material':'GRAY_CONCRETE'}]
    return result+rounded_key(0,.2,.53,.33,.06,.025,.13,color)


def wheel(segments, materials):
    radius, rim, rows, sides = .9*496/512, .035, 24, 80
    result = []
    for i, value in enumerate(segments):
        angle = -(i+.5)*math.tau/20
        for row in range(rows):
            low = .10+(radius-.02-.10)*row/rows
            high = .10+(radius-.02-.10)*(row+1)/rows
            distance = (low+high)/2
            result.append(cuboid((-math.sin(angle)*distance,math.cos(angle)*distance,
                                  -.00125+(i%2)*.00035),
                                 (2*distance*math.tan(math.pi/20),high-low,.0225),
                                 materials[value],roll=angle))
    for i in range(sides):
        angle = -(i+.5)*math.tau/sides
        distance = radius*math.cos(math.pi/sides)-rim/2
        result.append(cuboid((-math.sin(angle)*distance,math.cos(angle)*distance,
                              (i%2)*.00035),
                             (2*radius*math.sin(math.pi/sides),rim,.026),
                             'SMOOTH_SANDSTONE',roll=angle))
    result += disc(0,0,.12,-.012,.0145,'GOLD_BLOCK',32)
    for i in range(20):
        angle = -i*math.tau/20
        low, high = .12, radius-rim+.003
        distance = (low+high)/2
        result.append(cuboid((-math.sin(angle)*distance,math.cos(angle)*distance,.013),
                             (.0064,high-low,.004),'SMOOTH_SANDSTONE',roll=angle))
    return result


def hilo(parts):
    result = [p for p in parts if abs(p['from'][1]-.52) > 1e-6]
    low, high, depth, angle = .52, 1.50929, .72, math.radians(35)
    height = high-low
    length = (height*math.cos(angle)-depth*math.sin(angle))/math.cos(2*angle)
    thickness = (depth*math.cos(angle)-height*math.sin(angle))/math.cos(2*angle)
    for x in (-1.32,1.32):
        result.append(cuboid((x,(low+high)/2,0),(.12,length,thickness),
                             'OAK_PLANKS',pitch=-angle))
    return result


def dragon(parts):
    result = []
    for part in parts:
        a, b = part['from'], part['to']
        if abs(a[1]-1.06) > 1e-6 or abs(b[1]-3.04) > 1e-6:
            result.append(part)
            continue
        # Join adjacent chords; stagger the horizontal caps at their intersections.
        x0,x1 = a[0],b[0]
        z0,z1 = [.335+math.sqrt(1.65**2-x*x)-1.65 for x in (x0,x1)]
        yaw = math.atan2(z0-z1,x1-x0)
        stagger = .0003 * (round((x0+.7)/.05) % 2)
        result.append(cuboid(((x0+x1)/2,(a[1]+b[1]-.0003)/2+stagger,(z0+z1)/2-.05),
                             (math.hypot(x1-x0,z1-z0),b[1]-a[1]-.0003,.10),
                             part['material'],yaw=yaw))
    return result


def blackjack(parts):
    # Keep the rear table, recessed chips, card shoe, legs and front sign. Replace
    # only the three stepped front strips and their front rim.
    result = [p for p in parts if not (p['from'][2] >= .25-1e-6
              and any(abs(p['from'][1]-height) < 1e-6 for height in (.68,.82)))]
    for layer,(rx,rz,y0,y1,material,front) in enumerate([(.75,.80,.6802,.8198,'BROWN_CONCRETE',1.04),
            (.75,.80,.8202,.918,'DARK_OAK_PLANKS',1.05),
            (.65,.70,.821,.9198,'DARK_PRISMARINE',.95)]):
        # Build in XY, then map its depth axis to height and its Y axis to table Z.
        start = .2502+layer*.0002
        shapes = [(0,(start+front)/2,2.1+layer*.0002,front-start,0)]
        # Elliptic corners need nonuniform scaling after rotation, so use full matrices.
        front_parts = layered(shapes[:1],-y1,-y0,material)
        for sx in (-1,1):
            corner = layered([(math.cos((k+.5)*math.pi/24)*math.cos(math.pi/48)/2,
                               math.sin((k+.5)*math.pi/24)*math.cos(math.pi/48)/2,
                               math.cos(math.pi/48),2*math.sin(math.pi/48),
                               (k+.5)*math.pi/24) for k in range(12)],
                             -y1+.00022,-y0-.00022,material)
            front_parts += transform(corner,[sx*rx,0,0,sx*1.05, 0,rz,0,.25,
                                             0,0,1,0, 0,0,0,1])
        result += transform(front_parts,[1,0,0,0, 0,0,-1,0, 0,1,0,0, 0,0,0,1])
    return result
