import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';
import '../data/entry.dart';
import '../services/location.dart';
import '../services/cloud.dart';
import 'common.dart';
import 'entries.dart';

class PeoplePage extends StatelessWidget {
  const PeoplePage({super.key});
  @override Widget build(BuildContext context){final s=LifeScope.of(context);return Scaffold(appBar:AppBar(title:Text(s.t('Family & people','পরিবার ও প্রিয়জন'))),body:PageBody(children:[
    Heading(s.t('Keep your people close.','প্রিয়জনদের কাছে রাখো।'),subtitle:s.t('Care, without keeping tabs.','যত্ন, নজরদারি নয়।')),
    Panel(child:Column(children:[for(final k in [EntryKind.contact,EntryKind.birthday,EntryKind.anniversary])ModuleTile(kindIcon(k),s.label(k),s.t('Private contacts and important dates','ব্যক্তিগত যোগাযোগ ও গুরুত্বপূর্ণ তারিখ'),()=>openEntries(context,k))])),
    for(final e in s.entries(EntryKind.contact).take(6)) Panel(child:ListTile(contentPadding:EdgeInsets.zero,title:Text(e.title),subtitle:Text(e.text('phone')),trailing:IconButton(tooltip:s.t('Call','ফোন করো'),onPressed:s.demo?null:()=>attempt(context,()async{if(!await launchUrl(Uri(scheme:'tel',path:e.text('phone'))))throw StateError('No dialer');}),icon:const Icon(Icons.call_outlined)))),
    ModuleTile(Icons.location_on_outlined,s.t('Share live location','লাইভ অবস্থান শেয়ার'),s.t('Opt-in · foreground only · expires in 15 minutes','নিজের অনুমতিতে · অ্যাপ খোলা থাকলে · ১৫ মিনিটে শেষ'),()=>Navigator.push(context,MaterialPageRoute<void>(builder:(_)=>const FamilyLocationPage()))),
    ModuleTile(Icons.sos_outlined,s.t('Emergency help','জরুরি সহায়তা'),s.t('Up to 3 trusted contacts','সর্বোচ্চ ৩ জন বিশ্বস্ত যোগাযোগ'),()=>Navigator.push(context,MaterialPageRoute<void>(builder:(_)=>const SosPage())))
  ]));}
}
class SosPage extends StatefulWidget {const SosPage({super.key});@override State<SosPage> createState()=>_SosState();}
class _SosState extends State<SosPage> {
  String? location;bool locating=false;
  @override Widget build(BuildContext context){final s=LifeScope.of(context);final contacts=s.entries(EntryKind.contact).where((e)=>e.fields['emergency']==true).take(3).toList();return Scaffold(appBar:AppBar(title:Text(s.t('Emergency help','জরুরি সহায়তা'))),body:PageBody(children:[
    Panel(color:Theme.of(context).colorScheme.primaryContainer,child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[const Icon(Icons.sos_outlined,size:40),const SizedBox(height:14),Heading(s.t('Reach a real person.','মানুষের সাহায্য নাও।')),Text(s.t('Life Mate cannot guarantee delivery. SMS needs cellular service and may cost money. Calls and messages require system confirmation; three simultaneous calls are not possible.','Life Mate পৌঁছানোর নিশ্চয়তা দিতে পারে না। SMS-এর জন্য মোবাইল নেটওয়ার্ক লাগে ও খরচ হতে পারে। কল ও বার্তায় সিস্টেমের অনুমোদন প্রয়োজন; একসঙ্গে তিনটি কল সম্ভব নয়।'))])),
    FilledButton.icon(onPressed:s.demo?null:()=>attempt(context,()async{if(!await launchUrl(Uri(scheme:'tel',path:'999')))throw StateError('No dialer');}),icon:const Icon(Icons.call_outlined),label:Text(s.t('Call 999 · Bangladesh','৯৯৯-এ ফোন · বাংলাদেশ'))),
    if(contacts.isEmpty) TextButton(onPressed:()=>openEntries(context,EntryKind.contact),child:Text(s.t('Set up emergency contacts first','আগে জরুরি যোগাযোগ যোগ করো'))),
    OutlinedButton.icon(onPressed:locating||s.demo?null:()async{setState(()=>locating=true);try{final p=await FamilyShare.position();if(mounted)setState(()=>location='https://maps.google.com/?q=${p.latitude},${p.longitude}');}catch(_){if(context.mounted)message(context,s.t('Could not get a current location. You can still call or send a message without it.','বর্তমান অবস্থান পাওয়া যায়নি। অবস্থান ছাড়াও কল বা বার্তা দিতে পারো।'));}finally{if(mounted)setState(()=>locating=false);}},icon:const Icon(Icons.my_location),label:Text(s.t(locating?'Getting location…':'Attach current location',locating?'অবস্থান নেওয়া হচ্ছে…':'বর্তমান অবস্থান যুক্ত করো'))),
    if(location!=null) Text(s.t('A location snapshot is attached, not continuous tracking.','বর্তমান অবস্থানের একবারের তথ্য যুক্ত আছে, চলমান ট্র্যাকিং নয়।')),
    FilledButton.icon(onPressed:contacts.isEmpty||s.demo?null:()=>attempt(context,()async{final body=s.t('I need help. Please contact me.','আমার সাহায্য প্রয়োজন। দয়া করে যোগাযোগ করো।')+(location==null?'':'\n$location');final uri=Uri(scheme:'sms',path:contacts.map((e)=>e.text('phone')).join(Platform.isIOS?',':';'),query:'body=${Uri.encodeComponent(body)}');if(!await launchUrl(uri))throw StateError('No SMS app');}),icon:const Icon(Icons.sms_outlined),label:Text(s.t('Open SMS to my contacts','প্রিয়জনদের SMS খোলো'))),
    for(final e in contacts) OutlinedButton.icon(onPressed:()=>attempt(context,()async{if(!await launchUrl(Uri(scheme:'tel',path:e.text('phone'))))throw StateError('No dialer');}),icon:const Icon(Icons.call_outlined),label:Text('${s.t('Call','ফোন করো')} ${e.title}'))
  ]));}
}
class FamilyLocationPage extends StatefulWidget {const FamilyLocationPage({super.key});@override State<FamilyLocationPage> createState()=>_LocationState();}
class _LocationState extends State<FamilyLocationPage> with WidgetsBindingObserver {
  Timer? expiryTimer; final share=FamilyShare();final invite=TextEditingController();String? invitation;StreamSubscription<Map<String,dynamic>?>? subscription;Map<String,dynamic>? point;bool busy=false;
  @override void initState(){super.initState();WidgetsBinding.instance.addObserver(this);expiryTimer=Timer.periodic(const Duration(seconds:1),(_){if(!mounted)return;final until=DateTime.tryParse(point?['expires']?.toString()??'');if(until!=null&&DateTime.now().isAfter(until))setState(()=>point=null);if(invitation!=null&&!share.active)setState(()=>invitation=null);});}
  @override void didChangeAppLifecycleState(AppLifecycleState state){if(state==AppLifecycleState.paused||state==AppLifecycleState.detached){share.stop().then((_){if(mounted)setState(()=>invitation=null);});}}
  @override void dispose(){expiryTimer?.cancel();WidgetsBinding.instance.removeObserver(this);share.stop();subscription?.cancel();invite.dispose();super.dispose();}
  @override Widget build(BuildContext context){final s=LifeScope.of(context);return Scaffold(appBar:AppBar(title:Text(s.t('Location, by choice','অবস্থান, নিজের ইচ্ছায়'))),body:PageBody(children:[
    Text(s.t('Sharing is off by default. A secret invitation allows its holder to see your encrypted location. Share it only with someone you trust. Keep this screen open: updates stop when you leave the app and access expires after 15 minutes.','শেয়ারিং শুরুতে বন্ধ। গোপন আমন্ত্রণ যার কাছে থাকবে সে তোমার এনক্রিপ্ট করা অবস্থান দেখতে পারবে। শুধু বিশ্বস্ত কাউকে দাও। এই স্ক্রিন খোলা রাখো: অ্যাপ ছাড়লে আপডেট থামে, ১৫ মিনিটে প্রবেশাধিকার শেষ হয়।')),
    if(!CloudService.configured) Panel(child:Text(s.t('Firebase is not connected. No location has been uploaded.','Firebase সংযুক্ত নয়। কোনো অবস্থান আপলোড হয়নি।'))),
    FilledButton(onPressed:!CloudService.configured||s.demo||busy?null:()async{if(!await confirm(context,s.t('Share for 15 minutes?','১৫ মিনিট শেয়ার করবে?'),s.t('Anyone with your invitation can view the shared location. Stop anytime.','আমন্ত্রণ পেলে অন্যজন শেয়ার করা অবস্থান দেখতে পারবে। যেকোনো সময় বন্ধ করো।')))return;if(!mounted)return;setState(()=>busy=true);try{final value=await share.start();if(mounted)setState(()=>invitation=value);}catch(_){if(context.mounted)message(context,s.t('Sharing did not start. Check permissions and Firebase.','শেয়ার শুরু হয়নি। অনুমতি ও Firebase পরীক্ষা করো।'));}finally{if(mounted)setState(()=>busy=false);}},child:Text(s.t('Start sharing','শেয়ার শুরু করো'))),
    if(invitation!=null&&share.active) ...[Panel(child:SelectableText(invitation!)),OutlinedButton.icon(onPressed:()=>Clipboard.setData(ClipboardData(text:invitation!)),icon:const Icon(Icons.copy),label:Text(s.t('Copy private invitation','গোপন আমন্ত্রণ কপি করো'))),TextButton(onPressed:()async{await share.stop();if(mounted)setState(()=>invitation=null);},child:Text(s.t('Stop & revoke','বন্ধ ও বাতিল করো')))],
    if(share.error!=null) Text(s.t('Remote updates/revocation could not be confirmed. The original invitation still expires after 15 minutes.','দূরের আপডেট/বাতিল নিশ্চিত করা যায়নি। মূল আমন্ত্রণ ১৫ মিনিটে শেষ হবে।')),
    const Divider(),Heading(s.t('View a trusted invitation','বিশ্বস্ত আমন্ত্রণ দেখো')),TextField(controller:invite,decoration:InputDecoration(labelText:s.t('Invitation code','আমন্ত্রণ কোড'))),
    OutlinedButton(onPressed:!CloudService.configured||s.demo?null:(){subscription?.cancel();setState(()=>point=null);subscription=FamilyShare.watch(invite.text).listen((v){if(mounted)setState(()=>point=v);},onError:(_){if(context.mounted)message(context,s.t('Invalid, expired or unavailable invitation.','আমন্ত্রণ ভুল, মেয়াদোত্তীর্ণ বা পাওয়া যাচ্ছে না।'));});},child:Text(s.t('View location','অবস্থান দেখো'))),
    if(point!=null) Panel(child:Column(children:[Text('${s.t('Last shared','সর্বশেষ শেয়ার')} ${point!['at']}'),Text('${point!['latitude']}, ${point!['longitude']}'),TextButton(onPressed:()=>launchUrl(Uri.parse('https://maps.google.com/?q=${point!['latitude']},${point!['longitude']}'),mode:LaunchMode.externalApplication),child:Text(s.t('Open map','মানচিত্র খোলো')))]))
  ]));}
}
