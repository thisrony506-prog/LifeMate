import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:speech_to_text/speech_to_text.dart';
import '../data/entry.dart';
import '../services/native.dart';
import 'common.dart';
import 'entries.dart';
import 'mind.dart';
import 'people.dart';
import 'cloud_settings.dart';
import 'backup_page.dart';
import 'privacy_settings.dart';
import 'updates.dart';

class LifeShell extends StatefulWidget {const LifeShell({super.key});@override State<LifeShell> createState()=>_ShellState();}
class _ShellState extends State<LifeShell> {
  int tab=0;
  @override Widget build(BuildContext context){final s=LifeScope.of(context);final labels=[s.t('Home','হোম'),s.t('My Life','আমার জীবন'),s.t('Health','স্বাস্থ্য'),s.t('Money','হিসাব'),s.t('Profile','প্রোফাইল')];return Scaffold(
    appBar:AppBar(toolbarHeight:66,title:Row(children:[Image.asset('assets/logo.png',width:32,height:32),const SizedBox(width:10),const Text('Life Mate',style:TextStyle(fontSize:20,fontWeight:FontWeight.w600))]),actions:[IconButton(tooltip:s.t('Reminder hub','সব রিমাইন্ডার'),icon:const Icon(Icons.notifications_none),onPressed:()=>Navigator.push(context,MaterialPageRoute<void>(builder:(_)=>const ReminderHub()))),IconButton(tooltip:s.t('Emergency help','জরুরি সহায়তা'),icon:const Icon(Icons.sos_outlined),onPressed:()=>Navigator.push(context,MaterialPageRoute<void>(builder:(_)=>const SosPage())))]),
    body:Column(children:[if(s.demo)MaterialBanner(content:Text(s.t('DEMO · sample data, no saving or real-world actions','ডেমো · নমুনা তথ্য, সংরক্ষণ বা বাস্তব কার্যক্রম নয়')),actions:[TextButton(onPressed:()=>s.setDemo(false),child:Text(s.t('Exit demo','ডেমো বন্ধ')))]),Expanded(child:[const HomePage(),const MyLifePage(),const HealthPage(),const MoneyPage(),const ProfilePage()][tab])]),
    bottomNavigationBar:NavigationBar(selectedIndex:tab,onDestinationSelected:(v)=>setState(()=>tab=v),destinations:[for(var i=0;i<5;i++)NavigationDestination(icon:Icon([Icons.home_outlined,Icons.grid_view_outlined,Icons.favorite_outline,Icons.account_balance_wallet_outlined,Icons.person_outline][i]),label:labels[i])]),
    floatingActionButton:tab==0?FloatingActionButton(tooltip:s.t('Add to your day','দিনে যোগ করো'),onPressed:()=>showModalBottomSheet<void>(context:context,showDragHandle:true,builder:(context)=>SafeArea(child:Padding(padding:const EdgeInsets.fromLTRB(22,0,22,24),child:Wrap(spacing:12,runSpacing:10,children:[for(final k in [EntryKind.task,EntryKind.routine,EntryKind.medicine,EntryKind.expense,EntryKind.memory])ActionChip(avatar:Icon(kindIcon(k)),label:Text(s.label(k)),onPressed:(){Navigator.pop(context);editEntry(this.context,k);})])))),child:const Icon(Icons.add)):null);
  }
}
class HomePage extends StatelessWidget {
  const HomePage({super.key});
  @override Widget build(BuildContext context){final s=LifeScope.of(context);final now=DateTime.now();final tasks=s.entries(EntryKind.task).where((e)=>dayKey(e.date)==dayKey(now)).toList();final focus=tasks.where((e)=>e.fields['focus']==true).take(3).toList();final done=tasks.where((e)=>e.done).length;final routine=s.entries(EntryKind.routine)..sort((a,b)=>a.text('time').compareTo(b.text('time')));final clock='${now.hour.toString().padLeft(2,'0')}:${now.minute.toString().padLeft(2,'0')}';final next=routine.where((e)=>e.text('time').compareTo(clock)>=0).firstOrNull;
    return PageBody(children:[
      Heading('${s.t(now.hour<12?'Good morning':now.hour<17?'Good afternoon':'Good evening',now.hour<12?'শুভ সকাল':now.hour<17?'শুভ অপরাহ্ন':'শুভ সন্ধ্যা')}, ${s.demo?s.t('Rony','রনি'):s.name}.',subtitle:DateFormat('EEEE, d MMMM',s.language).format(now)),
      Panel(color:Theme.of(context).colorScheme.primaryContainer,child:Row(children:[Expanded(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Text(s.t('ONE THING AT A TIME','একবারে একটি কাজ'),style:Theme.of(context).textTheme.labelSmall),const SizedBox(height:10),Text(s.t('A day that feels\na little lighter.','দিনটা হোক\nআরেকটু হালকা।'),style:Theme.of(context).textTheme.headlineSmall),const SizedBox(height:12),Text('$done / ${tasks.length} ${s.t('tasks done','কাজ শেষ')}')])),SizedBox(width:78,height:78,child:Stack(alignment:Alignment.center,children:[SizedBox.expand(child:CircularProgressIndicator(value:tasks.isEmpty?0:done/tasks.length,strokeWidth:6,backgroundColor:Theme.of(context).colorScheme.surface,color:const Color(0xFF4A6D5C))),Text('${tasks.isEmpty?0:(done/tasks.length*100).round()}%',style:Theme.of(context).textTheme.titleMedium)]))])),
      Row(children:[Metric('${s.dailyStreak}',s.t('day streak','দিনের ধারাবাহিকতা'),Icons.local_fire_department_outlined),const SizedBox(width:12),Metric('${s.total(EntryKind.water,today:true).round()} ml',s.t('water today','আজকের পানি'),Icons.water_drop_outlined)]),
      Heading(s.t('Your top 3','তোমার প্রধান ৩টি কাজ'),action:IconButton(tooltip:s.t('Add focus task','ফোকাস কাজ যোগ'),onPressed:()=>editEntry(context,EntryKind.task),icon:const Icon(Icons.add))),
      Panel(padding:const EdgeInsets.symmetric(horizontal:14,vertical:8),child:focus.isEmpty?Padding(padding:const EdgeInsets.all(14),child:Text(s.t('Choose what matters today. Everything else can wait.','আজ গুরুত্বপূর্ণ কাজগুলো বেছে নাও। বাকিগুলো পরে হবে।'))):Column(children:[for(final e in focus)CheckboxListTile(contentPadding:EdgeInsets.zero,controlAffinity:ListTileControlAffinity.leading,title:Text(e.title,style:TextStyle(decoration:e.done?TextDecoration.lineThrough:null)),value:e.done,onChanged:s.demo?null:(_)=>attempt(context,()=>s.toggle(e)))])),
      if(s.reminderIssue!=null) Panel(child:Text(s.reminderIssue!)),
      if(next!=null) Panel(child:Row(children:[const Icon(Icons.schedule_outlined),const SizedBox(width:12),Expanded(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Text(s.t('UP NEXT','এরপরে'),style:Theme.of(context).textTheme.labelSmall),Text(next.title,style:Theme.of(context).textTheme.titleMedium)])),Text(next.text('time'))])),
      ModuleTile(Icons.sentiment_satisfied_alt,s.t('How is your mind today?','আজ তোমার মন কেমন?'),s.t('Take a gentle check-in','নিজের একটু খোঁজ নাও'),()=>Navigator.push(context,MaterialPageRoute<void>(builder:(_)=>const MindPage()))),
      const SizedBox(height:40)
    ]);
  }
}
class MyLifePage extends StatelessWidget {
  const MyLifePage({super.key});
  @override Widget build(BuildContext context){final s=LifeScope.of(context);return PageBody(children:[Heading(s.t('A home for everything.','সবকিছুর একটি ঠিকানা।'),subtitle:s.t('Small systems. More peace of mind.','ছোট্ট কিছু অভ্যাস। মনে আরও শান্তি।')),
    Panel(child:Column(children:[for(final k in [EntryKind.task,EntryKind.routine])ModuleTile(kindIcon(k),s.label(k),s.t('Plan what matters','গুরুত্বপূর্ণ বিষয় সাজাও'),()=>openEntries(context,k)),ModuleTile(Icons.spa_outlined,s.t('Mind Mate','মন মেট'),s.t('Mood, journal, Sathi & breathing','মন, ডায়েরি, সাথি ও শ্বাসের ব্যায়াম'),()=>Navigator.push(context,MaterialPageRoute<void>(builder:(_)=>const MindPage()))),ModuleTile(Icons.people_outline,s.t('Family & people','পরিবার ও প্রিয়জন'),s.t('Contacts, birthdays & voluntary sharing','যোগাযোগ, জন্মদিন ও স্বেচ্ছায় শেয়ার'),()=>Navigator.push(context,MaterialPageRoute<void>(builder:(_)=>const PeoplePage())))])),
    Panel(child:Column(children:[for(final k in [EntryKind.memory,EntryKind.goal])ModuleTile(kindIcon(k),s.label(k),s.t('Keep what means something','মূল্যবান স্মৃতি ও স্বপ্ন রাখো'),()=>openEntries(context,k))])),
    Heading(s.t('Routine starters','রুটিনের শুরু')),Wrap(spacing:8,children:[for(final label in [s.t('Prayer','নামাজ'),s.t('Class','ক্লাস'),s.t('Office','অফিস'),s.t('Drink water','পানি পান')])ActionChip(label:Text(label),onPressed:()=>editEntry(context,EntryKind.routine,Entry(kind:EntryKind.routine,title:label)))]),Text(s.t('Prayer times are entered by you; verify them with your local mosque/timetable. No guessed prayer schedule is used.','নামাজের সময় নিজে দাও; স্থানীয় মসজিদ/সময়সূচির সঙ্গে মিলিয়ে নাও। অনুমান করে সময় দেওয়া হয় না।'),style:Theme.of(context).textTheme.bodySmall),
  ]);}
}
class HealthPage extends StatelessWidget {
  const HealthPage({super.key});
  @override Widget build(BuildContext context){final s=LifeScope.of(context);final sleep=s.entries(EntryKind.sleep).firstOrNull;final cycle=s.entries(EntryKind.period).firstOrNull;return PageBody(children:[Heading(s.t('Care, not pressure.','যত্ন, চাপ নয়।'),subtitle:s.t('Notice your body’s everyday rhythm.','শরীরের প্রতিদিনের ছন্দ খেয়াল করো।')),
    Row(children:[Metric('${s.total(EntryKind.water,today:true).round()} ml',s.t('water today','আজকের পানি'),Icons.water_drop_outlined),const SizedBox(width:12),Metric(sleep==null?'—':'${sleep.number('amount')} h',s.t('last sleep','শেষ ঘুম'),Icons.bedtime_outlined)]),
    FilledButton.icon(onPressed:s.demo?null:()=>attempt(context,()=>s.save(Entry(kind:EntryKind.water,title:s.t('Glass of water','এক গ্লাস পানি'),fields:{'amount':250}))),icon:const Icon(Icons.add),label:Text(s.t('Log a glass · 250 ml','এক গ্লাস যোগ · ২৫০ মিলি'))),
    Panel(child:Column(children:[for(final k in [EntryKind.water,EntryKind.sleep,EntryKind.workout,EntryKind.period,EntryKind.medicine])ModuleTile(kindIcon(k),s.label(k),k==EntryKind.medicine?s.t('Encrypted prescription photo & reminders','এনক্রিপ্ট করা প্রেসক্রিপশন ছবি ও রিমাইন্ডার'):s.t('Private logs, at your pace','নিজের গতিতে ব্যক্তিগত হিসাব'),()=>openEntries(context,k))])),
    if(cycle!=null&&cycle.number('amount')>=15) Panel(child:Text('${s.t('Estimated next period','পরবর্তী পিরিয়ডের আনুমানিক সময়')}: ${DateFormat.yMMMd(s.language).format(cycle.date.add(Duration(days:cycle.number('amount').round())))}\n${s.t('An estimate only, not contraception or medical advice.','শুধু অনুমান, জন্মনিয়ন্ত্রণ বা চিকিৎসা পরামর্শ নয়।')}')),
    Text(s.t('Health logs stay on your device. Medicine times must follow your prescription. Seek medical advice for health concerns.','স্বাস্থ্যতথ্য এই ডিভাইসেই থাকে। প্রেসক্রিপশন অনুযায়ী ওষুধের সময় দাও। স্বাস্থ্যসমস্যায় চিকিৎসকের পরামর্শ নাও।'),style:Theme.of(context).textTheme.bodySmall)
  ]);}
}
class MoneyPage extends StatelessWidget {
  const MoneyPage({super.key});
  @override Widget build(BuildContext context){final s=LifeScope.of(context);final income=s.total(EntryKind.income,month:true),expense=s.total(EntryKind.expense,month:true);final f=NumberFormat.decimalPattern(s.language);final biggest=income>expense?income:expense;return PageBody(children:[Heading(s.t('Money, made simple.','হিসাব থাকুক সহজ।'),subtitle:s.t('A clear picture. No judgment.','হিসাব পরিষ্কার। বিচার নয়।')),
    Panel(color:Theme.of(context).colorScheme.primaryContainer,child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Text(s.t('THIS MONTH · NET','এই মাস · নিট'),style:Theme.of(context).textTheme.labelSmall),const SizedBox(height:10),Text('৳ ${f.format(income-expense)}',style:Theme.of(context).textTheme.headlineLarge),const SizedBox(height:20),for(final row in [(income,s.t('Income','আয়')),(expense,s.t('Expense','খরচ'))])Padding(padding:const EdgeInsets.only(bottom:12),child:Column(children:[Row(mainAxisAlignment:MainAxisAlignment.spaceBetween,children:[Text(row.$2),Text('৳ ${f.format(row.$1)}')]),const SizedBox(height:6),LinearProgressIndicator(value:biggest==0?0:row.$1/biggest,color:const Color(0xFF4A6D5C),backgroundColor:Theme.of(context).colorScheme.surface)]))])),
    FilledButton.icon(onPressed:s.demo?null:()=>Navigator.push(context,MaterialPageRoute<void>(builder:(_)=>const QuickExpense())),icon:const Icon(Icons.mic_none),label:Text(s.t('Quick expense · type or speak','দ্রুত খরচ · লেখো বা বলো'))),
    Panel(child:Column(children:[for(final k in [EntryKind.expense,EntryKind.income,EntryKind.saving,EntryKind.bill])ModuleTile(kindIcon(k),s.label(k),s.t('Simple, offline and encrypted','সহজ, অফলাইন ও এনক্রিপ্ট করা'),()=>openEntries(context,k))])),
    for(final e in s.entries(EntryKind.saving).take(3))Panel(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Text(e.title,style:Theme.of(context).textTheme.titleMedium),const SizedBox(height:12),LinearProgressIndicator(value:e.number('target')<=0?0:(e.number('amount')/e.number('target')).clamp(0,1).toDouble()),const SizedBox(height:8),Text('৳ ${f.format(e.number('amount'))} / ${f.format(e.number('target'))}')]))
  ]);}
}
class QuickExpense extends StatefulWidget {const QuickExpense({super.key});@override State<QuickExpense> createState()=>_ExpenseState();}
class _ExpenseState extends State<QuickExpense> with WidgetsBindingObserver {
  final input=TextEditingController(),speech=SpeechToText();bool listening=false,busy=false;
  @override void initState(){super.initState();WidgetsBinding.instance.addObserver(this);}
  @override void didChangeAppLifecycleState(AppLifecycleState state){if(state!=AppLifecycleState.resumed){speech.cancel();if(mounted)setState(()=>listening=false);}}
  @override void dispose(){WidgetsBinding.instance.removeObserver(this);speech.cancel();input.dispose();super.dispose();}
  @override Widget build(BuildContext context){final s=LifeScope.of(context);final parsed=parseExpense(input.text);return Scaffold(appBar:AppBar(title:Text(s.t('Quick expense','দ্রুত খরচ'))),body:PageBody(children:[Text(s.t('Try “120 taka rickshaw”. Always review before saving.','“১২০ টাকা রিকশা” লেখো বা বলো। সংরক্ষণের আগে দেখে নাও।')),TextField(controller:input,maxLength:200,onChanged:(_)=>setState((){}),decoration:InputDecoration(labelText:s.t('Amount and description','পরিমাণ ও বিবরণ'))),
    OutlinedButton.icon(onPressed:s.demo?null:()async{if(listening){await speech.stop();if(mounted)setState(()=>listening=false);return;}if(!await confirm(context,s.t('Use the microphone?','মাইক্রোফোন ব্যবহার করবে?'),s.t('On-device recognition is requested, but your system speech provider may process audio online. Type instead for guaranteed offline entry.','ডিভাইসে শনাক্তকরণ চাওয়া হয়, তবে সিস্টেমের ভয়েস সেবা অনলাইনে অডিও প্রক্রিয়া করতে পারে। নিশ্চিত অফলাইন তথ্য দিতে লেখো।')))return;try{final available=await speech.initialize(onError:(_){if(mounted)setState(()=>listening=false);},onStatus:(v){if(v!='listening'&&mounted)setState(()=>listening=false);});if(!available)throw StateError('Unavailable');final locales=await speech.locales();final locale=locales.where((v)=>v.localeId.startsWith(s.language)).firstOrNull;if(locale==null)throw StateError('Language unavailable');await speech.listen(localeId:locale.localeId,listenFor:const Duration(seconds:10),listenOptions:SpeechListenOptions(onDevice:true,partialResults:false),onResult:(r){if(mounted)setState(()=>input.text=r.recognizedWords);});if(mounted)setState(()=>listening=true);}catch(_){if(context.mounted)message(context,s.t('Offline speech is unavailable. You can type the same expense.','অফলাইন ভয়েস পাওয়া যাচ্ছে না। একই খরচ লিখে দিতে পারো।'));}},icon:Icon(listening?Icons.stop:Icons.mic_none),label:Text(s.t(listening?'Stop listening':'Speak on device',listening?'শোনা বন্ধ':'ডিভাইসে বলো'))),
    if(parsed!=null)Panel(child:Text('৳ ${parsed.amount.toStringAsFixed(2)} · ${parsed.description}')),
    FilledButton(onPressed:parsed==null||busy||s.demo?null:()async{setState(()=>busy=true);try{await speech.stop();await s.save(Entry(kind:EntryKind.expense,title:parsed.description,fields:{'amount':parsed.amount}));if(context.mounted)Navigator.pop(context);}catch(_){if(context.mounted)message(context,s.t('Could not save.','সংরক্ষণ হয়নি।'));}finally{if(mounted)setState(()=>busy=false);}},child:Text(s.t('Confirm & save expense','নিশ্চিত করে খরচ রাখো')))
  ]));}
}
class ProfilePage extends StatelessWidget {
  const ProfilePage({super.key});
  @override Widget build(BuildContext context){final s=LifeScope.of(context);return PageBody(children:[Heading(s.demo?s.t('Rony · Demo','রনি · ডেমো'):s.name,subtitle:s.t('Your Life, Organized in One App','তোমার জীবন, এক অ্যাপে গোছানো')),
    Panel(child:Row(children:[const Icon(Icons.lock_outline),const SizedBox(width:12),Expanded(child:Text(s.t('Your data stays yours, encrypted. Offline by default. Cloud and AI only by choice.','তোমার তথ্য তোমারই, এনক্রিপ্ট করা। শুরুতে অফলাইন। ক্লাউড ও AI শুধু নিজের ইচ্ছায়।')))])),
    Heading(s.t('Make it yours','নিজের মতো সাজাও')),SegmentedButton<String>(segments:const [ButtonSegment(value:'en',label:Text('English')),ButtonSegment(value:'bn',label:Text('বাংলা'))],selected:{s.language},onSelectionChanged:(v)=>s.setLanguage(v.first)),
    SegmentedButton<String>(segments:[ButtonSegment(value:'System',label:Text(s.t('System','সিস্টেম'))),ButtonSegment(value:'Light',label:Text(s.t('Light','হালকা'))),ButtonSegment(value:'Dark',label:Text(s.t('Dark','ডার্ক')))],selected:{s.appearance},onSelectionChanged:(v)=>attempt(context,()=>s.setAppearance(v.first))),
    SwitchListTile(contentPadding:EdgeInsets.zero,title:Text(s.t('Explore Demo mode','ডেমো মোড দেখো')),subtitle:Text(s.t('Sample data is separate and never saved or uploaded.','নমুনা তথ্য আলাদা, সংরক্ষণ বা আপলোড হয় না।')),value:s.demo,onChanged:s.setDemo),
    Panel(child:Column(children:[ModuleTile(Icons.enhanced_encryption_outlined,s.t('Encrypted vault backup','এনক্রিপ্ট করা ভল্ট ব্যাকআপ'),s.t('New records and photos · portable passphrase backup','নতুন তথ্য ও ছবি · পাসফ্রেজের ব্যাকআপ'),()=>Navigator.push(context,MaterialPageRoute<void>(builder:(_)=>const BackupPage()))),ModuleTile(Icons.cloud_outlined,s.t('Optional encrypted sync','ঐচ্ছিক এনক্রিপ্ট করা সিঙ্ক'),s.t('Firebase account & recovery phrase','Firebase অ্যাকাউন্ট ও রিকভারি বাক্য'),()=>Navigator.push(context,MaterialPageRoute<void>(builder:(_)=>const CloudSettings()))),
      ModuleTile(Icons.security_outlined,s.t('Profile & privacy','প্রোফাইল ও গোপনীয়তা'),s.t('Name, device app lock & local data controls','নাম, ডিভাইস অ্যাপ লক ও স্থানীয় তথ্য নিয়ন্ত্রণ'),()=>Navigator.push(context,MaterialPageRoute<void>(builder:(_)=>const PrivacySettings()))),
      if(NativeBridge.android) ModuleTile(Icons.system_update_outlined,s.t('Check for updates','আপডেট পরীক্ষা'),s.t('Verified private download & Android installation','যাচাই করা ব্যক্তিগত ডাউনলোড ও Android ইনস্টল'),()=>Navigator.push(context,MaterialPageRoute<void>(builder:(_)=>const UpdatePage()))),

    ]))
  ]);}
}
class ReminderHub extends StatelessWidget {
  const ReminderHub({super.key});
  @override Widget build(BuildContext context){final s=LifeScope.of(context);final entries=s.all.where((e)=>!e.deleted&&e.text('time').isNotEmpty).toList()..sort((a,b)=>a.text('time').compareTo(b.text('time')));return Scaffold(appBar:AppBar(title:Text(s.t('Reminder hub','সব রিমাইন্ডার'))),body:PageBody(children:[Text(s.t('Medicine, bills, routines and calls — together. Android notification permission and battery settings can affect delivery.','ওষুধ, বিল, রুটিন ও কল — এক জায়গায়। Android অনুমতি ও ব্যাটারি সেটিংসে পৌঁছানো প্রভাবিত হতে পারে।')),for(final e in entries)ModuleTile(kindIcon(e.kind),e.title,e.text('time'),()=>editEntry(context,e.kind,e)),if(entries.isEmpty)Text(s.t('No reminders yet. Add a time to any task, medicine, bill or routine.','এখনও রিমাইন্ডার নেই। কাজ, ওষুধ, বিল বা রুটিনে সময় যোগ করো।'))]));}
}
