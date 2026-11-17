#!/usr/bin/env python3
"""Build the isolated NewX 12.43 absolute-time component; never include compile stubs."""
import argparse, hashlib, json, pathlib, shutil, subprocess, zipfile
P=pathlib.Path
ROOT=P(__file__).resolve().parent
def run(*args):
    result=subprocess.run([str(x) for x in args],check=True,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    print(result.stdout,end='')
    return result.stdout
def archive(path,rows):
    with zipfile.ZipFile(path,'w',zipfile.ZIP_DEFLATED) as z:
        for name,data in sorted(rows.items()):
            i=zipfile.ZipInfo(name,(2026,11,17,0,0,0));i.compress_type=zipfile.ZIP_DEFLATED;i.external_attr=0o100644<<16
            z.writestr(i,data)
def files(folder,prefix=''):
    return {p.relative_to(folder).as_posix():p.read_bytes() for p in folder.rglob('*.class') if p.relative_to(folder).as_posix().startswith(prefix)}
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--tool-dir',required=True);ap.add_argument('--patcher',required=True);ap.add_argument('--dex-dir',required=True)
    a=ap.parse_args();tools=P(a.tool_dir);patcher=P(a.patcher);b=ROOT/'build';src=ROOT/'src'
    for d in ['runtimeclasses','patchclasses','testclasses','runtime-dex','patch-dex']:
        p=b/d
        if p.exists():shutil.rmtree(p)
        p.mkdir(parents=True)
    cc=['java','-jar',tools/'ecj.jar','-8','-nowarn','-encoding','UTF-8']
    run(*cc,'-d',b/'runtimeclasses',src/'runtime/NewXAbsoluteTime.java',*sorted((src/'stubs').glob('*.java')))
    run(*cc,'-cp',tools/'classpath.jar','-d',b/'patchclasses',src/'patch/NewXAbsoluteTimePatch.java')
    run(*cc,'-cp',b/'runtimeclasses','-d',b/'testclasses',src/'test/TimeTest.java')
    run(*cc,'-cp',str(tools/'classpath.jar')+':'+str(b/'patchclasses'),'-d',b/'testclasses',src/'test/TimeDexTest.java')
    host=run('java','-cp',str(b/'testclasses')+':'+str(b/'runtimeclasses'),'TimeTest')
    dex=run('java','-Xmx3g','-cp',str(b/'testclasses')+':'+str(b/'patchclasses')+':'+str(patcher),'TimeDexTest',a.dex_dir,b/'patched-targets.dex')
    runtime=files(b/'runtimeclasses','app/hiro/newx/runtime/');patch=files(b/'patchclasses')
    archive(b/'runtime.jar',runtime);archive(b/'patch.jar',patch)
    run('java','-cp',tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--classpath',b/'runtimeclasses','--output',b/'runtime-dex',b/'runtime.jar')
    run('java','-cp',tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--classpath',tools/'classpath.jar','--output',b/'patch-dex',b/'patch.jar')
    manifest='Manifest-Version: 1.0\r\nName: NewX Hiro Absolute Time\r\nVersion: 1.0.0\r\nDescription: Post timestamps for X 12.43.0\r\nPatcher-Version: 2.4.0\r\n\r\n'
    rows=dict(patch);rows['META-INF/MANIFEST.MF']=manifest.encode();rows['classes.dex']=(b/'patch-dex/classes.dex').read_bytes();rows['extensions/newx_absolute_time.mpe']=(b/'runtime-dex/classes.dex').read_bytes()
    archive(ROOT/'component.mpp',rows)
    report={'component_sha256':hashlib.sha256((ROOT/'component.mpp').read_bytes()).hexdigest(),'package':'com.twitter.android','version':'12.43.0-prod.01','format':'yyyy/MM/dd HH:mm','source_apk_sha256':'fe6e5214cb0220998e2106c5cd565a15fd1f7828fa8a4da13f99106502fd49b9','host_test':host.strip(),'dex_test':dex.strip(),'stubs_packaged':False,'device_tested':False}
    (ROOT/'QA_time.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(report,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
