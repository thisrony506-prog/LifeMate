import 'dart:convert';
import 'dart:io';
import 'package:cryptography/cryptography.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_storage/firebase_storage.dart';
import 'package:cloud_functions/cloud_functions.dart';
import '../data/entry.dart';
import '../data/store.dart';

class CloudService {
  static const project = String.fromEnvironment('FIREBASE_PROJECT_ID');
  static bool get configured => project.isNotEmpty;
  static Future<void> init() async {
    if (!configured) throw StateError('Firebase is not configured');
    if (Firebase.apps.isEmpty) await Firebase.initializeApp(options: FirebaseOptions(
      apiKey: const String.fromEnvironment('FIREBASE_API_KEY'),
      appId: Platform.isIOS ? const String.fromEnvironment('FIREBASE_IOS_APP_ID') : const String.fromEnvironment('FIREBASE_ANDROID_APP_ID'),
      messagingSenderId: const String.fromEnvironment('FIREBASE_SENDER_ID'), projectId: project,
      storageBucket: const String.fromEnvironment('FIREBASE_STORAGE_BUCKET'), iosBundleId: 'com.lifemate'));
  }
  static Future<User> account(String email, String password, bool create) async {
    await init();
    final auth = FirebaseAuth.instance;
    final result = create ? await auth.createUserWithEmailAndPassword(email: email.trim(), password: password) : await auth.signInWithEmailAndPassword(email: email.trim(), password: password);
    return result.user!;
  }
  static Future<void> signOut() async { await init(); await FirebaseAuth.instance.signOut(); }
  static Future<User> session() async {
    await init();
    return FirebaseAuth.instance.currentUser ?? (await FirebaseAuth.instance.signInAnonymously()).user!;
  }
  static Future<SecretKey> key(String phrase, String uid) {
    if (phrase.trim().length < 12) throw const FormatException('Use at least 12 characters');
    return Pbkdf2(macAlgorithm: Hmac.sha256(), iterations: 210000, bits: 256).deriveKey(secretKey: SecretKey(utf8.encode(phrase)), nonce: utf8.encode('LifeMate cloud v1:$uid'));
  }
  static Future<String> seal(String text, SecretKey key) async => base64Encode((await AesGcm.with256bits().encrypt(utf8.encode(text), secretKey: key)).concatenation());
  static Future<String> unseal(String text, SecretKey key) async => utf8.decode(await AesGcm.with256bits().decrypt(SecretBox.fromConcatenation(base64Decode(text), nonceLength: 12, macLength: 16), secretKey: key));

  /// Explicit, manual sync. Encryption happens before Firestore receives content.
  /// Revision mismatches abort rather than silently overwriting offline edits.
  static Future<void> sync(LifeStore store, String phrase) async {
    if (store.demo) throw StateError('Demo data cannot sync');
    await init();
    final user = FirebaseAuth.instance.currentUser;
    if (user == null || user.isAnonymous) throw StateError('Sign in with your account first');
    final secret = await key(phrase, user.uid);
    final db = FirebaseFirestore.instance;
    final marker = db.doc('users/${user.uid}/meta/vault');
    final check = await marker.get(const GetOptions(source: Source.server));
    if (check.exists) {
      if (await unseal(check.data()!['ciphertext'] as String, secret) != 'LifeMate-v1') throw StateError('Incorrect recovery phrase');
    } else {
      final sealed = await seal('LifeMate-v1', secret);
      await db.runTransaction((tx) async {
        if ((await tx.get(marker)).exists) throw StateError('Vault changed. Retry sync.');
        tx.set(marker, {'ciphertext': sealed});
      });
    }
    final collection = db.collection('users/${user.uid}/vault');
    final remote = await collection.get(const GetOptions(source: Source.server));
    final local = {for (final e in store.all.where((e) => e.syncable)) e.id: e};
    for (final doc in remote.docs) {
      final entry = Entry.fromJson(jsonDecode(await unseal(doc.data()['ciphertext'] as String, secret)) as Map<String,dynamic>);
      final revision = doc.data()['revision'] as int;
      if(entry.id != doc.id || entry.revision != revision || !entry.syncable) throw const FormatException('Invalid encrypted record binding');
      final existing = local[entry.id];
      if (existing != null && (revision < existing.revision || existing.dirty && existing.revision != revision)) {
        throw StateError(store.t('Sync conflict. Both copies are safe; export before resolving on the other device.', 'সিঙ্কে দ্বন্দ্ব। দুই কপিই নিরাপদ আছে; অন্য ডিভাইসে সমাধানের আগে ব্যাকআপ রাখো।'));
      }
      if (existing == null || !existing.dirty) await store.acceptCloud(entry.copy(revision: revision, dirty: false));
    }
    for (final entry in List<Entry>.from(store.pending)) {
      final next = entry.copy(revision: entry.revision + 1, dirty: false);
      final cipher = await seal(jsonEncode(next.toJson()), secret);
      final ref = collection.doc(entry.id);
      await db.runTransaction((tx) async {
        final old = await tx.get(ref);
        final revision = old.exists ? old.data()!['revision'] as int : 0;
        if (revision != entry.revision) throw StateError('Sync conflict: local edits were retained');
        tx.set(ref, {'ciphertext': cipher, 'revision': next.revision, 'updatedAt': FieldValue.serverTimestamp()});
      });
      // Do not overwrite edits made while the network transaction was in flight.
      final current = store.all.where((e) => e.id == entry.id).firstOrNull;
      if (identical(current, entry)) { await store.acceptCloud(next); }
      else if (current != null) { await store.acceptCloud(current.copy(revision: next.revision, dirty: true)); }
    }
  }

  /// Optional encrypted media upload, never called by record sync or startup.
  static Future<void> uploadEncryptedPhoto(String id, List<int> bytes, SecretKey key) async {
    final user = FirebaseAuth.instance.currentUser;
    if (user == null) throw StateError('Sign in first');
    final encrypted = (await AesGcm.with256bits().encrypt(bytes, secretKey: key)).concatenation();
    await FirebaseStorage.instance.ref('users/${user.uid}/encrypted/$id').putData(encrypted, SettableMetadata(contentType: 'application/octet-stream'));
  }
  static Future<String> sathi(String text, String language) async {
    await session();
    final result = await FirebaseFunctions.instance.httpsCallable('sathi', options: HttpsCallableOptions(timeout: const Duration(seconds: 30))).call({'message': text, 'language': language});
    final answer = (result.data as Map)['reply'];
    if (answer is! String || answer.isEmpty || answer.length > 6000) throw const FormatException('Invalid response');
    return answer;
  }
}
