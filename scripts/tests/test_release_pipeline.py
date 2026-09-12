import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[2]
class ReleasePipelineTest(unittest.TestCase):
    def test_version_formula_and_invalid_run_numbers(self):
        with tempfile.TemporaryDirectory() as d:
            env=dict(os.environ,GITHUB_ENV=d+'/env')
            for run in ('1','57','9999999'):
                env['GITHUB_RUN_NUMBER']=run
                subprocess.run(['python3',str(ROOT/'scripts/release-version.py')],env=env,check=True,capture_output=True)
                text=Path(d+'/env').read_text()
                self.assertIn(f'LIFEMATE_VERSION_CODE={100000+int(run)}',text)
                self.assertIn(f'LIFEMATE_VERSION_NAME=1.2.{run}',text)
            for run in ('','0','01','-1','10000000','abc'):
                env['GITHUB_RUN_NUMBER']=run
                self.assertNotEqual(subprocess.run(['python3',str(ROOT/'scripts/release-version.py')],env=env,capture_output=True).returncode,0)

    def test_missing_and_partial_signing_fail_closed(self):
        with tempfile.TemporaryDirectory() as d:
            env={k:v for k,v in os.environ.items() if not k.startswith('LIFEMATE_')}
            env.update(RUNNER_TEMP=d,GITHUB_ENV=d+'/env')
            for partial in (False,True):
                if partial: env['LIFEMATE_KEY_ALIAS']='lifemate'
                result=subprocess.run(['bash',str(ROOT/'scripts/prepare-signing.sh')],env=env,capture_output=True)
                self.assertNotEqual(result.returncode,0)
                self.assertFalse(Path(d+'/lifemate-release.jks').exists())

    def test_only_apk_uploaded_and_draft_verified_before_publication(self):
        workflow=(ROOT/'.github/workflows/android.yml').read_text()
        self.assertEqual(workflow.count('uses: actions/upload-artifact@'),1)
        self.assertIn('path: release-download/*.apk',workflow)
        script=(ROOT/'scripts/publish-release.sh').read_text()
        self.assertLess(script.index('--draft '),script.index("a.get('digest')"))
        self.assertLess(script.index("a.get('digest')"),script.index('--draft=false'))
        self.assertNotIn('--clobber',script)

    def test_wrapped_base64_and_bad_encoding(self):
        import base64
        with tempfile.TemporaryDirectory() as d:
            env=dict(os.environ,RUNNER_TEMP=d,GITHUB_ENV=d+'/env',
                     LIFEMATE_STORE_PASSWORD='INERT-TEST-INPUT',LIFEMATE_KEY_PASSWORD='INERT-TEST-INPUT',LIFEMATE_KEY_ALIAS='lifemate')
            encoded=base64.b64encode(b'INERT-FIXTURE-NOT-A-KEY').decode()
            env['LIFEMATE_KEYSTORE_BASE64']=' \r\n'.join(encoded[i:i+4] for i in range(0,len(encoded),4))
            subprocess.run(['bash',str(ROOT/'scripts/prepare-signing.sh')],env=env,check=True,capture_output=True)
            self.assertEqual(Path(d+'/lifemate-release.jks').read_bytes(),b'INERT-FIXTURE-NOT-A-KEY')
            for invalid in ('%%%not-base64%%%',' \r\n '):
                env['LIFEMATE_KEYSTORE_BASE64']=invalid
                result=subprocess.run(['bash',str(ROOT/'scripts/prepare-signing.sh')],env=env,capture_output=True)
                self.assertNotEqual(result.returncode,0)
                self.assertFalse(Path(d+'/lifemate-release.jks').exists())

    def test_release_verifier_uses_pinned_stable_build_tools(self):
        script=(ROOT/'scripts/prepare-upgrade-apks.sh').read_text()
        self.assertIn('$ANDROID_HOME/build-tools/35.0.0/apksigner',script)
        self.assertIn('buildToolsVersion = "35.0.0"',(ROOT/'app/build.gradle.kts').read_text())
        self.assertNotIn('sort -V',script)

    def test_signer_identity_comes_from_verified_certificate_objects(self):
        helper=(ROOT/'scripts/VerifiedApkSigner.java').read_text()
        self.assertIn('result.isVerified()',helper)
        self.assertIn('signers.size() != 1',helper)
        self.assertIn('getSignerCertificates()',helper)
        pipeline=(ROOT/'scripts/prepare-upgrade-apks.sh').read_text()
        self.assertIn('scripts/VerifiedApkSigner.java',pipeline)
        self.assertNotIn('verify-apk-signers.py',pipeline)

    def test_release_smoke_only_presses_back_when_keyboard_is_visible(self):
        import runpy
        scope=runpy.run_path(str(ROOT/'scripts/verify-release-install.py'))
        hide=scope['hide_keyboard_if_shown']
        calls=[]
        def adb_hidden(*args):
            calls.append(args)
            return 'mInputShown=false'
        hide.__globals__['adb']=adb_hidden
        hide()
        self.assertEqual(calls,[('shell','dumpsys','input_method')])
        calls.clear()
        def adb_shown(*args):
            calls.append(args)
            return 'mInputShown=true'
        hide.__globals__['adb']=adb_shown
        hide()
        self.assertIn(('shell','input','keyevent','4'),calls)
