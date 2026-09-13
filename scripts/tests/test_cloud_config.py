import runpy, unittest, json, base64
from pathlib import Path
encode=runpy.run_path(str(Path(__file__).resolve().parents[1]/'configure-firebase.py'))['encode_config']
class CloudConfigTest(unittest.TestCase):
 def test_offline_build_has_no_invented_project(self): self.assertEqual(encode(''),'')
 def test_only_complete_public_config_is_accepted(self):
  config={'FIREBASE_PROJECT_ID':'life-mate-demo','FIREBASE_API_KEY':'public-client-fixture','FIREBASE_ANDROID_APP_ID':'1:123:android:abcdef','FIREBASE_SENDER_ID':'123','FIREBASE_STORAGE_BUCKET':'life-mate-demo.firebasestorage.app'}
  decoded=[base64.b64decode(x).decode() for x in encode(json.dumps(config)).split(',')]
  self.assertIn('FIREBASE_FUNCTIONS_REGION=asia-south1',decoded)
  for key in ['GEMINI_API_KEY','private_key','service_account']:
   with self.assertRaises(ValueError): encode(json.dumps({**config,key:'never-bundle'}))
  with self.assertRaises(ValueError): encode(json.dumps({'FIREBASE_PROJECT_ID':'life-mate-demo'}))
  with self.assertRaises(ValueError): encode(json.dumps({**config,'FIREBASE_FUNCTIONS_REGION':'us-central1'}))
