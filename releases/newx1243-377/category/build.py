#!/usr/bin/env python3
import pathlib,subprocess,zipfile,json,hashlib,argparse,os,shutil
ROOT=pathlib.Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description='Build and audit category retention using local tools and original X DEX inputs.')
parser.add_argument('--tools',type=pathlib.Path,required=True,help='Directory containing ecj.jar and d8.jar')
parser.add_argument('--patcher',type=pathlib.Path,required=True,help='Morphe CLI JAR with Patcher 2.4.0')
parser.add_argument('--dex-dir',type=pathlib.Path,required=True,help='Original APK classes.dex and its multidex siblings')
parser.add_argument('--out',type=pathlib.Path,default=ROOT)
args=parser.parse_args()
TOOLS=args.tools.resolve();PATCHER=args.patcher.resolve();DEX=args.dex_dir.resolve();OUT=args.out.resolve()
for p in [TOOLS/'ecj.jar',TOOLS/'d8.jar',PATCHER]:
 if not p.is_file():parser.error('Required local JAR missing: '+str(p))
if not (DEX/'classes.dex').is_file():parser.error('--dex-dir must contain original classes.dex')
OUT.mkdir(parents=True,exist_ok=True)
for name in ['classes','dex']:
 p=OUT/name
 if p.exists():shutil.rmtree(p)
 p.mkdir()
def run(args):
 p=subprocess.run(list(map(str,args)),text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 print(p.stdout,end='');p.check_returncode();return p.stdout
run(['java','-jar',TOOLS/'ecj.jar','-8','-nowarn','-encoding','UTF-8','-cp',PATCHER,'-d',OUT/'classes',ROOT/'NewXKeepCategoriesPatch.java',ROOT/'CategoryAudit.java'])
log=run(['java','-cp',os.pathsep.join(map(str,[OUT/'classes',PATCHER])),'CategoryAudit',DEX])
(OUT/'audit.txt').write_text(log,encoding='utf-8')
def zipout(path,entries):
 with zipfile.ZipFile(path,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
  for n,b in sorted(entries.items()):
   i=zipfile.ZipInfo(n,(2026,11,17,0,0,0));i.compress_type=zipfile.ZIP_DEFLATED;z.writestr(i,b)
classes={p.relative_to(OUT/'classes').as_posix():p.read_bytes() for p in (OUT/'classes/app').rglob('*.class')}
zipout(OUT/'provider.jar',classes)
run(['java','-cp',TOOLS/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--classpath',PATCHER,'--output',OUT/'dex',OUT/'provider.jar'])
manifest='Manifest-Version: 1.0\r\nName: Hiro NewX Category Retention\r\nVersion: 1.0.0\r\nDescription: Native NewX saved category retention for 12.43.0.\r\nTimestamp: 2026-11-17T00:00:00\r\nPatcher-Version: 2.4.0\r\nSource: remote\r\nAuthor: Hiro\r\nLicense: GPLv3\r\n\r\n'
classes['classes.dex']=(OUT/'dex/classes.dex').read_bytes()
classes['META-INF/MANIFEST.MF']=manifest.encode()
zipout(OUT/'component.mpp',classes)
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
qa={'version':'1.0.0','supported':'12.43.0-prod.01','base_sha256':'fe6e5214cb0220998e2106c5cd565a15fd1f7828fa8a4da13f99106502fd49b9','file_sha256':sha(OUT/'component.mpp'),'host_audit':log,'runtime_hooks':False,'changed_methods':2,'device_tested':False,'scope':'Preserves X native snoozed-topic storage. Piko NewX topic selector uses its own persistent settings; positive-only native mode still uses original behavior.'}
qa['toolchain_sha256']={'ecj.jar':sha(TOOLS/'ecj.jar'),'d8.jar':sha(TOOLS/'d8.jar'),'morphe.jar':sha(PATCHER)}
qa['input_dex_sha256']={p.name:sha(p) for p in sorted(DEX.glob('*.dex'))}
qa['source_sha256']={name:sha(ROOT/name) for name in ['NewXKeepCategoriesPatch.java','CategoryAudit.java','build.py']}
(OUT/'qa.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('component.mpp sha256='+qa['file_sha256'])
