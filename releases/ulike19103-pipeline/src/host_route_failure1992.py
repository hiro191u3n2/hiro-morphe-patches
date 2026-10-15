#!/usr/bin/env python3
"""Execute typed failure branches against production Strong, tuner and geometry.

Controlled peers supply deterministic pixels and explicit transport failures.
They are not shader-quality or physical-device performance evidence.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, subprocess


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec); spec.loader.exec_module(value)
    return value


def replace(body, old, new):
    assert body.count(old) == 1, 'Fixture anchor changed: ' + old[:100]
    return body.replace(old, new)


def strong_fixture(source):
    adapter = module('failure92_forecast', source/'host_strong_diagnostics1986.py')
    body = adapter.forecast_peer1986((source/'tests1982/strong/StrongDiagnosticsFixtures1982.java').read_text())
    # These .94/.95 calls only publish optional diagnostics. Typed transport failure
    # behavior below still runs unchanged; host_strong_timing1995 exercises the
    # real timing boundary/counters separately.
    body = replace(body, 'final class ProcessingTiming1947 {', '''final class ProcessingTiming1947 {
        static long captureId1989(long epoch){return epoch;}
        static void strongBreakdown1994(Trace trace,long waitNanos,long waits,long submitNanos,long submits,long collectNanos,long collects){}
        static void strongProfile1994(Trace trace,int profile,int variant,int strips){}
        static void strongReadback1995(Trace trace,int strips,long pixels){}
    ''')
    body = replace(body, 'static volatile boolean available=true,supported=true,busy;', 'static volatile boolean available=true,supported=true,busy;static int fault1992,delay1992;')
    body = replace(body, 'int[] source,u;int program;', 'int[] source,u;int program,modelUploads;')
    body = replace(body, 'Batch upload(int slot,int[] values){', 'Batch upload(int slot,int[] values){if(slot==4||slot==5||slot==6)modelUploads++;')
    body = replace(body, 'Batch upload(int slot,float[] values){return this;}', 'Batch upload(int slot,float[] values){if(slot==2)modelUploads++;return this;}')
    body = replace(body, 'boolean closed;final Ticket[] pending=', 'boolean run(Batch b){uploads+=b.modelUploads;return fault1992!=4;}boolean closed;final Ticket[] pending=')
    body = replace(body, 'boolean upload(int slot,int[] values){uploads++;return true;}', 'boolean upload(int slot,int[] values){uploads++;return fault1992!=4;}')
    body = replace(body, 'boolean upload(int slot,float[] values){uploads++;return true;}', 'boolean upload(int slot,float[] values){uploads++;return fault1992!=4;}')
    body = replace(body, 'int[][] execute(Batch batch,int[] slots,int[] counts){return values(batch,counts);}', '''int[][] execute(Batch batch,int[] slots,int[] counts){
            if(delay1992>0)try{Thread.sleep(delay1992);}catch(InterruptedException e){throw new CancellationException();}
            if(fault1992==1)return null;
            int[][] out=values(batch,counts);
            if(fault1992==2)out[0]=null;
            if(fault1992==3)out[0]=new int[counts[0]-1];
            if(fault1992==5)out[0][out[0].length-1]^=1;
            if(fault1992==6&&out.length==3)out[1]=null;
            if(fault1992==7&&out.length==3)out[1][0]^=1;
            if(fault1992==8)return new int[0][];
            return out;
        }''')
    body = replace(body, 'static boolean gpuPrepareOracleSnapshot1961(int[] s,int[] d,int w,int h,int n,boolean shadows,float[] e,int mode){throw new AssertionError("background pixels are not part of this transport test");}', 'static boolean gpuPrepareOracleSnapshot1961(int[] s,int[] d,int w,int h,int n,boolean shadows,float[] e,int mode){for(int i=0;i<s.length;i++)d[i]=pixel(s[i]);return true;}')
    body = replace(body, 'static boolean gpuOracleSnapshot1961(int[] s,int[] d,int w,int rows,int begin,int end,int vb,int ve,int origin,int n,boolean shadows,Model m,int[] policy,int[] confidence){throw new AssertionError("background pixels are not part of this transport test");}', 'static boolean gpuOracleSnapshot1961(int[] s,int[] d,int w,int rows,int begin,int end,int vb,int ve,int origin,int n,boolean shadows,Model m,int[] policy,int[] confidence){for(int i=begin*w;i<end*w;i++)d[i]=pixel(s[i]);if(confidence!=null)Arrays.fill(confidence,37);return true;}')
    return module('failure92_peer19103',source/'host_strong_peers19103.py').transport(body)


def tuning_fixture(source):
    prior = module('failure92_tuning84', source/'host_tuning1984.py')
    body = prior.fixture1984((source/'gpu79-fixtures/TuningFixtures1979.java').read_text())
    body = replace(body, 'final class ProcessingTiming1947 {', 'final class ProcessingTiming1947 {static long captureId1989(long epoch){return epoch;}')
    body = replace(body, 'static int calls;static boolean slowerCpu,slowEarly1984;', 'static long[] comparison1992=new long[]{100,200};static int calls;static boolean slowerCpu,slowEarly1984;')
    body = replace(body, 'return slowerCpu||slowEarly1984&&f==4?new long[]{100,200}:new long[]{200,100};', 'return comparison1992;')
    return body+'\nfinal class GpuStrongDefault1990 {static boolean owns(int[] u){return false;}}\n'


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    if source.is_file(): source=source.parent
    work.mkdir(parents=True, exist_ok=True)
    java_home=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    holders=module('failure92_android',source/'host_qualification1981.py')
    android=[]
    for name,body in holders.FIXTURES.items():
        if name.startswith('android/'):
            p=work/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);android.append(p)
    fix=source/'tests1992/failure'
    cases={}
    def run(command, label):
        result=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=90)
        (work/(label+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise RuntimeError(label+'\n'+result.stdout+result.stderr)
        return result.stdout
    def suite(name, production, fixture, extras=()):
        classes=work/(name+'-classes');classes.mkdir(exist_ok=True)
        peer=work/(name+'Peers1992.java');peer.write_text(fixture)
        run([java_home/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,
             *[source/p for p in production],peer,fix/(name+'Failure1992Test.java'),*extras],name+'-compile')
        report=json.loads(run([java_home/'bin/java','-ea','-XX:ActiveProcessorCount=4','-Xmx512m','-cp',classes,'com.hiro.ulike.'+name+'Failure1992Test'],name+'-run').strip().splitlines()[-1])
        assert report.get('status')=='passed' and report.get('assertions',0)>0
        cases[name]=report
    default_peers=work/'DefaultPeers1990.java'
    default_peers.write_text(module('failure92_startup19103',source/'host_strong_peers19103.py').startup((source/'tests1990/strongroute/DefaultPeers1990.java').read_text()))
    suite('Strong',['GpuStrong1960.java','GpuQualification1961.java'],strong_fixture(source),[*android,default_peers])
    suite('Tuning',['GpuStrongTuning1975.java','GpuQualification1961.java','GpuSnapshotBudget1981.java'],tuning_fixture(source),android)
    geometry_android=sorted((fix/'android').rglob('*.java'))
    suite('Geometry',['GpuGeometry1960.java'],(fix/'GeometryPeers1992.java').read_text(),geometry_android)
    tracked=[source/name for name in ('GpuStrong1960.java','GpuStrongTuning1975.java','GpuGeometry1960.java','GpuQualification1961.java')]+sorted(fix.rglob('*.java'))+[Path(__file__),source/'host_strong_peers19103.py']
    result=dict(status='passed',assertions=sum(v['assertions'] for v in cases.values()),cases=cases,
                physical_android_tested=False,actual_jni=False,device_speedup_verified=False,
                scope='Production failure-classification branches with controlled transport, reference pixels and comparison samples; no shader or device-speed claim.',
                source_sha256={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked})
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',type=Path,default=Path(__file__).resolve().parent)
    p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path)
    a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),ensure_ascii=False,indent=2))
