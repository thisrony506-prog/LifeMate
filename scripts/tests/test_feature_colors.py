"""Sage must remain a single readable accent, not a rainbow palette."""
from pathlib import Path
import re
import unittest

class FeatureColorTest(unittest.TestCase):
    def test_single_premium_accent_and_readable_button_contrast(self):
        root=Path(__file__).resolve().parents[2]
        theme=(root/'app/src/main/java/com/lifemate/ui/Theme.kt').read_text()
        components=(root/'app/src/main/java/com/lifemate/ui/Components.kt').read_text()
        self.assertIn('fun Kind.tint() = PremiumPink',components)
        value=re.search(r'val Sage = Color\(0xFF([0-9A-F]{6})\)',theme)[1]
        channels=[int(value[i:i+2],16)/255 for i in (0,2,4)]
        linear=[x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4 for x in channels]
        luminance=sum(x*y for x,y in zip(linear,(.2126,.7152,.0722)))
        self.assertGreaterEqual((luminance+.05)/(.01033+.05),4.5)
        self.assertIn('0xFF151C18',theme)
