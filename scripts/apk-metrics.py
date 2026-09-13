"""Print a bounded APK composition report, never publish an extra artifact."""
from pathlib import Path
import sys, zipfile, json

def measure(path):
    with zipfile.ZipFile(path) as apk:
        libs=[x for x in apk.infolist() if x.filename.startswith('lib/') and x.filename.endswith('.so')]
        return {'apkBytes':Path(path).stat().st_size,'nativePackedBytes':sum(x.compress_size for x in libs),'nativeUnpackedBytes':sum(x.file_size for x in libs),'abis':sorted({x.filename.split('/')[1] for x in libs}),'nativeDeflated':all(x.compress_type==zipfile.ZIP_DEFLATED for x in libs),'zipPayloadBytes':sum(x.file_size for x in apk.infolist())}
if __name__=='__main__':
    optimized=measure(sys.argv[1])
    assert optimized['abis']==['arm64-v8a','armeabi-v7a','x86_64'],optimized['abis']
    assert optimized['nativeDeflated'],'Native APK compression was not applied'
    report={'optimized':optimized}
    if len(sys.argv)>2:
        baseline=measure(sys.argv[2]);report['sameCodeUncompressedBaseline']=baseline
        report['savedBytes']=baseline['apkBytes']-optimized['apkBytes']
        report['downloadReductionPercent']=round(100*report['savedBytes']/baseline['apkBytes'],2)
        assert report['savedBytes']>0,'Optimized APK must be smaller than same-code baseline'
    print('::notice title=APK size measurements::'+json.dumps(report,separators=(',',':')))
    print('Compressed native libraries are extracted once on install. APK download size is not installed disk usage.')
