"""Execute pinned observer edits and inverse against the actual published runtime DEX."""
from pathlib import Path
import hashlib,json,os,re,subprocess,zipfile
BASE_SHA='cf6621ed1ec55c5b115516a04f897e380c785c56d9772ab48043b32c568ae48b'
def test(root,work,jdk=None,ndk=None):
    root=Path(root).resolve();work=Path(work).resolve()/'hooks1965';work.mkdir(parents=True,exist_ok=True)
    baseline=Path(os.environ['ULIKE1965_BASELINE_MPP']);morphe=Path(os.environ['ULIKE_MORPHE_JAR']);jdk=Path(jdk or os.environ['ULIKE_JAVA_HOME'])
    if hashlib.sha256(baseline.read_bytes()).hexdigest()!=BASE_SHA:raise AssertionError('Actual published .64 baseline required')
    classes=work/'classes';classes.mkdir(exist_ok=True)
    with zipfile.ZipFile(baseline) as z:
        (work/'runtime.dex').write_bytes(z.read('ulike/runtime.dex'))
        for n in z.namelist():
            if n.startswith('app/hiro/ulike/patches/') and n.endswith('.class'):
                p=classes/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(n))
    cp=os.pathsep.join(map(str,(morphe,classes)))
    commands={'compile':[jdk/'bin/javac','-encoding','UTF-8','-cp',cp,'-d',classes,root/'MergePayloads.java',root/'CameraTraceHooks1965.java',root/'tests1965/Hooks1965Test.java'],
        'run':[jdk/'bin/java','-cp',cp,'Hooks1965Test',work/'runtime.dex',work/'observer.dex']}
    for name,cmd in commands.items():
        r=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,timeout=60);(work/(name+'.log')).write_text(r.stdout+r.stderr)
        if r.returncode:raise RuntimeError(name+'\n'+r.stdout[-3000:]+r.stderr[-7000:])
    m=re.search(r'^RESULT (\{[^\n]+\})$',r.stdout,re.M)
    if not m:raise AssertionError('Observer/inverse executed evidence missing')
    result=json.loads(m.group(1));result['baseline_mpp_sha256']=BASE_SHA
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
