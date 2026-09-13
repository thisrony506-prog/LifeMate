import 'dart:io';
import 'dart:typed_data';
import 'package:cryptography/cryptography.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hive/hive.dart';
import 'package:life_mate_flutter/data/vault.dart';
import 'package:life_mate_flutter/data/entry.dart';

void main() {
  test('encrypted Hive reopens; media decrypts only with its key', () async {
    final dir = await Directory.systemTemp.createTemp('life-mate-test');
    Hive.init(dir.path);
    final key = Hive.generateSecureKey();
    var box = await Hive.openBox<String>(
      'secure_test',
      encryptionCipher: HiveAesCipher(key),
    );
    var vault = Vault(box, SecretKey(key), dir);
    final entry = Entry(
      kind: EntryKind.journal,
      title: 'Secret title',
      fields: {'body': 'private_body_do_not_leak'},
    );
    await vault.put(entry);
    await box.flush();
    expect(
      String.fromCharCodes(
        await File('${dir.path}/secure_test.hive').readAsBytes(),
      ),
      isNot(contains('private_body_do_not_leak')),
    );
    final photo = await vault.saveMedia(Uint8List.fromList([1, 2, 3, 4]));
    expect(await vault.media(photo), [1, 2, 3, 4]);
    final wrong = Vault(box, SecretKey(Hive.generateSecureKey()), dir);
    await expectLater(
      wrong.media(photo),
      throwsA(isA<SecretBoxAuthenticationError>()),
    );
    await box.close();
    box = await Hive.openBox<String>(
      'secure_test',
      encryptionCipher: HiveAesCipher(key),
    );
    vault = Vault(box, SecretKey(key), dir);
    expect(vault.entries.single.fields['body'], 'private_body_do_not_leak');
    await vault.deleteMedia(photo);
    await box.close();
    await dir.delete(recursive: true);
  });
}
