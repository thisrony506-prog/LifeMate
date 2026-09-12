import 'dart:io';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest.dart' as tzdata;
import 'package:timezone/timezone.dart' as tz;
import '../data/entry.dart';
import 'native.dart';

class ReminderService {
  final plugin = FlutterLocalNotificationsPlugin();
  bool initialized = false;
  Future<void> init() async {
    if (initialized || Platform.isAndroid) return;
    tzdata.initializeTimeZones();
    await plugin.initialize(
      const InitializationSettings(
        iOS: DarwinInitializationSettings(
          requestAlertPermission: false,
          requestBadgePermission: false,
          requestSoundPermission: false,
        ),
      ),
    );
    initialized = true;
  }

  int id(String value) =>
      value.codeUnits.fold(0, (a, b) => (a * 31 + b) & 0x7fffffff);
  Future<void> schedule(Entry e) async {
    final time = e.text('time');
    if (time.isEmpty) return;
    final repeat = {EntryKind.routine, EntryKind.medicine}.contains(e.kind)
        ? 'DAILY'
        : {EntryKind.birthday, EntryKind.anniversary}.contains(e.kind)
        ? 'YEARLY'
        : e.kind == EntryKind.bill
        ? 'MONTHLY'
        : 'ONCE';
    if (Platform.isAndroid) {
      await NativeBridge.schedule({
        'id': e.id,
        'title': e.title,
        'date': dayKey(e.date),
        'time': time,
        'repeat': repeat,
      });
      return;
    }
    await init();
    final allowed = await plugin
        .resolvePlatformSpecificImplementation<
          IOSFlutterLocalNotificationsPlugin
        >()
        ?.requestPermissions(alert: true, badge: true, sound: true);
    if (allowed != true) throw StateError('Notification permission is off');
    final parts = time.split(':').map(int.parse).toList();
    var when = DateTime(
      e.date.year,
      e.date.month,
      e.date.day,
      parts[0],
      parts[1],
    );
    if (repeat == 'DAILY') {
      final now = DateTime.now();
      when = DateTime(now.year, now.month, now.day, parts[0], parts[1]);
      if (!when.isAfter(now)) when = when.add(const Duration(days: 1));
    }
    if (!when.isAfter(DateTime.now()))
      throw StateError('Choose a future reminder time');
    await plugin.zonedSchedule(
      id(e.id),
      'Life Mate',
      e.title,
      tz.TZDateTime.from(when, tz.UTC),
      const NotificationDetails(iOS: DarwinNotificationDetails()),
      androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
      matchDateTimeComponents: repeat == 'DAILY'
          ? DateTimeComponents.time
          : repeat == 'MONTHLY'
          ? DateTimeComponents.dayOfMonthAndTime
          : repeat == 'YEARLY'
          ? DateTimeComponents.dateAndTime
          : null,
    );
  }

  Future<void> cancel(Entry e) async {
    if (Platform.isAndroid) {
      await NativeBridge.cancel(e.id);
    } else {
      await init();
      await plugin.cancel(id(e.id));
    }
  }
}
