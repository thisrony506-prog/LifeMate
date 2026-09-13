import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:life_mate_flutter/data/store.dart';
import 'package:life_mate_flutter/services/native.dart';
import 'package:life_mate_flutter/ui/common.dart';
import 'package:life_mate_flutter/ui/existing_pin.dart';

void main() {
  testWidgets(
    'old PIN hides private content, validates and relocks on background',
    (tester) async {
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
        NativeBridge.channel,
        (call) async =>
            call.method == 'existing.verifyPin' &&
            call.arguments['pin'] == '123456',
      );
      addTearDown(
        () => tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
          NativeBridge.channel,
          null,
        ),
      );
      await tester.pumpWidget(
        LifeScope(
          store: LifeStore.memory(),
          child: const MaterialApp(
            home: ExistingPinGate(
              enabled: true,
              child: Scaffold(body: Text('PRIVATE_ACCOUNT')),
            ),
          ),
        ),
      );
      expect(find.text('PRIVATE_ACCOUNT'), findsNothing);
      await tester.enterText(find.byType(TextField), '000000');
      await tester.tap(find.text('Unlock'));
      await tester.pumpAndSettle();
      expect(find.text('PRIVATE_ACCOUNT'), findsNothing);
      await tester.enterText(find.byType(TextField), '123456');
      await tester.tap(find.text('Unlock'));
      await tester.pumpAndSettle();
      expect(find.text('PRIVATE_ACCOUNT'), findsOneWidget);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pumpAndSettle();
      expect(find.text('PRIVATE_ACCOUNT'), findsNothing);
    },
  );
}
