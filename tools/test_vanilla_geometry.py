"""Geometry regressions for the actual no-pack display assets."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class VanillaGeometryTest(unittest.TestCase):
    def test_curved_parts_use_rotated_facets_with_bounded_entity_counts(self):
        models = json.loads((ROOT / 'src/main/resources/vanilla-models.json').read_text())
        budgets = {'showcase_wheel_fortune': 650, 'showcase_wheel_money': 650,
                   'showcase_button_round_spin': 80, 'showcase_slider': 130,
                   'showcase_tile': 20, 'showcase_tile_selected': 20,
                   'cabinet_button_play': 50, 'cabinet_blackjack': 160}
        for name, budget in budgets.items():
            with self.subTest(model=name):
                boxes = models[name]['boxes']
                self.assertTrue(any(p.get('matrix') for p in boxes), 'Missing rotated curve: '+name)
                self.assertLessEqual(len(boxes), budget, name)

    def test_refined_mines_has_an_orange_sloped_console(self):
        models = json.loads((ROOT / 'src/main/resources/vanilla-models.json').read_text())
        self.assertTrue('cabinet_mines_refined' in models, 'Missing refined Mines console')
        console = [p for p in models['cabinet_mines_refined']['boxes'] if p.get('pitch')]
        self.assertTrue(any(p['material'] == 'ORANGE_CONCRETE' for p in console))
        self.assertTrue(all(abs(p['pitch'] + 0.6108652382) < 1e-6 for p in console))

    def test_shipped_boxes_have_no_overlapping_coplanar_outward_faces(self):
        models = json.loads((ROOT / 'src/main/resources/vanilla-models.json').read_text())
        conflicts = []
        for name, model in models.items():
            faces = {}
            for index, part in enumerate(model['boxes']):
                if part.get('matrix') or part.get('roll') or part.get('pitch'):
                    continue
                for axis in range(3):
                    other = [n for n in range(3) if n != axis]
                    for side in ('from', 'to'):
                        key = axis, side, round(part[side][axis], 6)
                        for previous, rectangle in faces.get(key, []):
                            if all(min(part['to'][n], rectangle['to'][n])
                                   - max(part['from'][n], rectangle['from'][n]) > 1e-7 for n in other):
                                conflicts.append(f'{name} boxes {previous}/{index} {side} axis {axis}')
                        faces.setdefault(key, []).append((index, part))
        self.assertEqual([], conflicts, '\n'.join(conflicts[:20]))


if __name__ == '__main__':
    unittest.main()
