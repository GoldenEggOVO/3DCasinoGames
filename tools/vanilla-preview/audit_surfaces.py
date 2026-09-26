"""Find coplanar, same-facing overlaps in actual runtime block-display snapshots."""
import json
import sys
from pathlib import Path
import numpy as np


def rotation(q):
    x, y, z, w = q
    return np.array([[1-2*(y*y+z*z), 2*(x*y-z*w), 2*(x*z+y*w)],
                     [2*(x*y+z*w), 1-2*(x*x+z*z), 2*(y*z-x*w)],
                     [2*(x*z-y*w), 2*(y*z+x*w), 1-2*(x*x+y*y)]])


def area(poly):
    return sum(np.linalg.det(np.array([a, b])) for a, b in zip(poly, poly[1:]+poly[:1]))/2


def intersection(a, b):
    if any(min(max(p[i] for p in a), max(p[i] for p in b))
           - max(min(p[i] for p in a), min(p[i] for p in b)) < 1e-5 for i in range(2)):
        return 0
    if area(a) < 0: a = a[::-1]
    if area(b) < 0: b = b[::-1]
    for p, q in zip(b, b[1:]+b[:1]):
        result = []
        def side(v): return float(np.linalg.det(np.array([q-p, v-p])))
        for u, v in zip(a, a[1:]+a[:1]):
            su, sv = side(u), side(v)
            if su >= -1e-9: result.append(u)
            if (su > 1e-9 and sv < -1e-9) or (su < -1e-9 and sv > 1e-9):
                result.append(u+(v-u)*su/(su-sv))
        a = result
        if len(a) < 3: return 0
    return abs(area(a))


def audit(entries):
    groups, conflicts = {}, []
    for index, e in enumerate(entries):
        if not e['kind'].endswith('BlockDisplay') or not e.get('visible', True): continue
        if 'matrix' in e:
            affine = np.array(e['matrix']).reshape(4,4)
            matrix, offset = affine[:3,:3], affine[:3,3]
        else:
            matrix = rotation(e['leftRotation']) @ np.diag(e['scale']) @ rotation(e['rightRotation'])
            offset = np.array(e['position']) + e['translation']
        for axis in range(3):
            other = [i for i in range(3) if i != axis]
            for side in (0, 1):
                points = []
                for u, v in ((0, 0), (1, 0), (1, 1), (0, 1)):
                    p = np.zeros(3); p[axis] = side; p[other] = [u, v]
                    points.append(matrix @ p + offset)
                normal = np.cross(points[1]-points[0], points[2]-points[0])
                normal /= np.linalg.norm(normal)
                if np.dot(normal, matrix[:, axis])*(side*2-1) < 0: normal = -normal
                distance = float(np.dot(normal, points[0]))
                # All faces in a plane group must use the SAME projection axis,
                # including diagonal normals whose components differ only by ULPs.
                projection = [i for i in range(3) if i != np.argmax(np.abs(np.round(normal, 4)))]
                poly = [p[projection] for p in points]
                key = (*np.round(normal, 4), round(distance, 4))
                for j, old_normal, old_distance, old_poly in groups.get(key, []):
                    if j == index or abs(distance-old_distance) > 1e-5: continue
                    if np.dot(normal, old_normal) < .999999: continue
                    overlap = intersection(poly, old_poly)
                    if overlap > 1e-7:
                        conflicts.append({'entities': [j, index], 'area': overlap,
                                          'normal': normal.tolist(), 'distance': distance})
                groups.setdefault(key, []).append((index, normal, distance, poly))
    return conflicts


if __name__ == '__main__':
    source, destination = map(Path, sys.argv[1:])
    snapshots = sorted(source.glob('*.json'))
    if not snapshots:
        raise SystemExit(f'No runtime snapshots found: {source}')
    result = {p.stem: audit(json.loads(p.read_text(encoding='utf-8'))) for p in snapshots}
    destination.write_text(json.dumps(result, indent=2), encoding='utf-8')
    print({name: len(conflicts) for name, conflicts in result.items()})
    if any(result.values()):
        raise SystemExit(1)
