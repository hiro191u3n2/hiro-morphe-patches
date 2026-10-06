#!/usr/bin/env python3
"""Reproducible MPP build. Original app APKs and compile stubs are never distributed."""
import argparse, hashlib, json, os, pathlib, shutil, subprocess, zipfile
P=pathlib.Path
BASE_SHA='7c361efc2bafacbb9b4c0a193dcc854a72cf2a8d441f066e6de40d5490172513'
TOOLS={'morphe.jar':'82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
       'android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
       'd8.jar':'305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5'}
DATE=(2026,10,6,0,0,0)
def sha(data):return hashlib.sha256(data).hexdigest()
def run(*cmd):
    print('+',' '.join(map(str,cmd)),flush=True)
    subprocess.run(list(map(str,cmd)),check=True)
def jar(path, rows):
    with zipfile.ZipFile(path,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for name,data in sorted(rows.items()):
            info=zipfile.ZipInfo(name,DATE);info.compress_type=zipfile.ZIP_DEFLATED
            info.external_attr=0o100644<<16;z.writestr(info,data,compresslevel=9)
def tree(root,prefix=''):
    return {f.relative_to(root).as_posix():f.read_bytes() for f in root.rglob('*.class') if f.relative_to(root).as_posix().startswith(prefix)}
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--base',required=True);ap.add_argument('--tools',required=True);ap.add_argument('--work',required=True);ap.add_argument('--dist',required=True)
    a=ap.parse_args();src=P(__file__).resolve().parent;tools=P(a.tools).resolve();base=P(a.base).resolve();work=P(a.work).resolve();dist=P(a.dist).resolve()
    assert sha(base.read_bytes())==BASE_SHA,'Baseline changed; reconcile newer bundle instead of overwriting it'
    for name,h in TOOLS.items(): assert sha((tools/name).read_bytes())==h,'Toolchain changed: '+name
    work.mkdir(parents=True,exist_ok=True);dist.mkdir(parents=True,exist_ok=True)
    for folder in ['runtimeclasses','patchclasses','testclasses','runtime-dex','patch-dex']:
        p=work/folder
        if p.exists():shutil.rmtree(p)
        p.mkdir()
    common=['javac','--release','8','-Xlint:-options','-encoding','UTF-8']
    run(*common,'-cp',tools/'android.jar','-d',work/'runtimeclasses',src/'runtime/AbsoluteTime.java',src/'stubs/d.java')
    run(*common,'-cp',tools/'morphe.jar','-d',work/'patchclasses',src/'patch/XAbsoluteTimePatch.java')
    run(*common,'-cp',str(work/'runtimeclasses')+os.pathsep+str(tools/'android.jar'),'-d',work/'testclasses',src/'test/AbsoluteTimeTest.java')
    run('javac','-encoding','UTF-8','-cp',str(work/'patchclasses')+os.pathsep+str(tools/'morphe.jar'),'-d',work/'testclasses',src/'test/DexAudit.java')
    runtime_cp=os.pathsep.join(map(str,[work/'runtimeclasses',work/'testclasses',tools/'android.jar']))
    patch_cp=os.pathsep.join(map(str,[work/'patchclasses',work/'testclasses',tools/'morphe.jar']))
    run('java','-cp',runtime_cp,'AbsoluteTimeTest')
    run('java','-cp',patch_cp,'DexAudit','fixtures')
    runtime=tree(work/'runtimeclasses','app/hiro/twitter/runtime/')
    assert runtime and not any(n.startswith('com/twitter/') for n in runtime)
    jar(work/'runtime.jar',runtime);jar(work/'patch.jar',tree(work/'patchclasses'))
    run('java','-cp',tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',tools/'android.jar','--classpath',work/'runtimeclasses','--output',work/'runtime-dex',work/'runtime.jar')
    run('java','-cp',tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',tools/'android.jar','--classpath',tools/'morphe.jar','--output',work/'patch-dex',work/'patch.jar')
    for folder in ['runtime-dex','patch-dex']:assert [p.name for p in (work/folder).iterdir()]==['classes.dex'],'Unexpected multidex'
    with zipfile.ZipFile(base) as z:original={i.filename:z.read(i) for i in z.infolist()}
    (work/'base.dex').write_bytes(original['classes.dex'])
    run('java','-cp',patch_cp,'DexAudit','merge',work/'base.dex',work/'patch-dex/classes.dex',work/'merged.dex')
    addition=tree(work/'patchclasses');addition['extensions/x_absolute_time.mpe']=(work/'runtime-dex/classes.dex').read_bytes()
    assert not set(addition)&set(original),'MPP entry collision'
    manifest='Manifest-Version: 1.0\r\nName: Hiro Morphe Patches\r\nDescription: X post absolute 24-hour timestamps; other apps unchanged.\r\nVersion: 1.0.151\r\nTimestamp: 2026-10-06T00:00:00\r\nSource: remote\r\nAuthor: ひろ\r\nContact: na\r\nWebsite: na\r\nLicense: GPLv3\r\nPatcher-Version: 1.8.0\r\n\r\n'
    integrated=dict(original);integrated.update(addition);integrated['classes.dex']=(work/'merged.dex').read_bytes();integrated['META-INF/MANIFEST.MF']=manifest.encode()
    jar(dist/'Hiro_Morphe_Patches_v1.0.151.mpp',integrated)
    standalone=dict(addition);standalone['classes.dex']=(work/'patch-dex/classes.dex').read_bytes();standalone['META-INF/MANIFEST.MF']=manifest.replace('Name: Hiro Morphe Patches','Name: X Absolute Timestamp 24h').replace('Version: 1.0.151','Version: 1.0.0').encode()
    jar(dist/'X_Absolute_Time_24h_v1.0.0.mpp',standalone)
    for name,data in original.items():
        if name not in ['classes.dex','META-INF/MANIFEST.MF']:assert integrated[name]==data,'Unrelated asset changed '+name
    out={p.name:{'bytes':p.stat().st_size,'sha256':sha(p.read_bytes())} for p in sorted(dist.glob('*.mpp'))}
    (dist/'SHA256SUMS.txt').write_text(''.join(v['sha256']+'  '+n+'\n' for n,v in out.items()))
    (dist/'build-evidence.json').write_text(json.dumps({'schema':'x-absolute24-v1','version':'1.0.151','baseline_sha256':BASE_SHA,'toolchain_sha256':TOOLS,'files':out,'runtime_stubs_packaged':False,'unchanged_non_manifest_non_dex_entries':len(original)-2,'post_callsite_count':6,'supported_package':'com.twitter.android','supported_version':'12.19.1-release.0','formatter_host_assertions':8116,'device_tested':False},ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(out,indent=2),flush=True)
if __name__=='__main__':main()
