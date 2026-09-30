#!/usr/bin/env python3
"""Reconstruct reviewed v1.5.8 sources from pinned public source releases.
Only whitelisted source entries are read. All final code hashes are checked.
"""
import hashlib, json, shutil, zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parent
OUT=Path('source')

def checked_zip(path, expected):
    assert hashlib.sha256(path.read_bytes()).hexdigest()==expected, path
    return zipfile.ZipFile(path)

def put(name, body):
    p=OUT/name
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_bytes(body if isinstance(body,bytes) else body.encode())

def main():
    OUT.mkdir(exist_ok=True)
    assert not any(OUT.iterdir()), 'Source output must be empty'
    with checked_zip(Path('input/source155.zip'),'6c4a1f84d253cb5badefaeece081a979021702d8ca4b174ad75a495873e9104d') as a, checked_zip(Path('input/source157.zip'),'b8f483d988ff2479397d8e16c52dcbae8220dba43d58a2e4799440992a4b872e') as b:
        for n in ('DumpDex.java','MergePatchDex.java','VerifyApplied.java','tool-SHA256SUMS'):
            put(n,b.read(n))
        for n in ('stubs/com/hiro/ulike/EdgeFaceCorrection.java','test/make_stubs.py'):
            put(n,a.read(n))
        put('test/PriorDetailPixels.java',a.read('java/com/hiro/ulike/DetailPixels.java').decode().replace('DetailPixels','PriorDetailPixels'))
        t=a.read('test/DetailPixelsTest.java').decode().replace('<=3','<=4').replace('2,2,0,2,4,0','2,2,0,2,5,0').replace('all 16 option','all 25 option')
        put('test/DetailPixelsTest.java',t)
        put('test/FullResolutionTest.java',a.read('test/FullResolutionTest.java').decode().replace('new PhotoDetail.Settings(true,2,true,2)','new PhotoDetail.Settings(true,4,true,4)'))
        addition='''  check(settings().texturePriority&&settings().haloSuppression&&settings().shadowPriority,"new protection defaults on");
  check(change(a,"photo_noise_level","4")&&change(a,"photo_sharp_level","4"),"maximum levels accepted");
  check(change(a,"photo_texture_priority",false)&&change(a,"photo_halo_suppression",false)&&change(a,"photo_shadow_priority",false),"three protections independently toggle");
  reset(p);a=new Activity(p);PhotoDetail.install(a);
  check(settings().noiseLevel==4&&settings().sharpLevel==4,"maximum survives restart");
  check(!settings().texturePriority&&!settings().haloSuppression&&!settings().shadowPriority,"three protections survive restart");
  p.fail=true;check(!change(a,"photo_texture_priority",true)&&!settings().texturePriority,"protection failed write rejected");p.fail=false;
  check(change(a,"photo_halo_suppression",true),"later protection write succeeds");reset(p);a=new Activity(p);PhotoDetail.install(a);
  check(!settings().texturePriority&&settings().haloSuppression&&!settings().shadowPriority,"complete protection snapshot rollback");
  check(change(a,"photo_noise_enabled",true),"enable noise for flags forwarding");
  Bitmap small=picture(31,19);
  for(int flags=0;flags<8;flags++){
   check(change(a,"photo_texture_priority",(flags&1)!=0),"texture forwarded");
   check(change(a,"photo_halo_suppression",(flags&2)!=0),"halos forwarded");
   check(change(a,"photo_shadow_priority",(flags&4)!=0),"shadows forwarded");
   Bitmap out=PhotoDetail.applyDetail(small,small);DetailPixels.Work wk=new DetailPixels.Work(31*19);
   System.arraycopy(small.pixels,0,wk.source,0,wk.source.length);
   DetailPixels.filter(wk,31,19,0,19,4,4,(flags&1)!=0,(flags&2)!=0,(flags&4)!=0);
   check(Arrays.equals(wk.output,out.pixels),"all booleans reach pixel processing");
  }
'''
        t=a.read('test/PhotoDetailTest.java').decode()
        anchor='  p.memory.put("photo_noise_enabled","bad");'
        assert t.count(anchor)==1
        put('test/PhotoDetailTest.java',t.replace(anchor,addition+anchor))
        pub=b.read('publish.py').decode()
        for x,y in [("VERSION = '1.5.7'","VERSION = '1.5.8'"),("BUNDLE = '1.0.91'","BUNDLE = '1.0.92'"),("PREVIOUS = '1.0.90'","PREVIOUS = '1.0.91'"),('望遠ワンタップ要求保持・ボタン上の白い倍率文字','ノイズ低減・くっきり補正強化'),('optical lens routing and explicit device test limits','enhanced denoise/sharp options and explicit device test limits')]:
            assert pub.count(x)==1,x
            pub=pub.replace(x,y)
        put('publish.py',pub)
    for n in ('MergeRuntime.java','UpdatePatchStrings.java','package_mpp.py','build.sh'):
        put(n,(ROOT/n).read_bytes())
    for n in ('DetailPixels.java','PhotoDetail.java'):
        put('java/com/hiro/ulike/'+n,(ROOT/n).read_bytes())
    put('test/EnhancedDetailTest.java',(ROOT/'EnhancedDetailTest.java').read_bytes())
    hashes=json.loads((ROOT/'source-hashes.json').read_text())
    for n,h in hashes.items():
        assert hashlib.sha256((OUT/n).read_bytes()).hexdigest()==h, 'Reviewed source mismatch: '+n
    put('source-hashes.json',(ROOT/'source-hashes.json').read_bytes())
    put('prepare_source.py',Path(__file__).read_bytes())
    notes=(ROOT/'RELEASE_NOTES.txt').read_text()
    put('RELEASE_NOTES.txt',notes)
    put('README.md','# ULike v1.5.8 / Hiro Morphe v1.0.92\n\n'+notes+'\n## Rebuild\n\nRun `bash build.sh OLD_ULIKE_MPP OLD_BUNDLE_MPP TOOLS BUILD DIST`. Predecessor MPPs and tools are hash pinned. The source archive includes CI test outputs; the separately uploaded QA JSON contains the final publication result. Original APKs, photos and credentials are not included.\n')
    qa={
      'version':'1.5.8','bundle_version':'1.0.92','previous_version':'1.5.7','previous_bundle_version':'1.0.91',
      'target':{'package':'com.gorgeous.liteinternational','version_name':'5.6.2','version_code':740},
      'artifacts':[
        {'file':'ULike_HQ_Texture_Online_v1.5.8.mpp','bytes':556489,'sha256':'ccb55a99df61d51e8a077e1c15331f72b1b674e44c58213ad6d1dce19dc83be0'},
        {'file':'Hiro_Morphe_Patches_v1.0.92.mpp','bytes':17088493,'sha256':'a3ef16c3277796c2a85a4bcb1bf76673557ca74a8ad332b3d87ff04d07579d28'}],
      'prior_local_verification_record':{'file':'QA_ULike_v1.5.8.json previously delivered in conversation','morphe_versions':['1.13.0','1.16.0'],'patch_and_rebuild_success':True,'applied_contracts_per_apk':598,'pixel_assertions':182673,'enhanced_assertions':724873,'settings_and_failure_assertions':86,'note':'Previous recorded tests, not new handset tests. New CI rebuild must match those MPP bytes exactly.'},
      'limitations':['Galaxy/ART execution, launch, settings UI, real-photo quality and HEIF completion untested.','No handset save-time or memory-pressure benchmark.','Synthetic tests and JVM API stand-ins do not establish actual camera quality.','Both patches are experimental; optional save-only processing; no preview/video changes.'],
      'privacy':{'original_apk_published':False,'user_photos_published':False,'credentials_included':False},
      'publication':{'status':'pending_publication_retry'}
    }
    put('LOCAL_QA.json',json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
    print('PASS: all 18 reviewed code and tool-manifest hashes match local v1.5.8 sources')

if __name__=='__main__':main()
