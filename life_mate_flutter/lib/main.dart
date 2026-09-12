import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'data/store.dart';
import 'data/vault.dart';
import 'services/native.dart';
import 'ui/common.dart';
import 'ui/shell.dart';
import 'ui/entries.dart';
final lifeNavigator=GlobalKey<NavigatorState>();
void openPending(LifeStore store) {
  final id=store.pendingEntry; if(id==null||store.demo)return; store.pendingEntry=null;
  final entry=store.all.where((e)=>e.id==id&&!e.deleted).firstOrNull;
  if(entry!=null) lifeNavigator.currentState?.push(MaterialPageRoute<void>(builder:(_)=>EntryEditor(entry.kind,entry:entry)));
}


Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    final store=LifeStore(await Vault.open());
    NativeBridge.channel.setMethodCallHandler((call) async {
      if(call.method=='erase') { await store.erase(); return true; }
      if(call.method=='refresh') { await store.refreshNative(); WidgetsBinding.instance.addPostFrameCallback((_)=>openPending(store)); return true; }
      return null;
    });
    await store.load();
    runApp(LifeMateApp(store: store));
    WidgetsBinding.instance.addPostFrameCallback((_)=>openPending(store));
  } catch (_) {
    runApp(MaterialApp(theme: lifeTheme(false), home: const Scaffold(body: SafeArea(child: Center(child: Padding(padding: EdgeInsets.all(28), child: Text('Your encrypted data could not be opened. It has not been replaced. Restart Life Mate or restore a backup.\n\nএনক্রিপ্ট করা তথ্য খোলা যায়নি। কোনো তথ্য মুছে ফেলা হয়নি। Life Mate আবার খোলো বা ব্যাকআপ ফিরিয়ে আনো।')))))));
  }
}
class LifeMateApp extends StatelessWidget {
  final LifeStore store;
  const LifeMateApp({super.key, required this.store});
  @override Widget build(BuildContext context) => AnimatedBuilder(animation: store, builder: (_,__) => LifeScope(store: store, child: MaterialApp(
    navigatorKey:lifeNavigator, title: 'Life Mate', debugShowCheckedModeBanner: false, theme: lifeTheme(false), darkTheme: lifeTheme(true),
    themeMode: store.appearance=='Dark' ? ThemeMode.dark : store.appearance=='Light' ? ThemeMode.light : ThemeMode.system,
    locale: Locale(store.language), supportedLocales: const [Locale('en'),Locale('bn')],
    localizationsDelegates: GlobalMaterialLocalizations.delegates,
    home: store.onboarded ? const LifeShell() : const WelcomeFlow())));
}
class WelcomeFlow extends StatefulWidget {
  const WelcomeFlow({super.key});
  @override State<WelcomeFlow> createState() => _WelcomeFlowState();
}
class _WelcomeFlowState extends State<WelcomeFlow> {
  final name=TextEditingController(); int step=0; TimeOfDay wake=const TimeOfDay(hour:7,minute:0); bool busy=false;
  @override void dispose(){ name.dispose(); super.dispose(); }
  @override Widget build(BuildContext context) {
    final s=LifeScope.of(context);
    return Scaffold(body: SafeArea(child: PageBody(children: [const SizedBox(height:36), Align(alignment:Alignment.centerLeft,child:Image.asset('assets/logo.png',width:64,height:64)),
      Text('Life Mate',style:Theme.of(context).textTheme.headlineLarge), Text(s.t('Your Life, Organized in One App','তোমার জীবন, এক অ্যাপে গোছানো'),style:Theme.of(context).textTheme.titleMedium),
      Text(s.t('A little less noise. A little more room for you.','একটু কম ব্যস্ততা। নিজের জন্য একটু বেশি সময়।')),
      if(step==0) ...[Heading(s.t('Make yourself at home','নিজের মতো করে শুরু করো')), SegmentedButton<String>(segments:const [ButtonSegment(value:'en',label:Text('English')),ButtonSegment(value:'bn',label:Text('বাংলা'))],selected:{s.language},onSelectionChanged:(v)=>s.setLanguage(v.first)),
        FilledButton(onPressed:()=>setState(()=>step=1),child:Text(s.t('Continue','পরবর্তী')))]
      else ...[Heading(s.t('Start with one small routine','ছোট্ট একটি রুটিন দিয়ে শুরু')), TextField(controller:name,maxLength:60,decoration:InputDecoration(labelText:s.t('What should we call you?','তোমাকে কী নামে ডাকব?'))),
        ListTile(leading:const Icon(Icons.wb_sunny_outlined),title:Text(s.t('Wake-up time','ঘুম থেকে ওঠার সময়')),trailing:Text(wake.format(context)),onTap:() async {final v=await showTimePicker(context:context,initialTime:wake);if(v!=null&&mounted)setState(()=>wake=v);}),
        FilledButton(onPressed:busy?null:() async {setState(()=>busy=true);try{await s.finishOnboarding(name.text,'${wake.hour.toString().padLeft(2,'0')}:${wake.minute.toString().padLeft(2,'0')}');}catch(_){if(context.mounted)message(context,s.t('Could not save. Please retry.','সংরক্ষণ হয়নি। আবার চেষ্টা করো।'));}finally{if(mounted)setState(()=>busy=false);}},child:Text(s.t('Organize my day','আমার দিন সাজাও')))],
      Panel(child:Row(children:[const Icon(Icons.lock_outline,size:20),const SizedBox(width:12),Expanded(child:Text(s.t('Your data stays yours, encrypted. No account needed.','তোমার তথ্য তোমারই, এনক্রিপ্ট করা। অ্যাকাউন্টের প্রয়োজন নেই।')))])),
      Text(s.t('Life Mate is a personal life companion. No matchmaking.','Life Mate ব্যক্তিগত জীবন গোছানোর সঙ্গী। বিয়ের সম্বন্ধ খোঁজার অ্যাপ নয়।'),style:Theme.of(context).textTheme.bodySmall)
    ])));
  }
}
