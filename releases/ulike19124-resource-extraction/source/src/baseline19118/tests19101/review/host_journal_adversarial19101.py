#!/usr/bin/env python3
"""Independent actual-Java journal races and two-JVM persistence regression.
The Android peer implements only file-backed diagnostic preferences; no GPU
transport or proof-admission implementation participates in these tests.
"""
from pathlib import Path
import argparse, hashlib, json, os, re, subprocess

PREFS='''package android.content;
public interface SharedPreferences {
 String getString(String key,String fallback); Editor edit();
 interface Editor {Editor putString(String key,String value);boolean commit();}
}
'''
CONTEXT='''package android.content;
import java.nio.file.*;
public class Context {
 public static final int MODE_PRIVATE=0; public static volatile int IO_COUNT,WRONG_THREAD;
 static void io(){IO_COUNT++;if(!Thread.currentThread().getName().equals("ulike-verification-journal19101"))WRONG_THREAD++;}
 public Context getApplicationContext(){return this;}
 public SharedPreferences getSharedPreferences(String store,int mode){
  if(!store.equals("ulike_verification_journal19101"))throw new AssertionError("non-diagnostic preference store");
  final Path path=Paths.get(System.getProperty("journal.directory"),"journal.txt");
  return new SharedPreferences(){
   public String getString(String key,String fallback){io();if(!key.equals("history_v1"))throw new AssertionError("unexpected journal key");
    try{return Files.exists(path)?new String(Files.readAllBytes(path),"UTF-8"):fallback;}catch(Exception e){throw new RuntimeException(e);}}
   public Editor edit(){return new Editor(){String text;public Editor putString(String key,String value){if(!key.equals("history_v1"))throw new AssertionError("unexpected journal key");text=value;return this;}
    public boolean commit(){io();try{Files.write(path,text.getBytes("UTF-8"));return true;}catch(Exception e){throw new RuntimeException(e);}}};}
  };
 }
}
'''

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);classes=work/'classes';classes.mkdir(exist_ok=True)
    production=source/'VerificationJournal19101.java';harness=source/'tests19101/review/JournalAdversarial19101Test.java'
    pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in (production,harness,Path(__file__).resolve())}
    peers=[]
    for name,text in [('SharedPreferences',PREFS),('Context',CONTEXT)]:
        p=work/'fixtures/android/content'/(name+'.java');p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);peers.append(p)
    version=re.search(r'public static final String VERSION\s*=\s*"([^"]+)"', (source/'ProcessingTiming1947.java').read_text()).group(1)
    timing=work/'fixtures/com/hiro/ulike/ProcessingTiming1947.java';timing.parent.mkdir(parents=True,exist_ok=True)
    timing.write_text('package com.hiro.ulike; final class ProcessingTiming1947 {static final String VERSION="'+version+'";}\n');peers.append(timing)
    def run(name,args):
        result=subprocess.run([str(x) for x in args],capture_output=True,text=True,timeout=40)
        (work/(name+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise AssertionError(name+'\n'+(result.stdout+result.stderr)[-10000:])
        m=re.search(r'^RESULT (.*)$',result.stdout,re.M);return json.loads(m.group(1)) if m else None
    run('compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,production,harness,*peers])
    state=work/'state';state.mkdir(exist_ok=True);(state/'proof-store.bin').write_bytes(b'GPU admission sentinel remains byte-identical\n')
    sentinel=hashlib.sha256((state/'proof-store.bin').read_bytes()).hexdigest();tests={}
    for mode in ('races','seed','restart'):
        tests[mode]=run(mode,[jdk/'bin/java','-ea','-Xmx64m','-Djournal.directory='+str(state),'-cp',classes,'com.hiro.ulike.JournalAdversarial19101Test',mode,state])
    if hashlib.sha256((state/'proof-store.bin').read_bytes()).hexdigest()!=sentinel:raise AssertionError('Proof store changed')
    if pins!={p:hashlib.sha256(Path(p).read_bytes()).hexdigest() for p in pins}:raise AssertionError('Journal source changed during independent test')
    result=dict(status='passed',assertions=sum(x['assertions'] for x in tests.values()),tests=tests,source_sha256=pins,
        separate_jvm_restart_unknown_and_terminal=True,delayed_load_preserves_current_updates=True,
        in_flight_generation_does_not_ack_newer_state=True,failed_commit_recovery_is_honest=True,
        bounded_corruption_checked_history=True,storage_off_caller_and_proof_store_untouched=True,
        vj3_stage_scalar_roundtrip=True,vj1_vj2_zero_extension=True,stage_signature_exact_result_distinct=True,
        unverified_replay_not_attributed_to_gpu=True,flat_evidence_not_image_coordinates=True,stage_schema_ranges_checked=True,
        physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args()
    print(json.dumps(test(a.source,a.work,a.jdk),ensure_ascii=False,indent=2))
