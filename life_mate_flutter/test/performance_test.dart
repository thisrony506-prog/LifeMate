import 'package:flutter_test/flutter_test.dart';
import 'package:life_mate_flutter/data/entry.dart';
import 'package:life_mate_flutter/data/store.dart';

void main() {
  test('indexed reads remain correct and caller sorting cannot corrupt cached order', () async {
    final store = LifeStore.memory();
    for (var i = 0; i < 1000; i++) {
      await store.save(Entry(id:'row-$i',kind:EntryKind.expense,title:'Fare $i',date:DateTime(2026,1,1).add(Duration(days:i)),fields:{'amount':i+1}));
    }
    final expected = store.entries(EntryKind.expense).map((e)=>e.id).toList();
    final watch = Stopwatch()..start();
    for (var i = 0; i < 100; i++) { expect(store.entries(EntryKind.expense).length,1000); }
    watch.stop();
    // A diagnostic, not a device speed guarantee or a flaky timing threshold.
    print('PERF indexed 100 reads / 1000 records: ${watch.elapsedMicroseconds} us (test host)');
    store.entries(EntryKind.expense).clear();
    expect(store.entries(EntryKind.expense).map((e)=>e.id),expected);
    await store.save(Entry(kind:EntryKind.income,title:'Income',fields:{'amount':500}));
    expect(store.entries(EntryKind.income).length,1);
    expect(store.total(EntryKind.expense),500500);
    store.setDemo(true);store.setDemo(false);
    expect(store.entries(EntryKind.expense).length,1000);
  });
  test('entry field maps cannot be mutated outside store writes', () {
    final fields = <String,dynamic>{'amount':120};
    final entry=Entry(kind:EntryKind.expense,title:'Fare',fields:fields);
    fields['amount']=999;
    expect(entry.number('amount'),120);
    expect(()=>entry.fields['amount']=9,throwsUnsupportedError);
  });
}
