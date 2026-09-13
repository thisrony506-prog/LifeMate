"""Validate PUBLIC Firebase client config; refuse provider/service-account secrets."""
import base64,json,os,re
from pathlib import Path
ALLOWED={'FIREBASE_PROJECT_ID','FIREBASE_API_KEY','FIREBASE_ANDROID_APP_ID','FIREBASE_IOS_APP_ID','FIREBASE_SENDER_ID','FIREBASE_STORAGE_BUCKET','FIREBASE_FUNCTIONS_REGION'}
def encode_config(raw):
    if not raw.strip(): return ''
    data=json.loads(raw)
    if not isinstance(data,dict) or set(data)-ALLOWED: raise ValueError('Only public Firebase client fields are allowed')
    for key in ('FIREBASE_PROJECT_ID','FIREBASE_API_KEY','FIREBASE_ANDROID_APP_ID','FIREBASE_SENDER_ID','FIREBASE_STORAGE_BUCKET'):
        if not isinstance(data.get(key),str) or not data[key].strip(): raise ValueError('Incomplete public Firebase client configuration')
    if not re.fullmatch(r'[a-z][a-z0-9-]{4,28}[a-z0-9]',data['FIREBASE_PROJECT_ID']): raise ValueError('Invalid project ID')
    if not re.fullmatch(r'1:[0-9]+:android:[a-f0-9]+',data['FIREBASE_ANDROID_APP_ID']): raise ValueError('Invalid Android app ID')
    if not data['FIREBASE_SENDER_ID'].isdigit(): raise ValueError('Invalid sender ID')
    data.setdefault('FIREBASE_FUNCTIONS_REGION','asia-south1')
    if data['FIREBASE_FUNCTIONS_REGION']!='asia-south1': raise ValueError('Client region must match deployed callable region')
    if any(not isinstance(v,str) or len(v)>500 or '\n' in v or 'PRIVATE KEY' in v for v in data.values()): raise ValueError('Unsafe config value')
    return ','.join(base64.b64encode(f'{k}={v}'.encode()).decode() for k,v in sorted(data.items()))
if __name__=='__main__':
    encoded=encode_config(os.environ.get('LIFEMATE_FIREBASE_CONFIG',''))
    if encoded:
        with Path(os.environ['GITHUB_ENV']).open('a') as f:f.write('LIFEMATE_DART_DEFINES='+encoded+'\n')
        print('Public Firebase client configuration validated. Provider keys are not bundled.')
    else: print('No Firebase client config: offline MVP build. Live API/sync/location remain unavailable.')
