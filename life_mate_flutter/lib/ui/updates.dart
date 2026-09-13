import 'package:flutter/material.dart';
import '../services/native.dart';
import '../services/updates.dart';
import 'backup_page.dart';
import 'common.dart';

/// Outside the app Navigator: old routes/dialogs cannot cover a required update.
class UpdateGate extends StatefulWidget {
  final Widget child;
  const UpdateGate({super.key, required this.child});
  @override
  State<UpdateGate> createState() => _UpdateGateState();
}

class _UpdateGateState extends State<UpdateGate> with WidgetsBindingObserver {
  bool backup = false;
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    if (NativeBridge.android) {
      appUpdates.load().then((_) => appUpdates.check());
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) appUpdates.check();
  }

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    animation: appUpdates,
    builder: (_, __) {
      if (!NativeBridge.android) return widget.child;
      if (!appUpdates.ready)
        return Scaffold(
          body: SafeArea(
            child: Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.security_outlined),
                  const SizedBox(height: 16),
                  const Text(
                    'Checking update safety…\nআপডেটের নিরাপত্তা পরীক্ষা হচ্ছে…',
                  ),
                  TextButton(
                    onPressed: appUpdates.load,
                    child: const Text('Retry · আবার চেষ্টা'),
                  ),
                ],
              ),
            ),
          ),
        );
      if (!appUpdates.required) return widget.child;
      return PopScope(
        canPop: false,
        child: backup
            ? Navigator(
                onGenerateRoute: (_) => MaterialPageRoute<void>(
                  builder: (_) => Scaffold(
                    appBar: AppBar(
                      automaticallyImplyLeading: false,
                      title: const Text('Life Mate'),
                      actions: [
                        TextButton(
                          onPressed: () => setState(() => backup = false),
                          child: Text(
                            LifeScope.of(
                              context,
                            ).t('Back to update', 'আপডেটে ফিরে যাও'),
                          ),
                        ),
                      ],
                    ),
                    body: const BackupPage(exportOnly: true),
                  ),
                ),
              )
            : UpdatePage(
                requiredGate: true,
                onBackup: () {
                  LifeScope.of(context).setDemo(false);
                  setState(() => backup = true);
                },
              ),
      );
    },
  );
}

class UpdatePage extends StatelessWidget {
  final bool requiredGate;
  final VoidCallback? onBackup;
  const UpdatePage({super.key, this.requiredGate = false, this.onBackup});
  @override
  Widget build(BuildContext context) {
    final s = LifeScope.of(context);
    return AnimatedBuilder(
      animation: appUpdates,
      builder: (_, __) {
        final u = appUpdates;
        final status = switch (u.status) {
          'current' => s.t(
            'No newer verified release was found.',
            'নতুন কোনো যাচাই করা সংস্করণ পাওয়া যায়নি।',
          ),
          'error' || 'startup' => s.t(
            'Update check failed. Try again when connected. Any known update is still required.',
            'আপডেট পরীক্ষা হয়নি। সংযোগ পেলে আবার চেষ্টা করো। জানা আবশ্যক আপডেট এখনও প্রযোজ্য।',
          ),
          'ready' => s.t(
            'Verified download ready to install.',
            'যাচাই করা ডাউনলোড ইনস্টলের জন্য প্রস্তুত।',
          ),
          'permission' => s.t(
            'Allow Life Mate to install updates in Android Settings, then tap Install again.',
            'Android সেটিংসে Life Mate-কে ইনস্টলের অনুমতি দিয়ে আবার ইনস্টল চাপো।',
          ),
          'installer' => s.t(
            'Confirm installation in Android. Cancelling does not skip this update.',
            'Android-এ ইনস্টল নিশ্চিত করো। বাতিল করলে আপডেট এড়ানো যাবে না।',
          ),
          'downloadError' || 'installError' => s.t(
            'Download or installation could not finish. Retry; unverified files are never installed.',
            'ডাউনলোড বা ইনস্টল শেষ হয়নি। আবার চেষ্টা করো; যাচাই ছাড়া ফাইল ইনস্টল হয় না।',
          ),
          _ => '',
        };
        return Scaffold(
          appBar: AppBar(
            automaticallyImplyLeading: !requiredGate,
            title: Text(s.t('Life Mate updates', 'Life Mate আপডেট')),
          ),
          body: SafeArea(
            child: PageBody(
              children: [
                const Icon(Icons.system_update_outlined, size: 48),
                Heading(
                  u.required
                      ? s.t('An update is required', 'আপডেট প্রয়োজন')
                      : s.t(
                          'Keep Life Mate up to date',
                          'Life Mate আপডেট রাখো',
                        ),
                ),
                Text('${s.t('Installed', 'ইনস্টল করা')}: ${u.installed}'),
                if (u.required)
                  Text('${s.t('New version', 'নতুন সংস্করণ')}: ${u.version}'),
                Text(
                  s.t(
                    'Private download, verified official signing key and Android installer. Your confirmation is required.',
                    'ব্যক্তিগত ডাউনলোড, অফিসিয়াল স্বাক্ষর যাচাই ও Android ইনস্টলার। তোমার নিশ্চিতকরণ প্রয়োজন।',
                  ),
                ),
                if (u.busy) ...[
                  LinearProgressIndicator(
                    value: u.total > 0 && u.phase == 'DOWNLOADING'
                        ? u.received / u.total
                        : null,
                  ),
                  Text(
                    '${(u.received / 1048576).toStringAsFixed(1)} / ${(u.total / 1048576).toStringAsFixed(1)} MB',
                  ),
                ],
                if (status.isNotEmpty) Text(status),
                if (u.required)
                  FilledButton(
                    onPressed: u.busy
                        ? null
                        : u.downloaded
                        ? u.install
                        : u.download,
                    child: Text(
                      u.downloaded
                          ? s.t('Install update', 'আপডেট ইনস্টল')
                          : s.t(
                              'Download verified update',
                              'যাচাই করা আপডেট ডাউনলোড',
                            ),
                    ),
                  ),
                OutlinedButton(
                  onPressed: u.checking || u.busy
                      ? null
                      : () => u.check(manual: true),
                  child: Text(s.t('Check again', 'আবার পরীক্ষা')),
                ),
                if (onBackup != null)
                  TextButton(
                    onPressed: u.busy ? null : onBackup,
                    child: Text(
                      s.t(
                        'Export encrypted backup',
                        'এনক্রিপ্ট করা ব্যাকআপ রপ্তানি',
                      ),
                    ),
                  ),
                if (requiredGate)
                  TextButton(
                    onPressed: () => NativeBridge.channel.invokeMethod('close'),
                    child: Text(s.t('Close app', 'অ্যাপ বন্ধ')),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }
}
