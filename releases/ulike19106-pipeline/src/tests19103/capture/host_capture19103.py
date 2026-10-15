#!/usr/bin/env python3
"""Run real .103 timing and capture identity regressions, then the new arrival tests."""
from pathlib import Path
import argparse, importlib.util, json, subprocess

def run(source,work,jdk):
    source=Path(source).resolve();work=Path(work).resolve();jdk=Path(jdk).resolve();work.mkdir(parents=True,exist_ok=True)
    # Retain the complete prior boundary suite, adapting release literals only.
    body=(source/'host_timing19101.py').read_text().replace('1.9.101','1.9.103')
    prior=work/'timing19103_runner.py';prior.write_text(body)
    spec=importlib.util.spec_from_file_location('capture19103_retained',prior);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    base=module.test(source,work/'retained',jdk)
    classes=work/'retained/classes'
    subprocess.run([str(jdk/'bin/javac'),'--release','8','-encoding','UTF-8','-cp',str(classes),'-d',str(classes),str(source/'tests19103/capture/CaptureArrival19103Test.java')],check=True)
    result=subprocess.run([str(jdk/'bin/java'),'-ea','-cp',str(classes),'com.hiro.ulike.CaptureArrival19103Test'],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,check=True)
    (work/'CaptureArrival19103Test.log').write_text(result.stdout)
    evidence=json.loads(result.stdout.split('RESULT ',1)[1].strip());evidence['new_assertions']=evidence['assertions'];evidence['retained_assertions']=base['assertions'];evidence['assertions']+=base['assertions'];evidence['physical_android_tested']=False;evidence['device_speedup_verified']=False
    # The hooked routine still filters preview results and supplemental requests before observation.
    burst=(source/'BurstCapture1933.java').read_text().split('public static void observedResult(Object owner,CaptureResult result)',1)[1].split('private static void requestNext',1)[0]
    assert burst.index('CONTROL_CAPTURE_INTENT_STILL_CAPTURE')<burst.index('ShotContext1932.resultObserved19103')
    assert burst.index('for(Ticket t:TICKETS)if(t.request==request)return;')<burst.index('ShotContext1932.resultObserved19103')
    evidence['existing_still_result_filters_retained']=True
    (work/'result.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n');return evidence
def test(source,work,jdk=None,ndk=None):
 return run(source,work,jdk)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk',required=True);a=p.parse_args();print(json.dumps(run(a.source,a.work,a.jdk),ensure_ascii=False))
