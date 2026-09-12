"""Expose a small diagnostic tail as an annotation, also readable through GitHub's API."""
from pathlib import Path
import sys
log = Path(sys.argv[1])
text = log.read_text(errors="replace") if log.exists() else "No build log was produced."
lines = text.splitlines()
errors = []
for index, line in enumerate(lines):
    if line.startswith("e: ") or " error:" in line or " FAILED" in line or "What went wrong" in line or "failure message=" in line or "Exception" in line and not line.startswith("\tat "):
        errors.extend(lines[max(0, index - 2):index + 8])
summary = "\n".join(errors[:100] + ["--- Last log lines ---"] + lines[-65:])[-18000:]
summary = summary.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
print(f"::notice title=Android diagnostics::{summary}")
import xml.etree.ElementTree as ET
for file in Path("app/build").rglob("TEST-*.xml"):
    try:
        root = ET.parse(file).getroot()
        for case in root.iter("testcase"):
            for failure in list(case.findall("failure")) + list(case.findall("error")):
                detail = (case.attrib.get("classname", "") + "." + case.attrib.get("name", "") + "\n" + failure.attrib.get("message", "") + "\n" + (failure.text or ""))[:12000]
                detail = detail.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
                print(f"::notice title=Device test failure::{detail}")
    except ET.ParseError:
        pass
