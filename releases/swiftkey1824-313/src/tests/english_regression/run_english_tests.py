#!/usr/bin/env python3
"""Exercise real MPE DEX dictionary code on the host, with only native boundary stubs.

Usage: python run_english_tests.py --baseline OLD.mpe --fixed NEW.mpe
       --dictionary katakana_en.tsv --tools TOOLS --work WORK --output REPORT.json

TOOLS must contain android.jar. A pinned dex2jar ZIP is bootstrapped into WORK;
--dex2jar-zip supplies an already downloaded ZIP for an offline run. Reports
contain hashes and observations, with no paths or elapsed times.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import urllib.request
import zipfile

DEX2JAR_URL='https://github.com/pxb1988/dex2jar/releases/download/v2.4/dex-tools-v2.4.zip'
DEX2JAR_SHA256='ee7c45eb3c1d2474a6145d8d447e651a736a22d9664b6d3d3be5a5a817dda23a'
CLASSES={
    'hiro/swiftkey/KatakanaEnglishCandidates.class',
    'hiro/swiftkey/KatakanaEnglishCandidates$Index.class',
    'hiro/swiftkey/KatakanaEnglishCandidates$1.class',
}
ROOT=Path(__file__).resolve().parent

def sha(path): return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def run(args,log):
    p=subprocess.run([str(x) for x in args],text=True,capture_output=True)
    log.write_text(p.stdout+p.stderr,encoding='utf-8')
    if p.returncode: raise RuntimeError(p.stdout+p.stderr)
    return p.stdout

def prepare_dex2jar(args):
    archive=args.dex2jar_zip or args.work/'dex-tools-v2.4.zip'
    if not archive.exists():
        archive.parent.mkdir(parents=True,exist_ok=True)
        partial=archive.with_suffix('.download')
        with urllib.request.urlopen(DEX2JAR_URL,timeout=120) as response,partial.open('wb') as output:
            while chunk:=response.read(1024*1024): output.write(chunk)
        if sha(partial)!=DEX2JAR_SHA256: raise RuntimeError('dex2jar bootstrap SHA-256 mismatch')
        partial.replace(archive)
    if sha(archive)!=DEX2JAR_SHA256: raise RuntimeError('dex2jar archive SHA-256 mismatch')
    root=args.work/'dependency';root.mkdir(exist_ok=True)
    with zipfile.ZipFile(archive) as z:
        for member in z.infolist():
            target=(root/member.filename).resolve()
            if not target.is_relative_to(root.resolve()): raise RuntimeError('ZIP path traversal')
        z.extractall(root)
    return os.pathsep.join(str(p) for p in sorted((root/'dex-tools-v2.4/lib').glob('*.jar')))

def convert(mpe,mode,cp,work):
    target=work/mode;target.mkdir(exist_ok=True)
    raw=target/'dex2jar.jar'
    run(['java','-Xmx1024m','-cp',cp,'com.googlecode.dex2jar.tools.Dex2jarCmd',
         '-f',mpe,'-o',raw,'-e',target/'conversion-errors.zip'],target/'dex2jar.log')
    if (target/'conversion-errors.zip').exists(): raise RuntimeError('dex2jar reported conversion failures')
    result=target/'english-runtime.jar'
    with zipfile.ZipFile(raw) as source,zipfile.ZipFile(result,'w') as output:
        for name in sorted(CLASSES):
            data=source.read(name)
            if data[:4]!=b'\xca\xfe\xba\xbe': raise RuntimeError('Invalid converted classfile')
            # dex2jar 2.4 emits methods without StackMapTable. Version 49 asks
            # the JVM to infer verification frames; method bytecode is unchanged.
            # These three dictionary classes use no Java-8-only classfile features.
            output.writestr(name,data[:6]+b'\x00\x31'+data[8:])
    return result

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('baseline','dictionary','tools','work','output'):
        p.add_argument('--'+name,required=True,type=Path)
    p.add_argument('--fixed',type=Path)
    p.add_argument('--dex2jar-zip',type=Path)
    a=p.parse_args()
    a.work.mkdir(parents=True,exist_ok=True)
    android=a.tools/'android.jar'
    if not android.is_file(): raise RuntimeError('TOOLS/android.jar is required')
    cp=prepare_dex2jar(a)
    jars={'baseline':convert(a.baseline,'baseline',cp,a.work)}
    if a.fixed: jars['fixed']=convert(a.fixed,'fixed',cp,a.work)
    compiled=a.work/'classes';compiled.mkdir(exist_ok=True)
    sources=[*sorted((ROOT/'stubs').rglob('*.java')),ROOT/'EnglishDictionaryRegressionTest.java']
    run(['java','com.sun.tools.javac.Main','--release','8','-encoding','UTF-8','-cp',
         os.pathsep.join(map(str,[jars['baseline'],android])),'-d',compiled,*sources],a.work/'javac.log')
    observed={}
    for mode,jar in jars.items():
        stdout=run(['java','-Xmx512m','-cp',os.pathsep.join(map(str,[compiled,jar,android])),
                    'hiro.swiftkey.EnglishDictionaryRegressionTest',a.dictionary,mode],a.work/mode/'test.log')
        observed[mode]=json.loads(stdout)
    report={
      'status':'PASSED',
      'whole_long_mark_examples_verified':bool(a.fixed and observed['fixed']['whole_long_mark_examples_verified']),
      'baseline_negative_examples_verified':observed['baseline']['baseline_negative_examples_verified'],
      'scope':'Actual MPE dictionary DEX converted to JVM; native conversion and candidate ABI are boundary stubs; no Android device test',
      'dependency':{'url':DEX2JAR_URL,'sha256':DEX2JAR_SHA256},
      'baseline_mpe_sha256':sha(a.baseline),
      'dictionary_sha256':sha(a.dictionary),
      'observations':observed,
    }
    if a.fixed: report['fixed_mpe_sha256']=sha(a.fixed)
    a.output.parent.mkdir(parents=True,exist_ok=True)
    a.output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False))

if __name__=='__main__': main()
