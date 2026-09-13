import 'dart:async';
import 'package:flutter/material.dart';
import 'package:audioplayers/audioplayers.dart';
import '../data/entry.dart';
import '../services/cloud.dart';
import 'common.dart';
import 'entries.dart';

class MindPage extends StatelessWidget {
  const MindPage({super.key});
  @override
  Widget build(BuildContext context) {
    final s = LifeScope.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(s.t('Mind Mate', 'মন মেট'))),
      body: PageBody(
        children: [
          Heading(
            s.t('You can just be you.', 'এখানে তুমি তোমার মতোই।'),
            subtitle: s.t(
              'A quiet check-in, without judgment.',
              'বিচার নয়, নিজের একটু খোঁজ নেওয়া।',
            ),
          ),
          Panel(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  s.t('How are you feeling today?', 'আজ তোমার মন কেমন?'),
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    for (final item in [
                      (3, '😊', 'Happy', 'ভালো'),
                      (2, '😐', 'Neutral', 'মোটামুটি'),
                      (1, '😔', 'Sad', 'খারাপ'),
                    ])
                      Expanded(
                        child: TextButton(
                          onPressed: s.demo
                              ? null
                              : () => attempt(context, () async {
                                  final now = DateTime.now();
                                  final old = s
                                      .entries(EntryKind.mood)
                                      .where(
                                        (e) => dayKey(e.date) == dayKey(now),
                                      )
                                      .firstOrNull;
                                  await s.save(
                                    old?.copy(
                                          fields: {'amount': item.$1},
                                          dirty: true,
                                        ) ??
                                        Entry(
                                          kind: EntryKind.mood,
                                          title: 'Mood',
                                          fields: {'amount': item.$1},
                                        ),
                                  );
                                }),
                          child: Column(
                            children: [
                              Text(
                                item.$2,
                                style: const TextStyle(fontSize: 34),
                              ),
                              Text(s.t(item.$3, item.$4)),
                            ],
                          ),
                        ),
                      ),
                  ],
                ),
              ],
            ),
          ),
          const MoodChart(),
          Panel(
            child: Column(
              children: [
                ModuleTile(
                  Icons.lock_outline,
                  s.t('Secret journal', 'গোপন ডায়েরি'),
                  s.t(
                    'Encrypted. Protected by your lock.',
                    'এনক্রিপ্ট করা। তোমার লকে সুরক্ষিত।',
                  ),
                  () => openEntries(context, EntryKind.journal),
                ),
                ModuleTile(
                  Icons.chat_bubble_outline,
                  s.t('Talk to Sathi', 'সাথির সঙ্গে কথা বলো'),
                  s.t('Support, not a diagnosis', 'সহায়তা, রোগ নির্ণয় নয়'),
                  () => Navigator.push(
                    context,
                    MaterialPageRoute<void>(builder: (_) => const SathiPage()),
                  ),
                ),
                ModuleTile(
                  Icons.air,
                  s.t('One-minute breathing', 'এক মিনিট শ্বাসের ব্যায়াম'),
                  s.t(
                    'Slow down, one breath at a time',
                    'ধীরে, একবারে একটি শ্বাস',
                  ),
                  () => Navigator.push(
                    context,
                    MaterialPageRoute<void>(
                      builder: (_) => const BreathingPage(),
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SleepSounds(),
          Text(
            s.t(
              'Life Mate is not a therapist or emergency service. If you might hurt yourself or are in immediate danger, contact someone you trust and call 999 in Bangladesh.',
              'Life Mate থেরাপিস্ট বা জরুরি সেবা নয়। নিজের ক্ষতি করার আশঙ্কা বা তাৎক্ষণিক বিপদে বিশ্বস্ত কাউকে জানাও এবং বাংলাদেশে ৯৯৯-এ ফোন করো।',
            ),
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ),
    );
  }
}

class MoodChart extends StatelessWidget {
  const MoodChart({super.key});
  @override
  Widget build(BuildContext context) {
    final s = LifeScope.of(context);
    final now = DateTime.now();
    final moods = s.entries(EntryKind.mood);
    return Panel(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            s.t('Your week, gently observed', 'তোমার সপ্তাহ, নিজের চোখে'),
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 22),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              for (var i = 6; i >= 0; i--)
                Expanded(
                  child: Builder(
                    builder: (context) {
                      final day = now.subtract(Duration(days: i));
                      final mood = moods
                          .where((e) => dayKey(e.date) == dayKey(day))
                          .firstOrNull;
                      final n = mood?.number('amount') ?? 0;
                      return Semantics(
                        label:
                            '${dayKey(day)}: ${n == 0 ? s.t('Not logged', 'তথ্য নেই') : n.toInt()}',
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.end,
                          children: [
                            SizedBox(
                              height: 80,
                              child: Align(
                                alignment: Alignment.bottomCenter,
                                child: Container(
                                  width: 18,
                                  height: n == 0 ? 3 : n * 25,
                                  decoration: BoxDecoration(
                                    color: n == 0
                                        ? Theme.of(
                                            context,
                                          ).colorScheme.outlineVariant
                                        : sage,
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                ),
                              ),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              '${day.day}',
                              style: Theme.of(context).textTheme.bodySmall,
                            ),
                          ],
                        ),
                      );
                    },
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class BreathingPage extends StatefulWidget {
  const BreathingPage({super.key});
  @override
  State<BreathingPage> createState() => _BreathingState();
}

class _BreathingState extends State<BreathingPage> with WidgetsBindingObserver {
  Timer? timer;
  int elapsed = 0;
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state != AppLifecycleState.resumed) {
      timer?.cancel();
      timer = null;
      if (mounted) setState(() {});
    }
  }

  @override
  void dispose() {
    timer?.cancel();
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = LifeScope.of(context);
    final inhale = elapsed % 10 < 4;
    return Scaffold(
      appBar: AppBar(
        title: Text(s.t('A minute for you', 'তোমার জন্য এক মিনিট')),
      ),
      body: PageBody(
        children: [
          const SizedBox(height: 32),
          Center(
            child: AnimatedContainer(
              duration: MediaQuery.disableAnimationsOf(context)
                  ? Duration.zero
                  : const Duration(seconds: 2),
              width: inhale && timer != null ? 230 : 180,
              height: inhale && timer != null ? 230 : 180,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: Theme.of(context).colorScheme.primaryContainer,
                border: Border.all(color: sage, width: 2),
              ),
              alignment: Alignment.center,
              child: Text(
                timer == null
                    ? s.t(
                        elapsed == 60 ? 'Well done' : 'Ready?',
                        elapsed == 60 ? 'হয়ে গেল' : 'প্রস্তুত?',
                      )
                    : s.t(
                        inhale ? 'Breathe in' : 'Breathe out',
                        inhale ? 'শ্বাস নাও' : 'শ্বাস ছাড়ো',
                      ),
                style: Theme.of(context).textTheme.headlineSmall,
              ),
            ),
          ),
          const SizedBox(height: 24),
          Center(
            child: Text(
              '${60 - elapsed} ${s.t('seconds', 'সেকেন্ড')}',
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ),
          FilledButton(
            onPressed: () {
              if (timer != null) {
                timer!.cancel();
                setState(() => timer = null);
                return;
              }
              setState(() => elapsed = 0);
              timer = Timer.periodic(const Duration(seconds: 1), (t) {
                if (!mounted) return;
                setState(() {
                  elapsed++;
                  if (elapsed >= 60) {
                    t.cancel();
                    timer = null;
                  }
                });
              });
              setState(() {});
            },
            child: Text(
              s.t(
                timer == null ? 'Begin' : 'Stop',
                timer == null ? 'শুরু' : 'থামো',
              ),
            ),
          ),
          Text(
            s.t(
              'Breathe comfortably: 4 seconds in, 6 out. No breath holding. Stop if you feel dizzy or uncomfortable.',
              'আরামে শ্বাস নাও: ৪ সেকেন্ড নাও, ৬ সেকেন্ড ছাড়ো। শ্বাস আটকে রেখো না। মাথা ঘোরা বা অস্বস্তি হলে থামো।',
            ),
          ),
        ],
      ),
    );
  }
}

class SleepSounds extends StatefulWidget {
  const SleepSounds({super.key});
  @override
  State<SleepSounds> createState() => _SleepSoundsState();
}

class _SleepSoundsState extends State<SleepSounds> with WidgetsBindingObserver {
  final player = AudioPlayer();
  Timer? stopTimer;
  bool playing = false;
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  Future<void> stop() async {
    stopTimer?.cancel();
    await player.stop();
    if (mounted) setState(() => playing = false);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state != AppLifecycleState.resumed) stop();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    stopTimer?.cancel();
    player.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = LifeScope.of(context);
    return Panel(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            s.t('A soft place to land', 'একটু শান্তির আশ্রয়'),
            style: Theme.of(context).textTheme.titleMedium,
          ),
          Text(
            s.t(
              'Offline rain · 15-minute timer · stops when leaving',
              'অফলাইন বৃষ্টির শব্দ · ১৫ মিনিট · অ্যাপ ছাড়লে থামে',
            ),
          ),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: () => attempt(context, () async {
              if (playing) {
                await stop();
                return;
              }
              await player.setReleaseMode(ReleaseMode.loop);
              await player.setVolume(.35);
              await player.play(AssetSource('rain.wav'));
              stopTimer = Timer(const Duration(minutes: 15), stop);
              if (mounted) setState(() => playing = true);
            }),
            icon: Icon(playing ? Icons.pause : Icons.play_arrow),
            label: Text(
              s.t(
                playing ? 'Pause rain' : 'Play rain',
                playing ? 'থামাও' : 'বৃষ্টির শব্দ শোনো',
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class SathiPage extends StatefulWidget {
  const SathiPage({super.key});
  @override
  State<SathiPage> createState() => _SathiState();
}

class _SathiState extends State<SathiPage> {
  final input = TextEditingController();
  final List<({bool user, String text})> messages = [];
  bool ai = false, busy = false;
  @override
  void dispose() {
    input.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = LifeScope.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(s.t('Sathi', 'সাথি'))),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 22),
            child: SwitchListTile(
              contentPadding: EdgeInsets.zero,
              value: ai,
              onChanged: CloudService.configured && !s.demo && !busy
                  ? (v) => setState(() => ai = v)
                  : null,
              title: Text(s.t('Gemini AI (optional)', 'জেমিনি AI (ঐচ্ছিক)')),
              subtitle: Text(
                s.t(
                  CloudService.configured
                      ? 'Only the message you approve is sent.'
                      : 'Not connected. Offline guided support is available.',
                  CloudService.configured
                      ? 'শুধু অনুমোদিত বার্তাটি পাঠানো হয়।'
                      : 'সংযুক্ত নয়। অফলাইন সহায়তা আছে।',
                ),
              ),
            ),
          ),
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(22),
              children: [
                Panel(
                  color: Theme.of(context).colorScheme.primaryContainer,
                  child: Text(
                    s.t(
                      'I’m Sathi. You do not have to have it all figured out. How are you feeling? I offer support, not medical care.',
                      'আমি সাথি। সবকিছু এখনই ঠিক করে ফেলতে হবে না। তোমার কেমন লাগছে? আমি সহায়তা করি, চিকিৎসা নয়।',
                    ),
                  ),
                ),
                const SizedBox(height: 10),
                ...messages.map(
                  (m) => Padding(
                    padding: EdgeInsets.only(
                      bottom: 12,
                      left: m.user ? 30 : 0,
                      right: m.user ? 0 : 30,
                    ),
                    child: Panel(child: Text(m.text)),
                  ),
                ),
                if (busy) const LinearProgressIndicator(),
              ],
            ),
          ),
          Padding(
            padding: EdgeInsets.fromLTRB(
              18,
              8,
              18,
              MediaQuery.paddingOf(context).bottom + 12,
            ),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: input,
                    maxLength: 2000,
                    minLines: 1,
                    maxLines: 3,
                    decoration: InputDecoration(
                      labelText: s.t(
                        'What’s on your mind?',
                        'তোমার মনে কী চলছে?',
                      ),
                      counterText: '',
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.filled(
                  onPressed: busy
                      ? null
                      : () async {
                          final text = input.text.trim();
                          if (text.isEmpty) return;
                          if (ai &&
                              !await confirm(
                                context,
                                s.t(
                                  'Send this message to Gemini?',
                                  'এই বার্তাটি জেমিনিতে পাঠাবে?',
                                ),
                                s.t(
                                  'Your message may contain personal information. No journal, health records, contacts, photos or chat history are sent.',
                                  'বার্তায় ব্যক্তিগত তথ্য থাকতে পারে। ডায়েরি, স্বাস্থ্যতথ্য, যোগাযোগ, ছবি বা আগের কথোপকথন পাঠানো হয় না।',
                                ),
                              ))
                            return;
                          if (!mounted) return;
                          setState(() {
                            busy = true;
                            messages.add((user: true, text: text));
                            input.clear();
                          });
                          String answer;
                          final crisis =
                              RegExp(
                                r'\b(suicide|kill myself|self.harm)\b',
                                caseSensitive: false,
                              ).hasMatch(text) ||
                              text.contains('আত্মহত্যা') ||
                              text.contains('নিজেকে মেরে');
                          if (crisis) {
                            answer = s.t(
                              'I’m concerned about your safety. Please move away from anything you could use to hurt yourself, contact someone you trust now, and call 999 in Bangladesh if you are in immediate danger. You deserve real human support.',
                              'তোমার নিরাপত্তা নিয়ে চিন্তা হচ্ছে। নিজের ক্ষতি হতে পারে এমন জিনিস থেকে দূরে যাও, এখনই বিশ্বস্ত কাউকে জানাও, তাৎক্ষণিক বিপদে বাংলাদেশে ৯৯৯-এ ফোন করো। তুমি মানুষের সহায়তা পাওয়ার যোগ্য।',
                            );
                          } else if (ai) {
                            try {
                              answer = await CloudService.sathi(
                                text,
                                s.language,
                              );
                            } catch (_) {
                              answer = s.t(
                                'AI is unavailable. Your message was not saved to your journal. You can use offline breathing support.',
                                'AI পাওয়া যাচ্ছে না। বার্তাটি ডায়েরিতে রাখা হয়নি। অফলাইন শ্বাসের ব্যায়াম ব্যবহার করতে পারো।',
                              );
                            }
                          } else {
                            answer = s.t(
                              'Offline guided support — not AI:\nThank you for sharing. Your feelings matter. Try noticing five things you can see, relax your shoulders, and take one comfortable breath. Would writing one small next step or reaching out to a trusted person help?',
                              'অফলাইন নির্দেশিত সহায়তা — AI নয়:\nকথা বলার জন্য ধন্যবাদ। তোমার অনুভূতি গুরুত্বপূর্ণ। দেখা যায় এমন পাঁচটি জিনিস খেয়াল করো, কাঁধ শিথিল করো, আরামে একটি শ্বাস নাও। ছোট একটি পরবর্তী কাজ লেখা বা বিশ্বস্ত কারও সঙ্গে কথা বলা কি সাহায্য করবে?',
                            );
                          }
                          if (mounted)
                            setState(() {
                              messages.add((user: false, text: answer));
                              busy = false;
                            });
                        },
                  icon: const Icon(Icons.arrow_upward),
                  tooltip: s.t('Send', 'পাঠাও'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
