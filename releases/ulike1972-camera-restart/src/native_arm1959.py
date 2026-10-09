#!/usr/bin/env python3
"""Mandatory actual AArch64/QEMU execution of H41/H42/H43 versus frozen .58 C."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import subprocess
from host_nr1955 import _java_tools
ROOT=Path(__file__).resolve().parent

def run(work,input=None,tools=None):
    from host_speed1959 import baseline_pins
    pins=baseline_pins()
    cc=shutil.which('aarch64-linux-gnu-gcc')
    qemu=shutil.which('qemu-aarch64')
    if not cc or not qemu:
        raise AssertionError('Actual AArch64 compiler and qemu-aarch64 are mandatory; install gcc-aarch64-linux-gnu qemu-user')
    _,java=_java_tools(tools)
    include=Path(java).resolve().parent.parent/'include'
    if not (include/'jni.h').is_file():raise AssertionError('Pinned JDK JNI headers required')
    out=Path(work).resolve()/'native-arm1959';out.mkdir(parents=True,exist_ok=True)
    driver=ROOT/'tests1959/native-arm1959.c';oracle=ROOT/'tests1959/native-oracle1959.c'
    binary=out/'native-arm1959'
    sources=[driver,oracle,ROOT/'native1958/smooth_noise1958.c',ROOT/'native1958/neon1959.h',ROOT/'native1958/scratch1959.h',ROOT/'tests1959/published1958-reference/native1958/smooth_noise1958.c']
    source_pins={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}
    flags=['-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-pthread','-static']
    command=[cc,*flags,'-I'+str(include),'-I'+str(include/'linux'),str(driver),str(oracle),'-lm','-o',str(binary)]
    build=subprocess.run(command,text=True,capture_output=True,timeout=180)
    (out/'compile.log').write_text(build.stdout+build.stderr)
    if build.returncode:raise AssertionError('Actual ARM differential compile failed: '+build.stderr[-12000:])
    execution=subprocess.run([qemu,str(binary)],text=True,capture_output=True,timeout=900)
    (out/'execute.log').write_text(execution.stdout+execution.stderr)
    if execution.returncode:raise AssertionError('Actual ARM differential execution failed: '+execution.stdout[-3000:]+execution.stderr[-12000:])
    for p in sources:
        if hashlib.sha256(p.read_bytes()).hexdigest()!=source_pins[str(p.relative_to(ROOT))]:raise AssertionError('Actual ARM compiled source changed during execution: '+str(p))
    report=json.loads(execution.stdout)
    if report.get('status')!='passed' or report.get('assertions',0)<=0 or report.get('actual_neon_four_pixel_blocks',0)<=0:raise AssertionError('Executed actual ARM assertions missing')
    report.update({'baseline_version':'1.9.58','baseline_mpp_sha256':pins['baseline_mpp_sha256'],'compiler':cc,'qemu':qemu,'flags':flags,'binary_sha256':hashlib.sha256(binary.read_bytes()).hexdigest(),'source_pins':source_pins,'scope':'actual AArch64 four-lane guide/25-tap kernel, scalar exact NLM/tails, NR13 Q8, bounded workspace; no physical Android benchmark'})
    (out/'actual-arm1959-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    return report
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--work',type=Path,required=True);parser.add_argument('--tools',type=Path);args=parser.parse_args();print(json.dumps(run(args.work,tools=args.tools)))
