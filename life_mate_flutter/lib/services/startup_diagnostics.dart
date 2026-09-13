import 'package:flutter/services.dart';

/// Only a small fixed vocabulary leaves this function. Never expose exception
/// messages, platform stack traces, key material, paths or profile values.
String startupCode(String phase, Object error) {
  final safePhase = const ['vault', 'existing_account', 'updates', 'records'].contains(phase) ? phase : 'unknown';
  final kind = error is PlatformException ? 'platform' : error is StateError ? 'state' : error is FormatException ? 'format' : 'other';
  var category = 'unavailable';
  if (error is PlatformException) {
    final technical = '${error.message ?? ''} ${error.details ?? ''}'.toLowerCase();
    if (technical.contains('protobuf') || (technical.contains('field') && technical.contains('not found'))) {
      category = 'serialization';
    } else if (technical.contains('unwrap')) {
      category = 'key_unwrap';
    } else if (technical.contains('badtag') || technical.contains('mac check') || technical.contains('tag mismatch')) {
      category = 'key_authentication';
    } else if (technical.contains('keystore')) {
      category = 'keystore';
    } else if (technical.contains('readonly') || technical.contains('read-only')) {
      category = 'readonly';
    } else if (error.code == 'existing_profile' || error.code == 'existing_status') {
      category = error.code;
    }
  }
  return '${safePhase}_${kind}_$category';
}
