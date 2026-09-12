import 'package:flutter/foundation.dart';
import 'entry.dart';
import 'vault.dart';
import 'demo.dart';
import '../services/native.dart';
import '../services/notifications.dart';

class LifeStore extends ChangeNotifier {
  final Vault? vault;
  final ReminderService reminders = ReminderService();
  List<Entry> _records = [];
  List<Map<String, dynamic>> legacy = [];
  String language = 'en', appearance = 'System', name = '', wakeTime = '07:00';
  bool onboarded = false, demo = false, ready = false;
  String? reminderIssue;
  LifeStore(this.vault);
  LifeStore.memory({this.language = 'en'}) : vault = null { ready = true; onboarded = true; }
  String t(String en, String bn) => language == 'bn' ? bn : en;
  List<Entry> get all => demo ? demoEntries(DateTime.now(), language == 'bn') : List.unmodifiable(_records);
  List<Entry> entries(EntryKind kind) => all.where((e) => e.kind == kind && !e.deleted).toList()..sort((a,b) => b.date.compareTo(a.date));
  List<Entry> get pending => _records.where((e) => e.syncable && e.dirty).toList();
  Future<void> load() async {
    final data = await NativeBridge.snapshot();
    _records = vault?.entries ?? [];
    language = vault?.settingValue('language', 'en') ?? 'en';
    appearance = vault?.settingValue('appearance', data['theme']?.toString() ?? 'System') ?? 'System';
    name = data['name']?.toString() ?? vault?.settingValue('name') ?? '';
    legacy = ((data['items'] as List?) ?? []).map((v) => Map<String, dynamic>.from(v as Map)).toList();
    onboarded = (vault?.settingValue('onboarded') == 'true') || name.isNotEmpty;
    ready = true; notifyListeners();
  }
  Future<void> refreshNative() async {
    final data = await NativeBridge.snapshot();
    if (data['name'] != null) name = data['name'].toString();
    if (data['notificationsAllowed']==true) reminderIssue=null;
    legacy = ((data['items'] as List?) ?? []).map((v) => Map<String, dynamic>.from(v as Map)).toList();
    notifyListeners();
  }
  Future<void> setLanguage(String value) async {
    await vault?.setting('language', value); language = value; notifyListeners();
  }
  Future<void> setAppearance(String value) async {
    await vault?.setting('appearance', value); await NativeBridge.theme(value); appearance = value; notifyListeners();
  }
  Future<void> finishOnboarding(String value, String time) async {
    final safe = value.trim().isEmpty ? t('Friend', 'বন্ধু') : value.trim();
    await NativeBridge.profile(safe);
    await vault?.setting('name', safe); await vault?.setting('onboarded', 'true');
    name = safe; wakeTime = time; onboarded = true;
    // No invented tasks, balances or contacts. Only the routine the user confirmed.
    await save(Entry(kind: EntryKind.routine, title: t('Wake up', 'ঘুম থেকে ওঠা'), fields: {'time': time}), remind: true);
    notifyListeners();
  }
  void setDemo(bool value) { demo = value; NativeBridge.demo=value; notifyListeners(); }
  Future<void> save(Entry entry, {bool remind = false}) async {
    if (demo) throw StateError(t('Leave Demo mode to save your own data.', 'নিজের তথ্য রাখতে ডেমো মোড বন্ধ করো।'));
    if (entry.title.trim().isEmpty || entry.title.length > 200) throw const FormatException('Invalid title');
    await vault?.put(entry);
    _records.removeWhere((e) => e.id == entry.id); _records.add(entry);
    reminderIssue = null;
    if (remind) {
      try { await reminders.schedule(entry); }
      catch (_) { reminderIssue = t('Saved. Reminder is not active: check time and notification permissions.', 'সংরক্ষিত। রিমাইন্ডার চালু হয়নি: সময় ও নোটিফিকেশন অনুমতি দেখো।'); }
    }
    notifyListeners();
  }
  Future<void> toggle(Entry e) => save(e.copy(fields: {...e.fields, 'done': !e.done, 'completedDay': !e.done ? dayKey(DateTime.now()) : ''}, dirty: true));
  Future<void> delete(Entry e) async {
    if (demo) throw StateError(t('Demo is read-only', 'ডেমো শুধু দেখার জন্য'));
    await reminders.cancel(e);
    await save(e.copy(deleted: true, dirty: true));
    if (e.text('photo').isNotEmpty) await vault?.deleteMedia(e.text('photo'));
  }
  void reloadVault() { _records=vault?.entries??[]; notifyListeners(); }
  Future<void> acceptCloud(Entry e) async {
    await vault?.put(e); _records.removeWhere((v) => v.id == e.id); _records.add(e); notifyListeners();
  }
  Future<void> erase() async {
    for (final e in _records) {
      if (e.text('photo').isNotEmpty) await vault?.deleteMedia(e.text('photo'));
    }
    await vault?.box.clear(); _records.clear(); name = ''; onboarded = false; demo = false; legacy = []; notifyListeners();
  }
  int get dailyStreak => streak(all, DateTime.now());
  double total(EntryKind kind, {bool today = false, bool month = false}) {
    final now = DateTime.now();
    return entries(kind).where((e) => (!today || dayKey(e.date) == dayKey(now)) && (!month || e.date.year == now.year && e.date.month == now.month)).fold(0, (sum,e) => sum + e.number('amount'));
  }
  String label(EntryKind kind) => switch(kind) {
    EntryKind.task => t('Focus task', 'ফোকাস কাজ'), EntryKind.routine => t('Routine', 'রুটিন'),
    EntryKind.mood => t('Mood', 'মন কেমন'), EntryKind.journal => t('Secret journal', 'গোপন ডায়েরি'),
    EntryKind.water => t('Water', 'পানি'), EntryKind.sleep => t('Sleep', 'ঘুম'),
    EntryKind.period => t('Period', 'পিরিয়ড'), EntryKind.workout => t('Workout', 'ব্যায়াম'),
    EntryKind.medicine => t('Medicine', 'ওষুধ'), EntryKind.expense => t('Expense', 'খরচ'),
    EntryKind.income => t('Income', 'আয়'), EntryKind.saving => t('Savings goal', 'সঞ্চয়ের লক্ষ্য'),
    EntryKind.bill => t('Bill alert', 'বিলের রিমাইন্ডার'), EntryKind.contact => t('Important contact', 'প্রিয়জনের যোগাযোগ'),
    EntryKind.birthday => t('Birthday', 'জন্মদিন'), EntryKind.anniversary => t('Anniversary', 'বার্ষিকী'),
    EntryKind.memory => t('Memory', 'স্মৃতি'), EntryKind.goal => t('Vision board', 'স্বপ্নের বোর্ড')
  };
}
