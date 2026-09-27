import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

class WheelRefinementTest(unittest.TestCase):
    def test_cabinets_have_distinct_colors_and_rear_housings(self):
        models = json.loads((ROOT/'src/main/resources/vanilla-models.json').read_text())
        for name, color in [('showcase_wheel_of_fortune','PURPLE_CONCRETE'),('showcase_money_wheel','BROWN_CONCRETE')]:
            parts = models[name]['boxes']
            self.assertGreater(sum(p['material'] == color for p in parts), 40)
            self.assertLess(len(parts), 340)
            self.assertTrue(any(p['material'] == 'GRAY_CONCRETE' for p in parts))
    def test_wheels_and_money_buttons_use_plain_matching_colors(self):
        models = json.loads((ROOT/'src/main/resources/vanilla-models.json').read_text())
        for name in ['showcase_wheel_fortune','showcase_wheel_money']:
            self.assertTrue(all(p['material'].endswith('_CONCRETE') for p in models[name]['boxes']))
        for i,color in enumerate(['LIME_CONCRETE','BLUE_CONCRETE','RED_CONCRETE','ORANGE_CONCRETE']):
            self.assertIn(color,{p['material'] for p in models[f'showcase_button_money_{i}']['boxes']})
