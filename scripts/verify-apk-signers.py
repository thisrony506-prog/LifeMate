"""Read verified apksigner output across legacy and SDK-range signer formats."""
import re
import sys
from pathlib import Path


def signer_digest(text):
    counts = re.findall(r'^\s*Number of signers:\s*(\d+)\s*$', text, re.M)
    if not counts or any(int(n) != 1 for n in counts):
        raise ValueError('Expected exactly one verified APK signer.')
    # New build tools may label v3 signers by supported SDK range instead of #1.
    digests = re.findall(r'^\s*Signer (?:#\d+|\([^\r\n]+\)) certificate SHA-256 digest:\s*([0-9a-fA-F]{64})\s*$', text, re.M)
    unique = {digest.lower() for digest in digests}
    if len(unique) != 1:
        raise ValueError('Missing or inconsistent APK signer certificate fingerprints.')
    return unique.pop()


if __name__ == '__main__':
    try:
        print(signer_digest(Path(sys.argv[1]).read_text()))
    except (ValueError, OSError) as error:
        raise SystemExit(str(error))
