#!/usr/bin/env python3
"""Build scoped layout repair over approved1921; no device or publication claims."""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess,zipfile
from host_tests1920 import test as host_test1920
from host_tests1921 import test as host_test1921
from host_tests1922 import test as host_test1922
ROOT=Path(__file__).resolve().parent
PINS={
 'ULike_HQ_Texture_Online_v1.9.21.mpp':'201dfb110a64da8962bf3a20ea6f531feb5a8ad39242c2f642a9cc872a3fe25a',
 'Hiro_Morphe_Patches_v1.0.146.mpp':'c248e0e278868187a9f35f1f4de698382735d48047dfe97108626aa40fd7b8d6',
 'morphe.jar':'82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
 'android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
 'd8.jar':'305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5'}
def sha(raw):return hashlib.sha256(raw).hexdigest()
def require(ok,msg):
 if not ok:raise RuntimeError(msg)
def run(cmd,log):
 p=subprocess.run(list(map(str,cmd)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
 Path(log).write_text(p.stdout);print(p.stdout,end='',flush=True);p.check_returncode();return p.stdout

def archive(p):
 with zipfile.ZipFile(p) as z:
  require(z.testzip() is None,'ZIP corruption');require(len(set(z.namelist()))==len(z.namelist()),'Duplicate ZIP entry')
  for n in z.namelist():require(not Path(n).is_absolute() and '..' not in Path(n).parts,'Unsafe archive path')
  return {n:z.read(n) for n in z.namelist() if not n.endswith('/')}
def write_zip(p,items):
 with zipfile.ZipFile(p,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
  for n in sorted(items,key=lambda n:(n!='META-INF/MANIFEST.MF',n)):
   i=zipfile.ZipInfo(n,(2026,10,6,0,0,0));i.external_attr=0o100644<<16;i.compress_type=zipfile.ZIP_DEFLATED;z.writestr(i,items[n],compress_type=zipfile.ZIP_DEFLATED,compresslevel=6)
def headers(raw):
 rows=[]
 for line in raw.replace(b'\r\n',b'\n').split(b'\n'):
  if line.startswith(b' '):rows[-1]+=line[1:]
  elif line:rows.append(line)
 return dict(line.decode().split(': ',1) for line in rows)
def manifest(fields):
 lines=[]
 for key,value in fields.items():
  require(not any(c in value for c in '\r\n\0'),'Invalid manifest')
  raw=(key+': '+value).encode();prefix=b''
  while len(prefix)+len(raw)>72:
   cut=72-len(prefix)
   while raw[cut]&192==128:cut-=1
   lines.append(prefix+raw[:cut]);raw=raw[cut:];prefix=b' '
  lines.append(prefix+raw)
 out=b'\r\n'.join(lines)+b'\r\n\r\n';require(headers(out)==fields,'Manifest roundtrip failed');return out


def build(a):
 require(not a.work.exists(),'Fresh work directory required');a.work.mkdir(parents=True);a.output.mkdir(parents=True,exist_ok=True)
 for name,pin in PINS.items():require(sha(((a.tools if name.endswith('.jar') else a.input)/name).read_bytes())==pin,'Input SHA mismatch '+name)
 require(sha((ROOT/'layout-stock-seed.dex').read_bytes())=='b3a953405ee6f8c7cbff0e5ef1c0cfd0923aea527aa535dfbf23fd538fe08f55','Stock layout seed differs')
 base=archive(a.input/'ULike_HQ_Texture_Online_v1.9.21.mpp');bundle=archive(a.input/'Hiro_Morphe_Patches_v1.0.146.mpp')
 baseline=a.work/'baseline'
 for name,raw in base.items():p=baseline/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
 (a.work/'bundle146.dex').write_bytes(bundle['classes.dex']);classes=a.work/'classes';classes.mkdir()
 helperclasses=a.work/'helper-classes';helperclasses.mkdir();helperdex=a.work/'helper-dex';helperdex.mkdir()
 run(['javac','--release','8','-encoding','UTF-8','-cp',a.tools/'android.jar','-d',helperclasses,*sorted((ROOT/'layout-stubs').rglob('*.java')),ROOT/'PreviewLayout1922.java'],a.work/'helper-javac.log')
 selected=sorted((helperclasses/'com/hiro/ulike').rglob('*.class'))
 run(['java','-cp',a.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',a.tools/'android.jar','--classpath',helperclasses,'--output',helperdex,*selected],a.work/'d8.log')
 cp=os.pathsep.join(map(str,[a.tools/'morphe.jar',baseline,classes]))
 run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,ROOT/'MergePayloads.java',ROOT/'Transform1922.java',ROOT/'VerifyHelperReferences.java'],a.work/'transform-javac.log')
 run(['javac','-encoding','UTF-8','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-cp',cp,'-d',classes,ROOT/'PatchClass1922.java'],a.work/'metadata-javac.log')
 for n in ('emitted','repeat'):
  run(['java','-cp',cp,'Transform1922',baseline,helperdex/'classes.dex',a.work/'bundle146.dex',ROOT/'layout-stock-seed.dex',a.work/n,a.work/(n+'-audit.tsv')],a.work/(n+'.log'))
 require({p.name:p.read_bytes() for p in (a.work/'emitted').iterdir()}=={p.name:p.read_bytes() for p in (a.work/'repeat').iterdir()},'DEX rebuild differs')
 emitted=a.work/'emitted'
 run(['java','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-cp',cp,'PatchClass1922',baseline/'app/hiro/ulike/patches/UlikeHqMaxPatch.class',emitted/'UlikeHqMaxPatch.class'],a.work/'metadata.log')
 run(['java','-cp',cp,'VerifyHelperReferences',baseline/'ulike/methods.dex',baseline/'ulike/runtime.dex',emitted/'methods.dex',emitted/'runtime.dex'],a.work/'references.log')
 # This unchanged inventory is bound to prior verified runtime bytes, which Transform1922 preserves.
 shutil.copyfile(ROOT/'busy-schema.tsv',emitted/'busy-schema.tsv')
 host_test1920(ROOT,a.work);exit_count=host_test1921(ROOT,a.work);layout_count=host_test1922(ROOT,a.work)
 final=dict(base);final.update({'classes.dex':(emitted/'loader.dex').read_bytes(),'ulike/runtime.dex':(emitted/'runtime.dex').read_bytes(),'ulike/methods.dex':(emitted/'methods.dex').read_bytes(),'ulike/methods.tsv':(emitted/'methods.tsv').read_bytes(),'app/hiro/ulike/patches/UlikeHqMaxPatch.class':(emitted/'UlikeHqMaxPatch.class').read_bytes()})
 mf=headers(base['META-INF/MANIFEST.MF']);mf.update(Version='1.9.22',Timestamp='2026-10-06T00:00:00',Description='Repair preview mask animation ownership, stale callbacks, layout bounds and inset invalidation. Keep native ratios, image pipeline and exit behavior. Device untested.');final['META-INF/MANIFEST.MF']=manifest(mf)
 own=lambda n:n.startswith(('ulike/','ulike181/','ulike182/','ulike186/','ulike191/','app/hiro/ulike/patches/'))
 unchanged={n:b for n,b in bundle.items() if not own(n) and n not in ('META-INF/MANIFEST.MF','classes.dex')}
 integrated={**unchanged,**{n:b for n,b in final.items() if own(n)},'classes.dex':(emitted/'bundle-loader.dex').read_bytes()}
 mf=headers(bundle['META-INF/MANIFEST.MF']);mf.update(Version='1.0.147',Timestamp='2026-10-06T00:00:00',Description='ULike1.9.22 preview layout race repair. Other apps unchanged from1.0.146. Device untested.');integrated['META-INF/MANIFEST.MF']=manifest(mf)
 require({n:b for n,b in integrated.items() if not own(n) and n not in ('META-INF/MANIFEST.MF','classes.dex')}==unchanged,'Other app resource change')
 require(set(final)==set(base),'Unexpected archive entries');changes=sorted(n for n in final if final[n]!=base[n]);require(set(changes)=={'classes.dex','ulike/runtime.dex','ulike/methods.dex','ulike/methods.tsv','META-INF/MANIFEST.MF','app/hiro/ulike/patches/UlikeHqMaxPatch.class'},'Unexpected byte changes')
 outputs={}
 for name,items in [('ULike_HQ_Texture_Online_v1.9.22.mpp',final),('Hiro_Morphe_Patches_v1.0.147.mpp',integrated)]:
  p=a.output/name;write_zip(p,items);again=a.work/(name+'.repeat');write_zip(again,items);require(p.read_bytes()==again.read_bytes(),'MPP rebuild differs');require(archive(p)==items,'ZIP mismatch')
  run(['java','-jar',a.tools/'morphe.jar','list-patches','--patches',p],a.work/(name+'.loader.log'))
  outputs[name]={'sha256':sha(p.read_bytes()),'bytes':p.stat().st_size}
 qa={'schema':'ulike1922-layout-hotfix-1','status':'BUILT_HOST_TESTED_DEVICE_UNVERIFIED_NOT_PUBLISHED','ulike_version':'1.9.22','bundle_version':'1.0.147','base_versions':['1.9.21','1.0.146'],'approved_lineage':'1.8.8','code_defects_confirmed':True,'screenshot_root_cause_device_confirmed':False,'code_defects':['Running shade animator handle discarded immediately after start; a new layout cannot cancel its old writes.','Delayed first-frame task reads whichever animator is current at execution instead of capturing the original.','Layout callback uses parent right/bottom as local dimensions.','Top offset setter does not invalidate computed mask targets.'],'host_tests':{'total':71+exit_count+layout_count,'back':26,'session':45,'exit':exit_count,'layout':layout_count,'android_device_test':False},'other_app_loader_classes_unchanged':220,'other_app_resources_byte_identical':True,'existing_runtime_classes_bytecode_unchanged':222,'stock_layout_methods_repaired':9,'preserved_face_feature_and_camera_processing':True,'preserved_native_panel_back':True,'rebuild_deterministic':True,'changed_standalone_entries':changes,'artifacts':outputs,'input_sha256':PINS,'published':False,'manager_feed_updated':False,'limitations':['No Android/Galaxy execution or logcat. Host tests use mocked View/Animator and task scheduling.','Code defects match a plausible intermittent mask mismatch; screenshot-trigger sequence is not device-confirmed.','No hardware image-quality comparison. Image-processing code is unchanged.']}
 (a.output/'QA_ULike_v1.9.22.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
 for name,source in [('transform1922-audit.tsv',a.work/'emitted-audit.tsv'),('host-tests1920.txt',a.work/'host-tests1920.txt'),('host-tests1921.txt',a.work/'host-tests1921.txt'),('host-tests1922.txt',a.work/'host-tests1922.txt'),('helper-references.txt',a.work/'references.log')]:shutil.copyfile(source,a.output/name)
 print(json.dumps(qa,ensure_ascii=False,indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for x in ('input','tools','work','output'):p.add_argument('--'+x,type=Path,required=True)
 a=p.parse_args()
 for k,v in vars(a).items():setattr(a,k,v.resolve())
 build(a)
