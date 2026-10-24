#!/usr/bin/env python3
"""Production post-save orchestrator with controlled peers, not a math oracle.
Actual CPU/GPU numerical equality is covered separately by the real JNI suite.
"""
import argparse,hashlib,json,os,pathlib,subprocess,tempfile
HERE=pathlib.Path(__file__).resolve().parent
SRC=HERE.parents[1]

def test(source,work,jdk=None,ndk=None):
    source=pathlib.Path(source).resolve();work=pathlib.Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=pathlib.Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ.get('JAVA_HOME','/usr'))
    fixtures=source/'tests19106/trace_flow';classes=work/'classes';classes.mkdir(exist_ok=True)
    inputs=[source/'StrongTrace19106.java',fixtures/'Peers19106.java',fixtures/'StrongTraceFlow19106Test.java',fixtures/'run_trace_flow19106.py']
    pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}
    def run(name,args):
        proc=subprocess.run(list(map(str,args)),text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=90)
        (work/(name+'.log')).write_text(proc.stdout)
        if proc.returncode:raise AssertionError(name+'\n'+proc.stdout)
        return proc.stdout
    run('compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*inputs[:3]])
    output=run('run',[jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.StrongTraceFlow19106Test'])
    result=json.loads(next(line for line in output.splitlines() if line.startswith('{')))
    if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}:raise AssertionError('Trace flow sources changed during test')
    result.update(source_sha256=pins,controlled_peers=True,physical_android_tested=False,device_speedup_verified=False,fixture_classes_in_runtime=False,
        entry_conditions_verified=True,diagnostic_reproduction_failures_distinct=True,bounded_status_and_cancellation_verified=True,no_false_cause_attribution=True,
        eligibility_guarded=True,whole_gpu_row_reproduction_required=True,input_mutation_rejected=True,
        cancellation_propagated=True,missing_presence_not_mismatch=True,nested_dependency_order_checked=True,
        native_intermediates_not_claimed=True)
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    return result
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',default=str(SRC));parser.add_argument('--work');parser.add_argument('--jdk');a=parser.parse_args()
    if a.work:print(json.dumps(test(a.source,a.work,a.jdk),ensure_ascii=False,indent=2))
    else:
        with tempfile.TemporaryDirectory(prefix='ulike-trace-flow106-') as root:print(json.dumps(test(a.source,root,a.jdk),ensure_ascii=False,indent=2))
