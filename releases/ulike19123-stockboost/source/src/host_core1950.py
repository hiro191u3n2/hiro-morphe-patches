#!/usr/bin/env python3
"""Run production main-NR C/JNI against the isolated unchanged production DEX."""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess
ROOT=Path(__file__).resolve().parent
ORACLE_SHA='064993911b5e9671beceb6caddf52a4659940189e185db1e9aaf9f74d8d7007e'
def run(args,out,timeout=180):
 p=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=timeout);out.write_text(p.stdout+p.stderr)
 if p.returncode:raise RuntimeError(p.stdout[-1000:]+p.stderr[-7000:])
 return p.stdout

def test(root,work,android=None):
 root=Path(root);out=Path(work)/'host-core1950';out.mkdir(parents=True,exist_ok=True)
 oracle=root/'core1950-reference/runtime-core1949.dex'
 if hashlib.sha256(oracle.read_bytes()).hexdigest()!=ORACLE_SHA:raise RuntimeError('Production NR DEX oracle changed')
 javac=Path(os.environ.get('ULIKE_JAVAC') or shutil.which('javac') or '/workspace/scratch/2537a200dd65/jdk21/bin/javac');java=Path(os.environ.get('ULIKE_JAVA') or str(javac.with_name('java')))
 jdk=javac.resolve().parent.parent
 tools=Path(os.environ.get('ULIKE_MORPHE_JAR') or '/workspace/scratch/7a74efb2da39/tooling/morphe.jar')
 lib=out/'libulike_core1950.so';cc=os.environ.get('CC') or shutil.which('cc')
 run([cc,'-std=c11','-O3','-shared','-fPIC','-Wall','-Wextra','-Werror','-fno-fast-math','-ffp-contract=off','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),root/'native1950/core1950.c','-o',lib],out/'native-compile.log')
 copy_test=out/'test-copy1950'
 run([cc,'-std=c11','-O2','-Wall','-Wextra','-Werror','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),root/'native1950/core1950.c',root/'native1950/test_copy1950.c','-o',copy_test],out/'copy-compile.log')
 copy_result=json.loads(run([copy_test],out/'copy-test.log'))
 if copy_result.get('status')!='passed' or not copy_result.get('concurrent_disjoint_rows_preserved'):raise RuntimeError('JNI copied-output race guard failed')
 classes=out/'classes';classes.mkdir(exist_ok=True)
 sources=list((root/'tests/core1950').rglob('*.java'))+[root/'CorePixels1950.java']
 run([javac,'-source','8','-target','8','-Xlint:-options','-cp',tools,'-d',classes,*sources],out/'compile.log')
 result=json.loads(run([java,'-Xcheck:jni','-Djava.library.path='+str(out),'-cp',str(classes)+os.pathsep+str(tools),'com.hiro.ulike.CoreNative1950Test',oracle],out/'test.log',240))
 result['assertions']+=copy_result['assertions']
 result.update(jni_copied_output_race_test=copy_result,oracle_sha256=ORACLE_SHA,oracle_class='Lcom/hiro/ulike/DetailSerial186;',native_source_sha256=hashlib.sha256((root/'native1950/core1950.c').read_bytes()).hexdigest(),jni_array_validation_executed=True,correction_scope='Existing exact NativeChroma186 algorithm retained; focused correction copy/data-flow optimized separately by H4.',fixture_scope='The pinned production DEX main NR kernel and own helpers execute in a host instruction interpreter, compared against production C/JNI. Only workspace allocation is a host fixture. ARM64 NEON/Galaxy execution unverified.')
 if result['status']!='passed' or result['assertions']<1000 or not result['production_dex_oracle_executed'] or not result['runtime_native_selfcheck_passed']:raise RuntimeError('Insufficient main noise verification')
 (out/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args();print(json.dumps(test(ROOT,a.out)))
