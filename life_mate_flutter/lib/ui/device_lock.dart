import 'package:flutter/material.dart';
import 'package:local_auth/local_auth.dart';
import 'common.dart';

/// iOS privacy curtain. Android is guarded by the retained native PIN/update host.
class DeviceLock extends StatefulWidget {
  final Widget child;
  const DeviceLock({super.key,required this.child});
  @override State<DeviceLock> createState()=>_DeviceLockState();
}
class _DeviceLockState extends State<DeviceLock> with WidgetsBindingObserver {
  bool locked=true,authenticating=false;
  @override void initState(){super.initState();WidgetsBinding.instance.addObserver(this);WidgetsBinding.instance.addPostFrameCallback((_)=>unlock());}
  @override void dispose(){WidgetsBinding.instance.removeObserver(this);super.dispose();}
  @override void didChangeAppLifecycleState(AppLifecycleState state){
    if(state!=AppLifecycleState.resumed&&!authenticating&&mounted)setState(()=>locked=true);
  }
  Future<void> unlock()async{
    if(authenticating||!mounted)return;setState(()=>authenticating=true);
    try{final s=LifeScope.of(context);final ok=await LocalAuthentication().authenticate(localizedReason:s.t('Unlock your private Life Mate space','Life Mate-এর ব্যক্তিগত জায়গা খোলো'),options:const AuthenticationOptions(stickyAuth:true));if(mounted)setState(()=>locked=!ok);}catch(_){if(mounted)setState(()=>locked=true);}finally{if(mounted)setState(()=>authenticating=false);}
  }
  @override Widget build(BuildContext context){final s=LifeScope.of(context);if(!locked)return widget.child;return Scaffold(body:SafeArea(child:Center(child:Padding(padding:const EdgeInsets.all(28),child:Column(mainAxisSize:MainAxisSize.min,children:[const Icon(Icons.lock_outline,size:48),const SizedBox(height:20),Text(s.t('Your space, kept safe.','তোমার জায়গা, সুরক্ষিত।'),style:Theme.of(context).textTheme.headlineSmall),const SizedBox(height:20),FilledButton(onPressed:authenticating?null:unlock,child:Text(s.t('Unlock','আনলক'))),TextButton(onPressed:()=>Navigator.maybePop(context),child:Text(s.t('Back','ফিরে যাও')))])))));}
}
