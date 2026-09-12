import 'entry.dart';

/// Preview records live only in memory. Never mixed into the user's vault/sync.
List<Entry> demoEntries(DateTime now, bool bangla) => [
  Entry(id: 'demo-1', kind: EntryKind.task, title: bangla ? 'অ্যাসাইনমেন্ট শেষ করো' : 'Finish the assignment', fields: {'focus': true}),
  Entry(id: 'demo-2', kind: EntryKind.task, title: bangla ? 'মাকে ফোন করো' : 'Call Ma', fields: {'focus': true, 'done': true, 'completedDay': dayKey(now)}),
  Entry(id: 'demo-3', kind: EntryKind.routine, title: bangla ? 'সকালের শান্ত সময়' : 'A little morning quiet', fields: {'time': '07:00'}),
  Entry(id: 'demo-4', kind: EntryKind.water, title: 'Water', fields: {'amount': 1000}),
  Entry(id: 'demo-5', kind: EntryKind.sleep, title: 'Sleep', fields: {'amount': 7.5}),
  Entry(id: 'demo-6', kind: EntryKind.expense, title: bangla ? 'রিকশা' : 'Rickshaw', fields: {'amount': 120}),
  Entry(id: 'demo-7', kind: EntryKind.income, title: bangla ? 'টিউশনি' : 'Tuition', fields: {'amount': 5000}),
  Entry(id: 'demo-8', kind: EntryKind.saving, title: bangla ? 'জরুরি তহবিল' : 'Emergency fund', fields: {'amount': 12500, 'target': 50000}),
  Entry(id: 'demo-9', kind: EntryKind.goal, title: bangla ? 'এক বছরে ১২টি বই' : '12 books in one year', fields: {'amount': 3, 'target': 12}),
  for (var i = 0; i < 7; i++) Entry(id: 'demo-mood-$i', kind: EntryKind.mood, title: 'Mood', date: now.subtract(Duration(days: i)), fields: {'amount': [3,2,3,1,2,3,2][i]}),
];
