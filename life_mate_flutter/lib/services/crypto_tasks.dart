import 'dart:convert';
import 'dart:typed_data';
import 'package:cryptography/cryptography.dart';
import 'package:flutter/foundation.dart';

/// CPU-heavy password derivation stays off the UI isolate. Iteration count and
/// backup/cloud formats are unchanged; reducing security is not an optimization.
Future<SecretKey> derivePhraseKey(String phrase, List<int> salt) async =>
    SecretKey(await compute(_derive, (phrase, salt)));
Future<Uint8List> _derive((String, List<int>) args) async {
  final key = await Pbkdf2(macAlgorithm: Hmac.sha256(), iterations: 210000, bits: 256)
      .deriveKey(secretKey: SecretKey(utf8.encode(args.$1)), nonce: args.$2);
  return Uint8List.fromList(await key.extractBytes());
}

Future<Uint8List> encryptMedia(Uint8List bytes, List<int> key) =>
    compute(_encrypt, (bytes, key));
Future<Uint8List> _encrypt((Uint8List, List<int>) args) async =>
    (await AesGcm.with256bits().encrypt(args.$1, secretKey: SecretKey(args.$2))).concatenation();
Future<Uint8List> decryptMedia(Uint8List bytes, List<int> key) =>
    compute(_decrypt, (bytes, key));
Future<Uint8List> _decrypt((Uint8List, List<int>) args) async => Uint8List.fromList(
    await AesGcm.with256bits().decrypt(
        SecretBox.fromConcatenation(args.$1, nonceLength: 12, macLength: 16),
        secretKey: SecretKey(args.$2)));
