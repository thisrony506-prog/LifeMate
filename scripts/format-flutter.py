"""Return formatted public source in <=10 small annotations per step, without artifacts."""
import base64
import gzip
import hashlib
import json
import subprocess
import sys
from pathlib import Path

root = Path('life_mate_flutter')
group = sys.argv[1]
paths = {
    'data': 'lib/data',
    'services': 'lib/services',
    'ui_forms': 'lib/ui',
    'ui_pages': 'lib/ui',
    'ui_system': 'lib/ui',
    'test': 'test',
    'main': 'lib/main.dart',
}
target = root / paths[group]
files = [target] if target.is_file() else sorted(target.glob('*.dart'))
forms = {'backup_page', 'cloud_settings', 'common', 'device_lock', 'entries'}
system = {'updates', 'privacy_settings'}
if group == 'ui_forms':
    files = [p for p in files if p.stem in forms]
elif group == 'ui_system':
    files = [p for p in files if p.stem in system]
elif group == 'ui_pages':
    files = [p for p in files if p.stem not in forms | system]
assert files, f'No source files for {group}'
original = {str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
# Format only this group's files. Formatting the whole UI directory in the first
# step would invalidate the original-source hashes exported by the next step.
subprocess.run(
    ['dart', 'format', *(str(p.relative_to(root)) for p in files)],
    cwd=root,
    check=True,
)
payload = [
    {'path': str(p), 'original': original[str(p)], 'content': p.read_text()}
    for p in files
]
encoded = base64.b64encode(
    gzip.compress(json.dumps(payload, ensure_ascii=False).encode())
).decode()
chunks = [encoded[i:i + 3000] for i in range(0, len(encoded), 3000)]
assert len(chunks) <= 10, 'Split this source group into smaller formatter steps'
for i, chunk in enumerate(chunks):
    print(f'::notice title=Flutter formatted {group} {i + 1}/{len(chunks)}::{chunk}')
