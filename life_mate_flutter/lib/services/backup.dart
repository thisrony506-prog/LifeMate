import 'dart:convert';
import 'dart:math';
import 'dart:typed_data';
import 'package:cryptography/cryptography.dart';
import '../data/store.dart';
import '../data/entry.dart';

/// Portable, authenticated backup. The device's Hive key is never exported.
class VaultBackup {
  static const limit = 32 * 1024 * 1024;
  static Future<SecretKey> derive(String phrase, List<int> salt) {
    if (phrase.length < 12)
      throw const FormatException('Use at least 12 characters');
    return Pbkdf2(
      macAlgorithm: Hmac.sha256(),
      iterations: 210000,
      bits: 256,
    ).deriveKey(secretKey: SecretKey(utf8.encode(phrase)), nonce: salt);
  }

  static Future<Uint8List> export(LifeStore store, String phrase) async {
    if (store.demo) throw StateError('Demo cannot be exported');
    var recordSize = 0;
    for (final e in store.all) {
      recordSize += utf8.encode(jsonEncode(e.toJson())).length;
      if (recordSize > limit)
        throw const FormatException('Record backup limit');
    }
    final media = <String, String>{};
    var size = 0;
    for (final e in store.all.where(
      (v) => !v.deleted && v.text('photo').isNotEmpty,
    )) {
      final id = e.text('photo');
      if (media.containsKey(id)) continue;
      final bytes = await store.vault!.media(id);
      size += bytes.length;
      if (size > limit)
        throw const FormatException('Backup media limit: 32 MB');
      media[id] = base64Encode(bytes);
    }
    final payload = utf8.encode(
      jsonEncode({
        'schema': 1,
        'name': store.name,
        'language': store.language,
        'entries': store.all.map((e) => e.toJson()).toList(),
        'media': media,
      }),
    );
    if (payload.length > limit * 2)
      throw const FormatException('Backup is too large');
    final salt = List<int>.generate(16, (_) => Random.secure().nextInt(256));
    final encrypted = await AesGcm.with256bits().encrypt(
      payload,
      secretKey: await derive(phrase, salt),
    );
    return Uint8List.fromList(
      utf8.encode(
        jsonEncode({
          'format': 'LifeMateVault1',
          'salt': base64Encode(salt),
          'ciphertext': base64Encode(encrypted.concatenation()),
        }),
      ),
    );
  }

  static Future<void> restore(
    LifeStore store,
    Uint8List bytes,
    String phrase,
  ) async {
    if (store.demo) throw StateError('Exit Demo first');
    if (bytes.length > limit * 3)
      throw const FormatException('Backup is too large');
    final envelope = jsonDecode(utf8.decode(bytes)) as Map;
    if (envelope['format'] != 'LifeMateVault1')
      throw const FormatException('Unsupported backup');
    final salt = base64Decode(envelope['salt'] as String);
    if (salt.length != 16) throw const FormatException('Invalid salt');
    final plaintext = await AesGcm.with256bits().decrypt(
      SecretBox.fromConcatenation(
        base64Decode(envelope['ciphertext'] as String),
        nonceLength: 12,
        macLength: 16,
      ),
      secretKey: await derive(phrase, salt),
    );
    final payload = jsonDecode(utf8.decode(plaintext)) as Map;
    if (payload['schema'] != 1)
      throw const FormatException('Unsupported backup schema');
    final entries = (payload['entries'] as List)
        .map((e) => Entry.fromJson(Map<String, dynamic>.from(e as Map)))
        .toList();
    if (entries.length > 100000 ||
        entries.map((e) => e.id).toSet().length != entries.length)
      throw const FormatException('Invalid records');
    final raw = Map<String, dynamic>.from(payload['media'] as Map);
    final media = <String, Uint8List>{};
    var total = 0;
    for (final pair in raw.entries) {
      if (!RegExp(r'^[a-f0-9-]{36}$').hasMatch(pair.key))
        throw const FormatException('Invalid photo');
      final b = base64Decode(pair.value as String);
      total += b.length;
      if (b.length > 20 * 1024 * 1024 || total > limit)
        throw const FormatException('Media limit');
      media[pair.key] = b;
    }
    for (final e in entries) {
      if (!e.deleted &&
          e.text('photo').isNotEmpty &&
          !media.containsKey(e.text('photo')))
        throw const FormatException('Missing photo');
    }
    // Import as additional records, never destructively replace local/offline work.
    final ids = <String, String>{};
    try {
      for (final m in media.entries) {
        ids[m.key] = await store.vault!.saveMedia(m.value);
      }
      final values = <String, String>{};
      final imported = <Entry>[];
      for (final e in entries.where((e) => !e.deleted)) {
        final copy = Entry(
          kind: e.kind,
          title: e.title,
          date: e.date,
          fields: {
            ...e.fields,
            'photo': ids[e.text('photo')] ?? '',
            'time': '',
          },
        );
        imported.add(copy);
        values['entry:${copy.id}'] = jsonEncode(copy.toJson());
      }
      await store.vault!.box.putAll(values);
      store.reloadVault();
    } catch (_) {
      for (final id in ids.values) {
        if (!store.vault!.entries.any((e) => e.text('photo') == id))
          await store.vault?.deleteMedia(id);
      }
      rethrow;
    }
  }
}
