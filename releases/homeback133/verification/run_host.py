#!/usr/bin/env python3
"""Re-run the recovered host suites; records current execution only, not device QA."""
from pathlib import Path
import argparse, hashlib, json, os, re, subprocess

ROOT = Path(__file__).resolve().parents[1]
EXPECTED_BUNDLE = '4f190bc54c91effbfa778b62bf913e2b6509f82c8e8b4f1b99b5a59ccae3a466'
ANDROID_SHA = '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b'
REQUIRED = {
    'Back116Regression','Back117InstagramTest','Back117TikTokTest','Back118InstagramGuardTest',
    'Back118InstagramTest','Back119InstagramTest','Back119TwitterTest','Back120InstagramTest',
    'Back120MainDispatchTest','Back121InstagramActivityLayersTest','Back121InstagramCurrentTest',
    'Back121InstagramDispatchTest','Back121InstagramScopeTest','Back122InstagramDispatcherTest',
    'Back123InstagramTabBackTest','Back123NativeReelsTest','Back124TwitterTest','Back125TwitterKeyTest',
    'Back125TwitterSystemTest','Back125TwitterTerminalTest','HomeBackHostTest',
    'Back126InstagramProfileTest','Back127Instagram452Test','Back128InstagramProfileTest',
    'Back4743TikTokTest','Back4743NativeBackTest','Back4753RejectedTest',
    'Back130InstagramProfileLinkTest','Back131InstagramProfileIdentityTest','Back131InstagramProfileRouteTest',
    'Back133InstagramDispatcherProfileTest',
}

def digest(p): return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def require(ok, message):
    if not ok: raise RuntimeError(message)
def sources():
    return {p.relative_to(ROOT).as_posix():digest(p) for folder in ('runtime','test-runtime','test-stubs','test-stubs4743','test-stubs4753')
            for p in sorted((ROOT/'src'/folder).rglob('*.java'))}

def main():
    ap=argparse.ArgumentParser(description=__doc__)
    for name in ('jdk','tools','work','bundle','report'): ap.add_argument('--'+name,type=Path,required=True)
    ap.add_argument('--baseline-runtime',type=Path)
    a=ap.parse_args()
    for name,value in vars(a).items():
        if isinstance(value,Path): setattr(a,name,value.resolve())
    require(a.bundle.stat().st_size==48001679 and digest(a.bundle)==EXPECTED_BUNDLE,'Retained317 bundle identity differs')
    require(digest(a.tools/'android.jar')==ANDROID_SHA,'Android API35 jar pin differs')
    require(not a.work.exists(),'Use a new host work directory')
    a.work.mkdir(parents=True);before=sources();transcript=[];results={}
    for folder in ('runtime','host','host4743','host4753'): (a.work/folder).mkdir()
    def run(name,command,expected=0):
        c=[str(x) for x in command]
        if c[0] in ('java','javac'): c[0]=str(a.jdk/'bin'/c[0])
        r=subprocess.run(c,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=300)
        (a.work/(name+'.log')).write_text(r.stdout)
        transcript.append('=== '+name+' (exit '+str(r.returncode)+') ===\n'+r.stdout)
        require(r.returncode==expected,name+' failed\n'+r.stdout[-8000:])
        return r.stdout
    java=run('java-version',['java','--version']);javac=run('javac-version',['javac','-version'])
    require('Temurin-21.0.8+9' in java and javac.strip()=='javac 21.0.8','Pinned Temurin21 toolchain required')
    src=ROOT/'src';runtime=sorted((src/'runtime').glob('*.java'))
    run('runtime-javac',['javac','--release','8','-encoding','UTF-8','-cp',a.tools/'android.jar','-d',a.work/'runtime',*runtime])
    modern={p.stem for p in (src/'test-runtime').glob('Back*.java')
            if p.stem.startswith(('Back4743','Back4753')) and 'public static void main' in p.read_text()}
    classic=[p for p in sorted((src/'test-runtime').glob('*.java')) if p.stem not in modern]
    cp=os.pathsep.join(map(str,[a.work/'host',a.work/'runtime',a.tools/'android.jar']))
    run('host-javac',['javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',a.work/'host',*sorted((src/'test-stubs').rglob('*.java')),*classic])
    def suite(name,classpath):
        output=run(name,['java','-ea','-cp',classpath,'app.hiro.oneback.runtime.'+name])
        require('PASS' in output or 'SUCCESS:' in output or re.search(r'REGRESSION: [0-9]+ cases, 0 failed',output),'Missing success marker '+name)
        results[name]={'exit_code':0,'summary':output.strip().splitlines()[-1],'log_sha256':digest(a.work/(name+'.log'))}
    for p in classic:
        if 'public static void main' in p.read_text(): suite(p.stem,cp)
    for abi in ('4743','4753'):
        names=sorted(n for n in modern if n.startswith('Back'+abi));folder=a.work/('host'+abi)
        abicp=os.pathsep.join(map(str,[folder,a.work/'host',a.work/'runtime',a.tools/'android.jar']))
        run('host'+abi+'-javac',['javac','--release','8','-encoding','UTF-8','-cp',abicp,'-d',folder,
            *sorted((src/('test-stubs'+abi)).rglob('*.java')),*[src/'test-runtime'/(n+'.java') for n in names]])
        for name in names: suite(name,abicp)
    require(set(results)==REQUIRED,'Host suite inventory differs')
    baseline=None
    if a.baseline_runtime:
        old=a.work/'baseline-runtime';old.mkdir()
        oldsrc=sorted(a.baseline_runtime.glob('*.java'));require(bool(oldsrc),'Baseline runtime sources absent')
        run('baseline-runtime-javac',['javac','--release','8','-encoding','UTF-8','-cp',a.tools/'android.jar','-d',old,*oldsrc])
        oldcp=os.pathsep.join(map(str,[a.work/'host',old,a.tools/'android.jar']))
        name='Back133InstagramDispatcherProfileTest'
        output=run('baseline-dispatcher-regression',['java','-ea','-cp',oldcp,'app.hiro.oneback.runtime.'+name],expected=1)
        failures=re.findall(r'^FAIL (.+):',output,re.M)
        require(len(failures)>0 and 'Back133InstagramDispatcherProfileTest failed='+str(len(failures)) in output,'Expected baseline regression not reproduced')
        baseline={'exit_code':1,'failed_cases':failures,'failed_count':len(failures),
                  'sources':{p.name:digest(p) for p in oldsrc},'log_sha256':digest(a.work/'baseline-dispatcher-regression.log')}
    require(sources()==before,'Sources changed during host execution')
    a.report.parent.mkdir(parents=True,exist_ok=True)
    log=a.report.with_suffix('.log');log.write_text('\n'.join(transcript))
    report={'schema':'homeback133-recovered-host-v1','result':'PASS','physical_android_tested':False,
            'retained_bundle':{'bytes':48001679,'sha256':EXPECTED_BUNDLE},'android_api_sha256':ANDROID_SHA,
            'java':java.strip(),'javac':javac.strip(),'source_sha256':before,'suite_count':len(results),
            'suites':results,'baseline_negative_control':baseline,'transcript_sha256':digest(log),
            'scope':'Fresh recovered source host execution, separately matched to the retained MPP by the source-recovery DEX audit. No recovered or fabricated earlier APK logs.'}
    a.report.write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n')
    print('PASS recovered host verification: '+str(len(results))+' suites; '+results['Back133InstagramDispatcherProfileTest']['summary'])

if __name__=='__main__':main()
