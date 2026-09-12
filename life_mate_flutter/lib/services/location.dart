import 'dart:async';
import 'dart:convert';
import 'package:geolocator/geolocator.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:cryptography/cryptography.dart';
import 'package:uuid/uuid.dart';
import 'cloud.dart';

class FamilyShare {
  Timer? _timer;
  String? id;
  SecretKey? secret;
  DateTime? expires;
  String? error;
  bool get active => id != null;
  static Future<Position> position() async {
    if (!await Geolocator.isLocationServiceEnabled()) throw StateError('Location is off');
    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) permission = await Geolocator.requestPermission();
    if (permission == LocationPermission.denied || permission == LocationPermission.deniedForever) throw StateError('Location permission is off');
    return Geolocator.getCurrentPosition(locationSettings: const LocationSettings(accuracy: LocationAccuracy.high, timeLimit: Duration(seconds: 15)));
  }
  Future<String> start() async {
    await stop();
    final user = await CloudService.session();
    secret = await AesGcm.with256bits().newSecretKey();
    id = const Uuid().v4(); expires = DateTime.now().add(const Duration(minutes: 15)); error = null;
    try {
      await _send(user.uid);
      _timer = Timer.periodic(const Duration(seconds: 30), (_) async {
        if (expires == null || DateTime.now().isAfter(expires!)) { await stop(); return; }
        try { await _send(user.uid); } catch (_) { error = 'Location update failed'; }
      });
      return '$id.${base64UrlEncode(await secret!.extractBytes())}';
    } catch (_) { await stop(); rethrow; }
  }
  Future<void> _send(String uid) async {
    final share = id, key = secret, until = expires;
    if (share == null || key == null || until == null) return;
    final point = await position();
    if (id != share) return;
    final cipher = await CloudService.seal(jsonEncode({'latitude': point.latitude, 'longitude': point.longitude, 'accuracy': point.accuracy, 'at': DateTime.now().toIso8601String()}), key);
    await FirebaseFirestore.instance.doc('live/$share').set({'owner': uid, 'ciphertext': cipher, 'expiresAt': Timestamp.fromDate(until), 'updatedAt': FieldValue.serverTimestamp()});
  }
  Future<void> stop() async {
    _timer?.cancel(); _timer = null;
    final old = id; id = null; secret = null; expires = null;
    if (old != null) {
      try { await FirebaseFirestore.instance.doc('live/$old').delete().timeout(const Duration(seconds: 5)); }
      catch (_) { error = 'Could not revoke remotely. The invitation still expires after 15 minutes.'; }
    }
  }
  static Stream<Map<String,dynamic>?> watch(String invitation) async* {
    final parts = invitation.trim().split('.');
    if (parts.length != 2 || !RegExp(r'^[a-f0-9-]{36}$').hasMatch(parts[0])) throw const FormatException('Invalid invitation');
    final keyBytes = base64Url.decode(parts[1]);
    if (keyBytes.length != 32) throw const FormatException('Invalid invitation');
    await CloudService.session();
    await for (final snapshot in FirebaseFirestore.instance.doc('live/${parts[0]}').snapshots()) {
      final data = snapshot.data();
      if (data == null || (data['expiresAt'] as Timestamp).toDate().isBefore(DateTime.now())) { yield null; continue; }
      yield jsonDecode(await CloudService.unseal(data['ciphertext'] as String, SecretKey(keyBytes))) as Map<String,dynamic>;
    }
  }
}
