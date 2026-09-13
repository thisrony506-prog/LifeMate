"""Reduce platform storage logs to a fixed, non-personal diagnostic vocabulary."""
import re

# Never return log messages, stack frames, paths, keys or arbitrary class names.
ERROR_TYPES = {
    'java.lang.NullPointerException': 'null_pointer',
    'java.lang.ClassCastException': 'class_cast',
    'java.lang.IllegalArgumentException': 'invalid_argument',
    'java.lang.IllegalStateException': 'invalid_state',
    'java.lang.SecurityException': 'access_denied',
    'java.lang.NoSuchFieldException': 'missing_field',
    'java.lang.NoSuchMethodException': 'missing_method',
    'java.lang.ClassNotFoundException': 'missing_class',
    'java.security.NoSuchAlgorithmException': 'crypto_algorithm',
    'java.security.NoSuchProviderException': 'crypto_provider',
    'java.security.InvalidKeyException': 'invalid_key',
    'java.security.InvalidAlgorithmParameterException': 'crypto_parameters',
    'java.security.UnrecoverableKeyException': 'unrecoverable_key',
    'java.security.KeyStoreException': 'key_store',
    'javax.crypto.NoSuchPaddingException': 'crypto_padding',
    'javax.crypto.BadPaddingException': 'bad_padding',
    'javax.crypto.AEADBadTagException': 'authentication',
    'javax.crypto.IllegalBlockSizeException': 'block_size',
    'android.security.KeyStoreException': 'android_key_store',
    'android.security.keystore.KeyPermanentlyInvalidatedException': 'invalidated_key',
    'java.io.IOException': 'io',
}
STAGES = {
    'StorageCipher initialization failed': 'storage_cipher',
    'EncryptedSharedPreferences initialization failed': 'encrypted_preferences',
    'Registration failed': 'plugin_registration',
}


def storage_codes(log):
    result = []
    stage = 'platform_storage'
    for line in log.splitlines():
        for marker, code in STAGES.items():
            if marker in line:
                stage = code
                result.append(stage + ':failed')
        for name, code in ERROR_TYPES.items():
            if re.search(r'(?<![\w.])' + re.escape(name) + r'(?![\w.])', line):
                result.append(stage + ':' + code)
    return list(dict.fromkeys(result))[:24]
