"""Compile and execute production PixelCopy observation with Android delivery doubles."""
from pathlib import Path
import json,re,subprocess

def test(root,work,jdk,ndk=None):
    root=Path(root);work=Path(work)/'preview-output1965';classes=work/'classes';classes.mkdir(parents=True,exist_ok=True);jdk=Path(jdk)
    fixtures=root/'renderer1938-host'
    sources=[fixtures/path for path in ('android/view/Surface.java','com/hiro/ulike/ProviderLifecycle1929.java',
             'com/hiro/ulike/ManualLens170.java','com/hiro/ulike/OpticalZoom.java')]
    sources+=list((root/'tests1965/output-fixtures').rglob('*.java'))+[root/'PreviewOutput1965.java',root/'tests1965/PreviewOutput1965Test.java']
    for name,command in [('compile',[jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-d',classes,*sources]),
                         ('run',[jdk/'bin/java','-cp',classes,'com.hiro.ulike.PreviewOutput1965Test'])]:
        result=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=60)
        (work/(name+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise RuntimeError(name+'\n'+result.stdout[-5000:]+result.stderr[-7000:])
    match=re.search(r'^RESULT (\{[^\n]+\})$',result.stdout,re.M)
    if not match:raise AssertionError('Production output observation test report absent')
    report=json.loads(match.group(1));report['public_api_source']='https://developer.android.com/reference/android/view/PixelCopy'
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
