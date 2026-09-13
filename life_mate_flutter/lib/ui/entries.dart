import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'device_lock.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:intl/intl.dart';
import '../data/store.dart';
import '../data/entry.dart';
import 'common.dart';

Future<void> openEntries(BuildContext context, EntryKind kind) async {
  final s = LifeScope.of(context);
  await Navigator.push<void>(
    context,
    MaterialPageRoute<void>(
      builder: (_) => kind == EntryKind.journal && !s.demo
          ? DeviceLock(child: EntryList(kind))
          : EntryList(kind),
    ),
  );
}

Future<void> editEntry(BuildContext context, EntryKind kind, [Entry? entry]) {
  final s = LifeScope.of(context);
  return Navigator.push<void>(
    context,
    MaterialPageRoute<void>(
      builder: (_) => kind == EntryKind.journal && !s.demo
          ? DeviceLock(child: EntryEditor(kind, entry: entry))
          : EntryEditor(kind, entry: entry),
    ),
  );
}

class EntryPhoto extends StatefulWidget {
  final String id;
  final double height;
  const EntryPhoto(this.id, {super.key, this.height = 180});
  @override
  State<EntryPhoto> createState() => _EntryPhotoState();
}

class _EntryPhotoState extends State<EntryPhoto> {
  Future<Uint8List>? bytes;
  Object? vault;
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final next = LifeScope.of(context).vault;
    if (!identical(next, vault)) {
      vault = next;
      bytes = next?.media(widget.id);
    }
  }

  @override
  void didUpdateWidget(EntryPhoto old) {
    super.didUpdateWidget(old);
    if (old.id != widget.id)
      bytes = LifeScope.of(context).vault?.media(widget.id);
  }

  @override
  Widget build(BuildContext context) => ClipRRect(
    borderRadius: BorderRadius.circular(14),
    child: FutureBuilder<Uint8List>(
      future: bytes,
      builder: (_, snapshot) => snapshot.hasData
          ? Image.memory(
              snapshot.data!,
              height: widget.height,
              width: double.infinity,
              fit: BoxFit.cover,
              cacheWidth:
                  (MediaQuery.sizeOf(context).width *
                          MediaQuery.devicePixelRatioOf(context))
                      .round()
                      .clamp(320, 1200)
                      .toInt(),
              filterQuality: FilterQuality.low,
            )
          : SizedBox(
              height: widget.height,
              child: const Center(child: Icon(Icons.lock_outline)),
            ),
    ),
  );
}

class EntryList extends StatelessWidget {
  final EntryKind kind;
  const EntryList(this.kind, {super.key});
  @override
  Widget build(BuildContext context) {
    final s = LifeScope.of(context);
    final items = s.entries(kind);
    return Scaffold(
      appBar: AppBar(title: Text(s.label(kind))),
      floatingActionButton: FloatingActionButton(
        onPressed: s.demo ? null : () => editEntry(context, kind),
        tooltip: s.t('Add', 'যোগ করো'),
        child: const Icon(Icons.add),
      ),
      body: ListView.separated(
        padding: const EdgeInsets.fromLTRB(22, 10, 22, 100),
        itemCount: items.isEmpty ? 1 : items.length,
        separatorBuilder: (_, __) => const SizedBox(height: 12),
        itemBuilder: (context, index) {
          if (items.isEmpty)
            return Panel(
              child: Column(
                children: [
                  Icon(kindIcon(kind), size: 38),
                  const SizedBox(height: 16),
                  Text(
                    s.t(
                      'A little space for your life.',
                      'তোমার জীবনের জন্য ছোট্ট একটি জায়গা।',
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextButton(
                    onPressed: () => editEntry(context, kind),
                    child: Text(
                      s.t('Add your first entry', 'প্রথম তথ্যটি যোগ করো'),
                    ),
                  ),
                ],
              ),
            );
          final e = items[index];
          return Panel(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (e.text('photo').isNotEmpty) ...[
                  EntryPhoto(e.text('photo')),
                  const SizedBox(height: 12),
                ],
                Row(
                  children: [
                    if (kind == EntryKind.task)
                      Checkbox(
                        value: e.done,
                        onChanged: s.demo
                            ? null
                            : (_) => attempt(context, () => s.toggle(e)),
                      ),
                    Expanded(
                      child: Text(
                        e.title,
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                    ),
                    IconButton(
                      tooltip: s.t('Edit', 'সম্পাদনা'),
                      onPressed: s.demo
                          ? null
                          : () => editEntry(context, kind, e),
                      icon: const Icon(Icons.edit_outlined),
                    ),
                    IconButton(
                      tooltip: s.t('Delete', 'মুছো'),
                      onPressed: s.demo
                          ? null
                          : () async {
                              if (await confirm(
                                    context,
                                    s.t(
                                      'Delete this entry?',
                                      'এই তথ্যটি মুছবে?',
                                    ),
                                    s.t(
                                      'Offline deletion will also sync when you next sync your account.',
                                      'পরবর্তী সিঙ্কে অ্যাকাউন্ট থেকেও তথ্যটি মুছে যাবে।',
                                    ),
                                  ) &&
                                  context.mounted)
                                await attempt(context, () => s.delete(e));
                            },
                      icon: const Icon(Icons.delete_outline),
                    ),
                  ],
                ),
                Text(
                  '${DateFormat.yMMMd(s.language).format(e.date)}${e.text('time').isNotEmpty ? ' · ${e.text('time')}' : ''}',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                if (e.text('body').isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Text(e.text('body')),
                ],
                if (e.fields.containsKey('amount'))
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(
                      '${NumberFormat.decimalPattern(s.language).format(e.number('amount'))}${kind == EntryKind.water
                          ? ' ml'
                          : kind == EntryKind.sleep
                          ? ' h'
                          : {EntryKind.expense, EntryKind.income, EntryKind.saving, EntryKind.bill}.contains(kind)
                          ? ' ৳'
                          : ''}',
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                  ),
                if (e.number('target') > 0) ...[
                  const SizedBox(height: 12),
                  LinearProgressIndicator(
                    value: (e.number('amount') / e.number('target'))
                        .clamp(0, 1)
                        .toDouble(),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '${(e.number('amount') / e.number('target') * 100).clamp(0, 100).round()}% · ${s.t('Target', 'লক্ষ্য')} ${e.number('target').toStringAsFixed(0)}',
                  ),
                ],
                if (kind == EntryKind.contact)
                  TextButton.icon(
                    onPressed: s.demo
                        ? null
                        : () => attempt(context, () async {
                            final ok = await launchUrl(
                              Uri(scheme: 'tel', path: e.text('phone')),
                            );
                            if (!ok) throw StateError('No dialer');
                          }),
                    icon: const Icon(Icons.call_outlined),
                    label: Text(e.text('phone')),
                  ),
                if (kind == EntryKind.period)
                  Text(
                    s.t(
                      'Cycle estimates are not contraception or a diagnosis.',
                      'চক্রের হিসাব জন্মনিয়ন্ত্রণ বা রোগ নির্ণয়ের উপায় নয়।',
                    ),
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class EntryEditor extends StatefulWidget {
  final EntryKind kind;
  final Entry? entry;
  const EntryEditor(this.kind, {super.key, this.entry});
  @override
  State<EntryEditor> createState() => _EntryEditorState();
}

class _EntryEditorState extends State<EntryEditor> {
  final form = GlobalKey<FormState>();
  late final TextEditingController title, body, amount, target, phone;
  late DateTime date;
  TimeOfDay? time;
  bool emergency = false, focus = true, busy = false;
  String photo = '', newPhoto = '';
  LifeStore? store;
  bool get numeric => {
    EntryKind.water,
    EntryKind.sleep,
    EntryKind.workout,
    EntryKind.expense,
    EntryKind.income,
    EntryKind.saving,
    EntryKind.bill,
    EntryKind.goal,
    EntryKind.period,
  }.contains(widget.kind);
  bool get reminder => {
    EntryKind.task,
    EntryKind.routine,
    EntryKind.medicine,
    EntryKind.bill,
    EntryKind.birthday,
    EntryKind.anniversary,
  }.contains(widget.kind);
  bool get withPhoto => {
    EntryKind.medicine,
    EntryKind.memory,
    EntryKind.goal,
  }.contains(widget.kind);
  @override
  void initState() {
    super.initState();
    final e = widget.entry;
    title = TextEditingController(text: e?.title);
    body = TextEditingController(text: e?.text('body'));
    amount = TextEditingController(text: e?.fields['amount']?.toString());
    target = TextEditingController(text: e?.fields['target']?.toString());
    phone = TextEditingController(text: e?.text('phone'));
    date = e?.date ?? DateTime.now();
    photo = e?.text('photo') ?? '';
    emergency = e?.fields['emergency'] == true;
    focus = e?.fields['focus'] != false;
    final parts = e?.text('time').split(':');
    if (parts?.length == 2)
      time = TimeOfDay(hour: int.parse(parts![0]), minute: int.parse(parts[1]));
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    store = LifeScope.of(context);
  }

  @override
  void dispose() {
    title.dispose();
    body.dispose();
    amount.dispose();
    target.dispose();
    phone.dispose();
    if (newPhoto.isNotEmpty && !busy) store?.vault?.deleteMedia(newPhoto);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = LifeScope.of(context);
    String? requiredText(String? v) => v == null || v.trim().isEmpty
        ? s.t('Please enter a value', 'তথ্য দাও')
        : null;
    return Scaffold(
      appBar: AppBar(
        title: Text(
          '${s.t(widget.entry == null ? 'Add' : 'Edit', widget.entry == null ? 'যোগ করো' : 'সম্পাদনা')} · ${s.label(widget.kind)}',
        ),
      ),
      body: Form(
        key: form,
        child: PageBody(
          children: [
            TextFormField(
              controller: title,
              maxLength: 200,
              decoration: InputDecoration(
                labelText: s.t('Title / name', 'শিরোনাম / নাম'),
              ),
              validator: requiredText,
            ),
            if (numeric)
              TextFormField(
                controller: amount,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                decoration: InputDecoration(
                  labelText: s.t(
                    widget.kind == EntryKind.water
                        ? 'Amount (ml)'
                        : widget.kind == EntryKind.sleep
                        ? 'Hours slept'
                        : widget.kind == EntryKind.workout
                        ? 'Minutes'
                        : widget.kind == EntryKind.period
                        ? 'Usual cycle length (days)'
                        : 'Amount / progress',
                    'পরিমাণ / অগ্রগতি',
                  ),
                ),
                validator: (v) {
                  final n = double.tryParse(latinDigits(v ?? ''));
                  if (n == null || !n.isFinite || n < 0 || n > 100000000)
                    return s.t(
                      'Enter a valid positive number',
                      'সঠিক ধনাত্মক সংখ্যা দাও',
                    );
                  if (widget.kind == EntryKind.sleep && n > 24)
                    return s.t('Maximum 24 hours', 'সর্বোচ্চ ২৪ ঘণ্টা');
                  if (widget.kind == EntryKind.period && (n < 15 || n > 60))
                    return s.t(
                      'Use 15–60 days; ask a clinician about irregular cycles.',
                      '১৫–৬০ দিন দাও; অনিয়মিত চক্র হলে চিকিৎসকের পরামর্শ নাও।',
                    );
                  return null;
                },
              ),
            if ({EntryKind.saving, EntryKind.goal}.contains(widget.kind))
              TextFormField(
                controller: target,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                decoration: InputDecoration(
                  labelText: s.t('Target amount', 'লক্ষ্যের পরিমাণ'),
                ),
                validator: (v) {
                  final n = double.tryParse(latinDigits(v ?? ''));
                  return n == null || !n.isFinite || n <= 0 || n > 100000000
                      ? s.t(
                          'Enter a target greater than zero',
                          'শূন্যের চেয়ে বড় লক্ষ্য দাও',
                        )
                      : null;
                },
              ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.calendar_today_outlined),
              title: Text(s.t('Date', 'তারিখ')),
              trailing: Text(DateFormat.yMMMd(s.language).format(date)),
              onTap: () async {
                final v = await showDatePicker(
                  context: context,
                  initialDate: date,
                  firstDate: DateTime(1900),
                  lastDate: DateTime(2200),
                );
                if (v != null && mounted) setState(() => date = v);
              },
            ),
            if (reminder)
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.notifications_none),
                title: Text(
                  s.t('Reminder time (optional)', 'রিমাইন্ডারের সময় (ঐচ্ছিক)'),
                ),
                subtitle: Text(
                  s.t(
                    'Daily: routine/medicine · Monthly: bills · Yearly: birthdays/anniversaries',
                    'রুটিন/ওষুধ প্রতিদিন · বিল প্রতি মাসে · জন্মদিন/বার্ষিকী প্রতি বছর',
                  ),
                ),
                trailing: Text(time?.format(context) ?? s.t('Off', 'বন্ধ')),
                onTap: () async {
                  final v = await showTimePicker(
                    context: context,
                    initialTime: time ?? TimeOfDay.now(),
                  );
                  if (v != null && mounted) setState(() => time = v);
                },
              ),
            if (reminder && time != null)
              TextButton(
                onPressed: () => setState(() => time = null),
                child: Text(s.t('Remove reminder', 'রিমাইন্ডার বন্ধ করো')),
              ),
            if (widget.kind == EntryKind.contact) ...[
              TextFormField(
                controller: phone,
                keyboardType: TextInputType.phone,
                decoration: InputDecoration(
                  labelText: s.t('Phone number', 'ফোন নম্বর'),
                ),
                validator: (v) =>
                    !RegExp(
                      r'^\+?[0-9]{7,15}$',
                    ).hasMatch(latinDigits(v ?? '').trim())
                    ? s.t(
                        'Use digits with optional + country code',
                        'দেশের কোডসহ সঠিক নম্বর দাও',
                      )
                    : null,
              ),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(
                  s.t(
                    'Emergency contact (maximum 3)',
                    'জরুরি যোগাযোগ (সর্বোচ্চ ৩ জন)',
                  ),
                ),
                value: emergency,
                onChanged: (v) => setState(() => emergency = v),
              ),
            ],
            if (widget.kind == EntryKind.task)
              CheckboxListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(
                  s.t(
                    'One of my top 3 focus tasks',
                    'আমার প্রধান ৩টি কাজের একটি',
                  ),
                ),
                value: focus,
                onChanged: (v) => setState(() => focus = v ?? false),
              ),
            TextFormField(
              controller: body,
              maxLines: widget.kind == EntryKind.journal ? 10 : 4,
              maxLength: 10000,
              decoration: InputDecoration(
                labelText: s.t(
                  widget.kind == EntryKind.medicine
                      ? 'Prescription notes (no OCR / dose advice)'
                      : 'Notes',
                  'নোট',
                ),
              ),
              validator: widget.kind == EntryKind.journal ? requiredText : null,
            ),
            if (withPhoto) ...[
              if (photo.isNotEmpty) EntryPhoto(photo),
              OutlinedButton.icon(
                onPressed: busy || s.demo
                    ? null
                    : () async {
                        await attempt(context, () async {
                          final source = await showDialog<ImageSource>(
                            context: context,
                            builder: (context) => SimpleDialog(
                              title: Text(
                                s.t('Choose a photo', 'ছবি বেছে নাও'),
                              ),
                              children: [
                                SimpleDialogOption(
                                  onPressed: () => Navigator.pop(
                                    context,
                                    ImageSource.gallery,
                                  ),
                                  child: Text(
                                    s.t('Photo library', 'ছবির লাইব্রেরি'),
                                  ),
                                ),
                                SimpleDialogOption(
                                  onPressed: () => Navigator.pop(
                                    context,
                                    ImageSource.camera,
                                  ),
                                  child: Text(s.t('Take a photo', 'ছবি তোলো')),
                                ),
                              ],
                            ),
                          );
                          if (source == null) return;
                          final image = await ImagePicker().pickImage(
                            source: source,
                            maxWidth: 2000,
                            maxHeight: 2000,
                            imageQuality: 88,
                          );
                          if (image == null) return;
                          final id = await s.vault!.saveMedia(
                            await image.readAsBytes(),
                          );
                          if (!mounted) {
                            await s.vault!.deleteMedia(id);
                            return;
                          }
                          if (newPhoto.isNotEmpty)
                            await s.vault!.deleteMedia(newPhoto);
                          setState(() => photo = newPhoto = id);
                        });
                      },
                icon: const Icon(Icons.add_photo_alternate_outlined),
                label: Text(
                  s.t('Add encrypted photo', 'এনক্রিপ্ট করা ছবি যোগ করো'),
                ),
              ),
            ],
            if (widget.kind == EntryKind.medicine)
              Text(
                s.t(
                  'Enter medicine timing from your prescription. Life Mate does not diagnose or change doses.',
                  'প্রেসক্রিপশন অনুযায়ী ওষুধের সময় দাও। Life Mate রোগ নির্ণয় বা ডোজ পরিবর্তন করে না।',
                ),
              ),
            FilledButton(
              onPressed: busy || s.demo
                  ? null
                  : () async {
                      if (!form.currentState!.validate()) return;
                      if (emergency &&
                          s
                                  .entries(EntryKind.contact)
                                  .where(
                                    (e) =>
                                        e.fields['emergency'] == true &&
                                        e.id != widget.entry?.id,
                                  )
                                  .length >=
                              3) {
                        message(
                          context,
                          s.t(
                            'Choose up to 3 emergency contacts',
                            'সর্বোচ্চ ৩ জন জরুরি যোগাযোগ রাখো',
                          ),
                        );
                        return;
                      }
                      if (widget.kind == EntryKind.task &&
                          focus &&
                          s
                                  .entries(EntryKind.task)
                                  .where(
                                    (e) =>
                                        e.fields['focus'] == true &&
                                        dayKey(e.date) == dayKey(date) &&
                                        e.id != widget.entry?.id,
                                  )
                                  .length >=
                              3) {
                        message(
                          context,
                          s.t(
                            'You already have 3 focus tasks for this day. Turn Focus off to add another task.',
                            'এই দিনের ৩টি ফোকাস কাজ আছে। অন্য কাজ যোগ করতে ফোকাস বন্ধ করো।',
                          ),
                        );
                        return;
                      }
                      setState(() => busy = true);
                      final fields = {
                        ...?(widget.entry?.fields),
                        'body': body.text.trim(),
                        'photo': photo,
                        if (numeric)
                          'amount': double.parse(latinDigits(amount.text)),
                        if (target.text.isNotEmpty)
                          'target': double.parse(latinDigits(target.text)),
                        if (widget.kind == EntryKind.contact) ...{
                          'phone': latinDigits(phone.text.trim()),
                          'emergency': emergency,
                        },
                        if (widget.kind == EntryKind.task) 'focus': focus,
                        'time': time == null
                            ? ''
                            : '${time!.hour.toString().padLeft(2, '0')}:${time!.minute.toString().padLeft(2, '0')}',
                      };
                      final e =
                          widget.entry?.copy(
                            title: title.text.trim(),
                            date: date,
                            fields: fields,
                            dirty: true,
                          ) ??
                          Entry(
                            kind: widget.kind,
                            title: title.text.trim(),
                            date: date,
                            fields: fields,
                          );
                      try {
                        if (reminder && time == null && widget.entry != null)
                          await s.reminders.cancel(widget.entry!);
                        await s.save(e, remind: reminder && time != null);
                        if (newPhoto.isNotEmpty &&
                            widget.entry?.text('photo').isNotEmpty == true)
                          await s.vault?.deleteMedia(
                            widget.entry!.text('photo'),
                          );
                        newPhoto = '';
                        if (context.mounted) {
                          final issue = s.reminderIssue;
                          Navigator.pop(context);
                          if (issue != null) message(context, issue);
                        }
                      } catch (_) {
                        if (context.mounted)
                          message(
                            context,
                            s.t(
                              'Could not save. Please try again.',
                              'সংরক্ষণ হয়নি। আবার চেষ্টা করো।',
                            ),
                          );
                      } finally {
                        if (mounted) setState(() => busy = false);
                      }
                    },
              child: Text(s.t('Save privately', 'ব্যক্তিগতভাবে সংরক্ষণ')),
            ),
            Row(
              children: [
                const Icon(Icons.lock_outline, size: 16),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    s.t(
                      'Encrypted on this device. No automatic photo upload.',
                      'এই ডিভাইসে এনক্রিপ্ট করা। ছবি নিজে থেকে আপলোড হয় না।',
                    ),
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
