"""Check visible curve coverage and actual affine faces, not only part counts."""
import json
import math
from pathlib import Path
import unittest
import numpy as np
from export_vanilla_models import load
from vanilla_curves import cuboid, disc, rounded_rectangle, wheel

ROOT = Path(__file__).resolve().parents[1]
audit = load('curve_surface_audit', ROOT/'tools/vanilla-preview/audit_surfaces.py').audit


def flat_material(parts, x, y):
    visible = []
    for p in parts:
        m = np.array(p['matrix']).reshape(4,4)
        uv = np.linalg.solve(m[:2,:2],np.array([x,y])-m[:2,3])
        if np.all(uv >= -1e-8) and np.all(uv <= 1+1e-8):
            visible.append((m[2,3]+m[2,2],p['material']))
    return max(visible)[1] if visible else None


class VanillaCurvesTest(unittest.TestCase):
    def test_shipped_affine_parts_keep_positive_handedness_for_client_face_culling(self):
        models = json.loads((ROOT/'src/main/resources/vanilla-models.json').read_text())
        for name,model in models.items():
            for index,p in enumerate(model['boxes']):
                if 'matrix' in p:
                    m = np.array(p['matrix']).reshape(4,4)
                    self.assertGreater(np.linalg.det(m[:3,:3]),0,(name,index))

    def test_disc_has_continuous_coverage_and_submillimetre_contour_error(self):
        parts = disc(0,0,.2,0,.16,'ORANGE_CONCRETE')
        for i in range(720):
            angle = i*math.tau/720
            self.assertIsNotNone(flat_material(parts,.199*math.cos(angle),.199*math.sin(angle)))
            self.assertIsNone(flat_material(parts,.2001*math.cos(angle),.2001*math.sin(angle)))

    def test_disc_inset_is_a_single_flat_depth_without_a_diameter_step(self):
        parts = disc(0,0,.2,0,.16,'ORANGE_CONCRETE')
        depths=[]
        for x in np.linspace(-.1,.1,11):
            for y in np.linspace(-.1,.1,11):
                heights=[]
                for part in parts:
                    m=np.array(part['matrix']).reshape(4,4)
                    uv=np.linalg.solve(m[:2,:2],np.array([x,y])-m[:2,3])
                    if np.all(uv>=-1e-8) and np.all(uv<=1+1e-8): heights.append(m[2,3]+m[2,2])
                self.assertTrue(heights)
                depths.append(max(heights))
        self.assertLess(max(depths)-min(depths),1e-8)

    def test_rounded_tile_fills_interior_without_square_corner_protrusions(self):
        parts = rounded_rectangle(0,0,.24,.24,.04,-.035,.035,'BLUE_TERRACOTTA')
        for x in np.linspace(-.121,.121,40):
            for y in np.linspace(-.121,.121,40):
                distance = math.hypot(max(0,abs(x)-.08),max(0,abs(y)-.08))
                if distance < .0385:
                    self.assertIsNotNone(flat_material(parts,x,y), (x,y))
                elif distance > .0401:
                    self.assertIsNone(flat_material(parts,x,y), (x,y))

    def test_wheel_preserves_twenty_sector_order_and_covers_outer_circle(self):
        parts = wheel(list(range(20)),{i:str(i) for i in range(20)})
        for i in range(20):
            angle = (i+.5)*math.tau/20
            for radius in (.2,.45,.8):
                self.assertEqual(str(i),flat_material(parts,radius*math.sin(angle),radius*math.cos(angle)))
        radius = .9*496/512
        for i in range(240):
            angle = i*math.tau/240
            self.assertIsNotNone(flat_material(parts,(radius-.002)*math.sin(angle),(radius-.002)*math.cos(angle)))
            self.assertIsNone(flat_material(parts,(radius+.0001)*math.sin(angle),(radius+.0001)*math.cos(angle)))

    def test_all_shipped_curves_have_no_overlapping_coplanar_faces(self):
        models = json.loads((ROOT/'src/main/resources/vanilla-models.json').read_text())
        for name,model in models.items():
            if not any('matrix' in p for p in model['boxes']):
                continue
            entries = []
            for p in model['boxes']:
                if 'matrix' not in p:
                    a,b = p['from'],p['to']
                    p = cuboid([(x+y)/2 for x,y in zip(a,b)], [y-x for x,y in zip(a,b)],
                               p['material'],roll=p.get('roll',0),pitch=p.get('pitch',0))
                entries.append(dict(p,kind='BlockDisplay'))
            with self.subTest(model=name):
                self.assertEqual([],audit(entries))

    def test_surface_audit_distinguishes_disjoint_diagonal_faces_from_real_overlaps(self):
        a = cuboid((.2,-.2,0),(.04,.03,.02),'STONE',roll=-3*math.pi/4)
        b = cuboid((-.2,.2,0),(.04,.03,.02),'STONE',roll=math.pi/4)
        self.assertEqual([],audit([dict(p,kind='BlockDisplay') for p in (a,b)]))
        self.assertTrue(audit([dict(a,kind='BlockDisplay'),dict(a,kind='BlockDisplay')]))


if __name__ == '__main__':
    unittest.main()
