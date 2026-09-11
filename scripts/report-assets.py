"""Small, generated screenshot previews accessible even when artifact hosts are unavailable."""
from pathlib import Path
import base64
for file in Path("screenshots").glob("*.jpg"):
    data = base64.b64encode(file.read_bytes()).decode()
    # Split annotations to respect GitHub's per-annotation size limit.
    for i in range(0, len(data), 40000):
        print(f"::notice title=Preview {file.name} part {i // 40000}::{data[i:i+40000]}")
