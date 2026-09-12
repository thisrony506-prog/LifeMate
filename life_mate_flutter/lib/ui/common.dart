import 'package:flutter/material.dart';
import '../data/store.dart';
import '../data/entry.dart';

const sage = Color(0xFFA8C3B9);
const ink = Color(0xFF1A1A1A);
class LifeScope extends InheritedNotifier<LifeStore> {
  const LifeScope({super.key, required LifeStore store, required super.child}) : super(notifier: store);
  static LifeStore of(BuildContext context) => context.dependOnInheritedWidgetOfExactType<LifeScope>()!.notifier!;
}
ThemeData lifeTheme(bool dark) {
  final scheme = ColorScheme.fromSeed(seedColor: sage, brightness: dark ? Brightness.dark : Brightness.light).copyWith(
    primary: sage, onPrimary: ink, primaryContainer: dark ? const Color(0xFF263A32) : const Color(0xFFEDF4F0),
    onPrimaryContainer: dark ? Colors.white : ink,
    surface: dark ? const Color(0xFF151C18) : Colors.white,
    onSurface: dark ? const Color(0xFFEAF0EC) : ink,
    outlineVariant: dark ? const Color(0xFF35443B) : const Color(0xFFE4ECE6),
    error: dark ? const Color(0xFFE9CA9A) : const Color(0xFF785F32));
  return ThemeData(useMaterial3: true, colorScheme: scheme, scaffoldBackgroundColor: scheme.surface,
    visualDensity: VisualDensity.standard,
    appBarTheme: AppBarTheme(backgroundColor: scheme.surface, foregroundColor: scheme.onSurface, surfaceTintColor: Colors.transparent),
    inputDecorationTheme: InputDecorationTheme(filled: true, fillColor: scheme.primaryContainer.withValues(alpha: .5), border: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide.none), contentPadding: const EdgeInsets.all(16)),
    filledButtonTheme: FilledButtonThemeData(style: FilledButton.styleFrom(minimumSize: const Size(48, 50), foregroundColor: ink, backgroundColor: sage, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)))),
    navigationBarTheme: NavigationBarThemeData(indicatorColor: scheme.primaryContainer, backgroundColor: scheme.surface, surfaceTintColor: Colors.transparent),
    textTheme: const TextTheme(headlineLarge: TextStyle(fontSize: 30, fontWeight: FontWeight.w600, height: 1.2), headlineSmall: TextStyle(fontSize: 23, fontWeight: FontWeight.w600), titleMedium: TextStyle(fontSize: 17, fontWeight: FontWeight.w600), bodyMedium: TextStyle(fontSize: 15, height: 1.5)));
}
class Panel extends StatelessWidget {
  final Widget child; final Color? color; final EdgeInsets padding;
  const Panel({super.key, required this.child, this.color, this.padding = const EdgeInsets.all(20)});
  @override Widget build(BuildContext context) => Container(width: double.infinity, padding: padding,
    decoration: BoxDecoration(color: color ?? Theme.of(context).colorScheme.surface, borderRadius: BorderRadius.circular(22), border: Border.all(color: Theme.of(context).colorScheme.outlineVariant)), child: child);
}
class PageBody extends StatelessWidget {
  final List<Widget> children;
  const PageBody({super.key, required this.children});
  @override Widget build(BuildContext context) => Center(child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 850), child: ListView.separated(padding: const EdgeInsets.fromLTRB(22, 14, 22, 28), itemCount: children.length, separatorBuilder: (_,__) => const SizedBox(height: 18), itemBuilder: (_,i) => children[i])));
}
class Heading extends StatelessWidget {
  final String title; final String? subtitle; final Widget? action;
  const Heading(this.title, {super.key, this.subtitle, this.action});
  @override Widget build(BuildContext context) => Row(crossAxisAlignment: CrossAxisAlignment.start, children: [Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: Theme.of(context).textTheme.headlineSmall), if(subtitle != null) Padding(padding: const EdgeInsets.only(top: 5), child: Text(subtitle!, style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant)))])), if(action != null) action!]);
}
class Metric extends StatelessWidget {
  final String value, label; final IconData icon;
  const Metric(this.value, this.label, this.icon, {super.key});
  @override Widget build(BuildContext context) => Expanded(child: Panel(padding: const EdgeInsets.all(16), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Icon(icon, size: 22), const SizedBox(height: 14), Text(value, style: Theme.of(context).textTheme.headlineSmall), Text(label, style: Theme.of(context).textTheme.bodySmall)])));
}
class ModuleTile extends StatelessWidget {
  final IconData icon; final String title, subtitle; final VoidCallback onTap;
  const ModuleTile(this.icon, this.title, this.subtitle, this.onTap, {super.key});
  @override Widget build(BuildContext context) => Material(color: Colors.transparent, child: InkWell(borderRadius: BorderRadius.circular(18), onTap: onTap,
    child: Padding(padding: const EdgeInsets.symmetric(vertical: 14), child: Row(children: [Container(padding: const EdgeInsets.all(12), decoration: BoxDecoration(color: Theme.of(context).colorScheme.primaryContainer, borderRadius: BorderRadius.circular(15)), child: Icon(icon, color: Theme.of(context).colorScheme.onPrimaryContainer)), const SizedBox(width: 14), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: Theme.of(context).textTheme.titleMedium), if(subtitle.isNotEmpty) Text(subtitle, style: Theme.of(context).textTheme.bodySmall)])), const Icon(Icons.chevron_right_rounded)]))));
}
IconData kindIcon(EntryKind kind) => switch(kind) {
  EntryKind.task => Icons.check_circle_outline, EntryKind.routine => Icons.schedule_outlined, EntryKind.mood => Icons.sentiment_satisfied_alt,
  EntryKind.journal => Icons.lock_outline, EntryKind.water => Icons.water_drop_outlined, EntryKind.sleep => Icons.bedtime_outlined,
  EntryKind.period => Icons.calendar_month_outlined, EntryKind.workout => Icons.fitness_center, EntryKind.medicine => Icons.medication_outlined,
  EntryKind.expense => Icons.north_east, EntryKind.income => Icons.south_west, EntryKind.saving => Icons.savings_outlined,
  EntryKind.bill => Icons.receipt_long_outlined, EntryKind.contact => Icons.people_outline, EntryKind.birthday => Icons.cake_outlined,
  EntryKind.anniversary => Icons.event_outlined, EntryKind.memory => Icons.photo_library_outlined, EntryKind.goal => Icons.landscape_outlined
};
void message(BuildContext context, String text) { if(context.mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text))); }
Future<void> attempt(BuildContext context, Future<void> Function() action) async {
  try { await action(); } catch (_) { if(context.mounted) message(context, LifeScope.of(context).t('Could not complete that action. Your saved data is unchanged. Check permissions or connection.', 'কাজটি সম্পন্ন হয়নি। সংরক্ষিত তথ্য অক্ষত আছে। অনুমতি বা সংযোগ পরীক্ষা করো।')); }
}
Future<bool> confirm(BuildContext context, String title, String body, {String? action}) async {
  final s=LifeScope.of(context);
  return await showDialog<bool>(context: context, builder: (context) => AlertDialog(title: Text(title), content: Text(body), actions: [TextButton(onPressed: ()=>Navigator.pop(context,false), child: Text(s.t('Cancel','বাতিল'))), FilledButton(onPressed: ()=>Navigator.pop(context,true), child: Text(action ?? s.t('Continue','চালিয়ে যাও')))])) ?? false;
}
