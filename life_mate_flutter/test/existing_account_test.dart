import 'dart:io';
import 'dart:typed_data';
import 'package:cryptography/cryptography.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hive/hive.dart';
import 'package:life_mate_flutter/data/vault.dart';
import 'package:life_mate_flutter/data/store.dart';
import 'package:life_mate_flutter/data/entry.dart';
import 'package:life_mate_flutter/services/backup.dart';
import 'package:life_mate_flutter/services/existing_account.dart';

void main() {
  test('retained profile is encrypted, repeat-safe and separate from demo data', () async {
    final directory = await Directory.systemTemp.createTemp('life-mate-continuity');
    Hive.init(directory.path);
    final key = Hive.generateSecureKey();
    final box = await Hive.openBox<String>('continuity', encryptionCipher: HiveAesCipher(key));
    final vault = Vault(box, SecretKey(key), directory);
    try {
      await ExistingAccount.adopt(vault, {'fullName':'Existing Person','preferredName':'ExistingUserProof','birthday':'2000-01-02','information':'private-profile-detail','photoBytes':Uint8List.fromList([1,2,3])}, {'present':true,'biometric':true,'theme':'Dark'});
      expect(vault.settingValue('name'), 'ExistingUserProof');
      expect(vault.settingValue('onboarded'), 'true');
      expect(vault.settingValue('appLock'), 'true');
      expect(vault.entries, isEmpty); // No invented tasks/balances/contacts.
      final store = LifeStore(vault);
      expect(await vault.media(store.originalProfile['photo'] as String), [1,2,3]);
      expect(store.originalProfile['birthday'], '2000-01-02');
      final backup = await VaultBackup.export(store, 'private fixture recovery phrase');
      await VaultBackup.restore(store, backup, 'private fixture recovery phrase');
      final archive = store.entries(EntryKind.memory).single;
      expect(archive.text('body'), contains('private-profile-detail'));
      expect(await vault.media(archive.text('photo')), [1,2,3]);
      expect(vault.settingValue('name'), 'ExistingUserProof');
      await vault.setting('name', 'EditedName');
      await ExistingAccount.adopt(vault, {'fullName':'DoNotOverwrite'}, {'present':true});
      expect(vault.settingValue('name'), 'EditedName');
      await box.flush();
      expect(String.fromCharCodes(await File('${directory.path}/continuity.hive').readAsBytes()), isNot(contains('private-profile-detail')));
    } finally { await box.close(); await directory.delete(recursive:true); }
  });
  test('a current Personal Life OS account is never overwritten by old identity', () async {
    final directory = await Directory.systemTemp.createTemp('life-mate-current');
    Hive.init(directory.path);
    final box = await Hive.openBox<String>('current');
    final vault = Vault(box, SecretKey(List.filled(32,1)), directory);
    try {
      await vault.setting('name', 'CurrentName');
      await ExistingAccount.adopt(vault, {'fullName':'OldName'}, {'present':true});
      expect(vault.settingValue('name'), 'CurrentName');
      expect(vault.settingValue('originalProfile'), isEmpty);
    } finally { await box.close(); await directory.delete(recursive:true); }
  });
}
