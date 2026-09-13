import 'package:flutter/material.dart';
import 'package:local_auth/local_auth.dart';
import 'common.dart';
import '../services/device_auth.dart';

/// Device PIN/passcode or biometrics on Android AND iOS. No legacy PIN store.
class DeviceLock extends StatefulWidget {
  final Widget child;
  final bool enabled, allowBack;
  const DeviceLock({
    super.key,
    required this.child,
    this.enabled = true,
    this.allowBack = true,
  });
  @override
  State<DeviceLock> createState() => _DeviceLockState();
}

class _DeviceLockState extends State<DeviceLock> with WidgetsBindingObserver {
  bool locked = true, authenticating = false;
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    if (widget.enabled)
      WidgetsBinding.instance.addPostFrameCallback((_) => unlock());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (widget.enabled &&
        state != AppLifecycleState.resumed &&
        !authenticating &&
        mounted)
      setState(() => locked = true);
  }

  Future<void> unlock() async {
    if (authenticating || !mounted) return;
    setState(() => authenticating = true);
    try {
      final s = LifeScope.of(context);
      final ok = await DeviceAuthentication.authenticate(
        localizedReason: s.t(
          'Unlock your private Life Mate space',
          'Life Mate-এর ব্যক্তিগত জায়গা খোলো',
        ),
        options: const AuthenticationOptions(stickyAuth: true),
      );
      if (mounted) setState(() => locked = !ok);
    } catch (_) {
      if (mounted) setState(() => locked = true);
    } finally {
      if (mounted) setState(() => authenticating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = LifeScope.of(context);
    final hidden = widget.enabled && locked;
    return Stack(
      children: [
        ExcludeFocus(
          excluding: hidden,
          child: Offstage(offstage: hidden, child: widget.child),
        ),
        if (hidden)
          Positioned.fill(
            child: Scaffold(
              body: SafeArea(
                child: Center(
                  child: Padding(
                    padding: const EdgeInsets.all(28),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.lock_outline, size: 48),
                        const SizedBox(height: 20),
                        Text(
                          s.t(
                            'Your space, kept safe.',
                            'তোমার জায়গা, সুরক্ষিত।',
                          ),
                          style: Theme.of(context).textTheme.headlineSmall,
                        ),
                        const SizedBox(height: 12),
                        Text(
                          s.t(
                            'Use your device PIN, passcode or biometrics. Configure a device screen lock first.',
                            'ডিভাইসের পিন, পাসকোড বা বায়োমেট্রিক ব্যবহার করো। আগে ডিভাইসে স্ক্রিন লক চালু করো।',
                          ),
                        ),
                        const SizedBox(height: 20),
                        FilledButton(
                          onPressed: authenticating ? null : unlock,
                          child: Text(s.t('Unlock', 'আনলক')),
                        ),
                        if (widget.allowBack)
                          TextButton(
                            onPressed: () => Navigator.maybePop(context),
                            child: Text(s.t('Back', 'ফিরে যাও')),
                          ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }
}
