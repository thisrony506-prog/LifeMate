import 'dart:io';
import 'package:flutter/services.dart';

class NativeBridge {
  static bool demo = false;
  static const channel = MethodChannel('com.lifemate/personal_os');
  static bool get android => Platform.isAndroid;
  static Future<Map<String, dynamic>> snapshot() async {
    if (!android) return {};
    return Map<String, dynamic>.from(
      await channel.invokeMethod<Map>('snapshot') ?? {},
    );
  }

  static Future<void> open(String route) async {
    if (demo) throw StateError("Exit Demo mode first");
    if (!android)
      throw UnsupportedError(
        'These retained Android tools are not available on iOS yet.',
      );
    await channel.invokeMethod('openLegacy', {'route': route});
  }

  static Future<void> profile(String name) async {
    if (android) await channel.invokeMethod('profile', {'name': name});
  }

  static Future<void> theme(String value) async {
    if (android) await channel.invokeMethod('theme', {'value': value});
  }

  static Future<void> schedule(Map<String, dynamic> value) async {
    if (android) await channel.invokeMethod('schedule', value);
  }

  static Future<void> cancel(String id) async {
    if (android) await channel.invokeMethod('cancel', {'id': id});
  }
}
