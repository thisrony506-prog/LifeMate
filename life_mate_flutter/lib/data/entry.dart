import 'package:uuid/uuid.dart';

enum EntryKind { task, routine, mood, journal, water, sleep, period, workout, medicine, expense, income, saving, bill, contact, birthday, anniversary, memory, goal }

/// One encrypted, versioned local record. Tombstones survive offline deletion.
class Entry {
  final String id;
  final EntryKind kind;
  final String title;
  final DateTime date;
  final Map<String, dynamic> fields;
  final int revision;
  final bool dirty, deleted;
  Entry({String? id, required this.kind, required this.title, DateTime? date,
    this.fields = const {}, this.revision = 0, this.dirty = true, this.deleted = false})
      : id = id ?? const Uuid().v4(), date = date ?? DateTime.now();
  Entry copy({String? title, DateTime? date, Map<String, dynamic>? fields, int? revision, bool? dirty, bool? deleted}) => Entry(
    id: id, kind: kind, title: title ?? this.title, date: date ?? this.date,
    fields: fields ?? this.fields, revision: revision ?? this.revision,
    dirty: dirty ?? this.dirty, deleted: deleted ?? this.deleted);
  Map<String, dynamic> toJson() => {'schema': 1, 'id': id, 'kind': kind.name, 'title': title,
    'date': date.toIso8601String(), 'fields': fields, 'revision': revision, 'dirty': dirty, 'deleted': deleted};
  factory Entry.fromJson(Map<String, dynamic> json) {
    if (json['schema'] != 1) throw const FormatException('Unsupported record version');
    final entry = Entry(id: json['id'] as String, kind: EntryKind.values.byName(json['kind'] as String),
      title: json['title'] as String, date: DateTime.parse(json['date'] as String),
      fields: Map<String, dynamic>.from(json['fields'] as Map), revision: json['revision'] as int,
      dirty: json['dirty'] as bool, deleted: json['deleted'] as bool);
    if (entry.id.isEmpty || entry.title.length > 200 || entry.revision < 0) throw const FormatException('Invalid record');
    return entry;
  }
  double number(String key) => (fields[key] as num?)?.toDouble() ?? 0;
  String text(String key) => fields[key]?.toString() ?? '';
  bool get done => fields['done'] == true;
  bool get syncable => {EntryKind.task, EntryKind.journal, EntryKind.expense, EntryKind.income}.contains(kind);
}

String dayKey(DateTime date) => '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';

/// Deterministic local parser. Never pretends to be speech recognition or AI.
({double amount, String description})? parseExpense(String input) {
  const bn = '০১২৩৪৫৬৭৮৯';
  var normalized = input.trim();
  for (var i = 0; i < 10; i++) { normalized = normalized.replaceAll(bn[i], '$i'); }
  final match = RegExp(r'^\s*(\d+(?:\.\d{1,2})?)\s*(?:taka|tk|৳|টাকা)?\s+(.+)$', caseSensitive: false).firstMatch(normalized);
  if (match == null) return null;
  final amount = double.tryParse(match[1]!);
  if (amount == null || amount <= 0 || amount > 100000000) return null;
  return (amount: amount, description: match[2]!.trim());
}

int streak(Iterable<Entry> entries, DateTime now) {
  final days = entries.where((e) => e.kind == EntryKind.task && e.done && !e.deleted).map((e) => e.text('completedDay')).toSet();
  var cursor = DateTime(now.year, now.month, now.day);
  if (!days.contains(dayKey(cursor))) cursor = cursor.subtract(const Duration(days: 1));
  var result = 0;
  while (days.contains(dayKey(cursor))) { result++; cursor = cursor.subtract(const Duration(days: 1)); }
  return result;
}

String latinDigits(String text) { const digits='০১২৩৪৫৬৭৮৯'; var result=text; for(var i=0;i<10;i++){result=result.replaceAll(digits[i],'$i');} return result; }
