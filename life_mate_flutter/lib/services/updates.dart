import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'native.dart';

final appUpdates = AppUpdates();

class AppUpdates extends ChangeNotifier {
  bool ready = !NativeBridge.android,
      required = false,
      checking = false,
      busy = false,
      downloaded = false;
  String installed = '', version = '', phase = '', status = '';
  int received = 0, total = 0;
  void accept(Map<dynamic, dynamic> data) {
    required = data['required'] == true;
    installed = data['installed']?.toString() ?? '';
    version = data['version']?.toString() ?? '';
    total = (data['bytes'] as num?)?.toInt() ?? 0;
    ready = true;
    notifyListeners();
  }

  Future<void> load() async {
    if (!NativeBridge.android) return;
    try {
      accept(await NativeBridge.channel.invokeMapMethod('updates.state') ?? {});
    } catch (_) {
      status = 'startup';
      notifyListeners();
    } // Fail closed if native gate state cannot be read.
  }

  Future<void> check({bool manual = false}) async {
    if (!NativeBridge.android || checking) return;
    checking = true;
    notifyListeners();
    try {
      accept(
        await NativeBridge.channel.invokeMapMethod('updates.check', {
              'manual': manual,
            }) ??
            {},
      );
      status = required ? 'required' : 'current';
    } on PlatformException catch (e) {
      if (e.details is Map) accept(e.details as Map);
      status = 'error';
    } catch (_) {
      status = 'error';
    } finally {
      checking = false;
      notifyListeners();
    }
  }

  void progress(Map<dynamic, dynamic> data) {
    phase = data['phase']?.toString() ?? '';
    received = (data['received'] as num?)?.toInt() ?? 0;
    total = (data['total'] as num?)?.toInt() ?? total;
    notifyListeners();
  }

  Future<void> download() async {
    if (busy || !required) return;
    busy = true;
    downloaded = false;
    status = '';
    notifyListeners();
    try {
      await NativeBridge.channel.invokeMethod('updates.download');
      downloaded = true;
      status = 'ready';
    } catch (_) {
      status = 'downloadError';
    } finally {
      busy = false;
      notifyListeners();
    }
  }

  Future<void> install() async {
    if (busy || !required) return;
    busy = true;
    notifyListeners();
    try {
      status =
          await NativeBridge.channel.invokeMethod<String>('updates.install') ??
          'installer';
    } catch (_) {
      downloaded = false;
      status = 'installError';
    } finally {
      busy = false;
      notifyListeners();
    }
  }
}
