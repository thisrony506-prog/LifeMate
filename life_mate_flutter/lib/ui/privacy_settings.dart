import 'package:flutter/material.dart';
import 'package:local_auth/local_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'common.dart';
import '../services/device_auth.dart';
import 'entries.dart';

class PrivacySettings extends StatefulWidget {
  const PrivacySettings({super.key});
  @override
  State<PrivacySettings> createState() => _PrivacySettingsState();
}

class _PrivacySettingsState extends State<PrivacySettings> {
  bool busy = false;
  @override
  Widget build(BuildContext context) {
    final s = LifeScope.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(s.t('Profile & privacy', 'প্রোফাইল ও গোপনীয়তা')),
      ),
      body: PageBody(
        children: [
          Heading(
            s.name,
            subtitle: s.t(
              'This is your new Personal Life OS.',
              'এটি তোমার নতুন ব্যক্তিগত জীবন গোছানোর অ্যাপ।',
            ),
          ),
          if (!s.demo && s.originalProfile.isNotEmpty) ...[
            if ((s.originalProfile['photo'] as String? ?? '').isNotEmpty)
              EntryPhoto(s.originalProfile['photo'] as String),
            for (final field in <String, String>{
              'fullName': s.t('Full name', 'পুরো নাম'),
              'nickname': s.t('Nickname', 'ডাকনাম'),
              'preferredName': s.t('Preferred name', 'পছন্দের নাম'),
              'birthday': s.t('Birthday', 'জন্মদিন'),
              'introduction': s.t('Introduction', 'পরিচয়'),
              'information': s.t('Personal information', 'ব্যক্তিগত তথ্য'),
            }.entries)
              if ((s.originalProfile[field.key] as String? ?? '').isNotEmpty)
                ListTile(title: Text(field.value), subtitle: Text(s.originalProfile[field.key] as String)),
          ],
          if (s.existingPin) Text(s.t('Your existing app PIN is still required.', 'আগের অ্যাপ পিন এখনও প্রয়োজন।')),
          OutlinedButton.icon(
            icon: const Icon(Icons.edit_outlined),
            label: Text(s.t('Edit your name', 'নাম সম্পাদনা')),
            onPressed: busy || s.demo
                ? null
                : () async {
                    final input = TextEditingController(text: s.name);
                    final value = await showDialog<String>(
                      context: context,
                      builder: (context) => AlertDialog(
                        title: Text(s.t('Your name', 'তোমার নাম')),
                        content: TextField(
                          controller: input,
                          maxLength: 80,
                          decoration: InputDecoration(
                            labelText: s.t('Name', 'নাম'),
                          ),
                        ),
                        actions: [
                          TextButton(
                            onPressed: () => Navigator.pop(context),
                            child: Text(s.t('Cancel', 'বাতিল')),
                          ),
                          FilledButton(
                            onPressed: () => Navigator.pop(context, input.text),
                            child: Text(s.t('Save', 'সংরক্ষণ')),
                          ),
                        ],
                      ),
                    );
                    // Let the closing dialog detach before releasing its controller.
                    await Future<void>.delayed(
                      const Duration(milliseconds: 300),
                    );
                    input.dispose();
                    if (value != null && mounted)
                      await attempt(context, () => s.setName(value));
                  },
          ),
          SwitchListTile(
            title: Text(s.t('App lock', 'অ্যাপ লক')),
            subtitle: Text(
              s.t(
                s.existingPin ? 'Your existing app PIN remains enabled. It is not replaced by this switch.' : 'Device PIN/passcode or biometrics. Locks when you leave the app.',
                s.existingPin ? 'আগের অ্যাপ পিন চালু আছে। এই সুইচ সেটি বদলায় না।' : 'ডিভাইসের পিন/পাসকোড বা বায়োমেট্রিক। অ্যাপ ছাড়লে লক হবে।',
              ),
            ),
            value: s.appLock || s.existingPin,
            onChanged: busy || s.demo || s.existingPin
                ? null
                : (value) async {
                    setState(() => busy = true);
                    try {
                      final ok = await DeviceAuthentication.authenticate(
                        localizedReason: s.t(
                          'Confirm a change to Life Mate app lock',
                          'Life Mate অ্যাপ লক পরিবর্তন নিশ্চিত করো',
                        ),
                        options: const AuthenticationOptions(stickyAuth: true),
                      );
                      if (ok) await s.setAppLock(value);
                    } catch (_) {
                      if (context.mounted)
                        message(
                          context,
                          s.t(
                            'Set a device screen lock first. App lock was not changed.',
                            'আগে ডিভাইসে স্ক্রিন লক দাও। অ্যাপ লক বদলায়নি।',
                          ),
                        );
                    } finally {
                      if (mounted) setState(() => busy = false);
                    }
                  },
          ),
          Text(
            s.t(
              'Secret journals always require device authentication, even when the app-wide lock is off. Demo journals contain only samples.',
              'অ্যাপ লক বন্ধ থাকলেও গোপন ডায়েরিতে ডিভাইস যাচাই প্রয়োজন। ডেমো ডায়েরিতে শুধু নমুনা থাকে।',
            ),
          ),
          Heading(s.t('Your data, your choice', 'তোমার তথ্য, তোমার পছন্দ')),
          Text(
            s.t(
              'New records and photos are encrypted on this device. The update does not reset original storage or keys. Your original profile is retained; other old records remain in their original storage, not yet converted into the new modules.',
              'নতুন তথ্য ও ছবি এই ডিভাইসে এনক্রিপ্ট করা। আপডেট পুরোনো স্টোরেজ বা চাবি reset করে না। আগের প্রোফাইল রাখা হয়; অন্য পুরোনো রেকর্ড আগের স্টোরেজে আছে, নতুন মডিউলে এখনও রূপান্তর করা হয়নি।',
            ),
          ),
          Text(
            s.t(
              'Firebase/Gemini require owner configuration. Sync is manual and encrypts records before upload. AI receives only messages you approve. SOS opens system SMS/dialer; it does not silently send or call. Live location is optional, foreground-only and expires.',
              'Firebase/Gemini-তে মালিকের কনফিগারেশন লাগে। সিঙ্ক নিজে করতে হয়, আপলোডের আগে তথ্য এনক্রিপ্ট হয়। AI শুধু অনুমোদিত বার্তা পায়। SOS সিস্টেমের SMS/ডায়ালার খোলে; চুপিসারে পাঠায় বা কল করে না। লাইভ লোকেশন ঐচ্ছিক, সামনে থাকা অ্যাপেই চলে ও মেয়াদ শেষ হয়।',
            ),
          ),
          Text(
            s.t(
              'Notification text is generic, not your journal or medicine name. Android battery policies and inexact scheduling can delay reminders. This is not a medical alarm system.',
              'নোটিফিকেশনে ডায়েরি বা ওষুধের নাম নয়, সাধারণ লেখা থাকে। Android ব্যাটারি নীতি ও আনুমানিক সময়সূচিতে রিমাইন্ডার দেরি হতে পারে। এটি চিকিৎসার জরুরি অ্যালার্ম নয়।',
            ),
          ),
          const Divider(),
          OutlinedButton.icon(
            icon: const Icon(Icons.delete_outline),
            label: Text(
              s.t('Delete new local data', 'নতুন স্থানীয় তথ্য মুছুন'),
            ),
            onPressed: busy || s.demo
                ? null
                : () async {
                    if (!await confirm(
                      context,
                      s.t('Delete your local vault?', 'স্থানীয় ভল্ট মুছবে?'),
                      s.t(
                        'All new records and private photos will be removed and reminders cancelled. Export a backup first if needed. Original app storage, cloud copies, exported backups and your gallery are not deleted.',
                        'নতুন সব তথ্য ও ব্যক্তিগত ছবি মুছবে, রিমাইন্ডার বন্ধ হবে। প্রয়োজন হলে আগে ব্যাকআপ করো। পুরোনো অ্যাপের স্টোরেজ, ক্লাউডের কপি, রপ্তানি করা ব্যাকআপ ও গ্যালারি মুছবে না।',
                      ),
                    ))
                      return;
                    if (!mounted) return;
                    setState(() => busy = true);
                    try {
                      if (Firebase.apps.isNotEmpty)
                        await FirebaseAuth.instance.signOut();
                      await s.erase();
                      if (context.mounted)
                        Navigator.of(
                          context,
                        ).popUntil((route) => route.isFirst);
                    } catch (_) {
                      if (context.mounted)
                        message(
                          context,
                          s.t(
                            'Deletion did not finish. Retry before continuing.',
                            'মুছা শেষ হয়নি। আবার চেষ্টা করো।',
                          ),
                        );
                    } finally {
                      if (mounted) setState(() => busy = false);
                    }
                  },
          ),
          if (busy) const LinearProgressIndicator(),
        ],
      ),
    );
  }
}
