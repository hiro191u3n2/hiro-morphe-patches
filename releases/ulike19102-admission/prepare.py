#!/usr/bin/env python3
"""Reconstruct the focused .102 candidate from the retained .101 source.
Every source edit has an exact old-text guard. Existing release/feed files are
not edited. The generated unified diff and per-file hashes remain auditable.
"""
from pathlib import Path
import difflib,hashlib,json,shutil
HERE=Path(__file__).resolve().parent
BASE=HERE.parent/'ulike19101-pipeline'
OUT=HERE/'generated'
def sha(raw):return hashlib.sha256(raw).hexdigest()
def main():
 if OUT.exists():raise AssertionError('Fresh candidate source directory required')
 shutil.copytree(BASE/'src',OUT/'src',ignore=shutil.ignore_patterns('__pycache__'))
 shutil.copytree(BASE/'publication',OUT/'publication')
 root=OUT/'src';before={};edits=[]
 def sub(name,old,new,count=1):
  p=root/name;text=p.read_text();actual=text.count(old)
  if actual!=count:raise AssertionError((name,'unexpected source shape',actual,count,old[:100]))
  before.setdefault(name,text);p.write_text(text.replace(old,new));edits.append({'path':name,'occurrences':count})
 sub('GpuStrong1960.java','        if(!GpuQualification1961.background()&&!GpuResident1976.cpuOracle())startupReady=GpuStartup1990.awaitCoreReady(30000L);', '''        if(!GpuResident1976.cpuOracle()) {
            // An owned idle probe may consume completed startup proof only.
            // Never request or wait for another queue worker from this probe.
            if(!GpuQualification1961.background())startupReady=GpuStartup1990.awaitCoreReady(30000L);
            else if(GpuResident1976.deployedProbe19102())startupReady=GpuStartup1990.coreDomainReady19102();
        }''')
 sub('GpuStrong1960.java','    static boolean enableResident1976(StrongNoise1958.Model model,int width,int height) {', '''    /** Full image capacity remains reserved. CPU fallback transfers at most
     * the production caller's 256-row strip; GPU rows use internal copies. */
    static long residentUploadBytes19102(int width,int height) {
        if(width<1||height<1||(long)width*height>Integer.MAX_VALUE)
            throw new IllegalArgumentException("resident dimensions");
        return 4L*width*Math.min(height,256);
    }
    static boolean enableResident1976(StrongNoise1958.Model model,int width,int height) {''')
 sub('GpuStrong1960.java','            GpuNoise1960.Lease1971 lease=stage.session.reserveCapacity1971(new int[]{24},new long[]{4L*width*height},0);', '''            GpuNoise1960.Lease1971 lease=stage.session.reserveCapacity1983(new int[]{24},
                new long[]{4L*width*height},new long[]{residentUploadBytes19102(width,height)},0);''')
 sub('GpuChain1961.java','int[] slots=new int[]{24};long[] capacities=new long[]{4L*n},uploads=new long[]{4L*n};','int[] slots=new int[]{24};long[] capacities=new long[]{4L*n},uploads=new long[]{GpuStrong1960.residentUploadBytes19102(sw,sh)};')
 sub('GpuResident1976.java','    static boolean cpuOracle(){return Boolean.TRUE.equals(CPU.get());}', '''    static boolean cpuOracle(){return Boolean.TRUE.equals(CPU.get());}
    /** Permission is private to the owned whole-output benchmark, never all
     * background work. Its independent CPU oracle remains strictly CPU. */
    static boolean deployedProbe19102(){
        GpuQualification1961.Cancellation owner=CANCELLATION.get();
        return benchmarking()&&!cpuOracle()&&owner!=null&&!owner.cancelled();
    }
    private static String deployedRoutes19102(){
        return "strong="+(GpuStartup1990.coreDomainReady19102()?GpuStrongDefault1990.profile1994():-1)+
            ";model="+(GpuStartup1990.modelDomainReady19102()?GpuModelDefault1990.variant1994():-1);
    }
    private static String routesFromKey19102(String key){
        String prefix="strong-resident1978:3:v2-deployed:";
        if(key==null||!key.startsWith(prefix))throw new IllegalArgumentException("resident route key");
        int end=key.indexOf(':',prefix.length());
        if(end<0)throw new IllegalArgumentException("resident route key boundary");
        return key.substring(prefix.length(),end);
    }''')
 sub('GpuResident1976.java','return "strong-resident1978:3:v1:"+GpuNoise1960.fingerprint()+":"+source.getWidth()+":"+source.getHeight()+":"+','return "strong-resident1978:3:v2-deployed:"+deployedRoutes19102()+":"+GpuNoise1960.fingerprint()+":"+source.getWidth()+":"+source.getHeight()+":"+')
 sub('GpuResident1976.java','        if(proof!=null) {','        if(proof!=null) {\n            if(!routesFromKey19102(base).equals(deployedRoutes19102())){terminal1982=13;return null;}')
 sub('GpuResident1976.java','                        long baseline=Long.MAX_VALUE,candidateWorst=0;', '''                        // Decode the existing captured key rather than add or
                        // reorder fields in the shipped anonymous Probe ABI.
                        final String observedRoutes=routesFromKey19102(base);
                        if(!observedRoutes.equals(deployedRoutes19102())){
                            GpuQualification1961.rejectNonSpeed1988(scheduledKey,"reference_unstable");
                            outcome1984("reference_unstable");return;
                        }
                        long baseline=Long.MAX_VALUE,candidateWorst=0;''')
 sub('GpuResident1976.java','                                    if(cancellation.cancelled())return;', '''                                    if(cancellation.cancelled())return;
                                    if(!observedRoutes.equals(deployedRoutes19102())){
                                        outcome1984("reference_unstable");return;
                                    }''',2)
 sub('GpuResident1976.java','                            if(!baselineKey.equals(GpuChain1961.residentBaseline1978(owned,rotation,width,height,frozenOutput,identity))){outcome1984("reference_unstable");return;}', '''                            if(!observedRoutes.equals(deployedRoutes19102())||
                                    !baselineKey.equals(GpuChain1961.residentBaseline1978(owned,rotation,width,height,frozenOutput,identity))){outcome1984("reference_unstable");return;}''')
 sub('GpuResident1976.java','                        if(!baselineKey.equals(GpuChain1961.residentBaseline1978(owned,rotation,width,height,frozenOutput,identity))) {', '''                        if(!observedRoutes.equals(deployedRoutes19102())||
                                !baselineKey.equals(GpuChain1961.residentBaseline1978(owned,rotation,width,height,frozenOutput,identity))) {''')
 sub('GpuStartup1990.java','    public static boolean domainReady(){return coreReady&&GpuStrongDefault1990.ready();}', '''    public static boolean domainReady(){return coreReady&&GpuStrongDefault1990.ready();}
    /** Read-only readiness. These cannot enqueue startup work or wait on the
     * current idle queue owner. Shared startup resources must be released. */
    static boolean coreDomainReady19102(){
        return sharedReleased1991&&coreReady&&GpuStrongDefault1990.ready();
    }
    static boolean modelDomainReady19102(){
        return sharedReleased1991&&modelReady&&GpuModelDefault1990.ready();
    }''')
 sub('GpuAnalysis1961.java','return !GpuQualification1961.background()&&GpuQualification1961.restore(key)!=null&&','return (!GpuQualification1961.background()||GpuResident1976.deployedProbe19102())&&GpuQualification1961.restore(key)!=null&&')
 sub('GpuAnalysis1961.java','        boolean standard=false;\n        if(!GpuQualification1961.background()&&exact){', '''        boolean standard=false;
        final boolean deployedProbe=GpuResident1976.deployedProbe19102();
        if((!GpuQualification1961.background()||deployedProbe)&&exact){''')
 sub('GpuAnalysis1961.java','            boolean released=GpuStartup1990.awaitModelReady1991(30000L);cancelled();', '''            boolean released=deployedProbe?GpuStartup1990.modelDomainReady19102():
                GpuStartup1990.awaitModelReady1991(30000L);cancelled();''')
 sub('GpuAnalysis1961.java','            if(standard)GpuModelDefault1990.requestOptional1994();','            if(standard&&!deployedProbe)GpuModelDefault1990.requestOptional1994();')
 sub('GpuAnalysis1961.java','        if(GpuQualification1961.background()){reason1988(selected1988,PipelineDetail1988.BACKGROUND);return null;}','        if(GpuQualification1961.background()&&!deployedProbe){reason1988(selected1988,PipelineDetail1988.BACKGROUND);return null;}')
 sub('GpuQualification1961.java','        volatile boolean cancelled,running;boolean failureCounted;','        volatile boolean cancelled,running;boolean failureCounted;int registeredOutcome19102;')
 sub('GpuQualification1961.java','                CURRENT.remove();close(job.probe);', '''                // Register an actual execution failure before clearing its
                // owner. Diagnostic history is never a source of admission.
                registerTerminal19102(job,terminal);
                CURRENT.remove();close(job.probe);''')
 sub('GpuQualification1961.java','    /** SpeedWorkers joins every interrupted child before throwing this exact', '''    private static void registerTerminal19102(Job job,int terminal){
        if(job==null||job.cancelled()||job.registeredOutcome19102==terminal)return;
        String reason=terminal==OUT_EXCEPTION1984?"exception":
            terminal==OUT_MEMORY1984?"memory_budget":terminal==OUT_UNSUPPORTED1984?"unsupported":null;
        if(reason==null)return;
        try{rejectNonSpeed1988(job.key,reason);}
        catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
    }
    /** SpeedWorkers joins every interrupted child before throwing this exact''')
 sub('GpuQualification1961.java','                RECORDS.remove(name);trimFailures();\n                if(exact||outcome==OUT_SPEED1984)', '''                if(job!=null&&job.key.equals(key))job.registeredOutcome19102=outcome;
                RECORDS.remove(name);trimFailures();
                if(exact||outcome==OUT_SPEED1984)''')
 sub('native1960/build_native1960.py'," digest=hashlib.sha256(b'ulike1990-compute-fingerprint-v1\\0'+raw);pins={}", ''' identity_manifest={'schema':'ulike19102-compute-identity-v1',
     'native_dependencies':sorted(manifest['native_dependencies']),
     'numerical_java':sorted(manifest['numerical_java'])}
 canonical=json.dumps(identity_manifest,sort_keys=True,separators=(',',':')).encode('utf-8')
 digest=hashlib.sha256(b'ulike19102-compute-fingerprint-v1\\0'+canonical);pins={}''')
 sub('native1960/build_native1960.py',"report['computeFingerprintSchema']='ulike1990-compute-fingerprint-v1'","report['computeFingerprintSchema']='ulike19102-compute-fingerprint-v1'")
 sub('ProcessingTiming1947.java','public static final String VERSION = "1.9.101";','public static final String VERSION = "1.9.102";')
 sub('Transform19101.java','static final String OLD_DESCRIPTION="v1.9.100（v1.9.99基準）";','static final String OLD_DESCRIPTION="v1.9.101（v1.9.100基準）";')
 old='static final String DESCRIPTION="v1.9.101（v1.9.100基準）保存後の検証履歴を上限付きで保存し、プロセスと現在・保存時の状態を区別。復元した記録数を正しく表示。画素処理・画質・解像度・既存の認定条件を維持。この版以降の履歴が対象。実機速度は未測定。";'
 new='static final String DESCRIPTION="v1.9.102（v1.9.101基準）連結用の全画像容量と区間転送予算を分離。独立CPU比較を維持して検証時の標準Strong・モデルGPU選択を撮影経路に合わせ、経路変更時は再認定。診断専用ファイルの変更と数値・採用条件の変更を区別。例外集計と再試行を整合。実機速度は未測定。";'
 sub('Transform19101.java',old,new)
 p=root/'VerifyRuntimeVersion19101.java';text=p.read_text();before.setdefault(p.name,text);p.write_text(text.replace('"1.9.101"','"1.9.102"'))
 review=OUT/'review';review.mkdir()
 diff=''.join(''.join(difflib.unified_diff(old.splitlines(True),(root/name).read_text().splitlines(True),fromfile='a/'+name,tofile='b/'+name)) for name,old in sorted(before.items()))
 (review/'implementation19102.patch').write_text(diff)
 record={'schema':'ulike19102-source-edit-review-v1','baseline_ulike':'1.9.101','candidate_ulike':'1.9.102','files':{name:{'before_sha256':sha(old.encode()),'after_sha256':sha((root/name).read_bytes())} for name,old in sorted(before.items())},'edit_guards':edits,'physical_android_tested':False,'published':False}
 (review/'source-edits.json').write_text(json.dumps(record,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
 for name in ('build.py','test.py'):
  if (HERE/name).is_file():shutil.copyfile(HERE/name,root/('driver19102_'+name))
 if (HERE/'tests').is_dir():shutil.copytree(HERE/'tests',root/'tests19102')
 print(json.dumps({'status':'source_prepared','changed_files':len(before),'generated':str(OUT)},ensure_ascii=False))
if __name__=='__main__':main()
