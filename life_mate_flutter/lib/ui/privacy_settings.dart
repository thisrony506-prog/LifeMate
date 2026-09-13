import 'package:flutter/material.dart';
import 'package:local_auth/local_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'common.dart';

class PrivacySettings extends StatefulWidget {
  const PrivacySettings({super.key});
  @override State<PrivacySettings> createState() => _PrivacySettingsState();
}
class _PrivacySettingsState extends State<PrivacySettings> {
  bool busy = false;
  @override Widget build(BuildContext context) {
    final s = LifeScope.of(context);
    return Scaffold(appBar: AppBar(title: Text(s.t('Profile & privacy', 'প্রোফাইল ও গোপনীয়তা'))), body: PageBody(children: [
      Heading(s.name, subtitle: s.t('This is your new Personal Life OS.', 'এটি তোমার নতুন ব্যক্তিগত জীবন গোছানোর অ্যাপ।')),
      OutlinedButton.icon(icon: const Icon(Icons.edit_outlined), label: Text(s.t('Edit your name', 'নাম সম্পাদনা')), onPressed: busy || s.demo ? null : () async {
        final input = TextEditingController(text: s.name);
        final value = await showDialog<String>(context: context, builder: (context) => AlertDialog(title: Text(s.t('Your name', 'তোমার নাম')), content: TextField(controller: input, maxLength: 80, decoration: InputDecoration(labelText: s.t('Name', 'নাম'))), actions: [TextButton(onPressed: () => Navigator.pop(context), child: Text(s.t('Cancel', 'বাতিল'))), FilledButton(onPressed: () => Navigator.pop(context, input.text), child: Text(s.t('Save', 'সংরক্ষণ')))]));
        // Let the closing dialog detach before releasing its controller.
        await Future<void>.delayed(const Duration(milliseconds: 300)); input.dispose();
        if (value != null && mounted) await attempt(context, () => s.setName(value));
      }),
      SwitchListTile(title: Text(s.t('App lock', 'অ্যাপ লক')), subtitle: Text(s.t('Device PIN/passcode or biometrics. Locks when you leave the app.', 'ডিভাইসের পিন/পাসকোড বা বায়োমেট্রিক। অ্যাপ ছাড়লে লক হবে।')), value: s.appLock, onChanged: busy || s.demo ? null : (value) async {
        setState(() => busy = true);
        try {
          final ok = await LocalAuthentication().authenticate(localizedReason: s.t('Confirm a change to Life Mate app lock', 'Life Mate অ্যাপ লক পরিবর্তন নিশ্চিত করো'), options: const AuthenticationOptions(stickyAuth: true));
          if (ok) await s.setAppLock(value);
        } catch (_) { if (context.mounted) message(context, s.t('Set a device screen lock first. App lock was not changed.', 'আগে ডিভাইসে স্ক্রিন লক দাও। অ্যাপ লক বদলায়নি।')); }
        finally { if (mounted) setState(() => busy = false); }
      }),
      Text(s.t('Secret journals always require device authentication, even when the app-wide lock is off. Demo journals contain only samples.', 'অ্যাপ লক বন্ধ থাকলেও গোপন ডায়েরিতে ডিভাইস যাচাই প্রয়োজন। ডেমো ডায়েরিতে শুধু নমুনা থাকে।')),
      Heading(s.t('Your data, your choice', 'তোমার তথ্য, তোমার পছন্দ')),
      Text(s.t('Records and photos are encrypted on this device. There are no old organizer screens, posts, card studio or profile imports. This clean replacement removes the old app’s private local data once. Future launches keep your new data.', 'তথ্য ও ছবি এই ডিভাইসে এনক্রিপ্ট করা। পুরোনো অর্গানাইজার, পোস্ট, কার্ড স্টুডিও বা প্রোফাইল আমদানি নেই। এই নতুন সংস্করণ পুরোনো অ্যাপের নিজস্ব স্থানীয় তথ্য একবার মুছে শুরু করে। পরেরবার তোমার নতুন তথ্য থাকবে।')),
      Text(s.t('Firebase/Gemini require owner configuration. Sync is manual and encrypts records before upload. AI receives only messages you approve. SOS opens system SMS/dialer; it does not silently send or call. Live location is optional, foreground-only and expires.', 'Firebase/Gemini-তে মালিকের কনফিগারেশন লাগে। সিঙ্ক নিজে করতে হয়, আপলোডের আগে তথ্য এনক্রিপ্ট হয়। AI শুধু অনুমোদিত বার্তা পায়। SOS সিস্টেমের SMS/ডায়ালার খোলে; চুপিসারে পাঠায় বা কল করে না। লাইভ লোকেশন ঐচ্ছিক, সামনে থাকা অ্যাপেই চলে ও মেয়াদ শেষ হয়।')),
      Text(s.t('Notification text is generic, not your journal or medicine name. Android battery policies and inexact scheduling can delay reminders. This is not a medical alarm system.', 'নোটিফিকেশনে ডায়েরি বা ওষুধের নাম নয়, সাধারণ লেখা থাকে। Android ব্যাটারি নীতি ও আনুমানিক সময়সূচিতে রিমাইন্ডার দেরি হতে পারে। এটি চিকিৎসার জরুরি অ্যালার্ম নয়।')),
      const Divider(),
      OutlinedButton.icon(icon: const Icon(Icons.delete_outline), label: Text(s.t('Delete new local data', 'নতুন স্থানীয় তথ্য মুছুন')), onPressed: busy || s.demo ? null : () async {
        if (!await confirm(context, s.t('Delete your local vault?', 'স্থানীয় ভল্ট মুছবে?'), s.t('All new records and private photos will be removed and reminders cancelled. Export a backup first if needed. Cloud copies, exported backups and your gallery are not deleted.', 'নতুন সব তথ্য ও ব্যক্তিগত ছবি মুছবে, রিমাইন্ডার বন্ধ হবে। প্রয়োজন হলে আগে ব্যাকআপ করো। ক্লাউডের কপি, রপ্তানি করা ব্যাকআপ ও গ্যালারি মুছবে না।'))) return;
        if (!mounted) return; setState(() => busy = true);
        try {
          if (Firebase.apps.isNotEmpty) await FirebaseAuth.instance.signOut();
          await s.erase();
          if (context.mounted) Navigator.of(context).popUntil((route) => route.isFirst);
        } catch (_) { if (context.mounted) message(context, s.t('Deletion did not finish. Retry before continuing.', 'মুছা শেষ হয়নি। আবার চেষ্টা করো।')); }
        finally { if (mounted) setState(() => busy = false); }
      }),
      if (busy) const LinearProgressIndicator(),
    ]));
  }
}
