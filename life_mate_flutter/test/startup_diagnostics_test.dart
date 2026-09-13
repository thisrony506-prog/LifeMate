import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:life_mate_flutter/services/startup_diagnostics.dart';

void main() {
  test('platform errors with spaces still have a safe diagnosable code', () {
    final error = PlatformException(code: 'Exception encountered', message: 'Field value_ not found in protobuf', details: 'PRIVATE_SECRET_OR_PROFILE');
    expect(startupCode('vault', error), 'vault_platform_serialization');
    expect(startupCode('PRIVATE_PROFILE', error), 'unknown_platform_serialization');
    expect(startupCode('vault', error), isNot(contains('PRIVATE')));
  });
  test('vault operation and framework failure remain non-personal', () {
    expect(startupCode('vault_path', PlatformException(code: 'channel-error', message: 'PRIVATE_CHANNEL')), 'vault_path_platform_plugin_channel');
    expect(startupCode('vault_key_write', PlatformException(code: 'Exception encountered', details: 'java.lang.NullPointerException: PRIVATE_KEY')), 'vault_key_write_platform_null_pointer');
    expect(startupCode('vault_key_read', PlatformException(code: 'Exception encountered', details: 'java.security.NoSuchAlgorithmException: PRIVATE_VALUE')), 'vault_key_read_platform_crypto_provider');
  });
  test('messages, keys and traces are never returned', () {
    expect(startupCode('records', StateError('PRIVATE_KEY')), 'records_state_unavailable');
    expect(startupCode('vault', PlatformException(code:'PRIVATE_KEY', message:'Failed to unwrap PRIVATE_KEY')), 'vault_platform_key_unwrap');
  });
}
