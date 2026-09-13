import 'entry.dart';

/// Preview records live only in memory. Never mixed into the user's vault/sync.
List<Entry> demoEntries(DateTime now, bool bangla) => [
  Entry(
    id: 'demo-1',
    kind: EntryKind.task,
    title: bangla ? 'অ্যাসাইনমেন্ট শেষ করো' : 'Finish the assignment',
    fields: {'focus': true},
  ),
  Entry(
    id: 'demo-2',
    kind: EntryKind.task,
    title: bangla ? 'মাকে ফোন করো' : 'Call Ma',
    fields: {'focus': true, 'done': true, 'completedDay': dayKey(now)},
  ),
  Entry(
    id: 'demo-3',
    kind: EntryKind.routine,
    title: bangla ? 'সকালের শান্ত সময়' : 'A little morning quiet',
    fields: {'time': '07:00'},
  ),
  Entry(
    id: 'demo-4',
    kind: EntryKind.water,
    title: bangla ? 'পানি' : 'Water',
    fields: {'amount': 1000},
  ),
  Entry(
    id: 'demo-5',
    kind: EntryKind.sleep,
    title: bangla ? 'ঘুম' : 'Sleep',
    fields: {'amount': 7.5},
  ),
  Entry(
    id: 'demo-6',
    kind: EntryKind.expense,
    title: bangla ? 'রিকশা' : 'Rickshaw',
    fields: {'amount': 120},
  ),
  Entry(
    id: 'demo-7',
    kind: EntryKind.income,
    title: bangla ? 'টিউশনি' : 'Tuition',
    fields: {'amount': 5000},
  ),
  Entry(
    id: 'demo-8',
    kind: EntryKind.saving,
    title: bangla ? 'জরুরি তহবিল' : 'Emergency fund',
    date: DateTime(now.year, now.month + 6, now.day),
    fields: {'amount': 12500, 'target': 50000},
  ),
  Entry(
    id: 'demo-9',
    kind: EntryKind.goal,
    title: bangla ? 'এক বছরে ১২টি বই' : '12 books in one year',
    date: DateTime(now.year + 1, now.month, now.day),
    fields: {'amount': 3, 'target': 12},
  ),
  Entry(id: 'demo-journal', kind: EntryKind.journal, title: bangla ? 'ছোট্ট ভালো লাগা' : 'A small good thing', date: now, fields: {'body': bangla ? 'আজ নিজের জন্য একটু সময় রেখেছি। এটি শুধু নমুনা।' : 'Today I made a little time for myself. This is only a sample.'}),
  Entry(id: 'demo-period', kind: EntryKind.period, title: bangla ? 'ব্যক্তিগত নমুনা' : 'Private sample', date: now, fields: {'amount': 28}),
  Entry(id: 'demo-workout', kind: EntryKind.workout, title: bangla ? 'বিকেলের হাঁটা' : 'Evening walk', date: now, fields: {'amount': 20}),
  Entry(id: 'demo-medicine', kind: EntryKind.medicine, title: bangla ? 'প্রেসক্রিপশনের নমুনা' : 'Prescription example', date: now, fields: {'body': bangla ? 'নিজের চিকিৎসকের নির্দেশনা এখানে রাখো। এটি চিকিৎসা পরামর্শ নয়।' : 'Keep your clinician’s instructions here. This sample is not medical advice.', 'time': '20:00'}),
  Entry(id: 'demo-bill', kind: EntryKind.bill, title: bangla ? 'ওয়াইফাই বিল' : 'WiFi bill', date: now.add(const Duration(days: 7)), fields: {'amount': 800, 'time': '10:00'}),
  Entry(id: 'demo-contact', kind: EntryKind.contact, title: bangla ? 'প্রিয়জন · নমুনা' : 'Family member · sample', date: now, fields: {'body': bangla ? 'নিজের যোগাযোগ যোগ করো। ডেমোতে কল বন্ধ।' : 'Add your own contact. Calls are disabled in demo.'}),
  Entry(id: 'demo-birthday', kind: EntryKind.birthday, title: bangla ? 'বন্ধুর জন্মদিন · নমুনা' : 'Friend’s birthday · sample', date: now.add(const Duration(days: 14)), fields: {'time': '09:00'}),
  Entry(id: 'demo-anniversary', kind: EntryKind.anniversary, title: bangla ? 'পরিবারের বিশেষ দিন · নমুনা' : 'Family anniversary · sample', date: now.add(const Duration(days: 30)), fields: {'time': '09:00'}),
  Entry(id: 'demo-memory', kind: EntryKind.memory, title: bangla ? 'নদীর ধারে বিকেল' : 'An afternoon by the river', date: now, fields: {'body': bangla ? 'প্রিয়জনের সঙ্গে শান্ত সময়। এটি নমুনা স্মৃতি।' : 'Quiet time with the people I care about. A sample memory.'}),
  for (var i = 0; i < 7; i++)
    Entry(
      id: 'demo-mood-$i',
      kind: EntryKind.mood,
      title: bangla ? 'মন কেমন' : 'Mood',
      date: now.subtract(Duration(days: i)),
      fields: {
        'amount': [3, 2, 3, 1, 2, 3, 2][i],
      },
    ),
];
