import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';
import 'package:cryptography/cryptography.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:life_mate_flutter/services/crypto_tasks.dart';

void main() {
  test('isolated derivation preserves existing key format and lets UI loop run', () async {
    const phrase = 'a long test recovery phrase';
    final salt = List<int>.generate(16, (i) => i);
    var ticks = 0;
    final timer = Timer.periodic(const Duration(milliseconds: 1), (_) => ticks++);
    late SecretKey key;
    try { key = await derivePhraseKey(phrase, salt); } finally { timer.cancel(); }
    expect(ticks, greaterThan(0));
    final previous = await Pbkdf2(macAlgorithm: Hmac.sha256(), iterations: 210000, bits: 256).deriveKey(secretKey: SecretKey(utf8.encode(phrase)), nonce: salt);
    expect(await key.extractBytes(), await previous.extractBytes());
  });
  test('isolated media is compatible, authenticated and rejects a wrong key', () async {
    final key = List<int>.generate(32, (i) => i);
    final bytes = Uint8List.fromList(utf8.encode('private image fixture'));
    final encrypted = await encryptMedia(bytes, key);
    expect(await decryptMedia(encrypted, key), bytes);
    expect(await AesGcm.with256bits().decrypt(SecretBox.fromConcatenation(encrypted, nonceLength: 12, macLength: 16), secretKey: SecretKey(key)), bytes);
    await expectLater(decryptMedia(encrypted, List.filled(32, 255)), throwsA(isA<SecretBoxAuthenticationError>()));
    encrypted[15] ^= 1;
    await expectLater(decryptMedia(encrypted, key), throwsA(isA<SecretBoxAuthenticationError>()));
  });
}
