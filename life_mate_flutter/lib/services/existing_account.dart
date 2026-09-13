import 'dart:convert';
import 'dart:typed_data';
import '../data/vault.dart';
import 'native.dart';

/// Copy identity into the new encrypted namespace without modifying original
/// databases, media, preferences, PIN or Keystore keys. Other old records stay
/// in their original storage; this is not a claim of full record migration.
class ExistingAccount {
  static Future<bool> prepare(Vault vault) async {
    if (!NativeBridge.android) return false;
    final status = await NativeBridge.channel.invokeMapMethod<String, dynamic>('existing.status');
    if (status == null) throw StateError('Existing account status unavailable');
    if (vault.settingValue('existing-account-v1') != 'done') {
      final profile = status['present'] == true
          ? await NativeBridge.channel.invokeMapMethod<String, dynamic>('existing.profile')
          : <String, dynamic>{};
      if (profile == null) throw StateError('Existing account could not be read');
      await adopt(vault, profile, status);
    }
    return status['pin'] == true;
  }

  static Future<void> adopt(Vault vault, Map<String, dynamic> snapshot, Map<String, dynamic> status) async {
    if (vault.settingValue('existing-account-v1') == 'done') return;
    final profile = Map<String, dynamic>.from(snapshot);
    final photo = profile.remove('photoBytes');
    final values = <String, String>{};
    String? copiedPhoto;
    try {
      // Never overwrite a profile already created in Personal Life OS.
      if (profile.isNotEmpty && vault.settingValue('name').isEmpty) {
        if (photo is Uint8List) {
          copiedPhoto = await vault.saveMedia(photo);
          profile['photo'] = copiedPhoto;
        }
        final full = (profile['fullName'] as String? ?? '').trim();
        final preferred = (profile['preferredName'] as String? ?? '').trim();
        final nick = (profile['nickname'] as String? ?? '').trim();
        final name = preferred.isNotEmpty ? preferred : nick.isNotEmpty ? nick : full.split(' ').first;
        if (name.isNotEmpty) {
          values['setting:name'] = name;
          values['setting:onboarded'] = 'true';
        }
        // Persist the recoverable full profile before the derived name/marker.
        await vault.setting('originalProfile', jsonEncode(profile));
        await vault.box.flush();
      }
      if (status['biometric'] == true && vault.settingValue('appLock').isEmpty) values['setting:appLock'] = 'true';
      if (vault.settingValue('appearance').isEmpty && ['System', 'Light', 'Dark'].contains(status['theme'])) values['setting:appearance'] = status['theme'] as String;
      values['setting:originalStorageRetained'] = '${status['present'] == true}';
      await vault.box.putAll(values);
      await vault.box.flush();
      await vault.setting('existing-account-v1', 'done');
      await vault.box.flush();
    } catch (_) {
      // Leave any copied encrypted photo for retry/recovery; never delete an
      // original or remove a file possibly referenced by a partially written box.
      rethrow;
    }
  }
}
