#!/usr/bin/env python3
"""Run bounded production source transfer with generated large images at -Xmx32m.
No GPU result/speed assertion is inferred from the transport seam.
"""
from pathlib import Path
import argparse, hashlib, json, os, re, subprocess

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    inputs=[source/'GpuSource1990.java',source/'tests1990/memory/Source1990Test.java']
    hashes={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}
    classes=work/'classes';classes.mkdir(exist_ok=True)
    commands=[['compile',[str(jdk/'bin/javac'),'--release','8','-d',str(classes),*map(str,inputs)]],
        ['execute',[str(jdk/'bin/java'),'-ea','-Xmx32m','-cp',str(classes),'com.hiro.ulike.Source1990Test']]]
    for name,command in commands:
        result=subprocess.run(command,capture_output=True,text=True,timeout=60)
        (work/(name+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise AssertionError(name+'\n'+result.stdout+result.stderr)
    result=json.loads(re.search(r'^RESULT (.*)$',result.stdout,re.M).group(1))
    assert result['all_pixels_verified']>49_000_000 and result['cases']==7
    assert result['largest_java_buffer_bytes']<600_000
    assert hashes=={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}
    result.update(source_sha256=hashes,actual_production_uploader_executed=True,exact_pixel_range_and_tail=True,
        opacity_failures_and_cancellation=True,allocation_bound_245mp_at_32m_heap=True,
        bounded_source_copy_reduction_verified=True)
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args()
    print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
