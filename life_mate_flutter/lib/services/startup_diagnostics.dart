import 'package:flutter/services.dart';

/// Only a small fixed vocabulary leaves this function. Never expose exception
/// messages, platform stack traces, key material, paths or profile values.
String startupCode(String phase, Object error) {
  final safePhase = const ['vault', 'vault_path', 'vault_key_read', 'vault_key_write', 'vault_box_check', 'vault_box_open', 'existing_account', 'updates', 'records'].contains(phase) ? phase : 'unknown';
  final kind = error is PlatformException ? 'platform' : error is StateError ? 'state' : error is FormatException ? 'format' : 'other';
  var category = 'unavailable';
  if (error is PlatformException) {
    final technical = '${error.message ?? ''} ${error.details ?? ''}'.toLowerCase();
    if (error.code == 'channel-error') {
      category = 'plugin_channel';
    } else if (technical.contains('protobuf') || (technical.contains('field') && technical.contains('not found'))) {
      category = 'serialization';
    } else if (technical.contains('unwrap')) {
      category = 'key_unwrap';
    } else if (technical.contains('badtag') || technical.contains('mac check') || technical.contains('tag mismatch')) {
      category = 'key_authentication';
    } else if (technical.contains('keystore')) {
      category = 'keystore';
    } else if (technical.contains('readonly') || technical.contains('read-only')) {
      category = 'readonly';
    } else if (technical.contains('nullpointerexception')) {
      category = 'null_pointer';
    } else if (technical.contains('classcastexception')) {
      category = 'class_cast';
    } else if (technical.contains('nosuchalgorithmexception') || technical.contains('nosuchpaddingexception') || technical.contains('nosuchproviderexception')) {
      category = 'crypto_provider';
    } else if (technical.contains('illegalargumentexception')) {
      category = 'invalid_argument';
    } else if (technical.contains('securityexception') || technical.contains('accessdeniedexception')) {
      category = 'access_denied';
    } else if (error.code == 'existing_profile' || error.code == 'existing_status') {
      category = error.code;
    }
  }
  return '${safePhase}_${kind}_$category';
}
