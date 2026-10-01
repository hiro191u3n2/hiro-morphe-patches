#!/usr/bin/env python3
"""Compile the real Morphe resource helper and verify actual package bytes without loading ORT."""
import argparse, hashlib, json, os, re, struct, subprocess, tempfile
from pathlib import Path

def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()

def elf64(path):
    data=path.read_bytes()
    if data[:6]!=b'\x7fELF\x02\x01' or struct.unpack_from('<H',data,18)[0]!=183:
        raise ValueError('Not little-endian arm64 ELF64')
    phoff=struct.unpack_from('<Q',data,32)[0]
    phsize,phnum=struct.unpack_from('<HH',data,54)
    if phsize!=56:raise ValueError('Unexpected program header size')
    headers=[struct.unpack_from('<IIQQQQQQ',data,phoff+i*phsize) for i in range(phnum)]
    loads=[h for h in headers if h[0]==1]
    if not loads or any(h[7]<16384 or h[2]%16384!=h[3]%16384 for h in loads):
        raise ValueError('ELF not 16KiB page aligned')
    return {'machine':'AARCH64','pt_load_count':len(loads),'pt_load_alignment':[h[7] for h in loads]}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('jdk-bin','morphe-jar','payload','report'):p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();root=Path(__file__).resolve().parent
    source=root/'src/app/hiro/ulike/patches/IntegrationPayload169.java'
    test=root/'test/app/hiro/ulike/patches/IntegrationPayload169Test.java'
    with tempfile.TemporaryDirectory(prefix='ulike-package-verify-') as temp:
        subprocess.run([str(a.jdk_bin/'javac'),'--release','8','-cp',str(a.morphe_jar),'-d',temp,str(source),str(test)],check=True)
        run=subprocess.run([str(a.jdk_bin/'java'),'-Xmx128m','-cp',temp+os.pathsep+str(a.morphe_jar),'app.hiro.ulike.patches.IntegrationPayload169Test',str(a.payload)],check=True,text=True,capture_output=True)
    match=re.search(r'PASS IntegrationPayload169 checks=(\d+)',run.stdout)
    if not match:raise ValueError('Missing test success marker')
    entries=[]
    for line in (a.payload/'resourceitems.tsv').read_text().splitlines():
        target,digest,length,resource=line.split('\t');file=a.payload/resource
        if sha(file)!=digest or file.stat().st_size!=int(length):raise ValueError('Payload mismatch')
        item={'target':target,'sha256':digest,'bytes':int(length)}
        if target.endswith('.so'):item['elf']=elf64(file)
        entries.append(item)
    report={'schema':'ulike-runtime-package-qa-v1','status':'host_pass','checks':int(match.group(1)),
            'source_sha256':sha(source),'test_sha256':sha(test),'prepare_sha256':sha(root/'prepare.py'),
            'verify_sha256':sha(Path(__file__)),'morphe_jar_sha256':sha(a.morphe_jar),'resources':entries,
            'android_runtime_executed':False,'real_morphe_resource_context_executed':False,
            'apk_install_tested':False,'telemetry_provider_merged':False,
            'limits':['Resource workspace tests use real bytes and file IO, not an Android device.',
                      'Actual patch application and final APK manifest/native packaging require separate integration validation.',
                      'ELF page alignment alone does not establish APK installation or runtime compatibility.']}
    a.report.write_text(json.dumps(report,indent=2)+'\n');print(run.stdout.strip())

if __name__=='__main__':main()
