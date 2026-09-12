"""Exercise actual non-debuggable, signed APK installation and encrypted-profile upgrade.
Uses Android's visible UI; no app backdoors, run-as access, or shipping test fixtures.
"""
import os, re, subprocess, time, xml.etree.ElementTree as ET

def adb(*args):
    return subprocess.check_output(['adb', *args], text=True, stderr=subprocess.STDOUT)

def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/lifemate-window.xml')
    return ET.fromstring(adb('shell', 'cat', '/sdcard/lifemate-window.xml'))

def find(label, scroll=False, tap=False):
    for _ in range(18):
        root = tree()
        parents = {child: parent for parent in root.iter() for child in parent}
        for node in root.iter('node'):
            if label not in (node.get('text', '') + ' ' + node.get('content-desc', '')):
                continue
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
    raise AssertionError('Release UI did not show: ' + label)

previous = os.path.join(os.environ['RUNNER_TEMP'], 'previous.apk')
current = f"release-download/LifeMate-{os.environ['LIFEMATE_VERSION_CODE']}.apk"
assert 'Success' in adb('install', previous)
adb('shell', 'am', 'start', '-W', '-n', 'com.lifemate/.MainActivity')
find('Full name', scroll=True, tap=True)
adb('shell', 'input', 'text', 'UpgradeProof')
adb('shell', 'input', 'keyevent', '4')
find('Make yourself at home', scroll=True, tap=True)
find('UpgradeProof')
adb('shell', 'am', 'force-stop', 'com.lifemate')
assert 'Success' in adb('install', '-r', current)  # No uninstall or downgrade override.
adb('shell', 'am', 'start', '-W', '-n', 'com.lifemate/.MainActivity')
find('UpgradeProof')
package = adb('shell', 'dumpsys', 'package', 'com.lifemate')
assert f"versionCode={os.environ['LIFEMATE_VERSION_CODE']} " in package
print('PASS: signed release installs and higher-version update preserves the encrypted profile')
