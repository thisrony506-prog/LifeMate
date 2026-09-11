"""Expose a small diagnostic tail as an annotation, also readable through GitHub's API."""
from pathlib import Path
import sys
log = Path(sys.argv[1])
text = log.read_text(errors="replace") if log.exists() else "No build log was produced."
lines = text.splitlines()
errors = [line for line in lines if line.startswith("e: ") or " error:" in line or " FAILED" in line or "What went wrong" in line or "failure message=" in line]
summary = "\n".join(errors[:45] + ["--- Last log lines ---"] + lines[-65:])[-18000:]
summary = summary.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
print(f"::notice title=Android diagnostics::{summary}")
if "compile" in str(log):
    import base64
    for file in Path("app/schemas").rglob("*.json"):
        encoded = base64.b64encode(file.read_bytes()).decode()
        if len(encoded) < 55000:
            print(f"::notice title=Room schema {file.name}::{encoded}")
