import 'dart:io';
import 'package:flutter/services.dart';

/// Android-only OS integration. The compatibility reader exposes only the existing account, never old feature routes.
class NativeBridge {
  static const channel = MethodChannel('com.lifemate/personal_os');
  static bool get android => Platform.isAndroid;
}
