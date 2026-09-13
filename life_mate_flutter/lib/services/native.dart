import 'dart:io';
import 'package:flutter/services.dart';

/// Android-only OS integration. There are no legacy routes or personal-data APIs.
class NativeBridge {
  static const channel = MethodChannel('com.lifemate/personal_os');
  static bool get android => Platform.isAndroid;
}
