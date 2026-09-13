import 'package:flutter/material.dart';
import '../services/native.dart';
import '../services/device_auth.dart';
import 'common.dart';

/// The previous app PIN remains in its original encrypted store and keeps its
/// retry cooldown. Never expose the new UI/backup before the original PIN passes.
class ExistingPinGate extends StatefulWidget {
  final bool enabled;
  final Widget child;
  const ExistingPinGate({
    super.key,
    required this.enabled,
    required this.child,
  });
  @override
  State<ExistingPinGate> createState() => _ExistingPinGateState();
}

class _ExistingPinGateState extends State<ExistingPinGate>
    with WidgetsBindingObserver {
  final input = TextEditingController();
  bool locked = true, busy = false, failed = false;
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    DeviceAuthentication.prompts.addListener(afterAuthentication);
  }

  @override
  void dispose() {
    DeviceAuthentication.prompts.removeListener(afterAuthentication);
    WidgetsBinding.instance.removeObserver(this);
    input.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state != AppLifecycleState.resumed &&
        DeviceAuthentication.prompts.value == 0 &&
        mounted &&
        widget.enabled) {
      input.clear();
      setState(() => locked = true);
    }
  }

  void afterAuthentication() {
    if (DeviceAuthentication.prompts.value == 0 &&
        WidgetsBinding.instance.lifecycleState != AppLifecycleState.resumed) {
      didChangeAppLifecycleState(AppLifecycleState.paused);
    }
  }

  Future<void> unlock() async {
    if (busy) return;
    setState(() {
      busy = true;
      failed = false;
    });
    try {
      final ok = await NativeBridge.channel.invokeMethod<bool>(
        'existing.verifyPin',
        {'pin': input.text},
      );
      input.clear();
      if (mounted)
        setState(() {
          locked =
              ok != true ||
              WidgetsBinding.instance.lifecycleState !=
                  AppLifecycleState.resumed;
          failed = ok != true;
        });
    } catch (_) {
      if (mounted) setState(() => failed = true);
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.enabled) return widget.child;
    if (!locked)
      return Stack(
        children: [
          ExcludeFocus(
            excluding: false,
            child: Offstage(offstage: false, child: widget.child),
          ),
        ],
      );
    final s = LifeScope.of(context);
    final lockScreen = Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(28),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.lock_outline, size: 48),
                const SizedBox(height: 20),
                Text(
                  s.t('Welcome back to Life Mate', 'Life Mate-এ আবার স্বাগতম'),
                  style: Theme.of(context).textTheme.headlineSmall,
                ),
                Text(
                  s.t(
                    'Use your existing app PIN. Your account and original data have not been reset.',
                    'আগের অ্যাপ পিন ব্যবহার করো। তোমার অ্যাকাউন্ট ও পুরোনো তথ্য reset করা হয়নি।',
                  ),
                ),
                TextField(
                  controller: input,
                  obscureText: true,
                  enableSuggestions: false,
                  autocorrect: false,
                  keyboardType: TextInputType.number,
                  maxLength: 12,
                  decoration: InputDecoration(
                    labelText: s.t('Existing app PIN', 'আগের অ্যাপ পিন'),
                  ),
                  onSubmitted: (_) => unlock(),
                ),
                if (failed)
                  Text(
                    s.t(
                      'Could not unlock. Check your PIN; after repeated attempts wait one minute.',
                      'আনলক হয়নি। পিন দেখো; বারবার ভুল হলে এক মিনিট অপেক্ষা করো।',
                    ),
                  ),
                FilledButton(
                  onPressed: busy ? null : unlock,
                  child: Text(s.t('Unlock', 'আনলক')),
                ),
              ],
            ),
          ),
        ),
      ),
    );
    return Stack(
      children: [
        ExcludeFocus(
          excluding: true,
          child: Offstage(offstage: true, child: widget.child),
        ),
        Positioned.fill(child: lockScreen),
      ],
    );
  }
}
