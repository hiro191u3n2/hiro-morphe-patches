"""Execute current Strong routing and preflight state guards.

Transport and suite evidence are explicit controlled test inputs here.
The separate gpu_math corpus runner tests numerical shader equality.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, subprocess


def module(name, path):
    spec=importlib.util.spec_from_file_location(name,path)
    value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value)
    return value


def test(source, work, jdk=None, ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve()
    if source.is_file():source=source.parent
    work.mkdir(parents=True,exist_ok=True)
    fix=source/'tests1990/strongroute'
    java_home=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent).resolve()
    javac,java=java_home/'bin/javac',java_home/'bin/java'
    holders=module('strong1990_android_holders',source/'host_qualification1981.py')
    adapter=module('strong1990_forecast_adapter',source/'host_strong_diagnostics1986.py')
    android=[]
    for name,body in holders.FIXTURES.items():
        if name.startswith('android/'):
            path=work/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body);android.append(path)
    fixture=adapter.forecast_peer1986((source/'tests1982/strong/StrongDiagnosticsFixtures1982.java').read_text())
    fixture=fixture.replace('final class ProcessingTiming1947 {','final class ProcessingTiming1947 {\n    static long captureId1989(long epoch){return epoch;}')
    (work/'StrongDiagnosticsFixtures1982.java').write_text(fixture)
    classes,preflight=work/'classes',work/'preflight-classes'
    classes.mkdir(exist_ok=True);preflight.mkdir(exist_ok=True)
    def run(command,name):
        result=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=90)
        (work/(name+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise RuntimeError(name+'\n'+result.stdout+result.stderr)
        return result.stdout
    run([javac,'--release','8','-encoding','UTF-8','-d',classes,source/'GpuStrong1960.java',source/'GpuQualification1961.java',work/'StrongDiagnosticsFixtures1982.java',source/'tests1982/strong/StrongDiagnostics1982Test.java',fix/'DefaultPeers1990.java',fix/'StrongDefaultRouting1990Test.java',*android],'compile')
    route=json.loads(run([java,'-ea','-XX:ActiveProcessorCount=4','-cp',classes,'com.hiro.ulike.StrongDefaultRouting1990Test'],'run').strip())
    run([javac,'--release','8','-encoding','UTF-8','-d',preflight,source/'GpuStrongDefault1990.java',fix/'PreflightPeers1990.java',fix/'StrongPreflight1990Test.java'],'preflight-compile')
    state=json.loads(run([java,'-ea','-Xmx384m','-cp',preflight,'com.hiro.ulike.StrongPreflight1990Test'],'preflight-run').strip())
    if any(item.get('status')!='passed' or item.get('assertions',0)<=0 for item in (route,state)):raise AssertionError('Executed Strong assertions absent')
    inputs=[source/name for name in ('GpuStrong1960.java','GpuQualification1961.java','GpuStrongDefault1990.java')]+sorted(fix.glob('*.java'))+[Path(__file__)]
    result={'status':'passed','assertions':route['assertions']+state['assertions'],'routing':route,'preflight_state':state,'physical_android_tested':False,'actual_jni':False,'device_speedup_verified':False,'source_sha256':{str(path.relative_to(source)):hashlib.sha256(path.read_bytes()).hexdigest() for path in inputs},'scope':'Current production Strong dispatcher and corpus state machine with controlled transport/startup evidence; does not certify shader quality or device speed.'}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source',type=Path,default=Path(__file__).resolve().parents[2])
    parser.add_argument('--work',type=Path,required=True)
    parser.add_argument('--jdk',type=Path)
    parser.add_argument('--ndk',type=Path)
    args=parser.parse_args()
    print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),indent=2))
