"""Only the new sage Flutter Personal Life OS remains in the product."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
class FeatureColorTest(unittest.TestCase):
    def test_sage_contrast_and_no_legacy_feature_routes(self):
        theme = (ROOT/'life_mate_flutter/lib/ui/common.dart').read_text()
        value = re.search(r'const sage = Color\(0xFF([0-9A-F]{6})\)', theme)[1]
        self.assertEqual(value, 'A8C3B9')
        channels = [int(value[i:i+2],16)/255 for i in (0,2,4)]
        linear = [x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4 for x in channels]
        luminance = sum(x*y for x,y in zip(linear,(.2126,.7152,.0722)))
        self.assertGreaterEqual((luminance+.05)/(.01033+.05),4.5)
        self.assertIn('0xFF1A1A1A', theme)
        all_source = '\n'.join(p.read_text() for p in (ROOT/'life_mate_flutter/lib').rglob('*.dart'))
        for removed in ('openLegacy', 'All retained tools', 'Edit original profile', 'NativeBridge.open', 's.legacy'):
            self.assertNotIn(removed, all_source)
        for removed in ('ui', 'database', 'domain', 'data', 'navigation', 'notifications', 'flutter', 'utils'):
            self.assertFalse((ROOT/'app/src/main/java/com/lifemate'/removed).exists())

    def test_reset_is_once_only_and_preserves_update_requirement_not_user_data(self):
        source = (ROOT/'app/src/main/java/com/lifemate/reset/FreshStartReset.kt').read_text()
        self.assertLess(source.index('if (marker.isFile) return false'), source.index('cancelAll()'))
        self.assertIn('release_updates.xml', source)
        self.assertIn('context.deleteDatabase(name)', source)
        self.assertIn('keys.deleteEntry(it)', source)
        self.assertIn('Files.isSymbolicLink', source)
        self.assertLess(source.index('keys.deleteEntry(it)'), source.index('marker.outputStream()'))
        gradle = (ROOT/'app/build.gradle.kts').read_text()
        for removed in ('room-runtime', 'sqlcipher', 'navigation-compose', 'work-runtime'):
            self.assertNotIn(removed, gradle)
