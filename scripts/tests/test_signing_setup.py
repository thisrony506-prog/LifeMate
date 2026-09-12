"""PTY tests with fake tools and inert bytes: no real signing identity/secrets are created."""
import hashlib
import json
import os
from pathlib import Path
import pty
import secrets
import select
import signal
import subprocess
import tempfile
import time
import unittest

ROOT = Path(__file__).resolve().parents[2]
FAKE = r'''#!/usr/bin/env python3
import hashlib, json, os, pathlib, sys
name=pathlib.Path(sys.argv[0]).name
a=sys.argv[1:]
if name in ('java','javac') or (name=='keytool' and '-J-version' in a):
    v=os.environ.get('TEST_JAVA','17')
    print('javac '+v+'.0.1' if name=='javac' else 'openjdk version "'+v+'.0.1"',file=sys.stderr);sys.exit()
if name=='gh':
    if a[:2]==['auth','status']: sys.exit(1 if os.environ.get('TEST_AUTH_FAIL') else 0)
    if a[:2]==['secret','list']: print(os.environ.get('TEST_EXISTING',''));sys.exit()
    if a[:2]==['secret','set']:
        data=sys.stdin.buffer.read()
        with open(os.environ['TEST_EVENTS'],'a') as f: f.write(json.dumps({'secret':a[2],'sha':hashlib.sha256(data).hexdigest()})+'\n')
        if os.environ.get('TEST_UPLOAD_FAIL'): print('upload failed',file=sys.stderr);sys.exit(1)
        sys.exit()
    if a[0]=='api':
        if 'public-key' in a[1]:
            if os.environ.get('TEST_PERMISSION_FAIL'): sys.exit(1)
            print('public-key-id')
        elif '/releases' in a[1]: print(os.environ.get('TEST_PUBLISHED',''))
        else: print('thisrony506-prog/LifeMate')
        sys.exit()
if name=='keytool':
    assert '-storepass:env' in a
    if '-genkeypair' in a:
        assert a[a.index('-alias')+1]=='lifemate' and a[a.index('-storetype')+1]=='JKS'
        assert a[a.index('-keysize')+1]=='3072' and a[a.index('-keyalg')+1]=='RSA'
        pathlib.Path(a[a.index('-keystore')+1]).write_bytes(b'INERT-TEST-FIXTURE-NOT-A-KEY')
    elif '-exportcert' in a: print('public test fixture')
    sys.exit()
if name=='openssl':
    sys.stdin.read()
    print('Public-Key: (3072 bit)' if a[0]=='pkey' else 'public test fixture');sys.exit()
raise SystemExit('Unexpected mock invocation: '+name)
'''

class SigningSetupTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.home = self.base / 'home'; self.home.mkdir()
        self.bin = self.base / 'bin'; self.bin.mkdir()
        for name in ('java', 'javac', 'keytool', 'openssl', 'gh'):
            p = self.bin / name; p.write_text(FAKE); p.chmod(0o700)
        self.env = dict(os.environ, HOME=str(self.home), PATH=str(self.bin)+':'+os.environ['PATH'],
                        CI='false', GITHUB_ACTIONS='false', TEST_EVENTS=str(self.base/'events'))
        self.password = secrets.token_urlsafe(24)  # Inert test input, never a production password/key.

    def run_script(self, confirm=None, args=()):
        master, slave = pty.openpty()
        # setsid + TIOCSCTTY makes /dev/tty available to the isolated child.
        def session():
            import fcntl, termios
            os.setsid(); fcntl.ioctl(slave, termios.TIOCSCTTY, 0)
        p = subprocess.Popen(['bash', str(ROOT/'scripts/setup-release-signing.sh'), *args], env=self.env,
                             stdin=slave, stdout=slave, stderr=slave, preexec_fn=session)
        os.close(slave)
        output = b''; first = second = False
        deadline = time.monotonic()+12
        try:
            while time.monotonic()<deadline:
                if select.select([master],[],[],.1)[0]:
                    try: data=os.read(master,65536)
                    except OSError: break
                    if not data: break
                    output += data
                    if b'at least 12 characters): ' in output and not first:
                        os.write(master,(self.password+'\n').encode());first=True
                    if b'Confirm password: ' in output and not second:
                        os.write(master,((confirm or self.password)+'\n').encode());second=True
                if p.poll() is not None: break
            if p.poll() is None: p.wait(timeout=2)
        finally:
            if p.poll() is None: os.killpg(p.pid, signal.SIGKILL);p.wait()
            os.close(master)
        text=output.decode(errors='replace')
        self.assertNotIn(self.password, text)
        return p.returncode, text

    @property
    def folder(self): return self.home/'lifemate-private-signing-backup'

    def test_interactive_password_fixed_location_private_permissions_and_four_secrets(self):
        code, text=self.run_script();self.assertEqual(code,0,text)
        self.assertEqual(sorted(x.name for x in self.folder.iterdir()),['lifemate-release.jks'])
        self.assertEqual(self.folder.stat().st_mode & 0o777,0o700)
        self.assertEqual((self.folder/'lifemate-release.jks').stat().st_mode & 0o777,0o600)
        events=[json.loads(x) for x in (self.base/'events').read_text().splitlines()]
        self.assertEqual({x['secret'] for x in events},{'LIFEMATE_KEYSTORE_BASE64','LIFEMATE_STORE_PASSWORD','LIFEMATE_KEY_ALIAS','LIFEMATE_KEY_PASSWORD'})
        for e in events:
            if e['secret'] in ('LIFEMATE_STORE_PASSWORD','LIFEMATE_KEY_PASSWORD'):
                self.assertEqual(e['sha'],hashlib.sha256(self.password.encode()).hexdigest())

    def test_mismatch_creates_nothing(self):
        self.assertNotEqual(self.run_script(confirm='different-inert-input')[0],0);self.assertFalse(self.folder.exists())

    def test_auth_and_permission_fail_before_creation(self):
        for flag in ('TEST_AUTH_FAIL','TEST_PERMISSION_FAIL'):
            self.env[flag]='1'
            self.assertNotEqual(self.run_script()[0],0);self.assertFalse(self.folder.exists())
            self.env.pop(flag)

    def test_wrong_jdk_fails_before_creation(self):
        self.env['TEST_JAVA']='21'
        self.assertNotEqual(self.run_script()[0],0);self.assertFalse(self.folder.exists())

    def test_existing_secret_or_official_apk_refuses_new_identity(self):
        for flag,value in [('TEST_EXISTING','LIFEMATE_KEY_ALIAS'),('TEST_PUBLISHED','LifeMate-100001.apk')]:
            self.env[flag]=value
            self.assertNotEqual(self.run_script()[0],0);self.assertFalse(self.folder.exists())
            self.env.pop(flag)

    def test_existing_folder_and_symlink_are_never_overwritten(self):
        self.folder.mkdir(); sentinel=self.folder/'keep';sentinel.write_text('keep')
        self.assertNotEqual(self.run_script()[0],0);self.assertEqual(sentinel.read_text(),'keep')
        sentinel.unlink();self.folder.rmdir();self.folder.symlink_to(self.base/'absent')
        self.assertNotEqual(self.run_script()[0],0);self.assertTrue(self.folder.is_symlink())

    def test_arbitrary_path_argument_and_ci_refused(self):
        self.assertNotEqual(self.run_script(args=(str(self.base/'other'),))[0],0)
        self.env['GITHUB_ACTIONS']='true';self.assertNotEqual(self.run_script()[0],0);self.assertFalse(self.folder.exists())

    def test_partial_upload_keeps_key_and_refuses_regeneration(self):
        self.env['TEST_UPLOAD_FAIL']='1'
        self.assertNotEqual(self.run_script()[0],0)
        self.assertTrue((self.folder/'lifemate-release.jks').exists())
        self.assertNotEqual(self.run_script()[0],0)
