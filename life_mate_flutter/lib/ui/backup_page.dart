import 'dart:io';
import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
import '../services/backup.dart';
import 'common.dart';

class BackupPage extends StatefulWidget {
  final bool exportOnly;
  const BackupPage({super.key, this.exportOnly = false});
  @override
  State<BackupPage> createState() => _BackupState();
}

class _BackupState extends State<BackupPage> {
  final phrase = TextEditingController();
  bool busy = false;
  @override
  void dispose() {
    phrase.clear();
    phrase.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = LifeScope.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(
          s.t('Encrypted vault backup', 'এনক্রিপ্ট করা ভল্ট ব্যাকআপ'),
        ),
      ),
      body: PageBody(
        children: [
          Text(
            s.t(
              'Back up new Life Mate records, the retained profile and encrypted photos. Restoring adds a profile archive to Memory Box without replacing the current user. Other original app storage is not included. Choose a private destination. A cloud document provider receives only encrypted backup bytes.',
              'নতুন Life Mate তথ্য, রাখা প্রোফাইল ও এনক্রিপ্ট করা ছবি ব্যাকআপ করো। ফেরালে Memory Box-এ প্রোফাইল আর্কাইভ যোগ হয়, বর্তমান ইউজার বদলায় না। অন্য পুরোনো স্টোরেজ এতে নেই। ব্যক্তিগত গন্তব্য বেছে নাও। ক্লাউড ডকুমেন্ট সেবা শুধু এনক্রিপ্ট করা ব্যাকআপ পাবে।',
            ),
          ),
          TextField(
            controller: phrase,
            obscureText: true,
            autocorrect: false,
            enableSuggestions: false,
            decoration: InputDecoration(
              labelText: s.t(
                'Backup passphrase (12+ characters)',
                'ব্যাকআপ পাসফ্রেজ (১২+ অক্ষর)',
              ),
            ),
          ),
          Text(
            s.t(
              'Keep this passphrase safe. It cannot be recovered. Current MVP limit: 32 MB of photos per vault backup.',
              'পাসফ্রেজ নিরাপদে রাখো। এটি উদ্ধার করা যায় না। MVP-তে প্রতি ব্যাকআপে সর্বোচ্চ ৩২ মেগাবাইট ছবি।',
            ),
          ),
          FilledButton.icon(
            onPressed: busy || s.demo
                ? null
                : () async {
                    setState(() => busy = true);
                    try {
                      final bytes = await VaultBackup.export(s, phrase.text);
                      final path = await FilePicker.platform.saveFile(
                        fileName: 'Life-Mate-vault.lmv',
                        bytes: bytes,
                        type: FileType.custom,
                        allowedExtensions: ['lmv'],
                      );
                      if (path != null && context.mounted)
                        message(
                          context,
                          s.t(
                            'Encrypted backup saved',
                            'এনক্রিপ্ট করা ব্যাকআপ সংরক্ষিত',
                          ),
                        );
                    } catch (_) {
                      if (context.mounted)
                        message(
                          context,
                          s.t(
                            'Backup not saved. Check passphrase, available storage and the size limit.',
                            'ব্যাকআপ সংরক্ষণ হয়নি। পাসফ্রেজ, জায়গা ও আকারের সীমা পরীক্ষা করো।',
                          ),
                        );
                    } finally {
                      if (mounted) setState(() => busy = false);
                    }
                  },
            icon: const Icon(Icons.file_upload_outlined),
            label: Text(
              s.t('Export encrypted backup', 'এনক্রিপ্ট করা ব্যাকআপ রপ্তানি'),
            ),
          ),
          if (!widget.exportOnly)
            OutlinedButton.icon(
              onPressed: busy || s.demo
                  ? null
                  : () async {
                      if (!await confirm(
                        context,
                        s.t(
                          'Import as additional records?',
                          'অতিরিক্ত তথ্য হিসেবে আমদানি?',
                        ),
                        s.t(
                          'Existing records stay unchanged. Imported reminders are off until reviewed and saved; repeating the import creates extra copies.',
                          'আগের তথ্য অক্ষত থাকবে। দেখে সংরক্ষণ না করা পর্যন্ত আমদানির রিমাইন্ডার বন্ধ থাকবে; আবার আমদানি করলে বাড়তি কপি হবে।',
                        ),
                      ))
                        return;
                      if (!mounted) return;
                      setState(() => busy = true);
                      try {
                        final result = await FilePicker.platform.pickFiles(
                          type: FileType.custom,
                          allowedExtensions: ['lmv'],
                          withData: false,
                        );
                        if (result == null) return;
                        final path = result.files.single.path;
                        if (path == null) throw StateError('No file');
                        final file = File(path);
                        if (await file.length() > VaultBackup.limit * 3)
                          throw const FormatException('Too large');
                        final bytes = await file.readAsBytes();
                        await VaultBackup.restore(s, bytes, phrase.text);
                        if (context.mounted)
                          message(
                            context,
                            s.t(
                              'Imported privately. Review reminder times.',
                              'ব্যক্তিগতভাবে আমদানি হয়েছে। রিমাইন্ডারের সময় দেখে নাও।',
                            ),
                          );
                      } catch (_) {
                        if (context.mounted)
                          message(
                            context,
                            s.t(
                              'Could not import. Check the file and passphrase.',
                              'আমদানি হয়নি। ফাইল ও পাসফ্রেজ পরীক্ষা করো।',
                            ),
                          );
                      } finally {
                        if (mounted) setState(() => busy = false);
                      }
                    },
              icon: const Icon(Icons.file_download_outlined),
              label: Text(
                s.t('Import encrypted backup', 'এনক্রিপ্ট করা ব্যাকআপ আমদানি'),
              ),
            ),
          if (busy) const LinearProgressIndicator(),
        ],
      ),
    );
  }
}
