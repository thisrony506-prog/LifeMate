import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:cryptography/cryptography.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:hive/hive.dart';
import 'package:path_provider/path_provider.dart';
import 'package:uuid/uuid.dart';
import 'entry.dart';

class Vault {
  final Box<String> box;
  final SecretKey mediaKey;
  final Directory directory;
  Vault(this.box, this.mediaKey, this.directory);
  static const secure = FlutterSecureStorage(aOptions: AndroidOptions(encryptedSharedPreferences: true));
  static Future<Vault> open() async {
    final directory = await getApplicationSupportDirectory();
    Hive.init('${directory.path}/life_mate_v1');
    final encoded = await secure.read(key: 'life-mate-vault-key-v1');
    final exists = await Hive.boxExists('vault');
    if (exists && encoded == null) throw StateError('The device encryption key is unavailable. Existing data was not replaced.');
    final key = encoded == null ? Hive.generateSecureKey() : base64Decode(encoded);
    if (encoded == null) await secure.write(key: 'life-mate-vault-key-v1', value: base64Encode(key));
    final box = await Hive.openBox<String>('vault', encryptionCipher: HiveAesCipher(key));
    return Vault(box, SecretKey(key), directory);
  }
  List<Entry> get entries => box.keys.where((k) => k.toString().startsWith('entry:'))
    .map((k) => Entry.fromJson(jsonDecode(box.get(k)!) as Map<String, dynamic>)).toList();
  Future<void> put(Entry entry) => box.put('entry:${entry.id}', jsonEncode(entry.toJson()));
  Future<void> setting(String key, String value) => box.put('setting:$key', value);
  String settingValue(String key, [String fallback = '']) => box.get('setting:$key', defaultValue: fallback)!;
  Future<String> saveMedia(Uint8List bytes) async {
    if (bytes.length > 20 * 1024 * 1024) throw const FormatException('Photo exceeds 20 MB');
    final encrypted = await AesGcm.with256bits().encrypt(bytes, secretKey: mediaKey);
    final id = const Uuid().v4();
    final file = File('${directory.path}/$id.encrypted');
    await file.writeAsBytes(encrypted.concatenation(), flush: true);
    return id;
  }
  Future<Uint8List> media(String id) async {
    if (!RegExp(r'^[a-f0-9-]{36}$').hasMatch(id)) throw const FormatException('Invalid photo ID');
    final bytes = await File('${directory.path}/$id.encrypted').readAsBytes();
    return Uint8List.fromList(await AesGcm.with256bits().decrypt(SecretBox.fromConcatenation(bytes, nonceLength: 12, macLength: 16), secretKey: mediaKey));
  }
  Future<void> deleteMedia(String id) async {
    if (!RegExp(r'^[a-f0-9-]{36}$').hasMatch(id)) return;
    final file = File('${directory.path}/$id.encrypted');
    if (await file.exists()) await file.delete();
  }
}
