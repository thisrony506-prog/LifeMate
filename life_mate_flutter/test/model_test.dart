import 'package:flutter_test/flutter_test.dart';
import 'package:life_mate_flutter/data/entry.dart';
import 'package:life_mate_flutter/data/store.dart';

void main() {
  test('all module models round-trip without losing fields', () {
    for(final kind in EntryKind.values) {
      final e=Entry(kind:kind,title:'ব্যক্তিগত তথ্য',fields:{'amount':120.5,'body':'A private note','done':true});
      final restored=Entry.fromJson(e.toJson());
      expect(restored.id,e.id);expect(restored.kind,kind);expect(restored.fields,e.fields);
    }
  });
  test('future schema fails closed', () {
    final json=Entry(kind:EntryKind.journal,title:'secret').toJson()..['schema']=2;
    expect(()=>Entry.fromJson(json),throwsFormatException);
  });
  test('English and Bangla expense parsing is offline and exact', () {
    expect(parseExpense('120 taka rickshaw')?.amount,120);
    expect(parseExpense('১২০ টাকা রিকশা')?.description,'রিকশা');
    expect(parseExpense('85.50 tk lunch')?.amount,85.50);
    expect(parseExpense('-120 taka taxi'),isNull);
    expect(parseExpense('buy something'),isNull);
    expect(parseExpense('0 taka bus'),isNull);
  });
  test('daily streak uses real completion days and ignores tombstones', () {
    final now=DateTime(2026,9,13);
    final list=[for(var i=0;i<3;i++) Entry(kind:EntryKind.task,title:'Task',fields:{'done':true,'completedDay':dayKey(now.subtract(Duration(days:i)))})];
    expect(streak(list,now),3);
    expect(streak([list.first.copy(deleted:true)],now),0);
  });
  test('demo data never enters persistence or outbox', () async {
    final s=LifeStore.memory();expect(s.all,isEmpty);
    s.setDemo(true);expect(s.all,isNotEmpty);expect(s.pending,isEmpty);
    expect(s.all.map((e)=>e.kind).toSet(),EntryKind.values.toSet());
    await expectLater(s.save(Entry(kind:EntryKind.task,title:'x')),throwsStateError);
    s.setDemo(false);expect(s.all,isEmpty);
  });
  test('deletions retain a sync tombstone; health and contacts never auto-sync', () {
    final e=Entry(kind:EntryKind.expense,title:'Fare').copy(deleted:true);
    expect(e.deleted,isTrue);expect(e.syncable,isTrue);expect(e.dirty,isTrue);
    expect(Entry(kind:EntryKind.period,title:'Private').syncable,isFalse);
    expect(Entry(kind:EntryKind.contact,title:'Family').syncable,isFalse);
  });
  test('offline save and completion update statistics', () async {
    final s=LifeStore.memory();final e=Entry(kind:EntryKind.task,title:'One thing');
    await s.save(e);await s.toggle(e);
    expect(s.dailyStreak,1);
    await s.save(Entry(kind:EntryKind.expense,title:'Fare',fields:{'amount':120}));
    expect(s.total(EntryKind.expense,month:true),120);
  });
}
