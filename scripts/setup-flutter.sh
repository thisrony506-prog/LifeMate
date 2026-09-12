#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../life_mate_flutter"
flutter pub get
python3 - <<'LOCK'
import base64,gzip
from pathlib import Path
encoded=base64.b64encode(gzip.compress(Path('pubspec.lock').read_bytes())).decode()
chunks=[encoded[i:i+3000] for i in range(0,len(encoded),3000)]
for i,chunk in enumerate(chunks): print(f'::notice title=Flutter lock {i+1}/{len(chunks)}::{chunk}')
LOCK
# Generated iOS development host: same public identity, explicit permission purpose.
python3 - <<'PY'
from pathlib import Path
import plistlib
p=Path('.ios/Runner/Info.plist')
if p.exists():
 d=plistlib.loads(p.read_bytes())
 d.update(CFBundleDisplayName='Life Mate',NSCameraUsageDescription='Choose a prescription or memory photo. / প্রেসক্রিপশন বা স্মৃতির ছবি।',NSPhotoLibraryUsageDescription='Choose a photo to encrypt privately. / ব্যক্তিগতভাবে এনক্রিপ্ট করার ছবি।',NSMicrophoneUsageDescription='Optional on-device expense voice input. / ঐচ্ছিক খরচের ভয়েস ইনপুট।',NSSpeechRecognitionUsageDescription='Convert the expense you approve into editable text. / অনুমোদিত খরচ সম্পাদনাযোগ্য লেখায় বদলানো।',NSLocationWhenInUseUsageDescription='Only for SOS or sharing you start, while using Life Mate. / শুধু চালু করা SOS বা অবস্থান শেয়ারের জন্য।',NSFaceIDUsageDescription='Protect your secret journal. / গোপন ডায়েরি সুরক্ষিত রাখা।')
 p.write_bytes(plistlib.dumps(d))
PY
