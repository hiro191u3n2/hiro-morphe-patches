#!/usr/bin/env python3
"""Executed direct JNI/overlap tests against immutable published .63 C."""
from pathlib import Path
import argparse, hashlib, json, os, re, subprocess
PINS = {
    'frozen1963/native1960/residual_blocks1961.c':'ac64b8f694ff6eecede3c5b92b17361273a7acac8afb6b135c223dc8745b178b',
    'frozen1963/native1955/single_noise1955.c':'925f9baf02b3897fdc6b3e8272b04c1278bee430d902bb5f7496c14cbdae25af',
}
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def run(argv,log):
    result=subprocess.run([str(x) for x in argv],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=180)
    Path(log).write_text(result.stdout)
    if result.returncode:raise RuntimeError('GX34/GX35 test failed: '+str(log)+'\n'+result.stdout[-9000:])
    return result.stdout

def test(root,work,jdk=None,ndk=None):
    root=Path(root).resolve();work=Path(work).resolve()/'residual-transfer1964';work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();tests=root/'tests1964'
    for name,digest in PINS.items():
        if sha(tests/name)!=digest:raise AssertionError('Immutable .63 native oracle changed: '+name)
    flags=['-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off',
        '-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux')]
    candidate=work/'libresidual1964.so';reference=work/'libresidual1963.so'
    run(['cc',*flags,root/'native1960/residual_blocks1961.c','-lm','-o',candidate],work/'candidate-compile.log')
    run(['cc',*flags,tests/'reference_residual1963_wrapper.c','-lm','-o',reference],work/'reference-compile.log')
    classes=work/'classes';classes.mkdir(exist_ok=True)
    sources=[root/'SingleResidual1961.java',root/'ResidualOverlap1962.java',tests/'ResidualTransfer1964Test.java',*sorted((tests/'transfer-fixtures').rglob('*.java'))]
    run([jdk/'bin/javac','-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-d',classes,*sources],work/'java-compile.log')
    output=run([jdk/'bin/java','-Xcheck:jni','-cp',classes,'com.hiro.ulike.ResidualTransfer1964Test',candidate,reference],work/'execute.log')
    match=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
    if not match:raise AssertionError('Executed JNI transfer report absent')
    result=json.loads(match.group(1))
    if result.get('status')!='passed' or result.get('assertions',0)<100000 or result.get('cases',0)<80 or result.get('record_floats',0)<100000:
        raise AssertionError('Incomplete JNI transfer coverage')
    result['reference_source_sha256']=PINS['frozen1963/native1960/residual_blocks1961.c']
    result['reference_helper_sha256']=PINS['frozen1963/native1955/single_noise1955.c']
    result['candidate_native_sha256']=sha(root/'native1960/residual_blocks1961.c')
    result['independent_published1963_native_oracle']=True
    result['java_owned_direct_banks']=2
    result['tail_rows_retained']=2
    result['device_speedup_verified']=False
    (work/'result.json').write_text(json.dumps(result,sort_keys=True,indent=2)+'\n')
    return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--root',default=str(Path(__file__).resolve().parent.parent));p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.root,a.work,a.jdk,a.ndk),indent=2))
