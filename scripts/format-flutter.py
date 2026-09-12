"""Expose formatted PUBLIC source as annotations, not downloadable build artifacts."""
import base64,gzip,hashlib,json,subprocess
from pathlib import Path
root=Path('life_mate_flutter')
files=list(root.glob('lib/**/*.dart'))+list(root.glob('test/**/*.dart'))
original={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
subprocess.run(['dart','format','lib','test'],cwd=root,check=True)
for p in files:
 payload={'path':str(p),'original':original[str(p)],'content':p.read_text()}
 encoded=base64.b64encode(gzip.compress(json.dumps(payload).encode())).decode()
 print(f'::notice title=Formatted Flutter source::{encoded}')
