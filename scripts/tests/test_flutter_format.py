"""Test source transport/group isolation; this does not test the Dart formatter."""
import base64
import gzip
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
GROUPS = ('data', 'services', 'ui_forms', 'ui_pages', 'ui_system', 'test', 'main')


class FlutterFormatTransportTest(unittest.TestCase):
    def test_all_source_roundtrips_with_isolated_groups_and_notice_budget(self):
        with tempfile.TemporaryDirectory() as directory:
            workspace = Path(directory)
            module = workspace / 'life_mate_flutter'
            for folder in ('lib', 'test'):
                shutil.copytree(ROOT / 'life_mate_flutter' / folder, module / folder)
            originals = {
                str(p.relative_to(workspace)): p.read_bytes()
                for p in module.rglob('*.dart')
            }
            # A stub mutation verifies pre-format hashes and proves that a group
            # never modifies another group's source before its hash is captured.
            dart = workspace / 'dart'
            dart.write_text(
                '#!/usr/bin/env python3\n'
                'from pathlib import Path\n'
                'import sys\n'
                'assert sys.argv[1] == "format"\n'
                'for name in sys.argv[2:]:\n'
                ' p=Path(name);p.write_bytes(p.read_bytes()+b"\\n")\n'
            )
            dart.chmod(0o755)
            environment = dict(os.environ, PATH=str(workspace) + os.pathsep + os.environ['PATH'])
            seen = set()
            for group in GROUPS:
                result = subprocess.run(
                    [sys.executable, str(ROOT / 'scripts/format-flutter.py'), group],
                    cwd=workspace, env=environment, text=True, capture_output=True, check=True,
                )
                notices = result.stdout.splitlines()
                self.assertLessEqual(len(notices), 10)
                parts = []
                for i, notice in enumerate(notices, 1):
                    match = re.fullmatch(
                        rf'::notice title=Flutter formatted {group} {i}/{len(notices)}::(.+)',
                        notice,
                    )
                    self.assertIsNotNone(match)
                    self.assertLessEqual(len(match[1]), 3000)
                    parts.append(match[1])
                payload = json.loads(gzip.decompress(base64.b64decode(''.join(parts))))
                for item in payload:
                    path = item['path']
                    self.assertNotIn(path, seen)
                    seen.add(path)
                    self.assertEqual(item['original'], hashlib.sha256(originals[path]).hexdigest())
                    self.assertEqual(item['content'].encode(), originals[path] + b'\n')
            self.assertEqual(seen, set(originals))


if __name__ == '__main__':
    unittest.main()
