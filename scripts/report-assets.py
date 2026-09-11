"""Small, generated screenshot previews accessible even when artifact hosts are unavailable."""
from pathlib import Path
import base64
import sys
for file in Path("screenshots").glob(sys.argv[1] if len(sys.argv) > 1 else "preview-*.jpg"):
    data = base64.b64encode(file.read_bytes()).decode()
    # Split annotations to respect GitHub's per-annotation size limit.
    for i in range(0, len(data), 2800):
        print(f"::notice title=Preview {file.name} part {i // 2800}::{data[i:i+2800]}")
