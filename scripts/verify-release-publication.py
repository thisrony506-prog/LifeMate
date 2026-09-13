"""Verify uploaded bytes while draft URLs are temporary; require final URL after publication."""
import hashlib
import json
import os
from pathlib import Path
import sys

REPO='thisrony506-prog/LifeMate'

def validate(release, code, version, name, size, sha, draft):
    if release.get('tag_name') != 'v'+version or release.get('draft') is not draft or release.get('prerelease') is not False:
        raise ValueError('Unexpected release version or publication state')
    assets=release.get('assets',[])
    if len(assets)!=1: raise ValueError('Exactly one APK asset is required')
    asset=assets[0]
    if name != f'LifeMate-{code}.apk' or asset.get('name')!=name or asset.get('size')!=size or asset.get('state')!='uploaded':
        raise ValueError('Unexpected APK asset metadata')
    if asset.get('digest')!='sha256:'+sha: raise ValueError('Uploaded APK digest mismatch or unavailable')
    identifier=asset.get('id')
    if not isinstance(identifier,int) or identifier<=0 or asset.get('url')!=f'https://api.github.com/repos/{REPO}/releases/assets/{identifier}':
        raise ValueError('Unexpected official APK asset identity')
    if not draft and asset.get('browser_download_url')!=f'https://github.com/{REPO}/releases/download/v{version}/{name}':
        raise ValueError('Published APK must have the canonical official download URL')
    # GitHub gives draft assets /untagged-... URLs. They are not offered to users or trusted for downloading.

if __name__=='__main__':
    mode=sys.argv[1]
    if mode not in ('draft','published'): raise SystemExit('Expected draft or published mode')
    release=json.loads(Path(sys.argv[2]).read_text())
    code=int(os.environ['LIFEMATE_VERSION_CODE']);version=os.environ['LIFEMATE_VERSION_NAME']
    apk=Path('release-download')/f'LifeMate-{code}.apk'
    validate(release,code,version,apk.name,apk.stat().st_size,hashlib.sha256(apk.read_bytes()).hexdigest(),mode=='draft')
    print(f'PASS: {mode} release has the correct version and verified APK bytes')
