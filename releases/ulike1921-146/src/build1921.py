#!/usr/bin/env python3
"""Build exact1921 hotfix over approved1920, without publishing or device claims."""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess,zipfile
from host_tests1920 import test as host_test1920
from host_tests1921 import test as host_test1921
ROOT=Path(__file__).resolve().parent
PINS={
 'ULike_HQ_Texture_Online_v1.9.20.mpp':'4a62ca3e345a918da881df33e2316a4bb1cdeb72bda042b7608b1018b094329a',
 'Hiro_Morphe_Patches_v1.0.145.mpp':'4a61b5ef5a11be93d79d23959a89ecbdb1ea3aac4ca970440e54428b55053477',
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
 base=archive(a.input/'ULike_HQ_Texture_Online_v1.9.20.mpp');bundle=archive(a.input/'Hiro_Morphe_Patches_v1.0.145.mpp')
 baseline=a.work/'baseline'
 for name,raw in base.items():p=baseline/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
 (a.work/'bundle145.dex').write_bytes(bundle['classes.dex']);classes=a.work/'classes';classes.mkdir()
 helperclasses=a.work/'helper-classes';helperclasses.mkdir();helperdex=a.work/'helper-dex';helperdex.mkdir()
 run(['javac','--release','8','-encoding','UTF-8','-cp',a.tools/'android.jar','-d',helperclasses,*sorted((ROOT/'stubs').rglob('*.java')),ROOT/'BackRoute1920.java',ROOT/'ExitFlow185.java',ROOT/'ExitBusy1921.java'],a.work/'helper-javac.log')
 selected=sorted(p for p in helperclasses.rglob('*.class') if p.name.startswith(('ExitFlow185','ExitBusy1921')))
 run(['java','-cp',a.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',a.tools/'android.jar','--classpath',helperclasses,'--output',helperdex,*selected],a.work/'d8.log')
 cp=os.pathsep.join(map(str,[a.tools/'morphe.jar',baseline,classes]))
 run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,ROOT/'MergePayloads.java',ROOT/'Transform1921.java',ROOT/'VerifyHelperReferences.java'],a.work/'transform-javac.log')
 run(['javac','-encoding','UTF-8','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-cp',cp,'-d',classes,ROOT/'PatchClass1921.java'],a.work/'metadata-javac.log')
 for n in ('emitted','repeat'):
  run(['java','-cp',cp,'Transform1921',baseline,helperdex/'classes.dex',a.work/'bundle145.dex',a.work/n,a.work/(n+'-audit.tsv')],a.work/(n+'.log'))
 require({p.name:p.read_bytes() for p in (a.work/'emitted').iterdir()}=={p.name:p.read_bytes() for p in (a.work/'repeat').iterdir()},'DEX rebuild differs')
 emitted=a.work/'emitted'
 run(['java','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-cp',cp,'PatchClass1921',baseline/'app/hiro/ulike/patches/UlikeHqMaxPatch.class',emitted/'UlikeHqMaxPatch.class'],a.work/'metadata.log')
 run(['java','-cp',cp,'VerifyHelperReferences',baseline/'ulike/methods.dex',baseline/'ulike/runtime.dex',emitted/'methods.dex',emitted/'runtime.dex'],a.work/'references.log')
 host_test1920(ROOT,a.work);exit_count=host_test1921(ROOT,a.work)
 final=dict(base);final.update({'classes.dex':(emitted/'loader.dex').read_bytes(),'ulike/runtime.dex':(emitted/'runtime.dex').read_bytes(),'app/hiro/ulike/patches/UlikeHqMaxPatch.class':(emitted/'UlikeHqMaxPatch.class').read_bytes()})
 require((emitted/'methods.dex').read_bytes()==base['ulike/methods.dex'],'Stock payload change');require((emitted/'methods.tsv').read_bytes()==base['ulike/methods.tsv'],'Stock contracts changed')
 mf=headers(base['META-INF/MANIFEST.MF']);mf.update(Version='1.9.21',Timestamp='2026-10-06T00:00:00',Description='Fix false busy due to deleted diagnostic field; cancellable exit wait, 15s pre-close limit. Actual save/record work protected; native panel Back preserved. Device untested.');final['META-INF/MANIFEST.MF']=manifest(mf)
 own=lambda n:n.startswith(('ulike/','ulike181/','ulike182/','ulike186/','ulike191/','app/hiro/ulike/patches/'))
 unchanged={n:b for n,b in bundle.items() if not own(n) and n not in ('META-INF/MANIFEST.MF','classes.dex')}
 integrated={**unchanged,**{n:b for n,b in final.items() if own(n)},'classes.dex':(emitted/'bundle-loader.dex').read_bytes()}
 mf=headers(bundle['META-INF/MANIFEST.MF']);mf.update(Version='1.0.146',Timestamp='2026-10-06T00:00:00',Description='ULike1.9.21 exit guard repair and cancellable wait. Other apps unchanged from1.0.145. Device untested.');integrated['META-INF/MANIFEST.MF']=manifest(mf)
 require({n:b for n,b in integrated.items() if not own(n) and n not in ('META-INF/MANIFEST.MF','classes.dex')}==unchanged,'Other app resource change')
 require(set(final)==set(base),'Unexpected archive entry change');changes=sorted(n for n in final if final[n]!=base[n]);require(set(changes)=={'classes.dex','ulike/runtime.dex','META-INF/MANIFEST.MF','app/hiro/ulike/patches/UlikeHqMaxPatch.class'},'Unexpected byte changes')
 outputs={}
 for name,items in [('ULike_HQ_Texture_Online_v1.9.21.mpp',final),('Hiro_Morphe_Patches_v1.0.146.mpp',integrated)]:
  p=a.output/name;write_zip(p,items);repeat=a.work/(name+'.repeat');write_zip(repeat,items);require(p.read_bytes()==repeat.read_bytes(),'MPP rebuild differs');require(archive(p)==items,'ZIP mismatch')
  run(['java','-jar',a.tools/'morphe.jar','list-patches','--patches',p],a.work/(name+'.loader.log'))
  outputs[name]={'sha256':sha(p.read_bytes()),'bytes':p.stat().st_size}
 qa={'schema':'ulike1921-exit-hotfix-1','status':'BUILT_HOST_TESTED_DEVICE_UNVERIFIED_NOT_PUBLISHED','ulike_version':'1.9.21','bundle_version':'1.0.146','base_versions':['1.9.20','1.0.145'],'approved_lineage':'1.8.8','root_cause_confirmed_in_code':True,'root_cause':'BackExit185.captureBusy reflects nonexistent StyleStill4.callbacks, catches NoSuchFieldException as busy; requested exit masks input for up to90s. StyleStill4.strictRequests is configuration, not pending work.','host_tests':{'total':71+exit_count,'back':26,'session':45,'exit':exit_count,'android_device_test':False},'other_app_loader_classes_unchanged':220,'other_app_resources_byte_identical':True,'stock_method_payload_byte_identical':True,'preserved_runtime_classes':217,'preserved_face_feature_and_camera_processing':True,'preserved_native_panel_back':True,'pending_exit_wait_limit_ms':15000,'pending_exit_user_cancellable':True,'active_work_never_force_cleared':True,'postclose_safety_deadline_ms':90000,'rebuild_deterministic':True,'changed_standalone_entries':changes,'artifacts':outputs,'input_sha256':PINS,'published':False,'manager_feed_updated':False,'limitations':['No Android/Galaxy execution or logcat. Host tests use mocked platform and work state.','The prior black-preview cause/device resolution remains unconfirmed; its1920 recovery is unchanged.','Timeout cancels the pending exit, never a running save/record job; a previously requested recording stop is not reversed.']}
 (a.output/'QA_ULike_v1.9.21.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
 for name,source in [('transform1921-audit.tsv',a.work/'emitted-audit.tsv'),('busy-schema.tsv',emitted/'busy-schema.tsv'),('host-tests1920.txt',a.work/'host-tests1920.txt'),('host-tests1921.txt',a.work/'host-tests1921.txt'),('helper-references.txt',a.work/'references.log')]:shutil.copyfile(source,a.output/name)
 print(json.dumps(qa,ensure_ascii=False,indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for x in ('input','tools','work','output'):p.add_argument('--'+x,type=Path,required=True)
 a=p.parse_args()
 for k,v in vars(a).items():setattr(a,k,v.resolve())
 build(a)
