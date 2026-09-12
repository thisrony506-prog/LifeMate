import 'package:flutter/material.dart';
import '../services/cloud.dart';
import 'common.dart';

class CloudSettings extends StatefulWidget {const CloudSettings({super.key});@override State<CloudSettings> createState()=>_CloudSettingsState();}
class _CloudSettingsState extends State<CloudSettings> {
  final email=TextEditingController(),password=TextEditingController(),phrase=TextEditingController();bool create=false,busy=false,connected=false;String? status;
  @override void dispose(){email.dispose();password.clear();password.dispose();phrase.clear();phrase.dispose();super.dispose();}
  @override Widget build(BuildContext context){final s=LifeScope.of(context);return Scaffold(appBar:AppBar(title:Text(s.t('Private cloud sync','ব্যক্তিগত ক্লাউড সিঙ্ক'))),body:PageBody(children:[
    Panel(child:Row(children:[const Icon(Icons.lock_outline),const SizedBox(width:12),Expanded(child:Text(s.t('Optional. Your tasks, journal and money records work offline. Cloud sync encrypts their contents on this device before upload. Photos, health logs, contacts and chat are not automatically uploaded.','ঐচ্ছিক। কাজ, ডায়েরি ও হিসাব অফলাইনে চলে। সিঙ্কের আগে তথ্য এই ডিভাইসে এনক্রিপ্ট হয়। ছবি, স্বাস্থ্যতথ্য, যোগাযোগ ও চ্যাট নিজে থেকে আপলোড হয় না।')))])),
    if(!CloudService.configured) Panel(child:Text(s.t('Firebase has not been connected for this build. No account or network is needed for offline use.','এই বিল্ডে Firebase সংযুক্ত হয়নি। অফলাইনে অ্যাকাউন্ট বা ইন্টারনেট লাগে না।'))),
    Text('${s.pending.length} ${s.t('local changes waiting','স্থানীয় পরিবর্তন অপেক্ষায়')}'),
    TextField(controller:email,keyboardType:TextInputType.emailAddress,decoration:InputDecoration(labelText:s.t('Email','ইমেইল'))),TextField(controller:password,obscureText:true,enableSuggestions:false,autocorrect:false,decoration:InputDecoration(labelText:s.t('Account password','অ্যাকাউন্টের পাসওয়ার্ড'))),
    SwitchListTile(contentPadding:EdgeInsets.zero,value:create,onChanged:busy?null:(v)=>setState(()=>create=v),title:Text(s.t('Create a new account','নতুন অ্যাকাউন্ট তৈরি'))),
    FilledButton(onPressed:busy||s.demo||!CloudService.configured?null:()async{setState(()=>busy=true);try{await CloudService.account(email.text,password.text,create);password.clear();if(mounted)setState(()=>connected=true);}catch(_){if(context.mounted)message(context,s.t('Sign-in failed. Check your details and connection.','প্রবেশ করা যায়নি। তথ্য ও সংযোগ পরীক্ষা করো।'));}finally{if(mounted)setState(()=>busy=false);}},child:Text(s.t('Connect account','অ্যাকাউন্ট সংযুক্ত করো'))),
    TextField(controller:phrase,obscureText:true,enableSuggestions:false,autocorrect:false,decoration:InputDecoration(labelText:s.t('Separate recovery phrase (12+ characters)','আলাদা রিকভারি বাক্য (১২+ অক্ষর)'))),
    Text(s.t('Keep this phrase somewhere safe, separate from your password. It is never sent to Firebase. Losing it means cloud contents cannot be recovered. Use the same phrase on your other device.','বাক্যটি পাসওয়ার্ড থেকে আলাদা নিরাপদে রাখো। এটি Firebase-এ পাঠানো হয় না। হারালে ক্লাউডের তথ্য উদ্ধার করা যাবে না। অন্য ডিভাইসেও একই বাক্য ব্যবহার করো।')),
    FilledButton.icon(onPressed:busy||!connected||s.demo?null:()async{
      if(!await confirm(context,s.t('Sync these records now?','এখন এই তথ্য সিঙ্ক করবে?'),s.t('Encrypted tasks, journal and income/expenses will be merged with your account, including saved deletions. Location and Sathi are separate opt-in features.','এনক্রিপ্ট করা কাজ, ডায়েরি ও আয়-ব্যয় অ্যাকাউন্টে মিলবে, মুছে দেওয়া তথ্যসহ। অবস্থান ও সাথি আলাদা অনুমতিনির্ভর সুবিধা।')))return;
      if(!mounted)return;setState(()=>busy=true);try{await CloudService.sync(s,phrase.text);if(mounted)setState(()=>status=s.t('Sync complete','সিঙ্ক সম্পন্ন'));}catch(_){if(mounted)setState(()=>status=s.t('Sync not complete. Offline edits are retained. Check connection/recovery phrase; another device’s conflicting edits are never silently overwritten.','সিঙ্ক শেষ হয়নি। অফলাইন পরিবর্তন আছে। সংযোগ/রিকভারি বাক্য দেখো; অন্য ডিভাইসের দ্বন্দ্বপূর্ণ পরিবর্তন চুপিসারে মুছে যায় না।'));}finally{if(mounted)setState(()=>busy=false);}
    },icon:const Icon(Icons.sync),label:Text(s.t('Review & sync now','দেখে এখন সিঙ্ক করো'))),if(busy)const LinearProgressIndicator(),if(status!=null)Text(status!)
  ]));}
}
