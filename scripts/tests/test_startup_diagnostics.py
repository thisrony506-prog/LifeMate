import sys
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from startup_diagnostics import storage_codes


class StartupDiagnosticsTest(unittest.TestCase):
    def test_only_fixed_stage_and_exception_categories_are_emitted(self):
        log = '''E SecureStorageAndroid: StorageCipher initialization failed
E SecureStorageAndroid: java.security.NoSuchAlgorithmException: PRIVATE_KEY
E SecureStorageAndroid: at private.profile.Name(PRIVATE_PATH)
E SecureStorageAndroid: EncryptedSharedPreferences initialization failed
E SecureStorageAndroid: java.lang.NullPointerException: PRIVATE_PROFILE'''
        self.assertEqual(storage_codes(log), [
            'storage_cipher:failed', 'storage_cipher:crypto_algorithm',
            'encrypted_preferences:failed', 'encrypted_preferences:null_pointer'])

    def test_unknown_names_and_raw_values_never_escape(self):
        self.assertEqual(storage_codes('PRIVATE_TOKEN com.example.PersonalException: PRIVATE_NAME'), [])
        self.assertEqual(storage_codes('notjava.lang.NullPointerExceptionExtra'), [])
        self.assertEqual(storage_codes('java.io.IOException: private\njava.io.IOException: private'), ['platform_storage:io'])


if __name__ == '__main__':
    unittest.main()
