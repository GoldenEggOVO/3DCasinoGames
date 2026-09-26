"""Regressions for plain surfaces and the smooth common control key."""
import json
from pathlib import Path
import unittest
import numpy as np
from vanilla_curves import cuboid

ROOT=Path(__file__).resolve().parents[1]


def front(parts,x,y):
    hits=[]
    for p in parts:
        if 'matrix' not in p:
            a,b=p['from'],p['to']
            p=cuboid([(u+v)/2 for u,v in zip(a,b)],[v-u for u,v in zip(a,b)],p['material'])
        m=np.array(p['matrix']).reshape(4,4)
        uv=np.linalg.solve(m[:2,:2],np.array([x,y])-m[:2,3])
        if np.all(uv>=-1e-8) and np.all(uv<=1+1e-8): hits.append(m[2,3]+m[2,2])
    return max(hits) if hits else None


class VanillaFinishTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.models=json.loads((ROOT/'src/main/resources/vanilla-models.json').read_text())

    def test_common_buttons_share_plain_frames_and_one_continuous_key_face(self):
        for name,model in self.models.items():
            if '_button_' not in name or name=='showcase_button_round_spin': continue
            with self.subTest(model=name):
                parts=model['boxes']
                self.assertEqual(['BLACK_CONCRETE','GRAY_CONCRETE'],[p['material'] for p in parts[:2]])
                self.assertTrue(all(p['material'].endswith('_CONCRETE') for p in parts))
                for x,y in [(-.24,.20),(.24,.20),(0,.055),(0,.345),(0,.20)]:
                    self.assertGreater(front(parts,x,y),.129)
                self.assertLessEqual(len(parts),50)

    def test_selected_keno_tile_is_orange_concrete(self):
        self.assertEqual({'ORANGE_CONCRETE'},
                         {p['material'] for p in self.models['showcase_tile_selected']['boxes']})

    def test_dragon_tiles_are_textured_vanilla_cubes(self):
        for state,material in [('hidden','GRAY_TERRACOTTA'),('safe','EMERALD_BLOCK'),('trap','TNT')]:
            with self.subTest(state=state):
                model=self.models['dragon_tile_'+state]
                self.assertEqual([],model['labels'])
                self.assertEqual([{'from':[-.14,-.14,-.14],
                                   'to':[.14,.14,.14],'material':material}],model['boxes'])

    def test_blackjack_has_plain_felt_and_no_wood_or_metal_textures(self):
        materials={p['material'] for p in self.models['cabinet_blackjack']['boxes']}
        self.assertIn('GREEN_CONCRETE',materials)
        self.assertFalse(materials & {'GOLD_BLOCK','DARK_OAK_PLANKS','OAK_PLANKS','DARK_PRISMARINE'})

    def test_slots_front_ledge_is_absent(self):
        for p in self.models['showcase_slots']['boxes']:
            self.assertFalse(all(a<v<b for a,v,b in zip(p['from'],(0,.97,.63),p['to'])))

    def test_hilo_rear_support_connects_table_to_panel_without_poking_through(self):
        supports=[p for p in self.models['showcase_hilo']['boxes'] if p['material']=='GRAY_CONCRETE']
        self.assertTrue(supports,'Missing rear panel support')
        self.assertLessEqual(len(supports),3)
        matrices=[np.array(p['matrix']).reshape(4,4) for p in supports]
        def contains(point):
            return any(np.all((q:=np.linalg.solve(m,np.array([*point,1])))[:3]>=-1e-6)
                       and np.all(q[:3]<=1+1e-6) for m in matrices)
        for x in (-.98,.98):
            for y in (.515,1.1,1.36):
                self.assertTrue(contains((x,y,-.34)),(x,y))
        angle=np.radians(35);c,s=np.cos(angle),np.sin(angle)
        for x in (-.98,0,.98):
            self.assertTrue(contains((x,1.1+.42*c-.075*s,-.42*s-.075*c)))
        for m in matrices:
            for x in (0,1):
                for y in (0,1):
                    for z in (0,1):
                        p=m@np.array([x,y,z,1])
                        self.assertLessEqual((p[1]-1.1)*s+p[2]*c,-.035+1e-6)

    def test_dragon_nameplate_text_is_in_front_of_the_top_cap(self):
        model=self.models['showcase_dragon_tower_compact']
        label=model['labels'][0]['position']
        origin=np.array([label[0],label[1],10,1])
        direction=np.array([0,0,-1,0])
        nearest=0
        for p in model['boxes']:
            if 'matrix' not in p:
                a,b=p['from'],p['to']
                p=cuboid([(u+v)/2 for u,v in zip(a,b)],[v-u for u,v in zip(a,b)],p['material'])
            inverse=np.linalg.inv(np.array(p['matrix']).reshape(4,4))
            a,b=(inverse@origin)[:3],(inverse@direction)[:3]
            low,high=0,float('inf')
            for axis in range(3):
                if abs(b[axis])<1e-9:
                    if not 0<=a[axis]<=1: high=-1
                else:
                    t0,t1=sorted((-a[axis]/b[axis],(1-a[axis])/b[axis]))
                    low,high=max(low,t0),min(high,t1)
            if low<=high: nearest=max(nearest,10-low)
        self.assertGreater(label[2],nearest+.001)


if __name__=='__main__': unittest.main()
