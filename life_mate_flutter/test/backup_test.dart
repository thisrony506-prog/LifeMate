import 'dart:io';
import 'dart:typed_data';
import 'package:cryptography/cryptography.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hive/hive.dart';
import 'package:life_mate_flutter/data/entry.dart';
import 'package:life_mate_flutter/data/store.dart';
import 'package:life_mate_flutter/data/vault.dart';
import 'package:life_mate_flutter/services/backup.dart';
void main(){
  test('portable backup authenticates and adds records without deleting existing work',()async{
    final dir=await Directory.systemTemp.createTemp('life-backup');Hive.init(dir.path);final key=Hive.generateSecureKey();
    final box=await Hive.openBox<String>('backup_test',encryptionCipher:HiveAesCipher(key));
    final store=LifeStore(Vault(box,SecretKey(key),dir));
    await store.save(Entry(kind:EntryKind.journal,title:'ব্যক্তিগত',fields:{'body':'A private journal'}));
    final bytes=await VaultBackup.export(store,'correct recovery phrase');
    expect(String.fromCharCodes(bytes),isNot(contains('A private journal')));
    await expectLater(VaultBackup.restore(store,bytes,'wrong recovery phrase'),throwsA(isA<SecretBoxAuthenticationError>()));
    expect(store.all.length,1);
    await VaultBackup.restore(store,bytes,'correct recovery phrase');expect(store.all.length,2);
    final corrupt=Uint8List.fromList(bytes);corrupt[corrupt.length~/2]^=1;
    await expectLater(VaultBackup.restore(store,corrupt,'correct recovery phrase'),throwsA(anything));
    expect(store.all.length,2);await box.close();await dir.delete(recursive:true);
  });
}
