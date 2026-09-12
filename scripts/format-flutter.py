"""Return formatted public source in <=10 small annotations per step (no extra artifacts)."""
import base64,gzip,hashlib,json,subprocess,sys
from pathlib import Path
root=Path('life_mate_flutter')
group=sys.argv[1]
paths={'data':'lib/data','services':'lib/services','ui':'lib/ui','test':'test','main':'lib/main.dart'}
target=root/paths[group]
files=[target] if target.is_file() else list(target.glob('*.dart'))
original={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
subprocess.run(['dart','format',paths[group]],cwd=root,check=True)
payload=[{'path':str(p),'original':original[str(p)],'content':p.read_text()} for p in files]
encoded=base64.b64encode(gzip.compress(json.dumps(payload).encode())).decode()
chunks=[encoded[i:i+3000] for i in range(0,len(encoded),3000)]
assert len(chunks)<=10, 'Split this source group into smaller formatter steps'
for i,chunk in enumerate(chunks):print(f'::notice title=Flutter formatted {group} {i+1}/{len(chunks)}::{chunk}')
