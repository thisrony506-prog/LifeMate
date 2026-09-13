import 'dart:io';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:timezone/data/latest.dart' as tzdata;
import 'package:timezone/timezone.dart' as tz;
import '../data/entry.dart';

class ReminderService {
  final plugin = FlutterLocalNotificationsPlugin();
  bool initialized = false;
  void Function(String)? onOpen;
  Future<void> init() async {
    if (initialized || (!Platform.isAndroid && !Platform.isIOS)) return;
    tzdata.initializeTimeZones();
    tz.setLocalLocation(
      tz.getLocation(await FlutterTimezone.getLocalTimezone()),
    );
    await plugin.initialize(
      const InitializationSettings(
        android: AndroidInitializationSettings('ic_notification'),
        iOS: DarwinInitializationSettings(
          requestAlertPermission: false,
          requestBadgePermission: false,
          requestSoundPermission: false,
        ),
      ),
      onDidReceiveNotificationResponse: (response) {
        final value = response.payload;
        if (value != null) onOpen?.call(value);
      },
    );
    initialized = true;
    final launch = await plugin.getNotificationAppLaunchDetails();
    final payload = launch?.notificationResponse?.payload;
    if (launch?.didNotificationLaunchApp == true && payload != null)
      onOpen?.call(payload);
  }

  int id(String value) =>
      value.codeUnits.fold(0, (a, b) => (a * 31 + b) & 0x7fffffff);
  Future<void> schedule(Entry e) async {
    final time = e.text('time');
    if (time.isEmpty || e.deleted || e.done) return;
    await init();
    final bool? allowed;
    if (Platform.isAndroid) {
      allowed = await plugin
          .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin
          >()
          ?.requestNotificationsPermission();
    } else {
      allowed = await plugin
          .resolvePlatformSpecificImplementation<
            IOSFlutterLocalNotificationsPlugin
          >()
          ?.requestPermissions(alert: true, badge: true, sound: true);
    }
    if (allowed != true) throw StateError('Notification permission is off');
    final repeat = {EntryKind.routine, EntryKind.medicine}.contains(e.kind)
        ? 'DAILY'
        : {EntryKind.birthday, EntryKind.anniversary}.contains(e.kind)
        ? 'YEARLY'
        : e.kind == EntryKind.bill
        ? 'MONTHLY'
        : 'ONCE';
    final parts = time.split(':').map(int.parse).toList();
    final now = tz.TZDateTime.now(tz.local);
    var when = tz.TZDateTime(
      tz.local,
      e.date.year,
      e.date.month,
      e.date.day,
      parts[0],
      parts[1],
    );
    if (repeat == 'DAILY') {
      if (!when.isAfter(now))
        when = tz.TZDateTime(
          tz.local,
          now.year,
          now.month,
          now.day,
          parts[0],
          parts[1],
        );
      if (!when.isAfter(now))
        when = tz.TZDateTime(
          tz.local,
          now.year,
          now.month,
          now.day + 1,
          parts[0],
          parts[1],
        );
    } else if (repeat == 'YEARLY') {
      var year = e.date.year > now.year ? e.date.year : now.year;
      while (true) {
        when = tz.TZDateTime(
          tz.local,
          year,
          e.date.month,
          e.date.day,
          parts[0],
          parts[1],
        );
        if (when.month == e.date.month &&
            when.day == e.date.day &&
            when.isAfter(now))
          break;
        year++;
      }
    } else if (repeat == 'MONTHLY') {
      var month = when.isAfter(now)
          ? (e.date.year - now.year) * 12 + e.date.month
          : now.month;
      // Skip months without the requested day rather than silently shifting a bill.
      while (true) {
        final first = DateTime(now.year, month, 1);
        final lastDay = DateTime(first.year, first.month + 1, 0).day;
        when = tz.TZDateTime(
          tz.local,
          first.year,
          first.month,
          e.date.day,
          parts[0],
          parts[1],
        );
        if (e.date.day <= lastDay && when.isAfter(now)) break;
        month++;
      }
    }
    if (!when.isAfter(now)) throw StateError('Choose a future reminder time');
    await plugin.zonedSchedule(
      id(e.id),
      'Life Mate',
      'A private reminder is ready · একটি ব্যক্তিগত রিমাইন্ডার আছে',
      when,
      const NotificationDetails(
        android: AndroidNotificationDetails(
          'life_mate_personal_os',
          'Life Mate reminders',
          channelDescription: 'Private routines, medicine and bill reminders',
          importance: Importance.high,
          priority: Priority.high,
          visibility: NotificationVisibility.private,
        ),
        iOS: DarwinNotificationDetails(),
      ),
      payload: e.id,
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
    await init();
    if (initialized) await plugin.cancel(id(e.id));
  }

  Future<void> cancelAll() async {
    await init();
    if (initialized) await plugin.cancelAll();
  }
}
