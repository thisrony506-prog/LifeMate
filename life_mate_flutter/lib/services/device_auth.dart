import 'package:flutter/foundation.dart';
import 'package:local_auth/local_auth.dart';

/// Shared only to distinguish our OS authentication dialog from leaving the app.
class DeviceAuthentication {
  static final prompts = ValueNotifier<int>(0);
  static Future<bool> authenticate({
    required String localizedReason,
    AuthenticationOptions options = const AuthenticationOptions(),
  }) async {
    prompts.value++;
    try {
      return await LocalAuthentication().authenticate(
        localizedReason: localizedReason,
        options: options,
      );
    } finally {
      prompts.value--;
    }
  }
}
