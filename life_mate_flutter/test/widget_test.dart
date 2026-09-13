import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:life_mate_flutter/main.dart';
import 'package:life_mate_flutter/data/store.dart';

void main() {
  testWidgets('five primary tabs and real empty state, no seeded balances', (
    tester,
  ) async {
    final s = LifeStore.memory();
    s.name = 'Rony';
    await tester.pumpWidget(LifeMateApp(store: s));
    await tester.pumpAndSettle();
    for (final label in ['Home', 'My Life', 'Health', 'Money', 'Profile']) {
      expect(find.text(label), findsOneWidget);
    }
    expect(find.textContaining('Rony'), findsOneWidget);
    await tester.tap(find.text('Money'));
    await tester.pumpAndSettle();
    expect(find.text('৳ 0'), findsWidgets);
  });
  testWidgets('language and full dark theme switch at runtime', (tester) async {
    final s = LifeStore.memory();
    await tester.pumpWidget(LifeMateApp(store: s));
    await tester.pumpAndSettle();
    await s.setLanguage('bn');
    await tester.pumpAndSettle();
    expect(find.text('আমার জীবন'), findsOneWidget);
    s.appearance = 'Dark';
    s.notifyListeners();
    await tester.pumpAndSettle();
    final context = tester.element(find.byType(NavigationBar));
    expect(Theme.of(context).brightness, Brightness.dark);
  });
  testWidgets('new onboarding offers Bangla before routine setup', (
    tester,
  ) async {
    final s = LifeStore.memory();
    s.onboarded = false;
    await tester.pumpWidget(LifeMateApp(store: s));
    await tester.pumpAndSettle();
    expect(find.text('বাংলা'), findsOneWidget);
    expect(find.text('Your Life, Organized in One App'), findsOneWidget);
    await tester.tap(find.text('Continue'));
    await tester.pumpAndSettle();
    expect(find.text('Wake-up time'), findsOneWidget);
  });
  testWidgets('app lock hides real content without device authentication', (tester) async {
    final s = LifeStore.memory();
    s.name = 'PrivateLockedName';
    s.appLock = true;
    await tester.pumpWidget(LifeMateApp(store: s));
    await tester.pumpAndSettle();
    expect(find.textContaining('PrivateLockedName'), findsNothing);
    expect(find.text('Unlock'), findsOneWidget);
  });
  testWidgets('My Life does not expose removed organizer tools', (tester) async {
    final s = LifeStore.memory();
    await tester.pumpWidget(LifeMateApp(store: s));
    await tester.pumpAndSettle();
    await tester.tap(find.text('My Life'));
    await tester.pumpAndSettle();
    for (final removed in ['All retained tools', 'Missions', 'Facebook Posts', 'Card Studio']) {
      expect(find.text(removed), findsNothing);
    }
    expect(find.text('Mind Mate'), findsOneWidget);
  });

}
