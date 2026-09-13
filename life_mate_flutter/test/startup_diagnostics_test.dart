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
  test('messages, keys and traces are never returned', () {
    expect(startupCode('records', StateError('PRIVATE_KEY')), 'records_state_unavailable');
    expect(startupCode('vault', PlatformException(code:'PRIVATE_KEY', message:'Failed to unwrap PRIVATE_KEY')), 'vault_platform_key_unwrap');
  });
}
