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
