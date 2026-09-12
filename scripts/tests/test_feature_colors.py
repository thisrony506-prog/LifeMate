"""Feature primary colors must support readable white button labels in light mode."""
from pathlib import Path
import re
import unittest

class FeatureColorTest(unittest.TestCase):
    def test_all_eight_feature_accents_meet_normal_text_contrast(self):
        source = (Path(__file__).resolve().parents[2] / 'app/src/main/java/com/lifemate/ui/Components.kt').read_text()
        block = source.split('fun Kind.tint()')[1].split('fun Kind.summary()')[0]
        colors = re.findall(r'Kind\.(\w+) -> Color\(0xFF([0-9A-F]{6})\)', block)
        self.assertEqual(8,len(colors))
        for kind, value in colors:
            channels = [int(value[i:i+2],16)/255 for i in (0,2,4)]
            linear = [x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4 for x in channels]
            luminance = sum(x*y for x,y in zip(linear,(.2126,.7152,.0722)))
            self.assertGreaterEqual(1.05/(luminance+.05),4.5,kind)
