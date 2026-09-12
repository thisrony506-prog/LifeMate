"""Exercise actual non-debuggable, signed APK installation and encrypted-profile upgrade.
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

def main():
    previous = os.path.join(os.environ['RUNNER_TEMP'], 'previous.apk')
    current = f"release-download/LifeMate-{os.environ['LIFEMATE_VERSION_CODE']}.apk"
    print('Installing earlier signed APK', flush=True)
    assert 'Success' in adb('install', previous)
    print('PASS: earlier signed APK installed', flush=True)
    adb('shell', 'am', 'start', '-W', '-n', 'com.lifemate/.MainActivity')
    print('Entering encrypted profile through onboarding', flush=True)
    find('Full name', scroll=True, tap=True)
    adb('shell', 'input', 'text', 'UpgradeProof')
    hide_keyboard_if_shown()
    find('Make yourself at home', scroll=True, tap=True)
    find('UpgradeProof')
    print('PASS: profile saved before upgrade', flush=True)
    adb('shell', 'am', 'force-stop', 'com.lifemate')
    print('Installing newer signed APK without uninstalling', flush=True)
    assert 'Success' in adb('install', '-r', current)  # No uninstall or downgrade override.
    print('PASS: Android accepted the signed in-place upgrade', flush=True)
    adb('shell', 'am', 'start', '-W', '-n', 'com.lifemate/.MainActivity')
    find('UpgradeProof')
    package = adb('shell', 'dumpsys', 'package', 'com.lifemate')
    assert f"versionCode={os.environ['LIFEMATE_VERSION_CODE']} " in package
    print('PASS: signed release installs and higher-version update preserves the encrypted profile')

if __name__ == '__main__':
    main()
