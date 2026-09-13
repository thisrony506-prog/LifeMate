"""Exercise actual non-debuggable, signed APK installation and fresh replacement and subsequent persistence.
Uses Android's visible UI; no app backdoors, run-as access, or shipping test fixtures.
"""
import os, re, subprocess, time, xml.etree.ElementTree as ET

def adb(*args):
    try:
        return subprocess.check_output(['adb', *args], text=True, stderr=subprocess.STDOUT, timeout=90)
    except subprocess.CalledProcessError as error:
        print('ADB operation failed:', ' '.join(args[:3]), (error.output or '')[-2000:], flush=True)
        raise

class UIUnavailable(RuntimeError):
    pass

def tree():
    # Never accept a stale dump from an earlier screen/installed version.
    adb('shell', 'rm', '-f', '/sdcard/lifemate-window.xml')
    result = adb('shell', 'uiautomator', 'dump', '/sdcard/lifemate-window.xml')
    if 'dumped to' not in result:
        raise UIUnavailable('Accessibility tree is not ready yet')
    try:
        return ET.fromstring(adb('shell', 'cat', '/sdcard/lifemate-window.xml'))
    except ET.ParseError as error:
        raise UIUnavailable('Incomplete accessibility dump') from error

def hide_keyboard_if_shown():
    ime = adb('shell', 'dumpsys', 'input_method')
    if re.search(r'(?:mInputShown|mIsInputViewShown|isInputViewShown)=true', ime):
        adb('shell', 'input', 'keyevent', '4')
    # When a hardware keyboard is active, Back would exit onboarding instead of hiding an IME.

def find(label, scroll=False, tap=False):
    for _ in range(18):
        try:
            root = tree()
        except UIUnavailable:
            time.sleep(1)
            continue
        parents = {child: parent for parent in root.iter() for child in parent}
        for node in root.iter('node'):
            if label not in (node.get('text', '') + ' ' + node.get('content-desc', '')):
                continue
            if not tap:
                return  # A visible greeting is text, not a clickable control.
            while node in parents and node.get('clickable') != 'true' and node.get('class') != 'android.widget.EditText':
                node = parents[node]
            bounds = [int(x) for x in re.findall(r'\d+', node.get('bounds', ''))]
            if len(bounds) != 4 or bounds[2] <= bounds[0] or bounds[3] <= bounds[1]:
                continue
            if tap: adb('shell', 'input', 'tap', str((bounds[0]+bounds[2])//2), str((bounds[1]+bounds[3])//2))
            return
        if scroll:
            width, height = map(int, re.findall(r'(\d+)x(\d+)', adb('shell', 'wm', 'size'))[-1])
            adb('shell', 'input', 'swipe', str(width//2), str(height*3//4), str(width//2), str(height//3), '350')
        time.sleep(1)
    try:
        labels = [n.get('text','') or n.get('content-desc','') for n in tree().iter('node')]
        print('Visible emulator test labels:', ' | '.join(x for x in labels if x)[:2000], flush=True)
    except Exception:
        pass
    print(adb('logcat', '-d', '-t', '80', '-s', 'AndroidRuntime'), flush=True)
    raise AssertionError('Release UI did not show: ' + label)

def start():
    adb('shell', 'am', 'start', '-W', '-n', 'com.lifemate/.MainActivity')

def labels():
    return ' '.join(n.get('text','') + ' ' + n.get('content-desc','') for n in tree().iter('node'))

def focus_name_field():
    for _ in range(18):
        try:
            for node in tree().iter('node'):
                if node.get('package') == 'com.lifemate' and node.get('class') == 'android.widget.EditText':
                    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds','')))
                    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
                    return
        except UIUnavailable: pass
        time.sleep(1)
    raise AssertionError('Editable name field not found')

def onboard_new(name):
    find('Continue', tap=True)
    focus_name_field()
    adb('shell', 'input', 'text', name)
    find(name)
    hide_keyboard_if_shown()
    find('Organize my day', scroll=True, tap=True)
    find('Money')
    find(name)

def main():
    previous = os.path.join(os.environ['RUNNER_TEMP'], 'previous.apk')
    current = f"release-download/LifeMate-{os.environ['LIFEMATE_VERSION_CODE']}.apk"
    assert 'Success' in adb('install', previous)
    adb('shell', 'pm', 'grant', 'com.lifemate', 'android.permission.POST_NOTIFICATIONS')
    start()
    legacy = None
    for _ in range(25):
        try:
            text = labels()
            if 'Full name' in text:
                legacy = True
                break
            if 'Continue' in text and 'Life Mate' in text:
                legacy = False
                break
        except UIUnavailable:
            pass
        time.sleep(1)
    assert legacy is not None, 'Previous signed APK onboarding was not recognized'
    if legacy:
        find('Full name', scroll=True, tap=True)
        adb('shell', 'input', 'text', 'OldResetProof')
        hide_keyboard_if_shown()
        find('Make yourself at home', scroll=True, tap=True)
        find('OldResetProof')
        adb('shell', 'am', 'force-stop', 'com.lifemate')
        start()
        find('OldResetProof')
        assert 'Make yourself at home' not in labels(), 'Old profile was not saved before reset'
    else:
        onboard_new('NewDataProof')
    adb('shell', 'am', 'force-stop', 'com.lifemate')
    target = os.path.join(os.environ['RUNNER_TEMP'], 'flutter-retention.apk') if legacy else current
    assert 'Success' in adb('install', '-r', target)
    adb('shell', 'pm', 'grant', 'com.lifemate', 'android.permission.POST_NOTIFICATIONS')
    start()
    if legacy:
        find('Continue')
        assert 'OldResetProof' not in labels(), 'Old profile leaked into the fresh app'
        onboard_new('FreshStartProof')
        expected = 'FreshStartProof'
        print('PASS: requested clean replacement starts new onboarding, not the old profile')
    else:
        expected = 'NewDataProof'
        find(expected)
        print('PASS: a subsequent Personal Life OS update does not reset new data')
    adb('shell', 'am', 'force-stop', 'com.lifemate')
    start()
    find(expected)
    if legacy:
        adb('shell', 'am', 'force-stop', 'com.lifemate')
        assert 'Success' in adb('install', '-r', current)
        start()
        find('Money')
        find(expected)
        print('PASS: subsequent higher-version Flutter install preserves the new profile')
    package = adb('shell', 'dumpsys', 'package', 'com.lifemate')
    assert f"versionCode={os.environ['LIFEMATE_VERSION_CODE']} " in package
    print('PASS: retained-key higher-version install; new profile survives process restart')
    measurements=[]
    for _ in range(3):
        adb('shell','am','force-stop','com.lifemate')
        adb('logcat','-c')
        launch=adb('shell','am','start','-W','-n','com.lifemate/.MainActivity')
        find(expected)
        frame=adb('logcat','-d','-s','LifeMatePerf:I','*:S')
        total=re.search(r'TotalTime:\s*(\d+)',launch)
        first=re.search(r'activity_to_flutter_frame_ms=(\d+)',frame)
        assert total and first, 'Startup measurement was not captured'
        measurements.append({'androidTotalTimeMs':int(total[1]),'activityToFlutterFrameMs':int(first[1])})
    import json
    print('::notice title=Release startup measurements::'+json.dumps({'environment':'Android 15 x86_64 hosted emulator; 3 process-cold launches; new profile', 'samples':measurements}))


if __name__ == '__main__':
    main()
